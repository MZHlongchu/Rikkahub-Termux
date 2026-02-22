package me.rerere.rikkahub.data.ai.transformers

import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.model.MessageInjectionTemplate
import me.rerere.rikkahub.data.model.MessageTemplateNode

object MessageTemplateInjectionTransformer : InputMessageTransformer {
    override suspend fun transform(
        ctx: TransformerContext,
        messages: List<UIMessage>
    ): List<UIMessage> {
        return applyMessageTemplate(
            messages = messages,
            template = ctx.assistant.messageInjectionTemplate
        )
    }
}

internal fun applyMessageTemplate(
    messages: List<UIMessage>,
    template: MessageInjectionTemplate
): List<UIMessage> {
    val enabledNodes = template.nodes.filter { it.enabled }
    if (enabledNodes.isEmpty()) return messages

    val hasLastUserMessageNode = enabledNodes.any { it is MessageTemplateNode.LastUserMessageNode }
    val lastUserMessage = messages.lastOrNull { it.role == MessageRole.USER }
    val historyMessages = if (hasLastUserMessageNode && lastUserMessage != null) {
        messages.filter { it.id != lastUserMessage.id }
    } else {
        messages
    }

    val result = mutableListOf<UIMessage>()
    enabledNodes.forEach { node ->
        when (node) {
            is MessageTemplateNode.PromptNode -> {
                if (node.content.isNotBlank()) {
                    result += UIMessage(
                        role = node.role,
                        parts = listOf(UIMessagePart.Text(node.content))
                    )
                }
            }

            is MessageTemplateNode.HistoryNode -> {
                result += historyMessages.map { it.copy(role = node.roleMapping.map(it.role)) }
            }

            is MessageTemplateNode.LastUserMessageNode -> {
                if (lastUserMessage != null) {
                    result += lastUserMessage.copy(
                        role = node.roleMapping.map(lastUserMessage.role)
                    )
                }
            }
        }
    }
    return result
}
