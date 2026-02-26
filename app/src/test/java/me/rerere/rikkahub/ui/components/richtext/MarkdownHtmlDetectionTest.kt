package me.rerere.rikkahub.ui.components.richtext

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownHtmlDetectionTest {
    @Test
    fun htmlDocumentMarkersAreDetected() {
        assertTrue(isFrontendHtmlContent("<!doctype html><html><body>hello</body></html>"))
        assertTrue(isFrontendHtmlContent("<head><title>Test</title></head>"))
        assertTrue(isFrontendHtmlContent("<body><div>content</div></body>"))
    }

    @Test
    fun plainCodeIsNotDetectedAsFrontendHtml() {
        assertFalse(isFrontendHtmlContent("console.log('hello world');"))
        assertFalse(isFrontendHtmlContent("SELECT * FROM users;"))
    }
}
