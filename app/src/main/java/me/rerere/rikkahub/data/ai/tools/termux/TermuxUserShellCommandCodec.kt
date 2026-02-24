package me.rerere.rikkahub.data.ai.tools.termux

object TermuxUserShellCommandCodec {
    const val StartTag = "<user_shell_command>"
    const val EndTag = "</user_shell_command>"

    private val WrappedRegex = Regex(
        pattern = "^<user_shell_command>\\r?\\n([\\s\\S]*?)</user_shell_command>$"
    )

    fun wrap(payload: String): String {
        return buildString {
            append(StartTag)
            append('\n')
            append(payload)
            append('\n')
            append(EndTag)
        }
    }

    fun unwrap(text: String): String? {
        val match = WrappedRegex.matchEntire(text) ?: return null
        return match.groupValues[1].removeSuffix("\n")
    }

    fun isWrapped(text: String): Boolean {
        return WrappedRegex.matches(text)
    }
}
