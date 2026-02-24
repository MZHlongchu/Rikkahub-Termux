package me.rerere.rikkahub.data.ai.tools.termux

import me.rerere.ai.ui.UIMessagePart

enum class TermuxDirectCommandSource {
    SlashPrefix,
    CommandMode,
}

data class TermuxDirectCommandParseResult(
    val isDirect: Boolean,
    val command: String = "",
    val source: TermuxDirectCommandSource? = null,
) {
    companion object {
        val None = TermuxDirectCommandParseResult(isDirect = false)
    }
}

object TermuxDirectCommandParser {
    private val SlashCommandRegex = Regex("^/termux(?:\\s+([\\s\\S]*))?$")

    fun parse(parts: List<UIMessagePart>, commandModeEnabled: Boolean): TermuxDirectCommandParseResult {
        val text = firstText(parts) ?: return TermuxDirectCommandParseResult.None
        return parseText(text, commandModeEnabled)
    }

    fun parseText(text: String, commandModeEnabled: Boolean): TermuxDirectCommandParseResult {
        val slashMatch = SlashCommandRegex.matchEntire(text)
        if (slashMatch != null) {
            return TermuxDirectCommandParseResult(
                isDirect = true,
                command = slashMatch.groupValues.getOrNull(1)?.trim().orEmpty(),
                source = if (commandModeEnabled) {
                    TermuxDirectCommandSource.CommandMode
                } else {
                    TermuxDirectCommandSource.SlashPrefix
                }
            )
        }

        if (!commandModeEnabled) return TermuxDirectCommandParseResult.None

        val command = text.trim()
        if (command.isBlank()) return TermuxDirectCommandParseResult.None

        return TermuxDirectCommandParseResult(
            isDirect = true,
            command = command,
            source = TermuxDirectCommandSource.CommandMode
        )
    }

    private fun firstText(parts: List<UIMessagePart>): String? {
        return (parts.firstOrNull { it is UIMessagePart.Text } as? UIMessagePart.Text)?.text
    }
}
