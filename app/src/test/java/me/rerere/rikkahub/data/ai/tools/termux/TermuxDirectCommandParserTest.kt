package me.rerere.rikkahub.data.ai.tools.termux

import me.rerere.ai.ui.UIMessagePart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TermuxDirectCommandParserTest {
    @Test
    fun `slash command in normal mode is detected`() {
        val result = TermuxDirectCommandParser.parse(
            parts = listOf(UIMessagePart.Text("/termux ls -la")),
            commandModeEnabled = false
        )

        assertTrue(result.isDirect)
        assertEquals("ls -la", result.command)
        assertEquals(TermuxDirectCommandSource.SlashPrefix, result.source)
    }

    @Test
    fun `slash command without body is detected as empty command`() {
        val result = TermuxDirectCommandParser.parse(
            parts = listOf(UIMessagePart.Text("/termux")),
            commandModeEnabled = false
        )

        assertTrue(result.isDirect)
        assertTrue(result.command.isBlank())
    }

    @Test
    fun `non-leading slash command is ignored in normal mode`() {
        val result = TermuxDirectCommandParser.parse(
            parts = listOf(UIMessagePart.Text("hello /termux pwd")),
            commandModeEnabled = false
        )

        assertFalse(result.isDirect)
    }

    @Test
    fun `command mode treats plain text as command`() {
        val result = TermuxDirectCommandParser.parse(
            parts = listOf(UIMessagePart.Text("pwd")),
            commandModeEnabled = true
        )

        assertTrue(result.isDirect)
        assertEquals("pwd", result.command)
        assertEquals(TermuxDirectCommandSource.CommandMode, result.source)
    }

    @Test
    fun `command mode strips slash prefix when provided`() {
        val result = TermuxDirectCommandParser.parse(
            parts = listOf(UIMessagePart.Text("/termux echo hi")),
            commandModeEnabled = true
        )

        assertTrue(result.isDirect)
        assertEquals("echo hi", result.command)
        assertEquals(TermuxDirectCommandSource.CommandMode, result.source)
    }
}
