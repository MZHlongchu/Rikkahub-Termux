package me.rerere.rikkahub.service

import kotlinx.serialization.json.JsonObject
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.TextRequestPreview

data class ChatPromptPreviewMessage(
    val role: MessageRole,
    val content: String,
    val tokenEstimate: Int,
)

data class ChatRuntimeInspection(
    val assistantName: String,
    val modelName: String,
    val promptMessages: List<ChatPromptPreviewMessage>,
    val promptTokenEstimate: Int,
    val contextVariables: JsonObject,
    val payloadPreview: TextRequestPreview,
)
