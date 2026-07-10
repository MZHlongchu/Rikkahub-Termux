package me.rerere.rikkahub.data.sync

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import me.rerere.rikkahub.data.datastore.Settings

internal object UpstreamSettingsCompatibility {
    fun encode(json: Json, settings: Settings): String {
        return sanitizeEncodedSettings(json, json.encodeToString(settings))
    }

    internal fun sanitizeEncodedSettings(json: Json, encodedSettings: String): String {
        val root = json.parseToJsonElement(encodedSettings).jsonObject
        return json.encodeToString(sanitizeSettings(root))
    }
}

private val SETTINGS_KEYS = setOf(
    "dynamicColor",
    "themeId",
    "customThemes",
    "developerMode",
    "displaySetting",
    "enableWebSearch",
    "favoriteModels",
    "chatModelId",
    "fastModelId",
    "titleModelId",
    "imageGenerationModelId",
    "titlePrompt",
    "translateModeId",
    "translatePrompt",
    "translateThinkingBudget",
    "enableSuggestion",
    "suggestionModelId",
    "suggestionPrompt",
    "ocrModelId",
    "ocrPrompt",
    "compressModelId",
    "compressPrompt",
    "assistantId",
    "providers",
    "assistants",
    "assistantTags",
    "searchServices",
    "searchCommonOptions",
    "searchServiceSelected",
    "mcpServers",
    "webDavConfig",
    "s3Config",
    "ttsProviders",
    "selectedTTSProviderId",
    "asrProviders",
    "selectedASRProviderId",
    "modeInjections",
    "lorebooks",
    "quickMessages",
    "webServerEnabled",
    "webServerPort",
    "webServerJwtEnabled",
    "webServerAccessPassword",
    "webServerLocalhostOnly",
    "backupReminderConfig",
    "launchCount",
    "sponsorAlertDismissedAt",
)

private val DISPLAY_SETTING_KEYS = setOf(
    "userAvatar",
    "userNickname",
    "useAppIconStyleLoadingIndicator",
    "showUserAvatar",
    "showAssistantBubble",
    "bubbleOpacity",
    "showModelIcon",
    "showModelName",
    "showDateTimeInMessage",
    "showTokenUsage",
    "showThinkingContent",
    "autoCloseThinking",
    "showUpdates",
    "showMessageJumper",
    "messageJumperOnLeft",
    "fontSizeRatio",
    "enableMessageGenerationHapticEffect",
    "skipCropImage",
    "enableNotificationOnMessageGeneration",
    "enableLiveUpdateNotification",
    "codeBlockAutoWrap",
    "codeBlockAutoCollapse",
    "showLineNumbers",
    "ttsOnlyReadQuoted",
    "ttsOnlyReadOutsideBrackets",
    "autoPlayTTSAfterGeneration",
    "pasteLongTextAsFile",
    "pasteLongTextThreshold",
    "sendOnEnter",
    "enableAutoScroll",
    "enableLatexRendering",
    "enableBlurEffect",
    "chatFontFamily",
    "chatCustomFontPath",
    "chatCustomFontName",
    "enableVolumeKeyScroll",
    "volumeKeyScrollRatio",
)

private val ASSISTANT_KEYS = setOf(
    "id",
    "chatModelId",
    "name",
    "avatar",
    "useAssistantAvatar",
    "tags",
    "systemPrompt",
    "temperature",
    "topP",
    "contextMessageSize",
    "streamOutput",
    "enableMemory",
    "useGlobalMemory",
    "enableRecentChatsReference",
    "messageTemplate",
    "presetMessages",
    "quickMessageIds",
    "regexes",
    "reasoningLevel",
    "maxTokens",
    "customHeaders",
    "customBodies",
    "mcpServers",
    "localTools",
    "workspaceId",
    "background",
    "backgroundOpacity",
    "useGradientBackground",
    "modeInjectionIds",
    "lorebookIds",
    "enabledSkills",
    "enableTimeReminder",
    "allowConversationSystemPrompt",
    "allowConversationPromptInjection",
)

private val ASSISTANT_REGEX_KEYS = setOf(
    "id",
    "name",
    "enabled",
    "findRegex",
    "replaceString",
    "affectingScope",
    "visualOnly",
)

