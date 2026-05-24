package me.rerere.rikkahub.ui.pages.assistant.detail

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.Avatar
import me.rerere.rikkahub.data.model.SillyTavernCharacterData
import me.rerere.rikkahub.data.model.StDepthPrompt
import me.rerere.rikkahub.utils.jsonPrimitiveOrNull

internal fun parseCharacterCardImport(
    json: JsonObject,
    sourceName: String,
    avatarImportSourceUri: String?,
): AssistantImportPayload {
    val data = json["data"]?.jsonObject ?: error("Missing card data")
    val name = data["name"]?.jsonPrimitiveOrNull?.contentOrNull ?: error("Missing card name")
    val firstMessage = data["first_mes"]?.jsonPrimitiveOrNull?.contentOrNull.orEmpty()
    val system = data["system_prompt"]?.jsonPrimitiveOrNull?.contentOrNull
    val description = data["description"]?.jsonPrimitiveOrNull?.contentOrNull
    val personality = data["personality"]?.jsonPrimitiveOrNull?.contentOrNull
    val scenario = data["scenario"]?.jsonPrimitiveOrNull?.contentOrNull
    val characterData = parseCharacterData(data, sourceName)
    val regexes = parseRegexScripts(
        element = data["extensions"]?.jsonObjectOrNull()?.get("regex_scripts"),
        sourceName = name,
    )

    return AssistantImportPayload(
        kind = AssistantImportKind.CHARACTER_CARD,
        sourceName = sourceName,
        assistant = Assistant(
            name = name,
            avatar = Avatar.Dummy,
            systemPrompt = buildAssistantSystemPrompt(
                name = name,
                system = system,
                description = description,
                personality = personality,
                scenario = scenario,
            ),
            presetMessages = firstMessage
                .takeIf { it.isNotBlank() }
                ?.let { listOf(UIMessage.assistant(it)) }
                ?: emptyList(),
            stCharacterData = characterData,
        ),
        regexes = regexes,
        avatarImportSourceUri = avatarImportSourceUri,
    )
}

private fun buildAssistantSystemPrompt(
    name: String,
    system: String?,
    description: String?,
    personality: String?,
    scenario: String?,
): String {
    return buildString {
        appendLine("You are roleplaying as $name.")
        appendLine()
        if (!system.isNullOrBlank()) {
            appendLine(system)
            appendLine()
        }
        appendLine("## Description of the character")
        appendLine(description ?: "Empty")
        appendLine()
        appendLine("## Personality of the character")
        appendLine(personality ?: "Empty")
        appendLine()
        appendLine("## Scenario")
        append(scenario ?: "Empty")
    }
}

private fun parseCharacterData(
    data: JsonObject,
    sourceName: String,
): SillyTavernCharacterData {
    val extensions = data["extensions"]?.jsonObjectOrNull()
    val depthPrompt = extensions?.get("depth_prompt")?.jsonObjectOrNull()?.let { prompt ->
        StDepthPrompt(
            prompt = prompt["prompt"]?.jsonPrimitiveOrNull?.contentOrNull.orEmpty(),
            depth = prompt["depth"]?.jsonPrimitiveOrNull?.intOrNull ?: 4,
            role = prompt["role"]?.jsonPrimitiveOrNull?.contentOrNull.toMessageRole(),
        )
    }

    return SillyTavernCharacterData(
        sourceName = sourceName,
        name = data["name"]?.jsonPrimitiveOrNull?.contentOrNull.orEmpty(),
        version = data["character_version"]?.jsonPrimitiveOrNull?.contentOrNull.orEmpty(),
        description = data["description"]?.jsonPrimitiveOrNull?.contentOrNull.orEmpty(),
        personality = data["personality"]?.jsonPrimitiveOrNull?.contentOrNull.orEmpty(),
        scenario = data["scenario"]?.jsonPrimitiveOrNull?.contentOrNull.orEmpty(),
        systemPromptOverride = data["system_prompt"]?.jsonPrimitiveOrNull?.contentOrNull.orEmpty(),
        postHistoryInstructions = data["post_history_instructions"]?.jsonPrimitiveOrNull?.contentOrNull.orEmpty(),
        firstMessage = data["first_mes"]?.jsonPrimitiveOrNull?.contentOrNull.orEmpty(),
        exampleMessagesRaw = data["mes_example"]?.jsonPrimitiveOrNull?.contentOrNull.orEmpty(),
        alternateGreetings = data["alternate_greetings"]?.jsonArrayOrNull()
            ?.mapNotNull { it.jsonPrimitiveOrNull?.contentOrNull }
            ?: emptyList(),
        creatorNotes = data["creator_notes"]?.jsonPrimitiveOrNull?.contentOrNull.orEmpty(),
        depthPrompt = depthPrompt,
    )
}
