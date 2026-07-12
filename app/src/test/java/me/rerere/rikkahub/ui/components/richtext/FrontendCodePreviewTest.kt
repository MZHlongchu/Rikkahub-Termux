package me.rerere.rikkahub.ui.components.richtext

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrontendCodePreviewTest {
    @Test
    fun `recognizes frontend code by content instead of fence language`() {
        assertTrue(isFrontendCodeBlock("<!DOCTYPE html><div>content</div>"))
        assertTrue(isFrontendCodeBlock("<head><title>Preview</title></head>"))
        assertTrue(isFrontendCodeBlock("<body class=\"app\">content</body>"))
    }

    @Test
    fun `does not recognize html fragments without a frontend marker`() {
        assertFalse(isFrontendCodeBlock("<div>content</div>"))
        assertFalse(isFrontendCodeBlock("<svg viewBox=\"0 0 10 10\"></svg>"))
        assertFalse(isFrontendCodeBlock("<HTML><BODY>content</BODY></HTML>"))
    }

    @Test
    fun `recognizes svg content labeled as svg or xml`() {
        assertTrue(isPreviewableFrontendCodeBlock("<svg></svg>", "svg"))
        assertTrue(isPreviewableFrontendCodeBlock("<svg viewBox=\"0 0 10 10\"></svg>", "XML"))
        assertFalse(isPreviewableFrontendCodeBlock("<root />", "xml"))
        assertFalse(isPreviewableFrontendCodeBlock("<div>content</div>", "html"))
    }

    @Test
    fun `wraps frontend code in an isolated document`() {
        val html = buildFrontendPreviewHtml(
            code = "<body><main>content</main></body>",
            viewportHeightCssPx = 720f,
        )

        assertTrue(html.startsWith("<!DOCTYPE html>"))
        assertTrue(html.contains("<meta name=\"viewport\""))
        assertTrue(html.contains(":root{--rikkahub-viewport-height:720px;}"))
        assertTrue(html.contains("window.RikkaHubPreviewHeight?.reportHeight(height, viewportHeight)"))
        assertTrue(html.contains("<body>\n<body><main>content</main></body>"))
    }

    @Test
    fun `rewrites min height vh values against the host viewport`() {
        val html = buildFrontendPreviewHtml(
            code = """
                <style>.full { min-height: 100vh; }</style>
                <div style="min-height: 50vh"></div>
                <script>
                element.style.minHeight = '25vh';
                element.style.setProperty('min-height', '75vh');
                </script>
            """.trimIndent(),
            viewportHeightCssPx = 800f,
        )

        assertTrue(html.contains("min-height: var(--rikkahub-viewport-height)"))
        assertTrue(html.contains("min-height: calc(var(--rikkahub-viewport-height) * 0.5)"))
        assertTrue(html.contains(".style.minHeight = 'calc(var(--rikkahub-viewport-height) * 0.25)'"))
        assertTrue(
            html.contains(
                "setProperty('min-height', 'calc(var(--rikkahub-viewport-height) * 0.75)')"
            )
        )
    }

    @Test
    fun `keeps fullscreen previews scrollable without a height bridge`() {
        val html = buildFrontendPreviewHtml(
            code = "<body>content</body>",
            viewportHeightCssPx = 720f,
            autoHeight = false,
        )

        assertTrue(html.contains("overflow:auto!important"))
        assertFalse(html.contains("RikkaHubPreviewHeight"))
    }

    @Test
    fun `keeps host height when content fills the web viewport`() {
        assertEquals(
            500f,
            calculatePreviewHeightDp(
                contentHeightCssPx = 375f,
                viewportHeightCssPx = 375f,
                currentHeightDp = 500f,
            )!!,
            0f,
        )
    }

    @Test
    fun `converts content height using the current web viewport scale`() {
        assertEquals(
            800f,
            calculatePreviewHeightDp(
                contentHeightCssPx = 600f,
                viewportHeightCssPx = 300f,
                currentHeightDp = 400f,
            )!!,
            0f,
        )
    }

    @Test
    fun `rejects invalid preview measurements`() {
        assertNull(
            calculatePreviewHeightDp(
                contentHeightCssPx = 500f,
                viewportHeightCssPx = 0f,
                currentHeightDp = 500f,
            )
        )
    }
}