private val MODEL_KEYS = setOf(
    "modelId",
    "displayName",
    "id",
    "type",
    "customHeaders",
    "customBodies",
    "inputModalities",
    "outputModalities",
    "abilities",
    "tools",
    "providerOverwrite",
)

private val PROVIDER_COMMON_KEYS = setOf("type", "id", "enabled", "name", "models", "balanceOption")
private val CUSTOM_HEADER_KEYS = setOf("name", "value")
private val CUSTOM_BODY_KEYS = setOf("key", "value")
private val BALANCE_OPTION_KEYS = setOf("enabled", "apiPath", "resultPath")
private val CUSTOM_THEME_KEYS = setOf("id", "name", "primaryColorArgb", "secondaryColorArgb", "tertiaryColorArgb")
private val TAG_KEYS = setOf("id", "name")
private val QUICK_MESSAGE_KEYS = setOf("id", "title", "content")
private val SEARCH_COMMON_OPTIONS_KEYS = setOf("resultSize")
private val MODE_INJECTION_KEYS = setOf(
    "type",
    "id",
    "name",
    "enabled",
    "priority",
    "position",
    "content",
    "injectDepth",
    "role",
)
private val MCP_COMMON_OPTIONS_KEYS = setOf("enable", "name", "headers", "tools")
private val MCP_TOOL_KEYS = setOf("enable", "name", "description", "inputSchema", "needsApproval")
private val WEBDAV_CONFIG_KEYS = setOf("url", "username", "password", "path", "items")
private val S3_CONFIG_KEYS = setOf(
    "endpoint", "accessKeyId", "secretAccessKey", "bucket", "region", "pathStyle", "items"
)
private val BACKUP_REMINDER_CONFIG_KEYS = setOf("enabled", "intervalDays", "lastBackupTime")

private val UPSTREAM_LOCAL_TOOL_TYPES = setOf(
    "javascript_engine",
    "time_info",
    "clipboard",
    "tts",
    "ask_user",
    "screen_time",
    "calendar",
)
private val UPSTREAM_MCP_SERVER_TYPES = setOf("sse", "streamable_http")
private val UPSTREAM_BUILT_IN_TOOL_TYPES = setOf("search", "url_context", "image_generation")
private val UPSTREAM_ASSISTANT_SCOPES = setOf("USER", "ASSISTANT")
private val UPSTREAM_INJECTION_POSITIONS = setOf(
    "before_system_prompt",
    "after_system_prompt",
    "top_of_chat",
    "bottom_of_chat",
    "at_depth",
)
private val UPSTREAM_MESSAGE_ROLES = setOf("system", "user", "assistant", "tool")

private val SEARCH_SERVICE_KEYS = mapOf(
    "bing_local" to setOf("id"),
    "zhipu" to setOf("id", "apiKey"),
    "tavily" to setOf("id", "apiKey", "depth"),
    "exa" to setOf("id", "apiKey"),
    "searxng" to setOf("id", "url", "engines", "language", "username", "password"),
    "linkup" to setOf("id", "apiKey", "depth"),
    "brave" to setOf("id", "apiKey"),
    "metaso" to setOf("id", "apiKey"),
    "ollama" to setOf("id", "apiKey"),
    "perplexity" to setOf("id", "apiKey", "maxTokens", "maxTokensPerPage"),
    "firecrawl" to setOf("id", "apiKey"),
    "jina" to setOf("id", "apiKey", "searchUrl", "scrapeUrl"),
    "bocha" to setOf("id", "apiKey", "summary"),
    "rikkahub" to setOf("id", "apiKey", "depth"),
    "grok" to setOf("id", "apiKey", "model", "customUrl", "systemPrompt"),
    "tinyfish" to setOf("id", "apiKey"),
    "serper" to setOf("id", "apiKey"),
    "custom_js" to setOf("id", "name", "searchScript", "scrapeScript"),
)

