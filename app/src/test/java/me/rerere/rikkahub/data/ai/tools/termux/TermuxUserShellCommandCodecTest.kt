package me.rerere.rikkahub.data.ai.tools.termux

import me.rerere.ai.core.MessageRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TermuxUserShellCommandCodecTest {
    @Test
    fun `wrap and unwrap should keep multiline payload`() {
        val payload = "line1\nline2\nline3"
        val wrapped = TermuxUserShellCommandCodec.wrap(payload)

        assertTrue(TermuxUserShellCommandCodec.isWrapped(wrapped))
        assertEquals(payload, TermuxUserShellCommandCodec.unwrap(wrapped))
    }

    @Test
    fun `wrap and unwrap should preserve trailing newline`() {
        val payload = "line1\nline2\n"
        val wrapped = TermuxUserShellCommandCodec.wrap(payload)

        assertTrue(TermuxUserShellCommandCodec.isWrapped(wrapped))
        assertEquals(payload, TermuxUserShellCommandCodec.unwrap(wrapped))
    }

    @Test
    fun `wrap and unwrap should handle empty payload`() {
        val wrapped = TermuxUserShellCommandCodec.wrap("")

        assertTrue(TermuxUserShellCommandCodec.isWrapped(wrapped))
        assertEquals("", TermuxUserShellCommandCodec.unwrap(wrapped))
    }

    @Test
    fun `unwrap should return null for malformed message`() {
        assertNull(TermuxUserShellCommandCodec.unwrap("plain text"))
        assertNull(TermuxUserShellCommandCodec.unwrap("<user_shell_command>abc</user_shell_command>"))
        assertNull(TermuxUserShellCommandCodec.unwrap("<user_shell_command>\nabc"))
        assertFalse(TermuxUserShellCommandCodec.isWrapped("<user_shell_command>\nabc"))
    }

    @Test
    fun `extractOutput should read metadata marked part for user role`() {
        val part = TermuxUserShellCommandCodec.createTextPart("echo hello")

        assertTrue(TermuxUserShellCommandCodec.isWrapped(part.text))
        assertEquals(
            "echo hello",
            TermuxUserShellCommandCodec.extractOutput(MessageRole.USER, part)
        )
    }

    @Test
    fun `extractOutput should support legacy metadata marked plain text`() {
        val legacyPart = TermuxUserShellCommandCodec.createTextPart("echo hello").copy(text = "echo hello")

        assertEquals(
            "echo hello",
            TermuxUserShellCommandCodec.extractOutput(MessageRole.USER, legacyPart)
        )
    }

    @Test
    fun `createTextPart should trim one trailing line break from payload`() {
        val part = TermuxUserShellCommandCodec.createTextPart("line1\nline2\n")

        assertEquals(
            "line1\nline2",
            TermuxUserShellCommandCodec.extractOutput(MessageRole.USER, part)
        )
    }

    @Test
    fun `createTextPart should trim one trailing CRLF from payload`() {
        val part = TermuxUserShellCommandCodec.createTextPart("line1\r\n")

        assertEquals(
            "line1",
            TermuxUserShellCommandCodec.extractOutput(MessageRole.USER, part)
        )
    }

    @Test
    fun `extractOutput should not parse assistant role even with wrapped text`() {
        val wrapped = TermuxUserShellCommandCodec.wrap("echo hello")
        val textPart = me.rerere.ai.ui.UIMessagePart.Text(wrapped)

        assertNull(TermuxUserShellCommandCodec.extractOutput(MessageRole.ASSISTANT, textPart))
    }
}
