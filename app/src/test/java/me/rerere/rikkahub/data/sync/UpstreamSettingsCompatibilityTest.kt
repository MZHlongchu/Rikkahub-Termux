package me.rerere.rikkahub.data.sync

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.rerere.rikkahub.data.datastore.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpstreamSettingsCompatibilityTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `current settings export omits fork only top level fields`() {
        val encoded = BackupCompatibility.encodeUpstreamSettings(json, Settings())
        val root = json.parseToJsonElement(encoded).jsonObject

        assertFalse("runtime initialization marker must not be exported", "init" in root)
        assertFalse("app language is fork-only", "appLanguageTag" in root)
        assertFalse("scheduled tasks are fork-only", "scheduledTasks" in root)
        assertFalse("Termux settings are fork-only", "termuxWorkdir" in root)
        assertFalse("ST compatibility settings are fork-only", "stCompatScriptSource" in root)
        assertFalse("global regex settings are fork-only", "globalRegexes" in root)
        assertFalse("text selection settings are fork-only", "textSelectionConfig" in root)

        assertTrue("shared providers should remain", root["providers"] is JsonArray)
        assertTrue("shared assistants should remain", root["assistants"] is JsonArray)
    }

    @Test
    fun `nested fork fields and unsupported polymorphic values are removed`() {
        val output = sanitize(
            """
            {
              "dynamicColor": true,
              "termuxWorkdir": "/data/data/com.termux/files/home",
              "scheduledTasks": [{"id":"task-1"}],
              "displaySetting": {
                "showUserAvatar": true,
                "enableToolApprovalNotification": true
              },
              "customThemes": [{
                "id":"theme-1",
                "name":"Theme",
                "primaryColorArgb":1,
                "forkColor":2
              }],
              "providers": [{
                "type":"openai",
                "id":"provider-1",
                "enabled":true,
                "name":"OpenAI",
                "models":[{
                  "modelId":"gpt-test",
                  "displayName":"Test",
                  "id":"model-1",
                  "type":"CHAT",
                  "customHeaders":[{"name":"x-test","value":"yes","extra":"drop"}],
                  "customBodies":[],
                  "inputModalities":["TEXT"],
                  "outputModalities":["TEXT"],
                  "abilities":[],
                  "tools":[{"type":"search"},{"type":"fork_tool"}],
                  "forkModelField":true
                }],
                "balanceOption":{"enabled":false,"apiPath":"/credits","resultPath":"data","extra":1},
                "apiKey":"key",
                "baseUrl":"https://example.com",
                "chatCompletionsPath":"/chat/completions",
                "useResponseApi":false,
                "sendFullReasoningHistory":true,
                "forkProviderField":true
              }],
              "assistants": [{
                "id":"assistant-1",
                "name":"Assistant",
                "userPersona":"fork-only",
                "skillsEnabled":true,
                "selectedSkills":["shared-skill"],
                "regexes":[{
                  "id":"regex-1",
                  "name":"Regex",
                  "enabled":true,
                  "findRegex":"",
                  "rawFindRegex":"foo",
                  "replaceString":"bar",
                  "affectingScope":["SYSTEM","USER"],
                  "visualOnly":false,
                  "promptOnly":true
                }],
                "localTools":[{"type":"time_info"},{"type":"termux_exec"}]
              }],
              "searchServices": [
                {"type":"serper","id":"search-1","apiKey":"key","extra":true},
                {"type":"fork_search","id":"search-2"}
              ],
              "searchServiceSelected": 8,
              "mcpServers": [
                {
                  "type":"streamable_http",
                  "id":"mcp-1",
                  "url":"https://example.com/mcp",
                  "commonOptions":{
                    "enable":true,
                    "name":"MCP",
                    "headers":[],
                    "tools":[],
                    "oauth":{"accessToken":"secret"}
                  }
                },
                {"type":"stdio","id":"mcp-2","command":"node"}
              ],
              "ttsProviders": [
                {
                  "type":"minimax",
                  "id":"tts-1",
                  "name":"MiniMax",
                  "apiKey":"key",
                  "baseUrl":"https://example.com",
                  "model":"speech",
                  "voiceId":"voice",
                  "emotion":"calm",
                  "speed":1.0
                },
                {"type":"fork_tts","id":"tts-2"}
              ],
              "selectedTTSProviderId":"tts-1",
              "asrProviders":[{
                "type":"step",
                "id":"asr-1",
                "name":"Step",
                "apiKey":"key",
                "baseUrl":"https://example.com",
                "model":"asr",
                "language":"auto",
                "sampleRate":16000,
                "segmentDurationSec":30,
                "enableItn":true,
                "enableTimestamp":false,
                "hotwords":[],
                "extra":true
              }],
              "selectedASRProviderId":"missing-asr",
              "modeInjections":[{
                "type":"mode",
                "id":"mode-1",
                "name":"Mode",
                "enabled":true,
                "priority":0,
                "position":"author_note_top",
                "content":"prompt",
                "injectDepth":4,
                "role":"system",
                "extra":true
              }]
            }
            """.trimIndent()
        )

        assertFalse("termuxWorkdir" in output)
        assertFalse("scheduledTasks" in output)
        assertEquals(setOf("showUserAvatar"), output.objectAt("displaySetting").keys)
        assertEquals(
            setOf("id", "name", "primaryColorArgb"),
            output.arrayAt("customThemes").single().jsonObject.keys
        )

        val provider = output.arrayAt("providers").single().jsonObject
        assertFalse("forkProviderField" in provider)
        assertTrue(provider["includeHistoryReasoning"]!!.jsonPrimitive.boolean)
        assertFalse("sendFullReasoningHistory" in provider)
        assertEquals(
            setOf("enabled", "apiPath", "resultPath"),
            provider.objectAt("balanceOption").keys
        )

        val model = provider.arrayAt("models").single().jsonObject
        assertFalse("forkModelField" in model)
        assertEquals(listOf("search"), model.typeValues("tools"))
        assertEquals(
            setOf("name", "value"),
            model.arrayAt("customHeaders").single().jsonObject.keys
        )

        val assistant = output.arrayAt("assistants").single().jsonObject
        assertFalse("userPersona" in assistant)
        assertEquals("shared-skill", assistant.arrayAt("enabledSkills").single().jsonPrimitive.content)
        assertEquals(listOf("time_info"), assistant.typeValues("localTools"))
        val regex = assistant.arrayAt("regexes").single().jsonObject
        assertEquals("foo", regex["findRegex"]!!.jsonPrimitive.content)
        assertEquals(listOf("USER"), regex.arrayAt("affectingScope").map { it.jsonPrimitive.content })
        assertFalse("promptOnly" in regex)

        assertEquals(1, output.arrayAt("searchServices").size)
        assertEquals(0, output["searchServiceSelected"]!!.jsonPrimitive.content.toInt())
        assertEquals(1, output.arrayAt("mcpServers").size)
        assertFalse("oauth" in output.arrayAt("mcpServers").single().jsonObject.objectAt("commonOptions"))

        val tts = output.arrayAt("ttsProviders").single().jsonObject
        assertFalse("emotion" in tts)
        assertEquals("tts-1", output["selectedTTSProviderId"]!!.jsonPrimitive.content)
        assertFalse("selectedASRProviderId" in output)

        val injection = output.arrayAt("modeInjections").single().jsonObject
        assertEquals("after_system_prompt", injection["position"]!!.jsonPrimitive.content)
        assertEquals("system", injection["role"]!!.jsonPrimitive.content)
        assertFalse("extra" in injection)
    }

    @Test
    fun `nullable ASR selection remains null`() {
        val output = sanitize(
            """{"asrProviders":[],"selectedASRProviderId":null}"""
        )

        assertTrue(output["selectedASRProviderId"] is JsonNull)
    }

    private fun sanitize(input: String): JsonObject {
        val encoded = UpstreamSettingsCompatibility.sanitizeEncodedSettings(json, input)
        return json.parseToJsonElement(encoded).jsonObject
    }
}

private fun JsonObject.arrayAt(key: String): JsonArray {
    val value = this[key]
    assertNotNull("Missing array: $key", value)
    return value!!.jsonArray
}

private fun JsonObject.objectAt(key: String): JsonObject {
    val value = this[key]
    assertNotNull("Missing object: $key", value)
    return value!!.jsonObject
}

private fun JsonObject.typeValues(key: String): List<String> =
    arrayAt(key).map { it.jsonObject["type"]!!.jsonPrimitive.content }
