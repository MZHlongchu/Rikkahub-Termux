package me.rerere.rikkahub.data.ai.transformers

import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.model.MessageInjectionTemplate
import me.rerere.rikkahub.data.model.MessageTemplateNode
import me.rerere.rikkahub.data.model.TemplateRoleMapping
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageTemplateInjectionTransformerTest {
    private fun getMessageText(msg: UIMessage): String =
        msg.parts.filterIsInstance<UIMessagePart.Text>().joinToString("") { it.text }

    @Test
    fun `disabled template nodes should keep original messages`() {
        val messages = listOf(
            UIMessage.system("system"),
            UIMessage.user("hello"),
            UIMessage.assistant("hi")
        )
        val template = MessageInjectionTemplate(
            nodes = listOf(
                MessageTemplateNode.PromptNode(enabled = false, content = "ignored")
            )
        )

        val result = applyMessageTemplate(messages, template)

        assertEquals(messages, result)
    }

    @Test
    fun `template should apply nodes in order and avoid duplicate last user from history`() {
        val messages = listOf(
            UIMessage.system("sys"),
            UIMessage.user("u1"),
            UIMessage.assistant("a1"),
            UIMessage.user("u2"),
        )
        val template = MessageInjectionTemplate(
            nodes = listOf(
                MessageTemplateNode.PromptNode(
                    role = MessageRole.SYSTEM,
                    content = "custom system"
                ),
                MessageTemplateNode.HistoryNode(
                    roleMapping = TemplateRoleMapping(
                        system = MessageRole.USER,
                        user = MessageRole.USER,
                        assistant = MessageRole.ASSISTANT
                    )
                ),
                MessageTemplateNode.LastUserMessageNode(
                    roleMapping = TemplateRoleMapping(
                        system = MessageRole.SYSTEM,
                        user = MessageRole.USER,
                        assistant = MessageRole.ASSISTANT
                    )
                )
            )
        )

        val result = applyMessageTemplate(messages, template)

        assertEquals(5, result.size)
        assertEquals(MessageRole.SYSTEM, result[0].role)
        assertEquals("custom system", getMessageText(result[0]))
        assertEquals("sys", getMessageText(result[1]))
        assertEquals("u1", getMessageText(result[2]))
        assertEquals("a1", getMessageText(result[3]))
        assertEquals("u2", getMessageText(result[4]))
        assertTrue(result.drop(1).none { getMessageText(it) == "u2" && it !== result[4] })
    }

    @Test
    fun `last user message should respect role mapping`() {
        val messages = listOf(
            UIMessage.user("question")
        )
        val template = MessageInjectionTemplate(
            nodes = listOf(
                MessageTemplateNode.LastUserMessageNode(
                    roleMapping = TemplateRoleMapping(user = MessageRole.ASSISTANT)
                )
            )
        )

        val result = applyMessageTemplate(messages, template)

        assertEquals(1, result.size)
        assertEquals(MessageRole.ASSISTANT, result.first().role)
        assertEquals("question", getMessageText(result.first()))
    }
}
