package me.rerere.rikkahub.ui.pages.assistant.detail

import me.rerere.ai.core.MessageRole
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.AssistantAffectScope
import me.rerere.rikkahub.data.model.AssistantRegex
import me.rerere.rikkahub.data.model.AssistantRegexPlacement
import me.rerere.rikkahub.data.model.AssistantRegexSourceKind
import me.rerere.rikkahub.utils.base64Encode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.uuid.Uuid

class SillyTavernImportTest {
    @Test
    fun `should parse character card into assistant data`() {
        val payload = parseAssistantImportFromJson(
            jsonString = characterCardJson(),
            sourceName = "alice-card",
            avatarImportSourceUri = "content://avatar",
        )

        assertEquals(AssistantImportKind.CHARACTER_CARD, payload.kind)
        assertEquals("Alice", payload.assistant.name)
        assertEquals("content://avatar", payload.avatarImportSourceUri)
        assertEquals(MessageRole.ASSISTANT, payload.assistant.presetMessages.single().role)
        assertEquals("Hello there", payload.assistant.presetMessages.single().toText())

        val character = payload.assistant.stCharacterData
        assertNotNull(character)
        assertEquals("alice-card", character?.sourceName)
        assertEquals("A careful assistant", character?.description)
        assertEquals("Kind", character?.personality)
        assertEquals("Library", character?.scenario)
        assertEquals("Stay concise", character?.systemPromptOverride)
        assertEquals("Remember context", character?.postHistoryInstructions)
        assertEquals("<START>\nAlice: Hi", character?.exampleMessagesRaw)
        assertEquals(listOf("Alt 1", "Alt 2"), character?.alternateGreetings)
        assertEquals("assistant", character?.depthPrompt?.role?.name?.lowercase())
        assertEquals(3, character?.depthPrompt?.depth)
    }

    @Test
    fun `should parse character card regex scripts as assistant regexes`() {
        val payload = parseAssistantImportFromJson(
            jsonString = characterCardJson(),
            sourceName = "alice-card",
        )

        val regex = payload.regexes.single()
        assertEquals("Trim output", regex.name)
        assertEquals("foo", regex.findRegex)
        assertEquals("bar", regex.replaceString)
        assertEquals(setOf(AssistantAffectScope.ASSISTANT), regex.affectingScope)
        assertEquals(setOf(AssistantRegexPlacement.AI_OUTPUT), regex.stPlacements)
        assertEquals(AssistantRegexSourceKind.ST_SCRIPT, regex.sourceKind)
        assertTrue(regex.promptOnly)
    }

    @Test
    fun `embedded character book should be ignored`() {
        val payload = parseAssistantImportFromJson(
            jsonString = characterCardJson(
                extraData = """
                    ,
                    "character_book": {
                      "entries": [
                        {
                          "keys": ["secret"],
                          "content": "World info that should not be imported"
                        }
                      ]
                    }
                """.trimIndent(),
            ),
            sourceName = "alice-card",
        )

        assertEquals("Alice", payload.assistant.name)
        assertEquals(0, payload.assistant.modeInjectionIds.size)
        assertEquals(1, payload.regexes.size)
    }

    @Test
    fun `regex scripts with unsupported placements should be ignored`() {
        val payload = parseAssistantImportFromJson(
            jsonString = """
                {
                  "spec": "chara_card_v2",
                  "spec_version": "2.0",
                  "data": {
                    "name": "Alice",
                    "extensions": {
                      "regex_scripts": [
                        {
                          "scriptName": "Unsupported",
                          "findRegex": "secret",
                          "replaceString": "",
                          "placement": [5]
                        }
                      ]
                    }
                  }
                }
            """.trimIndent(),
            sourceName = "alice-card",
        )

        assertTrue(payload.regexes.isEmpty())
    }

    @Test
    fun `apply imported character card should update existing assistant without runtime state`() {
        val current = Assistant(
            id = Uuid.random(),
            name = "Old",
            regexes = listOf(
                AssistantRegex(
                    id = Uuid.random(),
                    name = "Existing",
                    findRegex = "old",
                    replaceString = "new",
                    affectingScope = setOf(AssistantAffectScope.USER),
                )
            ),
        )
        val payload = parseAssistantImportFromJson(
            jsonString = characterCardJson(),
            sourceName = "alice-card",
        )

        val skippedRegexes = applyImportedAssistantToExisting(
            currentAssistant = current,
            payload = payload,
            includeRegexes = false,
        ).assistant
        val mergedRegexes = applyImportedAssistantToExisting(
            currentAssistant = current,
            payload = payload,
            includeRegexes = true,
        ).assistant

        assertEquals(current.id, skippedRegexes.id)
        assertEquals("Alice", skippedRegexes.name)
        assertNotNull(skippedRegexes.stCharacterData)
        assertEquals(1, skippedRegexes.regexes.size)
        assertEquals("Existing", skippedRegexes.regexes.single().name)
        assertEquals(2, mergedRegexes.regexes.size)
    }

    @Test
    fun `base64 character metadata should be decoded`() {
        val json = characterCardJson()

        assertEquals(json, decodeImportedCharacterCardJson(json.base64Encode()))
    }

    @Test(expected = IllegalStateException::class)
    fun `preset json should no longer be imported`() {
        parseAssistantImportFromJson(
            jsonString = """
                {
                  "name": "Preset",
                  "prompts": []
                }
            """.trimIndent(),
            sourceName = "preset",
        )
    }

    private fun characterCardJson(extraData: String = ""): String {
        return """
            {
              "spec": "chara_card_v2",
              "spec_version": "2.0",
              "data": {
                "name": "Alice",
                "description": "A careful assistant",
                "personality": "Kind",
                "scenario": "Library",
                "first_mes": "Hello there",
                "mes_example": "<START>\nAlice: Hi",
                "creator_notes": "Imported note",
                "system_prompt": "Stay concise",
                "post_history_instructions": "Remember context",
                "alternate_greetings": ["Alt 1", "Alt 2"],
                "character_version": "1.2",
                "extensions": {
                  "depth_prompt": {
                    "prompt": "Depth note",
                    "depth": 3,
                    "role": "assistant"
                  },
                  "regex_scripts": [
                    {
                      "scriptName": "Trim output",
                      "findRegex": "foo",
                      "replaceString": "bar",
                      "placement": [2],
                      "promptOnly": true
                    }
                  ]
                }
                $extraData
              }
            }
        """.trimIndent()
    }
}
