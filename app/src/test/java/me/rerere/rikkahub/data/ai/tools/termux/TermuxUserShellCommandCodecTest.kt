package me.rerere.rikkahub.data.ai.tools.termux

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
}
