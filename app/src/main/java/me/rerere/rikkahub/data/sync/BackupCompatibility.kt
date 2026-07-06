package me.rerere.rikkahub.data.sync

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.utils.fileSizeToString
import java.io.File

private const val TAG = "BackupCompatibility"
private const val DATABASE_NAME = "rikka_hub"
private const val UPSTREAM_DATABASE_VERSION = 24
private const val UPSTREAM_IDENTITY_HASH = "0ea1aaebfa031c7995c45a1e35822e1a"

object BackupCompatibility {
    fun encodeUpstreamSettings(json: Json, settings: Settings): String {
        val root = json.parseToJsonElement(json.encodeToString(settings)).jsonObject
        return json.encodeToString(sanitizeSettings(root))
    }

    fun createUpstreamDatabaseCopy(context: Context): File? {
        val source = context.getDatabasePath(DATABASE_NAME)
        if (!source.exists()) {
            Log.w(TAG, "createUpstreamDatabaseCopy: database does not exist")
            return null
        }

        checkpointWal(source)

        val target = File(context.cacheDir, "upstream_${DATABASE_NAME}_${System.currentTimeMillis()}.db")
        if (target.exists()) target.delete()
        source.copyTo(target, overwrite = true)

        runCatching {
            SQLiteDatabase.openDatabase(target.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                migrateDatabaseToUpstream(db)
            }
        }.onFailure {
            target.delete()
            Log.e(TAG, "createUpstreamDatabaseCopy: failed to create upstream compatible database", it)
            throw it
        }

        Log.i(
            TAG,
            "createUpstreamDatabaseCopy: created ${target.name} (${target.length().fileSizeToString()})"
        )
        return target
    }

    private fun checkpointWal(source: File) {
        runCatching {
            SQLiteDatabase.openDatabase(source.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { cursor ->
                    if (cursor.moveToFirst()) {
                        val busy = cursor.getInt(0)
                        Log.i(
                            TAG,
                            "checkpointWal: busy=$busy, log=${cursor.getInt(1)}, checkpointed=${cursor.getInt(2)}"
                        )
                        check(busy == 0) { "Database is busy; cannot create a complete backup snapshot" }
                    }
                }
            }
        }.onFailure {
            Log.e(TAG, "checkpointWal: failed", it)
            throw IllegalStateException("Unable to checkpoint database before backup", it)
        }
    }

    private fun migrateDatabaseToUpstream(db: SQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys=OFF")
        db.beginTransaction()
        try {
            recreateConversationEntity(db)
            recreateUpstreamWorkspaceTables(db)
            db.execSQL("DROP TABLE IF EXISTS `scheduled_task_run`")
            db.execSQL("DROP INDEX IF EXISTS `index_scheduled_task_run_task_id`")
            db.execSQL("DROP INDEX IF EXISTS `index_scheduled_task_run_started_at`")
            db.execSQL("DROP INDEX IF EXISTS `index_scheduled_task_run_status`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_message_node_conversation_id` ON `message_node` (`conversation_id`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_managed_files_relative_path` ON `managed_files` (`relative_path`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_managed_files_folder` ON `managed_files` (`folder`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_favorites_ref_key` ON `favorites` (`ref_key`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_favorites_type` ON `favorites` (`type`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_favorites_created_at` ON `favorites` (`created_at`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            db.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, '$UPSTREAM_IDENTITY_HASH')"
            )
            db.version = UPSTREAM_DATABASE_VERSION
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.execSQL("PRAGMA foreign_keys=ON")
        }

        checkPragmaOk(db, "PRAGMA integrity_check")
        checkForeignKeys(db)
        db.execSQL("VACUUM")
    }

