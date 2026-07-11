package me.rerere.rikkahub.data.ai.transformers

import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.AssistantAffectScope
import me.rerere.rikkahub.data.model.AssistantRegex
import me.rerere.rikkahub.data.model.RegexTransformTarget
import me.rerere.rikkahub.data.model.replaceRegexes
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.uuid.Uuid

class RegexPromptTransformerTest {
    @Test
    fun `prompt-only regex applies only to the prompt target`() {
        val assistant = Assistant(
            regexes = listOf(
                AssistantRegex(
                    id = Uuid.random(),
                    findRegex = "secret",
                    replaceString = "redacted",
                    affectingScope = setOf(AssistantAffectScope.USER),
                    promptOnly = true,
                )
            )
        )

        assertEquals("secret", "secret".replaceRegexes(assistant, AssistantAffectScope.USER))
        assertEquals(
            "secret",
            "secret".replaceRegexes(
                assistant,
                AssistantAffectScope.USER,
                RegexTransformTarget.VISUAL,
            ),
        )
        assertEquals(
            "redacted",
            "secret".replaceRegexes(
                assistant,
                AssistantAffectScope.USER,
                RegexTransformTarget.PROMPT,
            ),
        )
    }

    @Test
    fun `prompt-only regex changes request copy without changing original messages`() {
        val assistant = Assistant(
            regexes = listOf(
                AssistantRegex(
                    id = Uuid.random(),
                    findRegex = "secret",
                    replaceString = "redacted",
                    affectingScope = setOf(AssistantAffectScope.USER, AssistantAffectScope.ASSISTANT),
                    promptOnly = true,
                )
            )
        )
        val messages = listOf(
            UIMessage(role = MessageRole.USER, parts = listOf(UIMessagePart.Text("user secret"))),
            UIMessage(
                role = MessageRole.ASSISTANT,
                parts = listOf(
                    UIMessagePart.Text("assistant secret"),
                    UIMessagePart.Reasoning(reasoning = "reasoning secret"),
                )
            ),
            UIMessage.system("system secret"),
        )

        val result = applyPromptRegexes(messages, assistant)

        assertEquals("user redacted", result[0].parts.filterIsInstance<UIMessagePart.Text>().single().text)
        assertEquals("assistant redacted", result[1].parts.filterIsInstance<UIMessagePart.Text>().single().text)
        assertEquals(
            "reasoning redacted",
            result[1].parts.filterIsInstance<UIMessagePart.Reasoning>().single().reasoning,
        )
        assertEquals("system secret", result[2].parts.filterIsInstance<UIMessagePart.Text>().single().text)
        assertEquals("user secret", messages[0].parts.filterIsInstance<UIMessagePart.Text>().single().text)
    }

    @Test
    fun `prompt transform ignores regular and visual-only regexes`() {
        val assistant = Assistant(
            regexes = listOf(
                AssistantRegex(
                    id = Uuid.random(),
                    findRegex = "message",
                    replaceString = "regular",
                    affectingScope = setOf(AssistantAffectScope.USER),
                ),
                AssistantRegex(
                    id = Uuid.random(),
                    findRegex = "message",
                    replaceString = "visual",
                    affectingScope = setOf(AssistantAffectScope.USER),
                    visualOnly = true,
                ),
            )
        )
        val messages = listOf(
            UIMessage(role = MessageRole.USER, parts = listOf(UIMessagePart.Text("message")))
        )

        val result = applyPromptRegexes(messages, assistant)

        assertEquals("message", result.single().parts.filterIsInstance<UIMessagePart.Text>().single().text)
    }
}