private val TTS_PROVIDER_KEYS = mapOf(
    "openai" to setOf("id", "name", "apiKey", "baseUrl", "model", "voice"),
    "gemini" to setOf("id", "name", "apiKey", "baseUrl", "model", "voiceName"),
    "system" to setOf("id", "name", "speechRate", "pitch"),
    "minimax" to setOf("id", "name", "apiKey", "baseUrl", "model", "voiceId", "speed"),
    "qwen" to setOf("id", "name", "apiKey", "baseUrl", "model", "voice", "languageType"),
    "groq" to setOf("id", "name", "apiKey", "baseUrl", "model", "voice"),
    "xai" to setOf("id", "name", "apiKey", "baseUrl", "voiceId", "language"),
    "mimo" to setOf("id", "name", "apiKey", "baseUrl", "model", "voice"),
    "elevenlabs" to setOf(
        "id", "name", "apiKey", "baseUrl", "model", "voiceId", "stability", "similarityBoost"
    ),
    "step" to setOf(
        "id", "name", "apiKey", "baseUrl", "model", "voice", "responseFormat", "speed",
        "volume", "sampleRate", "instruction"
    ),
    "fish-audio" to setOf(
        "id", "name", "apiKey", "baseUrl", "model", "referenceId", "temperature", "speed",
        "format", "topP", "chunkLength", "normalize", "latency"
    ),
)

private val ASR_PROVIDER_KEYS = mapOf(
    "openai_realtime" to setOf(
        "id", "name", "apiKey", "websocketUrl", "model", "language", "prompt", "sampleRate",
        "vadThreshold", "prefixPaddingMs", "silenceDurationMs"
    ),
    "dashscope" to setOf(
        "id", "name", "apiKey", "websocketUrl", "model", "language", "sampleRate",
        "vadThreshold", "silenceDurationMs"
    ),
    "volcengine" to setOf("id", "name", "apiKey", "websocketUrl", "resourceId", "language"),
    "mimo" to setOf(
        "id", "name", "apiKey", "baseUrl", "model", "language", "sampleRate", "segmentDurationSec"
    ),
    "step" to setOf(
        "id", "name", "apiKey", "baseUrl", "model", "language", "sampleRate",
        "segmentDurationSec", "enableItn", "enableTimestamp", "hotwords"
    ),
)

private fun sanitizeSettings(root: JsonObject): JsonObject {
    val customThemes = root["customThemes"].sanitizeArray { it.objectOrNull()?.keep(CUSTOM_THEME_KEYS) }
    val providers = root["providers"].sanitizeArray(::sanitizeProvider)
    val assistants = root["assistants"].sanitizeArray(::sanitizeAssistant)
    val assistantTags = root["assistantTags"].sanitizeArray { it.objectOrNull()?.keep(TAG_KEYS) }
    val searchServices = root["searchServices"].sanitizeArray { it.sanitizeTypedObject(SEARCH_SERVICE_KEYS) }
    val mcpServers = root["mcpServers"].sanitizeArray(::sanitizeMcpServer)
    val ttsProviders = root["ttsProviders"].sanitizeArray { it.sanitizeTypedObject(TTS_PROVIDER_KEYS) }
    val asrProviders = root["asrProviders"].sanitizeArray { it.sanitizeTypedObject(ASR_PROVIDER_KEYS) }
    val modeInjections = root["modeInjections"].sanitizeArray(::sanitizeModeInjection)
    val quickMessages = root["quickMessages"].sanitizeArray { it.objectOrNull()?.keep(QUICK_MESSAGE_KEYS) }

    val result = linkedMapOf<String, JsonElement>()
    SETTINGS_KEYS.forEach { key ->
        val sanitized = when (key) {
            "customThemes" -> customThemes
            "displaySetting" -> root[key]?.objectOrNull()?.keep(DISPLAY_SETTING_KEYS)
            "providers" -> providers
            "assistants" -> assistants
            "assistantTags" -> assistantTags
            "searchServices" -> searchServices
            "searchCommonOptions" -> root[key]?.objectOrNull()?.keep(SEARCH_COMMON_OPTIONS_KEYS)
            "searchServiceSelected" -> sanitizeSearchServiceIndex(root[key], searchServices)
            "mcpServers" -> mcpServers
            "webDavConfig" -> root[key]?.objectOrNull()?.keep(WEBDAV_CONFIG_KEYS)
            "s3Config" -> root[key]?.objectOrNull()?.keep(S3_CONFIG_KEYS)
            "ttsProviders" -> ttsProviders
            "selectedTTSProviderId" -> root[key].takeIfIdExistsIn(ttsProviders)
            "asrProviders" -> asrProviders
            "selectedASRProviderId" -> root[key].takeIfNullOrIdExistsIn(asrProviders)
            "modeInjections" -> modeInjections
            "quickMessages" -> quickMessages
            "backupReminderConfig" -> root[key]?.objectOrNull()?.keep(BACKUP_REMINDER_CONFIG_KEYS)
            else -> root[key]
        }
        if (sanitized != null) result[key] = sanitized
    }
    return JsonObject(result)
}

