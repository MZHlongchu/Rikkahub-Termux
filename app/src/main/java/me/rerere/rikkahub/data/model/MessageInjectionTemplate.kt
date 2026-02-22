package me.rerere.rikkahub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.rerere.ai.core.MessageRole
import kotlin.uuid.Uuid

@Serializable
data class MessageInjectionTemplate(
    val name: String = "",
    @SerialName("template")
    val nodes: List<MessageTemplateNode> = defaultNodes(),
) {
    companion object {
        fun default() = MessageInjectionTemplate(
            name = "Message Template",
            nodes = defaultNodes()
        )

        private fun defaultNodes() = listOf(
            MessageTemplateNode.HistoryNode(),
            MessageTemplateNode.LastUserMessageNode(),
        )
    }
}

@Serializable
data class TemplateRoleMapping(
    val system: MessageRole = MessageRole.SYSTEM,
    val user: MessageRole = MessageRole.USER,
    val assistant: MessageRole = MessageRole.ASSISTANT,
) {
    fun map(role: MessageRole): MessageRole = when (role) {
        MessageRole.SYSTEM -> system
        MessageRole.USER -> user
        MessageRole.ASSISTANT -> assistant
        else -> role
    }
}

@Serializable
sealed class MessageTemplateNode {
    abstract val id: Uuid
    abstract val enabled: Boolean

    @Serializable
    @SerialName("prompt")
    data class PromptNode(
        override val id: Uuid = Uuid.random(),
        override val enabled: Boolean = true,
        val role: MessageRole = MessageRole.USER,
        val content: String = "",
        val name: String = "",
    ) : MessageTemplateNode()

    @Serializable
    @SerialName("history")
    data class HistoryNode(
        override val id: Uuid = Uuid.random(),
        override val enabled: Boolean = true,
        @SerialName("role_mapping")
        val roleMapping: TemplateRoleMapping = TemplateRoleMapping(),
    ) : MessageTemplateNode()

    @Serializable
    @SerialName("last_user_message")
    data class LastUserMessageNode(
        override val id: Uuid = Uuid.random(),
        override val enabled: Boolean = true,
        @SerialName("role_mapping")
        val roleMapping: TemplateRoleMapping = TemplateRoleMapping(),
    ) : MessageTemplateNode()
}

fun MessageTemplateNode.withNewId(): MessageTemplateNode = when (this) {
    is MessageTemplateNode.PromptNode -> copy(id = Uuid.random())
    is MessageTemplateNode.HistoryNode -> copy(id = Uuid.random())
    is MessageTemplateNode.LastUserMessageNode -> copy(id = Uuid.random())
}

fun MessageInjectionTemplate.withNewNodeIds(): MessageInjectionTemplate {
    return copy(nodes = nodes.map { it.withNewId() })
}
