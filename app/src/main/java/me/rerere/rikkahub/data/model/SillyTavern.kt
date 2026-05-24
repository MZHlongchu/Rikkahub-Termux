package me.rerere.rikkahub.data.model

import kotlinx.serialization.Serializable
import me.rerere.ai.core.MessageRole

@Serializable
data class SillyTavernCharacterData(
    val sourceName: String = "",
    val name: String = "",
    val version: String = "",
    val description: String = "",
    val personality: String = "",
    val scenario: String = "",
    val systemPromptOverride: String = "",
    val postHistoryInstructions: String = "",
    val firstMessage: String = "",
    val exampleMessagesRaw: String = "",
    val alternateGreetings: List<String> = emptyList(),
    val creatorNotes: String = "",
    val depthPrompt: StDepthPrompt? = null,
)

@Serializable
data class StDepthPrompt(
    val prompt: String = "",
    val depth: Int = 4,
    val role: MessageRole = MessageRole.SYSTEM,
)