private fun sanitizeAssistant(element: JsonElement): JsonElement? {
    val obj = element.objectOrNull() ?: return null
    val result = linkedMapOf<String, JsonElement>()
    ASSISTANT_KEYS.forEach { key ->
        val sanitized = when (key) {
            "regexes" -> obj[key].sanitizeArray(::sanitizeAssistantRegex)
            "customHeaders" -> obj[key].sanitizeArray { it.objectOrNull()?.keep(CUSTOM_HEADER_KEYS) }
            "customBodies" -> obj[key].sanitizeArray { it.objectOrNull()?.keep(CUSTOM_BODY_KEYS) }
            "localTools" -> obj[key].sanitizeTypedArray(UPSTREAM_LOCAL_TOOL_TYPES)
            else -> obj[key]
        }
        if (sanitized != null && sanitized !is JsonNull) result[key] = sanitized
    }

    if (!result.containsKey("enabledSkills") && obj["skillsEnabled"].booleanValue() == true) {
        obj["selectedSkills"]?.let { result["enabledSkills"] = it }
    }
    return JsonObject(result)
}

private fun sanitizeAssistantRegex(element: JsonElement): JsonElement? {
    val obj = element.objectOrNull() ?: return null
    val result = linkedMapOf<String, JsonElement>()
    ASSISTANT_REGEX_KEYS.forEach { key ->
        val sanitized = when (key) {
            "findRegex" -> obj["findRegex"]?.takeUnless { it.stringValue().isNullOrEmpty() } ?: obj["rawFindRegex"]
            "affectingScope" -> obj[key].sanitizeStringArray(UPSTREAM_ASSISTANT_SCOPES)
            else -> obj[key]
        }
        if (sanitized != null) result[key] = sanitized
    }
    return JsonObject(result)
}

private fun sanitizeProvider(element: JsonElement): JsonElement? {
    val obj = element.objectOrNull() ?: return null
    val typeKeys = when (obj.typeValue()) {
        "openai" -> setOf(
            "apiKey", "baseUrl", "chatCompletionsPath", "useResponseApi", "includeHistoryReasoning"
        )
        "google" -> setOf(
            "apiKey", "baseUrl", "vertexAI", "useServiceAccount", "privateKey",
            "serviceAccountEmail", "location", "projectId"
        )
        "claude" -> setOf("apiKey", "baseUrl", "promptCaching", "promptCacheTtl")
        else -> return null
    }

    val result = linkedMapOf<String, JsonElement>()
    (PROVIDER_COMMON_KEYS + typeKeys).forEach { key ->
        val sanitized = when (key) {
            "models" -> obj[key].sanitizeArray(::sanitizeModel)
            "balanceOption" -> obj[key]?.objectOrNull()?.keep(BALANCE_OPTION_KEYS)
            "includeHistoryReasoning" -> obj[key] ?: obj["sendFullReasoningHistory"]
            else -> obj[key]
        }
        if (sanitized != null) result[key] = sanitized
    }
    return JsonObject(result)
}

private fun sanitizeModel(element: JsonElement): JsonElement? {
    val obj = element.objectOrNull() ?: return null
    val result = linkedMapOf<String, JsonElement>()
    MODEL_KEYS.forEach { key ->
        val sanitized = when (key) {
            "customHeaders" -> obj[key].sanitizeArray { it.objectOrNull()?.keep(CUSTOM_HEADER_KEYS) }
            "customBodies" -> obj[key].sanitizeArray { it.objectOrNull()?.keep(CUSTOM_BODY_KEYS) }
            "tools" -> obj[key].sanitizeTypedArray(UPSTREAM_BUILT_IN_TOOL_TYPES)
            "providerOverwrite" -> obj[key]?.let(::sanitizeProvider)
            else -> obj[key]
        }
        if (sanitized != null && sanitized !is JsonNull) result[key] = sanitized
    }
    return JsonObject(result)
}

