package me.rerere.rikkahub.data.ai.tools.termux

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessagePart

object TermuxUserShellCommandCodec {
    const val StartTag = "<user_shell_command>"
    const val EndTag = "</user_shell_command>"
    private const val MetadataKey = "rikkahub.user_shell_command"

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

    fun createTextPart(payload: String): UIMessagePart.Text {
        return UIMessagePart.Text(
            text = payload,
            metadata = buildJsonObject {
                put(MetadataKey, JsonPrimitive(true))
            }
        )
    }

    fun extractOutput(role: MessageRole, textPart: UIMessagePart.Text): String? {
        if (role != MessageRole.USER) return null
        val markedShellOutput = textPart.metadata
            ?.get(MetadataKey)
            ?.jsonPrimitive
            ?.booleanOrNull == true
        if (markedShellOutput) {
            return textPart.text
        }
        return unwrap(textPart.text)
    }
}
