package me.rerere.rikkahub.data.skills

import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ModelAbility
import me.rerere.rikkahub.data.model.Assistant

internal fun shouldInjectSkillsCatalog(
    assistant: Assistant,
    model: Model,
): Boolean {
    return assistant.skillsEnabled &&
        assistant.selectedSkills.isNotEmpty() &&
        model.abilities.contains(ModelAbility.TOOL)
}

internal fun buildSkillsCatalogPrompt(
    assistant: Assistant,
    model: Model,
    catalog: SkillsCatalogState,
): String? {
    if (!shouldInjectSkillsCatalog(assistant, model)) return null
    if (catalog.rootPath.isBlank()) return null

    val selectedEntries = catalog.entries
        .filter { it.directoryName in assistant.selectedSkills }
        .sortedBy { it.directoryName }

    if (selectedEntries.isEmpty()) return null

    return buildString {
        appendLine("Local skills are available in the Termux workdir.")
        appendLine("Skills root: ${catalog.rootPath}")
        appendLine("Each skill is a directory package. Only inspect a skill when it is relevant to the user's request.")
        appendLine("Do not load every skill preemptively.")
        appendLine(
            "When a skill is relevant, call use_skill with its directory or name before following that skill's workflow."
        )
        appendLine("The use_skill result contains the skill's SKILL.md and a bounded file listing.")
        appendLine()
        appendLine("Available skills:")
        selectedEntries.forEach { skill ->
            appendLine("- directory: ${skill.directoryName}")
            appendLine("  name: ${skill.name}")
            appendLine("  description: ${skill.description}")
            appendLine("  path: ${skill.path}")
        }
    }.trim()
}
