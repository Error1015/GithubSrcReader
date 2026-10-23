package com.error1015.githubsrcreader.data.github

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubUrlParserTest {

    @Test
    fun `repo root`() {
        val parsed = GitHubUrlParser.parse("https://github.com/JetBrains/kotlin")!!
        assertEquals("JetBrains", parsed.owner)
        assertEquals("kotlin", parsed.repo)
        assertEquals(1, parsed.candidates.size)
        assertNull(parsed.candidates.first().ref)
        assertEquals("", parsed.candidates.first().path)
        assertFalse(parsed.pointsToFile)
    }

    @Test
    fun `tree with directory`() {
        val parsed = GitHubUrlParser.parse("https://github.com/JetBrains/kotlin/tree/master/compiler/cli")!!
        assertEquals("kotlin", parsed.repo)
        assertEquals("master", parsed.candidates.first().ref)
        assertEquals("compiler/cli", parsed.candidates.first().path)
        assertFalse(parsed.pointsToFile)
    }

    @Test
    fun `blob url points to file`() {
        val parsed = GitHubUrlParser.parse("https://github.com/a/b/blob/main/app/src/Main.kt")!!
        assertTrue(parsed.pointsToFile)
        assertEquals("main", parsed.candidates.first().ref)
        assertEquals("app/src/Main.kt", parsed.candidates.first().path)
    }

    @Test
    fun `branch names containing slashes produce candidates`() {
        val parsed = GitHubUrlParser.parse("https://github.com/a/b/tree/feature/foo/app")!!
        val refs = parsed.candidates.map { it.ref }
        assertEquals(listOf("feature", "feature/foo", "feature/foo/app"), refs)
        assertEquals("app", parsed.candidates[1].path)
    }

    @Test
    fun `raw url`() {
        val parsed = GitHubUrlParser.parse("https://raw.githubusercontent.com/a/b/main/README.md")!!
        assertTrue(parsed.pointsToFile)
        assertEquals("main", parsed.candidates.first().ref)
        assertEquals("README.md", parsed.candidates.first().path)
    }

    @Test
    fun `shorthand and git suffix`() {
        val parsed = GitHubUrlParser.parse("a/b.git")!!
        assertEquals("a", parsed.owner)
        assertEquals("b", parsed.repo)
    }

    @Test
    fun `non github host is rejected`() {
        assertNull(GitHubUrlParser.parse("https://gitlab.com/a/b"))
        assertNull(GitHubUrlParser.parse("not a url"))
    }
}
