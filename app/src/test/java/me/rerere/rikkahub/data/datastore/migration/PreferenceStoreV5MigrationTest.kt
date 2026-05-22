package me.rerere.rikkahub.data.datastore.migration

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.rerere.rikkahub.utils.JsonInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PreferenceStoreV5MigrationTest {
    @Test
    fun `migrateAssistantToolCallKeepMessages should rename legacy rounds field`() {
        val assistantsJson = JsonInstant.encodeToString(
            JsonArray(
                listOf(
                    JsonObject(
                        mapOf(
                            "id" to JsonPrimitive("assistant-a"),
                            "toolCallKeepRounds" to JsonPrimitive(6),
                        )
                    ),
                    JsonObject(
                        mapOf(
                            "id" to JsonPrimitive("assistant-b"),
                            "toolCallKeepRounds" to JsonPrimitive(0),
                        )
                    ),
                )
            )
        )

        val migrated = JsonInstant.parseToJsonElement(
            migrateAssistantToolCallKeepMessages(assistantsJson)
        ).jsonArray

        assertEquals(6, migrated[0].jsonObject["toolCallKeepMessages"]?.jsonPrimitive?.int)
        assertEquals(1, migrated[1].jsonObject["toolCallKeepMessages"]?.jsonPrimitive?.int)
        migrated.forEach { assistant ->
            assertFalse(assistant.jsonObject.containsKey("toolCallKeepRounds"))
        }
    }

    @Test
    fun `migrateAssistantToolCallKeepMessages should keep existing messages field`() {
        val assistantsJson = JsonInstant.encodeToString(
            JsonArray(
                listOf(
                    JsonObject(
                        mapOf(
                            "id" to JsonPrimitive("assistant-a"),
                            "toolCallKeepRounds" to JsonPrimitive(6),
                            "toolCallKeepMessages" to JsonPrimitive(4),
                        )
                    ),
                )
            )
        )

        val migrated = JsonInstant.parseToJsonElement(
            migrateAssistantToolCallKeepMessages(assistantsJson)
        ).jsonArray.single().jsonObject

        assertEquals(4, migrated["toolCallKeepMessages"]?.jsonPrimitive?.int)
        assertFalse(migrated.containsKey("toolCallKeepRounds"))
    }
}
