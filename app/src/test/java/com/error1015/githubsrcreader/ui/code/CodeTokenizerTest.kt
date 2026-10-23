package com.error1015.githubsrcreader.ui.code

import com.error1015.githubsrcreader.data.model.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeTokenizerTest {

    private fun tokenTexts(code: String, language: Language): List<String> =
        CodeTokenizer.tokenize(code, language).map { code.substring(it.start, it.end) }

    @Test
    fun `kotlin keywords strings and comments`() {
        val code = """
            // comment
            fun main() {
                val name = "hello"
            }
        """.trimIndent()
        val texts = tokenTexts(code, Language.KOTLIN)
        assertTrue(texts.contains("// comment"))
        assertTrue(texts.contains("fun"))
        assertTrue(texts.contains("val"))
        assertTrue(texts.contains("\"hello\""))
        assertTrue(texts.contains("main"))
    }

    @Test
    fun `python hash comment`() {
        val code = "# note\ndef run():\n    return 1"
        val tokens = CodeTokenizer.tokenize(code, Language.PYTHON)
        val comment = tokens.first { it.kind == TokenKind.COMMENT }
        assertEquals("# note", code.substring(comment.start, comment.end))
    }

    @Test
    fun `json keys are properties`() {
        val code = """{"name": 1, "ok": true}"""
        val tokens = CodeTokenizer.tokenize(code, Language.JSON)
        val properties = tokens.filter { it.kind == TokenKind.PROPERTY }.map { code.substring(it.start, it.end) }
        assertEquals(listOf("\"name\"", "\"ok\""), properties)
        assertTrue(tokens.any { it.kind == TokenKind.KEYWORD && code.substring(it.start, it.end) == "true" })
    }

    @Test
    fun `tokens never overlap`() {
        val code = """
            /* block */
            class A<T>(val x: String = "a\"b") { fun y() = 0x1F }
        """.trimIndent()
        val tokens = CodeTokenizer.tokenize(code, Language.KOTLIN)
        var lastEnd = 0
        tokens.forEach { token ->
            assertTrue("token start ${token.start} < $lastEnd", token.start >= lastEnd)
            assertTrue(token.end > token.start)
            assertTrue(token.end <= code.length)
            lastEnd = token.end
        }
    }
}
