package me.rerere.rikkahub.data.export

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.rerere.ai.ui.UIMessage
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.AssistantAffectScope
import me.rerere.rikkahub.data.model.AssistantRegex
import me.rerere.rikkahub.data.model.AssistantRegexPlacement
import me.rerere.rikkahub.data.model.SillyTavernCharacterData
import me.rerere.rikkahub.data.model.StDepthPrompt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.uuid.Uuid

class SillyTavernExportSerializerTest {
    @Test
    fun `character card export should include assistant character data and regexes`() {
        val assistant = Assistant(
            name = "Fallback",
            stCharacterData = SillyTavernCharacterData(
                name = "Alice",
                version = "1.2",
                description = "A careful assistant",
                personality = "Kind",
                scenario = "Library",
                systemPromptOverride = "Stay concise",
                postHistoryInstructions = "Remember context",
                firstMessage = "Hello there",
                exampleMessagesRaw = "<START>\nAlice: Hi",
                alternateGreetings = listOf("Alt 1"),
                creatorNotes = "Imported note",
                depthPrompt = StDepthPrompt(prompt = "Depth note", depth = 3),
            ),
            regexes = listOf(
                AssistantRegex(
                    id = Uuid.random(),
                    name = "Trim output",
                    findRegex = "foo",
                    replaceString = "bar",
                    affectingScope = setOf(AssistantAffectScope.ASSISTANT),
                    stPlacements = setOf(AssistantRegexPlacement.AI_OUTPUT),
                    promptOnly = true,
                )
            ),
        )

        val json = Json.parseToJsonElement(
            SillyTavernCharacterCardSerializer.exportToJson(
                SillyTavernCharacterCardExportData(assistant),
                Json,
            )
        ).jsonObject
        val data = json.getValue("data").jsonObject
        val extensions = data.getValue("extensions").jsonObject
        val regex = extensions.getValue("regex_scripts").jsonArray.single().jsonObject

        assertEquals("chara_card_v2", json.getValue("spec").jsonPrimitive.contentOrNull)
        assertEquals("Alice", data.getValue("name").jsonPrimitive.contentOrNull)
        assertEquals("A careful assistant", data.getValue("description").jsonPrimitive.contentOrNull)
        assertEquals("Hello there", data.getValue("first_mes").jsonPrimitive.contentOrNull)
        assertEquals("Stay concise", data.getValue("system_prompt").jsonPrimitive.contentOrNull)
        assertEquals("1.2", data.getValue("character_version").jsonPrimitive.contentOrNull)
        assertEquals(
            "Depth note",
            extensions.getValue("depth_prompt").jsonObject.getValue("prompt").jsonPrimitive.contentOrNull,
        )
        assertEquals("Trim output", regex.getValue("scriptName").jsonPrimitive.contentOrNull)
        assertEquals("foo", regex.getValue("findRegex").jsonPrimitive.contentOrNull)
        assertEquals("bar", regex.getValue("replaceString").jsonPrimitive.contentOrNull)
        assertTrue(regex.getValue("promptOnly").jsonPrimitive.contentOrNull == "true")
        assertFalse(data.containsKey("character_book"))
    }

    @Test
    fun `character card export should fall back to assistant first message`() {
        val assistant = Assistant(
            name = "Assistant",
            presetMessages = listOf(UIMessage.assistant("Fallback greeting")),
            stCharacterData = SillyTavernCharacterData(
                name = "Alice",
                firstMessage = "",
            ),
        )

        val data = Json.parseToJsonElement(
            SillyTavernCharacterCardSerializer.exportToJson(
                SillyTavernCharacterCardExportData(assistant),
                Json,
            )
        ).jsonObject.getValue("data").jsonObject

        assertEquals("Fallback greeting", data.getValue("first_mes").jsonPrimitive.contentOrNull)
    }
}