private fun sanitizeModeInjection(element: JsonElement): JsonElement? {
    val obj = element.objectOrNull() ?: return null
    val result = linkedMapOf<String, JsonElement>()
    MODE_INJECTION_KEYS.forEach { key ->
        val sanitized = when (key) {
            "type" -> JsonPrimitive("mode")
            "position" -> obj[key].stringValue()
                ?.takeIf { it in UPSTREAM_INJECTION_POSITIONS }
                ?.let(::JsonPrimitive)
                ?: JsonPrimitive("after_system_prompt")
            "role" -> obj[key].stringValue()
                ?.takeIf { it in UPSTREAM_MESSAGE_ROLES }
                ?.let(::JsonPrimitive)
                ?: JsonPrimitive("system")
            else -> obj[key]
        }
        if (sanitized != null) result[key] = sanitized
    }
    return JsonObject(result)
}

private fun sanitizeMcpServer(element: JsonElement): JsonElement? {
    val obj = element.objectOrNull() ?: return null
    val type = obj.typeValue()?.takeIf { it in UPSTREAM_MCP_SERVER_TYPES } ?: return null
    val result = linkedMapOf<String, JsonElement>("type" to JsonPrimitive(type))
    obj["id"]?.let { result["id"] = it }
    obj["commonOptions"]?.objectOrNull()?.let { commonOptions ->
        val sanitized = linkedMapOf<String, JsonElement>()
        MCP_COMMON_OPTIONS_KEYS.forEach { key ->
            val value = when (key) {
                "tools" -> commonOptions[key].sanitizeArray { it.objectOrNull()?.keep(MCP_TOOL_KEYS) }
                else -> commonOptions[key]
            }
            if (value != null) sanitized[key] = value
        }
        result["commonOptions"] = JsonObject(sanitized)
    }
    obj["url"]?.let { result["url"] = it }
    return JsonObject(result)
}

private fun JsonElement.sanitizeTypedObject(keysByType: Map<String, Set<String>>): JsonObject? {
    val obj = objectOrNull() ?: return null
    val type = obj.typeValue() ?: return null
    val keys = keysByType[type] ?: return null
    return obj.keep(setOf("type") + keys)
}

private fun sanitizeSearchServiceIndex(value: JsonElement?, services: JsonArray?): JsonPrimitive {
    val lastIndex = ((services?.size ?: 0) - 1).coerceAtLeast(0)
    return JsonPrimitive((value as? JsonPrimitive)?.intOrNull?.coerceIn(0, lastIndex) ?: 0)
}

private fun JsonElement?.takeIfIdExistsIn(items: JsonArray?): JsonElement? {
    val id = stringValue() ?: return null
    return takeIf { candidate ->
        items.orEmpty().any { item -> item.objectOrNull()?.get("id").stringValue() == id }
    }
}

private fun JsonElement?.takeIfNullOrIdExistsIn(items: JsonArray?): JsonElement? {
    if (this is JsonNull) return this
    return takeIfIdExistsIn(items)
}

private fun JsonObject.keep(keys: Set<String>): JsonObject {
    val result = linkedMapOf<String, JsonElement>()
    keys.forEach { key -> this[key]?.let { result[key] = it } }
    return JsonObject(result)
}

private fun JsonElement?.sanitizeArray(transform: (JsonElement) -> JsonElement?): JsonArray? {
    val array = this as? JsonArray ?: return null
    return JsonArray(array.mapNotNull(transform))
}

private fun JsonElement?.sanitizeTypedArray(allowedTypes: Set<String>): JsonArray? {
    return sanitizeArray { element ->
        val obj = element.objectOrNull() ?: return@sanitizeArray null
        if (obj.typeValue() in allowedTypes) obj else null
    }
}

private fun JsonElement?.sanitizeStringArray(allowedValues: Set<String>): JsonArray? {
    val array = this as? JsonArray ?: return null
    return JsonArray(
        array.mapNotNull { element ->
            element.stringValue()?.takeIf { it in allowedValues }?.let(::JsonPrimitive)
        }
    )
}

private fun JsonElement?.objectOrNull(): JsonObject? = this as? JsonObject

private fun JsonObject.typeValue(): String? = this["type"].stringValue()

private fun JsonElement?.stringValue(): String? = (this as? JsonPrimitive)?.contentOrNull

private fun JsonElement?.booleanValue(): Boolean? = (this as? JsonPrimitive)?.booleanOrNull
