package me.rerere.rikkahub.ui.components.richtext

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserHtmlBlockDocumentTest {
    @Test
    fun htmlFragmentInjectsBridgeAssets() {
        val rendered = buildBrowserHtmlDocument(
            """
                <div style="min-height:100vh">
                  <a href="https://example.com">Example</a>
                </div>
            """.trimIndent()
        )

        assertTrue(rendered.contains("id=\"rikkahub-html-style\""))
        assertTrue(rendered.contains("id=\"rikkahub-html-bridge\""))
        assertTrue(rendered.contains("window.__rikkahubReportHeight"))
        assertTrue(rendered.contains("<body>"))
    }

    @Test
    fun fullDocumentKeepsOriginalContentAndInjectsAssets() {
        val rendered = buildBrowserHtmlDocument(
            """
                <!doctype html>
                <html>
                  <head>
                    <title>Hello</title>
                  </head>
                  <body>
                    <main>content</main>
                  </body>
                </html>
            """.trimIndent()
        )

        assertTrue(rendered.contains("<title>Hello</title>"))
        assertTrue(rendered.contains("<main>content</main>"))
        assertTrue(rendered.contains("id=\"rikkahub-html-style\""))
        assertTrue(rendered.contains("id=\"rikkahub-html-bridge\""))
    }

    @Test
    fun wrappedHtmlDoesNotDuplicateBridgeAssets() {
        val firstPass = buildBrowserHtmlDocument("<section>hello</section>")
        val secondPass = buildBrowserHtmlDocument(firstPass)

        assertEquals(1, "id=\"rikkahub-html-style\"".toRegex().findAll(secondPass).count())
        assertEquals(1, "id=\"rikkahub-html-bridge\"".toRegex().findAll(secondPass).count())
    }
}
