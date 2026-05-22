package me.rerere.rikkahub.data.model

import me.rerere.ai.ui.UIMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssistantTest {
    @Test
    fun `resolveToolCallKeepMessagesLimit should coerce values below one and keep max unlimited`() {
        assertEquals(1, Assistant(toolCallKeepMessages = 0).resolveToolCallKeepMessagesLimit())
        assertNull(
            Assistant(
                toolCallKeepMessages = ASSISTANT_TOOL_CALL_KEEP_MESSAGES_SLIDER_MAX
            ).resolveToolCallKeepMessagesLimit()
        )
    }

    @Test
    fun `conversation starters should use alternate greetings when available`() {
        val assistant = Assistant(
            presetMessages = listOf(
                UIMessage.system("Prelude"),
                UIMessage.assistant("Original greeting"),
            ),
            stCharacterData = SillyTavernCharacterData(
                alternateGreetings = listOf("Alternate greeting"),
            ),
        )

        val result = assistant.resolveConversationStarterMessages()

        assertEquals(listOf("Prelude", "Alternate greeting"), result.map { it.toText() })
    }
}
