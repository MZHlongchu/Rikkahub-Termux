package me.rerere.rikkahub.ui.components.richtext

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownSvgDetectionTest {
    @Test
    fun standaloneSvgIsDetected() {
        val code = """
            <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32">
              <circle cx="16" cy="16" r="12" />
            </svg>
        """.trimIndent()

        assertTrue(isStandaloneSvgDocument(code))
    }

    @Test
    fun standaloneSvgWithXmlDeclarationAndDoctypeIsDetected() {
        val code = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN"
              "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
            <svg xmlns="http://www.w3.org/2000/svg"></svg>
        """.trimIndent()

        assertTrue(isStandaloneSvgDocument(code))
    }

    @Test
    fun standaloneSvgWithDoctypeInternalSubsetIsDetected() {
        val code = """
            <!DOCTYPE svg [
              <!ENTITY ns "http://www.w3.org/2000/svg">
            ]>
            <svg xmlns="&ns;"></svg>
        """.trimIndent()

        assertTrue(isStandaloneSvgDocument(code))
    }

    @Test
    fun xhtmlWithInlineSvgIsNotDetectedAsStandaloneSvg() {
        val code = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml">
              <body>
                <svg xmlns="http://www.w3.org/2000/svg"></svg>
              </body>
            </html>
        """.trimIndent()

        assertFalse(isStandaloneSvgDocument(code))
    }

    @Test
    fun xmlWithoutSvgRootIsNotDetectedAsStandaloneSvg() {
        val code = """
            <note>
              <to>Alice</to>
            </note>
        """.trimIndent()

        assertFalse(isStandaloneSvgDocument(code))
    }
}
