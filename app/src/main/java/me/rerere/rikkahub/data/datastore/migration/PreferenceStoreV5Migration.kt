package me.rerere.rikkahub.data.datastore.migration

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.utils.JsonInstant

class PreferenceStoreV5Migration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        val version = currentData[SettingsStore.VERSION]
        return version == null || version < 5
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val prefs = currentData.toMutablePreferences()
        prefs[SettingsStore.ASSISTANTS] = prefs[SettingsStore.ASSISTANTS]?.let { json ->
            migrateAssistantToolCallKeepMessages(json)
        } ?: "[]"
        prefs[SettingsStore.VERSION] = 5
        return prefs.toPreferences()
    }

    override suspend fun cleanUp() {}
}

internal fun migrateAssistantToolCallKeepMessages(assistantsJson: String): String {
    return runCatching {
        val root = JsonInstant.parseToJsonElement(assistantsJson) as? JsonArray
            ?: return@runCatching assistantsJson
        val migrated = JsonArray(
            root.map { assistant ->
                val assistantObject = assistant as? JsonObject
                    ?: return@map assistant
                val fields = assistantObject.toMutableMap()
                val legacyValue = fields.remove("toolCallKeepRounds")
                    ?: return@map assistant
                if (!fields.containsKey("toolCallKeepMessages")) {
                    val normalized = (legacyValue as? JsonPrimitive)?.intOrNull?.coerceAtLeast(1)
                    fields["toolCallKeepMessages"] = normalized?.let(::JsonPrimitive) ?: legacyValue
                }
                JsonObject(fields)
            }
        )
        if (migrated == root) assistantsJson else JsonInstant.encodeToString(migrated)
    }.getOrElse { assistantsJson }
}
