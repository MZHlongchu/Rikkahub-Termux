package me.rerere.rikkahub.data.export

import android.content.Context
import android.net.Uri
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.utils.JsonInstantPretty

data class SillyTavernCharacterCardExportData(
    val assistant: Assistant,
)

object SillyTavernCharacterCardSerializer : ExportSerializer<SillyTavernCharacterCardExportData> {
    override val type: String = "st_character_card"

    override fun export(data: SillyTavernCharacterCardExportData): ExportData {
        return ExportData(type = type, data = buildCharacterCardJson(data))
    }

    override fun exportToJson(data: SillyTavernCharacterCardExportData, json: Json): String {
        return JsonInstantPretty.encodeToString(JsonObject.serializer(), buildCharacterCardJson(data))
    }

    override fun getExportFileName(data: SillyTavernCharacterCardExportData): String {
        val name = data.assistant.stCharacterData?.name
            ?.takeIf { it.isNotBlank() }
            ?: data.assistant.name.ifBlank { "character-card" }
        return "${sanitizeExportName(name, "character-card")}.json"
    }

    override fun import(context: Context, uri: Uri): Result<SillyTavernCharacterCardExportData> {
        return Result.failure(UnsupportedOperationException("Character card export serializer does not support import"))
    }
}

private fun buildCharacterCardJson(data: SillyTavernCharacterCardExportData): JsonObject {
    val assistant = data.assistant
    val character = assistant.stCharacterData
    val cardName = character?.name?.takeIf { it.isNotBlank() } ?: assistant.name.ifBlank { "Assistant" }
    val systemPrompt = character?.systemPromptOverride
        ?.takeIf { it.isNotBlank() }
        ?: assistant.systemPrompt
    return buildJsonObject {
        put("spec", "chara_card_v2")
        put("spec_version", "2.0")
        putJsonObject("data") {
            put("name", cardName)
            put("description", character?.description.orEmpty())
            put("personality", character?.personality.orEmpty())
            put("scenario", character?.scenario.orEmpty())
            put("first_mes", character?.firstMessage?.ifBlank { null } ?: assistant.firstAssistantPresetMessage())
            put("mes_example", character?.exampleMessagesRaw.orEmpty())
            put("creator_notes", character?.creatorNotes.orEmpty())
            put("system_prompt", systemPrompt)
            put("post_history_instructions", character?.postHistoryInstructions.orEmpty())
            put("alternate_greetings", buildJsonArray {
                character?.alternateGreetings.orEmpty().forEach { add(JsonPrimitive(it)) }
            })
            putJsonArray("tags") {}
            put("creator", "Rikkahub")
            put("character_version", character?.version?.ifBlank { "1.0" } ?: "1.0")
            putJsonObject("extensions") {
                character?.depthPrompt?.let { depthPrompt ->
                    putJsonObject("depth_prompt") {
                        put("prompt", depthPrompt.prompt)
                        put("depth", depthPrompt.depth)
                        put("role", depthPrompt.role.name.lowercase())
                    }
                }
                if (assistant.regexes.isNotEmpty()) {
                    putJsonArray("regex_scripts") {
                        assistant.regexes.forEach { regex ->
                            add(buildRegexScript(regex))
                        }
                    }
                }
            }
        }
    }
}
