package me.rerere.rikkahub.data.export

import me.rerere.ai.core.MessageRole
import me.rerere.rikkahub.data.model.MessageTemplateNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MessageTemplateSerializerTest {
    @Test
    fun `should parse zhuru message template format`() {
        val json = """
            {
              "name": "预设消息",
              "template": [
                {
                  "type": "prompt",
                  "role": "system",
                  "content": "测试系统消息。",
                  "name": "System Prompt",
                  "enabled": true
                },
                {
                  "type": "history",
                  "role_mapping": {
                    "system": "user",
                    "user": "user",
                    "assistant": "assistant"
                  },
                  "enabled": true
                },
                {
                  "type": "last_user_message",
                  "role_mapping": {},
                  "enabled": true
                }
              ]
            }
        """.trimIndent()

        val parsed = MessageTemplateSerializer.tryImportTemplateJson(json)

        assertNotNull(parsed)
        parsed!!
        assertEquals("预设消息", parsed.name)
        assertFalse(parsed.enabled)
        assertEquals(3, parsed.nodes.size)

        val first = parsed.nodes[0] as MessageTemplateNode.PromptNode
        assertEquals(MessageRole.SYSTEM, first.role)
        assertEquals("测试系统消息。", first.content)
        assertEquals("System Prompt", first.name)

        val second = parsed.nodes[1] as MessageTemplateNode.HistoryNode
        assertEquals(MessageRole.USER, second.roleMapping.system)
        assertEquals(MessageRole.USER, second.roleMapping.user)
        assertEquals(MessageRole.ASSISTANT, second.roleMapping.assistant)

        val third = parsed.nodes[2] as MessageTemplateNode.LastUserMessageNode
        assertEquals(MessageRole.SYSTEM, third.roleMapping.system)
        assertEquals(MessageRole.USER, third.roleMapping.user)
        assertEquals(MessageRole.ASSISTANT, third.roleMapping.assistant)

        assertEquals(parsed.nodes.size, parsed.nodes.map { it.id }.toSet().size)
    }

    @Test
    fun `should return null for unsupported json`() {
        val parsed = MessageTemplateSerializer.tryImportTemplateJson("""{"foo":"bar"}""")

        assertNull(parsed)
    }
}
