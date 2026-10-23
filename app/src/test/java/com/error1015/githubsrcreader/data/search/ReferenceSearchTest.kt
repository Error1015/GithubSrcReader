package com.error1015.githubsrcreader.data.search

import com.error1015.githubsrcreader.data.model.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferenceSearchTest {

    private val kotlinCode = """
        class Repo {
            fun findUser(id: String): User? = cache[id]

            fun load() {
                val user = findUser("1")
                // findUser 在注释里，不应算引用
                log("findUser")
            }
        }
    """.trimIndent()

    @Test
    fun `comments and strings are skipped`() {
        val hits = ReferenceSearch.searchInFile(kotlinCode, "Repo.kt", "findUser")
        assertEquals(2, hits.size)
        assertTrue(hits[0].isDeclaration)
        assertFalse(hits[1].isDeclaration)
        assertEquals(2, hits[0].line)
        assertEquals(5, hits[1].line)
    }

    @Test
    fun `declarations are extracted for the symbol list`() {
        val symbols = ReferenceSearch.declarationsInText(kotlinCode, Language.KOTLIN, "Repo.kt")
        val names = symbols.map { it.lineText.trim() }
        assertTrue(symbols.isNotEmpty())
        assertTrue(symbols.any { it.line == 1 && it.lineText.contains("Repo") })
        assertTrue(symbols.any { it.line == 2 && it.lineText.contains("findUser") })
        assertTrue(symbols.any { it.line == 4 && it.lineText.contains("load") })
        assertTrue(names.isNotEmpty())
    }

    @Test
    fun `word at offset extracts identifier`() {
        val text = "val user = findUser(id)"
        val start = text.indexOf("findUser")
        assertEquals("findUser", ReferenceSearch.wordAt(text, start + 3))
        assertEquals("findUser", ReferenceSearch.wordAt(text, start))
        assertEquals("user", ReferenceSearch.wordAt(text, text.indexOf("user") + 1))
        // 落在空白上时回退到前一个单词
        assertEquals("val", ReferenceSearch.wordAt(text, 3))
        assertNull(ReferenceSearch.wordAt("   x", 0))
    }

    @Test
    fun `word boundary prevents partial matches`() {
        val text = "findUserById(); findUser()"
        val hits = ReferenceSearch.searchInText(text, "findUser", Language.KOTLIN, "a.kt")
        assertEquals(1, hits.size)
        assertEquals(17, hits.first().column)
    }
}