    private fun recreateConversationEntity(db: SQLiteDatabase) {
        if (!tableExists(db, "ConversationEntity")) return

        db.execSQL("DROP TABLE IF EXISTS `ConversationEntity_upstream`")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ConversationEntity_upstream` (
                `id` TEXT NOT NULL,
                `assistant_id` TEXT NOT NULL DEFAULT '0950e2dc-9bd5-4801-afa3-aa887aa36b4e',
                `title` TEXT NOT NULL,
                `nodes` TEXT NOT NULL,
                `create_at` INTEGER NOT NULL,
                `update_at` INTEGER NOT NULL,
                `suggestions` TEXT NOT NULL DEFAULT '[]',
                `is_pinned` INTEGER NOT NULL DEFAULT 0,
                `custom_system_prompt` TEXT NOT NULL DEFAULT '',
                `mode_injection_ids` TEXT NOT NULL DEFAULT '[]',
                `lorebook_ids` TEXT NOT NULL DEFAULT '[]',
                `workspace_cwd` TEXT NOT NULL DEFAULT '',
                `folder_id` TEXT NOT NULL DEFAULT '',
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        val suggestions = columnExpression(db, "ConversationEntity", "suggestions", "'[]'")
        val customSystemPrompt = columnExpression(db, "ConversationEntity", "custom_system_prompt", "''")
        val modeInjectionIds = columnExpression(db, "ConversationEntity", "mode_injection_ids", "'[]'")
        val lorebookIds = columnExpression(db, "ConversationEntity", "lorebook_ids", "'[]'")
        val workspaceCwd = columnExpression(db, "ConversationEntity", "workspace_cwd", "''")
        val folderId = columnExpression(db, "ConversationEntity", "folder_id", "''")

        db.execSQL(
            """
            INSERT INTO `ConversationEntity_upstream` (
                `id`,
                `assistant_id`,
                `title`,
                `nodes`,
                `create_at`,
                `update_at`,
                `suggestions`,
                `is_pinned`,
                `custom_system_prompt`,
                `mode_injection_ids`,
                `lorebook_ids`,
                `workspace_cwd`,
                `folder_id`
            )
            SELECT
                `id`,
                `assistant_id`,
                `title`,
                `nodes`,
                `create_at`,
                `update_at`,
                $suggestions,
                `is_pinned`,
                $customSystemPrompt,
                $modeInjectionIds,
                $lorebookIds,
                $workspaceCwd,
                $folderId
            FROM `ConversationEntity`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `ConversationEntity`")
        db.execSQL("ALTER TABLE `ConversationEntity_upstream` RENAME TO `ConversationEntity`")
    }

    private fun recreateUpstreamWorkspaceTables(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `workspaces`")
        db.execSQL("DROP TABLE IF EXISTS `conversation_folder`")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `workspaces` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `root` TEXT NOT NULL,
                `shell_status` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                `last_access_at` INTEGER,
                `tool_approvals` TEXT NOT NULL DEFAULT '{}',
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_workspaces_root` ON `workspaces` (`root`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workspaces_updated_at` ON `workspaces` (`updated_at`)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `conversation_folder` (
                `id` TEXT NOT NULL,
                `assistant_id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `sort_index` INTEGER NOT NULL DEFAULT 0,
                `create_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_conversation_folder_assistant_id` " +
                "ON `conversation_folder` (`assistant_id`)"
        )
    }

    private fun checkPragmaOk(db: SQLiteDatabase, pragma: String) {
        db.rawQuery(pragma, null).use { cursor ->
            if (cursor.moveToFirst() && cursor.getString(0) != "ok") {
                error("$pragma failed: ${cursor.getString(0)}")
            }
        }
    }

    private fun checkForeignKeys(db: SQLiteDatabase) {
        db.rawQuery("PRAGMA foreign_key_check", null).use { cursor ->
            if (cursor.moveToFirst()) {
                error("PRAGMA foreign_key_check failed")
            }
        }
    }

