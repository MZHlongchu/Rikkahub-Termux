package me.rerere.rikkahub.ui.pages.assistant.detail

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import me.rerere.rikkahub.data.files.FilesManager
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.AssistantRegex
import me.rerere.rikkahub.data.model.Avatar
import me.rerere.rikkahub.utils.ImageUtils
import me.rerere.rikkahub.utils.base64Decode

internal val ImportJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

enum class AssistantImportKind {
    CHARACTER_CARD,
}

data class AssistantImportPayload(
    val kind: AssistantImportKind,
    val sourceName: String,
    val assistant: Assistant,
    val regexes: List<AssistantRegex> = emptyList(),
    val avatarImportSourceUri: String? = null,
)

data class AssistantImportApplication(
    val assistant: Assistant,
)

internal suspend fun parseAssistantImportFromUri(
    context: Context,
    uri: Uri,
    filesManager: FilesManager,
): AssistantImportPayload {
    val displayName = getDisplayName(context, uri)
    val sourceName = displayName?.substringBeforeLast('.')?.ifBlank { "Imported" } ?: "Imported"
    val mime = withContext(Dispatchers.IO) { filesManager.resolveMimeType(uri, displayName) }
    val (jsonString, avatarImportSourceUri) = withContext(Dispatchers.IO) {
        when (mime) {
            "image/png" -> {
                val result = ImageUtils.getTavernCharacterMeta(context, uri)
                result.map { rawCharacterMeta ->
                    val json = decodeImportedCharacterCardJson(rawCharacterMeta)
                    json to uri.toString()
                }.getOrElse { throw it }
            }

            "application/json", "text/plain" -> {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()
                    .use { it?.readText() }
                    ?: error("Failed to read import file")
                json to null
            }

            else -> error("Unsupported file type: $mime")
        }
    }
    return parseAssistantImportFromJson(
        jsonString = jsonString,
        sourceName = sourceName,
        avatarImportSourceUri = avatarImportSourceUri,
    )
}

internal fun decodeImportedCharacterCardJson(rawCharacterMeta: String): String {
    val trimmedMeta = rawCharacterMeta.trim()
    if (trimmedMeta.isEmpty()) error("Empty character data")
    return if (trimmedMeta.startsWith("{") || trimmedMeta.startsWith("[")) {
        trimmedMeta
    } else {
        trimmedMeta.base64Decode()
    }
}

internal fun parseAssistantImportFromJson(
    jsonString: String,
    sourceName: String,
    avatarImportSourceUri: String? = null,
): AssistantImportPayload {
    val json = ImportJson.parseToJsonElement(jsonString).jsonObject
    return when {
        json["spec"] != null -> parseCharacterCardImport(json, sourceName, avatarImportSourceUri)
        else -> error("Unsupported character card format")
    }
}

internal suspend fun AssistantImportPayload.materializeImportedAvatar(
    filesManager: FilesManager,
): AssistantImportPayload {
    val sourceUri = avatarImportSourceUri ?: return this
    val localAvatarUri = withContext(Dispatchers.IO) {
        filesManager.createChatFilesByContents(listOf(Uri.parse(sourceUri))).firstOrNull()?.toString()
    } ?: error("Failed to import avatar")
    return withMaterializedImportedAvatar(localAvatarUri)
}

internal fun AssistantImportPayload.withMaterializedImportedAvatar(
    localAvatarUri: String,
): AssistantImportPayload {
    return copy(
        assistant = assistant.copy(avatar = Avatar.Image(localAvatarUri)),
        avatarImportSourceUri = null,
    )
}

internal fun applyImportedAssistantForCreate(
    payload: AssistantImportPayload,
    includeRegexes: Boolean,
): AssistantImportApplication {
    return applyImportedAssistantForCreate(
        currentAssistant = Assistant(),
        payload = payload,
        includeRegexes = includeRegexes,
    )
}

internal fun applyImportedAssistantForCreate(
    currentAssistant: Assistant,
    payload: AssistantImportPayload,
    includeRegexes: Boolean,
): AssistantImportApplication {
    require(payload.kind == AssistantImportKind.CHARACTER_CARD) {
        "Only character cards can be imported as assistants"
    }
    val assistant = currentAssistant.copy(
        name = payload.assistant.name.ifBlank { currentAssistant.name },
        avatar = (payload.assistant.avatar as? Avatar.Image) ?: currentAssistant.avatar,
        presetMessages = payload.assistant.presetMessages.ifEmpty { currentAssistant.presetMessages },
        stCharacterData = payload.assistant.stCharacterData ?: currentAssistant.stCharacterData,
        regexes = mergeImportedRegexes(
            current = currentAssistant.regexes,
            imported = payload.regexes,
            includeImported = includeRegexes,
        ),
    )
    return AssistantImportApplication(
        assistant = assistant,
    )
}

internal fun applyImportedAssistantToExisting(
    currentAssistant: Assistant,
    payload: AssistantImportPayload,
    includeRegexes: Boolean,
): AssistantImportApplication {
    require(payload.kind == AssistantImportKind.CHARACTER_CARD) {
        "Only character cards can be imported as assistants"
    }
    val nextAssistant = currentAssistant.copy(
        name = payload.assistant.name.ifBlank { currentAssistant.name },
        avatar = (payload.assistant.avatar as? Avatar.Image) ?: currentAssistant.avatar,
        presetMessages = payload.assistant.presetMessages.ifEmpty { currentAssistant.presetMessages },
        stCharacterData = payload.assistant.stCharacterData ?: currentAssistant.stCharacterData,
        regexes = mergeImportedRegexes(
            current = currentAssistant.regexes,
            imported = payload.regexes,
            includeImported = includeRegexes,
        ),
    )
    return AssistantImportApplication(
        assistant = nextAssistant,
    )
}

internal fun mergeImportedRegexes(
    current: List<AssistantRegex>,
    imported: List<AssistantRegex>,
    includeImported: Boolean,
): List<AssistantRegex> {
    if (!includeImported || imported.isEmpty()) return current
    return (current + imported).distinctBy(::regexDedupKey)
}

private fun getDisplayName(context: Context, uri: Uri): String? {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (column >= 0) cursor.getString(column) else null
    }
}
