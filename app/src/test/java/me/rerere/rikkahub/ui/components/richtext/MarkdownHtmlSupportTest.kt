package me.rerere.rikkahub.ui.components.richtext

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownHtmlSupportTest {
    private val parser = MarkdownParser(
        GFMFlavourDescriptor(
            makeHttpsAutoLinks = true,
            useSafeLinks = true
        )
    )

    @Test
    fun paragraph_with_inline_html_is_rendered_as_simple_html() {
        val paragraph = parseFirstParagraph("<span style=\"display:none;\">secret</span>")

        assertTrue(shouldRenderParagraphWithSimpleHtml(paragraph))
    }

    @Test
    fun paragraph_with_plain_text_and_inline_html_is_rendered_as_simple_html() {
        val paragraph = parseFirstParagraph("before <span style=\"color:red;\">red</span> after")

        assertTrue(shouldRenderParagraphWithSimpleHtml(paragraph))
    }

    @Test
    fun paragraph_with_markdown_syntax_and_inline_html_is_not_forced_to_simple_html() {
        val paragraph = parseFirstParagraph("[link](https://example.com) <span>tail</span>")

        assertFalse(shouldRenderParagraphWithSimpleHtml(paragraph))
    }

    @Test
    fun paragraph_without_inline_html_is_not_rendered_as_simple_html() {
        val paragraph = parseFirstParagraph("plain markdown text")

        assertFalse(shouldRenderParagraphWithSimpleHtml(paragraph))
    }

    @Test
    fun html_document_renderer_is_disabled_while_streaming() {
        assertFalse(
            shouldRenderMarkdownWithHtmlRenderer(
                hasHtmlBlocks = true,
                streaming = true,
            )
        )
    }

    @Test
    fun html_document_renderer_is_enabled_for_finished_html_blocks() {
        assertTrue(
            shouldRenderMarkdownWithHtmlRenderer(
                hasHtmlBlocks = true,
                streaming = false,
            )
        )
    }

    @Test
    fun streaming_html_block_uses_html_code_language() {
        assertEquals(
            "html",
            streamingHtmlBlockLanguage("<div>loading</div>")
        )
    }

    @Test
    fun streaming_svg_block_uses_svg_code_language() {
        assertEquals(
            "svg",
            streamingHtmlBlockLanguage("""<?xml version="1.0"?><svg width="10"></svg>""")
        )
    }

    private fun parseFirstParagraph(markdown: String): ASTNode {
        val ast = parser.buildMarkdownTreeFromString(markdown)
        return ast.children.firstOrNull { it.type == MarkdownElementTypes.PARAGRAPH }
            ?: error("No paragraph node found")
    }
}
