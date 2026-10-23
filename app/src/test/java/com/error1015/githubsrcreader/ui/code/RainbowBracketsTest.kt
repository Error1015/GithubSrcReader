package com.error1015.githubsrcreader.ui.code

import com.error1015.githubsrcreader.data.model.Language
import org.junit.Assert.assertEquals
import org.junit.Test

class RainbowBracketsTest {

    /** 把扫描结果转换成 "括号字符 -> 层级" 列表，便于断言。 */
    private fun levels(text: String, tokens: List<CodeToken> = emptyList()): List<Pair<String, Int>> =
        RainbowBrackets.spans(text, tokens).map { text.substring(it.start, it.end) to it.level }

    private fun codeLevels(code: String, language: Language): List<Pair<String, Int>> =
        levels(code, CodeTokenizer.tokenize(code, language))

    @Test
    fun `nested brackets increase and match`() {
        val text = "f(a[b])"
        assertEquals(
            listOf("(" to 0, "[" to 1, "]" to 1, ")" to 0),
            levels(text),
        )
    }

    @Test
    fun `braces pair up level by level`() {
        val text = "{ { } }"
        assertEquals(
            listOf("{" to 0, "{" to 1, "}" to 1, "}" to 0),
            levels(text),
        )
    }

    @Test
    fun `brackets inside strings are ignored`() {
        val code = """val s = "([{" + other"""
        assertEquals(emptyList<Pair<String, Int>>(), codeLevels(code, Language.KOTLIN))
    }

    @Test
    fun `brackets inside comments are ignored`() {
        val code = """
            // (a) [b] {c}
            fun run() = 1
        """.trimIndent()
        assertEquals(
            listOf("(" to 0, ")" to 0),
            codeLevels(code, Language.KOTLIN),
        )
    }

    @Test
    fun `extra closing bracket does not go negative`() {
        val text = "})"
        assertEquals(listOf("}" to 0, ")" to 0), levels(text))
    }

    @Test
    fun `empty text yields nothing`() {
        assertEquals(emptyList<Pair<String, Int>>(), levels(""))
    }
}
