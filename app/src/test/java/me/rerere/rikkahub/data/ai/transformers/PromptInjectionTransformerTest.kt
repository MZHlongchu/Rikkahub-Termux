package me.rerere.rikkahub.data.ai.transformers

import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.InjectionPosition
import me.rerere.rikkahub.data.model.PromptInjection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.uuid.Uuid

class PromptInjectionTransformerTest {
    private fun createAssistant(
        id: Uuid = Uuid.random(),
        modeInjectionIds: Set<Uuid> = emptySet(),
        allowConversationPromptInjection: Boolean = false,
    ) = Assistant(
        id = id,
        modeInjectionIds = modeInjectionIds,
        allowConversationPromptInjection = allowConversationPromptInjection,
    )

    private fun createModeInjection(
        id: Uuid = Uuid.random(),
        enabled: Boolean = true,
        priority: Int = 0,
        position: InjectionPosition = InjectionPosition.AFTER_SYSTEM_PROMPT,
        content: String = "Injected content",
        role: MessageRole = MessageRole.SYSTEM,
    ) = PromptInjection.ModeInjection(
        id = id,
        enabled = enabled,
        priority = priority,
        position = position,
        content = content,
        role = role,
    )

    private fun getMessageText(message: UIMessage): String {
        return message.parts
            .filterIsInstance<UIMessagePart.Text>()
            .joinToString("") { it.text }
    }

    @Test
    fun `no injections should return original messages`() {
        val messages = listOf(
            UIMessage.system("System prompt"),
            UIMessage.user("Hello"),
            UIMessage.assistant("Hi there!"),
        )

        val result = transformMessages(
            messages = messages,
            assistant = createAssistant(),
            modeInjections = emptyList(),
        )

        assertEquals(messages, result)
    }

    @Test
    fun `disabled mode injection should not be applied`() {
        val injectionId = Uuid.random()
        val injection = createModeInjection(
            id = injectionId,
            enabled = false,
            content = "Should not appear",
        )
        val messages = listOf(
            UIMessage.system("System prompt"),
            UIMessage.user("Hello"),
        )

        val result = transformMessages(
            messages = messages,
            assistant = createAssistant(modeInjectionIds = setOf(injectionId)),
            modeInjections = listOf(injection),
        )

        assertEquals(messages, result)
    }

    @Test
    fun `mode injection with AFTER_SYSTEM_PROMPT should append to system message`() {
        val injectionId = Uuid.random()
        val injection = createModeInjection(
            id = injectionId,
            position = InjectionPosition.AFTER_SYSTEM_PROMPT,
            content = "Appended content",
        )
        val messages = listOf(
            UIMessage.system("Original system prompt"),
            UIMessage.user("Hello"),
        )

        val result = transformMessages(
            messages = messages,
            assistant = createAssistant(modeInjectionIds = setOf(injectionId)),
            modeInjections = listOf(injection),
        )

        assertEquals(2, result.size)
        val systemText = getMessageText(result[0])
        assertTrue(systemText.startsWith("Original system prompt"))
        assertTrue(systemText.endsWith("Appended content"))
    }

    @Test
    fun `mode injection with BEFORE_SYSTEM_PROMPT should prepend to system message`() {
        val injectionId = Uuid.random()
        val injection = createModeInjection(
            id = injectionId,
            position = InjectionPosition.BEFORE_SYSTEM_PROMPT,
            content = "Prepended content",
        )
        val messages = listOf(
            UIMessage.system("Original system prompt"),
            UIMessage.user("Hello"),
        )

        val result = transformMessages(
            messages = messages,
            assistant = createAssistant(modeInjectionIds = setOf(injectionId)),
            modeInjections = listOf(injection),
        )

        assertEquals(2, result.size)
        val systemText = getMessageText(result[0])
        assertTrue(systemText.startsWith("Prepended content"))
        assertTrue(systemText.contains("Original system prompt"))
    }

    @Test
    fun `injection without existing system message should create new system message`() {
        val injectionId = Uuid.random()
        val injection = createModeInjection(
            id = injectionId,
            content = "New system content",
        )
        val messages = listOf(
            UIMessage.user("Hello"),
            UIMessage.assistant("Hi!"),
        )

        val result = transformMessages(
            messages = messages,
            assistant = createAssistant(modeInjectionIds = setOf(injectionId)),
            modeInjections = listOf(injection),
        )

        assertEquals(3, result.size)
        assertEquals(MessageRole.SYSTEM, result[0].role)
        assertEquals("New system content", getMessageText(result[0]))
    }

    @Test
    fun `conversation mode injection should apply only when assistant allows it`() {
        val injectionId = Uuid.random()
        val injection = createModeInjection(
            id = injectionId,
            content = "Conversation content",
        )
        val messages = listOf(
            UIMessage.system("System prompt"),
            UIMessage.user("Hello"),
        )

        val disabledResult = transformMessages(
            messages = messages,
            assistant = createAssistant(allowConversationPromptInjection = false),
            modeInjections = listOf(injection),
            conversationModeInjectionIds = setOf(injectionId),
        )
        val enabledResult = transformMessages(
            messages = messages,
            assistant = createAssistant(allowConversationPromptInjection = true),
            modeInjections = listOf(injection),
            conversationModeInjectionIds = setOf(injectionId),
        )

        assertEquals(messages, disabledResult)
        assertTrue(getMessageText(enabledResult.first()).contains("Conversation content"))
    }

    @Test
    fun `assistant mode injection should be ignored when conversation injection is allowed`() {
        val assistantInjectionId = Uuid.random()
        val conversationInjectionId = Uuid.random()
        val assistantInjection = createModeInjection(
            id = assistantInjectionId,
            content = "Assistant content",
        )
        val conversationInjection = createModeInjection(
            id = conversationInjectionId,
            content = "Conversation content",
        )
        val messages = listOf(
            UIMessage.system("System prompt"),
            UIMessage.user("Hello"),
        )

        val result = transformMessages(
            messages = messages,
            assistant = createAssistant(
                modeInjectionIds = setOf(assistantInjectionId),
                allowConversationPromptInjection = true,
            ),
            modeInjections = listOf(assistantInjection, conversationInjection),
            conversationModeInjectionIds = setOf(conversationInjectionId),
        )
        val systemText = getMessageText(result.first())

        assertFalse(systemText.contains("Assistant content"))
        assertTrue(systemText.contains("Conversation content"))
    }

    @Test
    fun `mode injection should normalize non system positions to AFTER_SYSTEM_PROMPT`() {
        val injectionId = Uuid.random()
        val injection = createModeInjection(
            id = injectionId,
            position = InjectionPosition.TOP_OF_CHAT,
            content = "Top of chat content",
        )
        val messages = listOf(
            UIMessage.system("System prompt"),
            UIMessage.user("Hello"),
            UIMessage.assistant("Hi!"),
        )

        val result = transformMessages(
            messages = messages,
            assistant = createAssistant(modeInjectionIds = setOf(injectionId)),
            modeInjections = listOf(injection),
        )

        assertEquals(3, result.size)
        assertEquals(MessageRole.SYSTEM, result[0].role)
        val systemText = getMessageText(result[0])
        assertTrue(systemText.startsWith("System prompt"))
        assertTrue(systemText.endsWith("Top of chat content"))
    }
}
