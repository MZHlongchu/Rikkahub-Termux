package me.rerere.rikkahub.data.ai.tools.termux

import me.rerere.rikkahub.utils.JsonInstant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TermuxCommandToolResponseTest {
    @Test
    fun `toToolResponse should preserve failure status`() {
        val payload = TermuxResult(
            stdout = "",
            stderr = "",
            exitCode = 1,
        ).toToolResponse().encode(JsonInstant)

        assertTrue(payload.contains("\"exit_code\":1"))
        assertTrue(payload.contains("\"success\":false"))
        assertTrue(payload.contains("\"error\":\"Exit code: 1\""))
    }

    @Test
    fun `toToolResponse should preserve timeout status`() {
        val payload = TermuxResult(
            stdout = "partial output",
            timedOut = true,
            errMsg = "Timed out after 5000ms",
        ).toToolResponse().encode(JsonInstant)

        assertTrue(payload.contains("\"timed_out\":true"))
        assertTrue(payload.contains("\"success\":false"))
        assertTrue(payload.contains("\"output\":\"partial output\""))
    }

    @Test
    fun `toToolResponse should expose truncation metadata`() {
        val payload = TermuxResult(
            stdout = "trimmed",
            stdoutOriginalLength = 42,
            exitCode = 0,
        ).toToolResponse()

        assertTrue(payload.truncated)
        assertTrue(payload.stdoutOriginalLength == 42)
    }

    @Test
    fun `throwable toToolResponse should keep json contract`() {
        val payload = IllegalStateException("missing permission")
            .toToolResponse("Install Termux first.")
            .encode(JsonInstant)

        assertTrue(payload.contains("\"success\":false"))
        assertTrue(payload.contains("\"error\":\"missing permission\\nInstall Termux first.\""))
        assertFalse(payload.contains("\"output\":\"missing permission\""))
    }
}