    private fun tableExists(db: SQLiteDatabase, tableName: String): Boolean {
        db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(tableName)
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun hasColumn(db: SQLiteDatabase, tableName: String, columnName: String): Boolean {
        db.rawQuery("PRAGMA table_info(`$tableName`)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            if (nameIndex < 0) return false
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == columnName) return true
            }
        }
        return false
    }

    private fun columnExpression(db: SQLiteDatabase, tableName: String, columnName: String, fallback: String): String {
        return if (hasColumn(db, tableName, columnName)) "`$columnName`" else fallback
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

private val PROVIDER_COMMON_KEYS = setOf(
    "type",
    "id",
    "enabled",
    "name",
    "models",
    "balanceOption",
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

private val MODE_INJECTION_KEYS = setOf(
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
private val S3_CONFIG_KEYS = setOf("endpoint", "accessKeyId", "secretAccessKey", "bucket", "region", "pathStyle", "items")
private val BACKUP_REMINDER_CONFIG_KEYS = setOf("enabled", "intervalDays", "lastBackupTime")
private val BALANCE_OPTION_KEYS = setOf("enabled", "apiPath", "resultPath")

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

private fun sanitizeSettings(root: JsonObject): JsonObject {
    val result = linkedMapOf<String, JsonElement>()
    SETTINGS_KEYS.forEach { key ->
        val sanitized = when (key) {
            "displaySetting" -> root[key]?.objectOrNull()?.keep(DISPLAY_SETTING_KEYS)
            "providers" -> root[key].sanitizeArray(::sanitizeProvider)
            "assistants" -> root[key].sanitizeArray(::sanitizeAssistant)
            "searchServices" -> root[key].sanitizeArray(::sanitizeKnownSearchService)
            "mcpServers" -> root[key].sanitizeArray(::sanitizeMcpServer)
            "webDavConfig" -> root[key]?.objectOrNull()?.keep(WEBDAV_CONFIG_KEYS)
            "s3Config" -> root[key]?.objectOrNull()?.keep(S3_CONFIG_KEYS)
            "ttsProviders" -> root[key].sanitizeArray(::sanitizeKnownTtsProvider)
            "asrProviders" -> root[key].sanitizeArray(::sanitizeKnownAsrProvider)
            "modeInjections" -> root[key].sanitizeArray(::sanitizeModeInjection)
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
            "localTools" -> obj[key].sanitizeTypedArray(UPSTREAM_LOCAL_TOOL_TYPES)
            else -> obj[key]
        }
        if (sanitized != null) result[key] = sanitized
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
            "findRegex" -> obj["findRegex"] ?: obj["rawFindRegex"]
            "affectingScope" -> obj[key].sanitizeStringArray(UPSTREAM_ASSISTANT_SCOPES)
            else -> obj[key]
        }
        if (sanitized != null) result[key] = sanitized
    }
    return JsonObject(result)
}

private fun sanitizeProvider(element: JsonElement): JsonElement? {
    val obj = element.objectOrNull() ?: return null
    val type = obj.typeValue() ?: return null
    val typeKeys = when (type) {
        "openai" -> setOf(
            "apiKey",
            "baseUrl",
            "chatCompletionsPath",
            "useResponseApi",
            "includeHistoryReasoning",
        )

        "google" -> setOf(
            "apiKey",
            "baseUrl",
            "vertexAI",
            "useServiceAccount",
            "privateKey",
            "serviceAccountEmail",
            "location",
            "projectId",
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
            "position" -> obj[key].stringValue()
                ?.takeIf { it in UPSTREAM_INJECTION_POSITIONS }
                ?.let(::JsonPrimitive)
                ?: JsonPrimitive("after_system_prompt")

            "role" -> obj[key].stringValue()
                ?.takeIf { it in UPSTREAM_MESSAGE_ROLES }
                ?.let(::JsonPrimitive)

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
        result["commonOptions"] = sanitizeMcpCommonOptions(commonOptions)
    }
    obj["url"]?.let { result["url"] = it }
    return JsonObject(result)
}

private fun sanitizeMcpCommonOptions(obj: JsonObject): JsonObject {
    val result = linkedMapOf<String, JsonElement>()
    MCP_COMMON_OPTIONS_KEYS.forEach { key ->
        val sanitized = when (key) {
            "tools" -> obj[key].sanitizeArray { tool ->
                tool.objectOrNull()?.keep(MCP_TOOL_KEYS)
            }

            else -> obj[key]
        }
        if (sanitized != null) result[key] = sanitized
    }
    return JsonObject(result)
}

private fun sanitizeKnownSearchService(element: JsonElement): JsonElement? {
    val obj = element.objectOrNull() ?: return null
    return if (obj.typeValue() in SEARCH_SERVICE_TYPES) obj else null
}

private fun sanitizeKnownTtsProvider(element: JsonElement): JsonElement? {
    val obj = element.objectOrNull() ?: return null
    return if (obj.typeValue() in TTS_PROVIDER_TYPES) obj else null
}

private fun sanitizeKnownAsrProvider(element: JsonElement): JsonElement? {
    val obj = element.objectOrNull() ?: return null
    return if (obj.typeValue() in ASR_PROVIDER_TYPES) obj else null
}

private val SEARCH_SERVICE_TYPES = setOf(
    "bing_local",
    "rikkahub",
    "zhipu",
    "tavily",
    "exa",
    "searxng",
    "linkup",
    "brave",
    "metaso",
    "ollama",
    "perplexity",
    "firecrawl",
    "jina",
    "bocha",
    "grok",
    "tinyfish",
    "serper",
    "custom_js",
)

private val TTS_PROVIDER_TYPES = setOf(
    "openai",
    "gemini",
    "system",
    "minimax",
    "qwen",
    "groq",
    "xai",
    "mimo",
    "elevenlabs",
    "step",
)

private val ASR_PROVIDER_TYPES = setOf(
    "openai_realtime",
    "dashscope",
    "volcengine",
    "mimo",
    "step",
)

private fun JsonObject.keep(keys: Set<String>): JsonObject {
    val result = linkedMapOf<String, JsonElement>()
    keys.forEach { key ->
        this[key]?.let { result[key] = it }
    }
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

private fun JsonElement?.stringValue(): String? {
    return (this as? JsonPrimitive)?.contentOrNull
}

private fun JsonElement?.booleanValue(): Boolean? {
    return (this as? JsonPrimitive)?.booleanOrNull
}
