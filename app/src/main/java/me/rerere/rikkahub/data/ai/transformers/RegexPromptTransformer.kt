package me.rerere.rikkahub.data.ai.transformers

import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.AssistantAffectScope
import me.rerere.rikkahub.data.model.RegexTransformTarget
import me.rerere.rikkahub.data.model.replaceRegexes

object RegexPromptTransformer : InputMessageTransformer {
    override suspend fun transform(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> = applyPromptRegexes(messages, ctx.assistant)
}

internal fun applyPromptRegexes(messages: List<UIMessage>, assistant: Assistant): List<UIMessage> {
    if (assistant.regexes.none { it.enabled && it.promptOnly }) return messages
    return messages.map { message ->
        val scope = when (message.role) {
            MessageRole.USER -> AssistantAffectScope.USER
            MessageRole.ASSISTANT -> AssistantAffectScope.ASSISTANT
            else -> return@map message
        }
        message.copy(
            parts = message.parts.map { part ->
                when (part) {
                    is UIMessagePart.Text -> part.copy(
                        text = part.text.replaceRegexes(assistant, scope, RegexTransformTarget.PROMPT)
                    )

                    is UIMessagePart.Reasoning -> part.copy(
                        reasoning = part.reasoning.replaceRegexes(assistant, scope, RegexTransformTarget.PROMPT)
                    )

                    else -> part
                }
            }
        )
    }
}
