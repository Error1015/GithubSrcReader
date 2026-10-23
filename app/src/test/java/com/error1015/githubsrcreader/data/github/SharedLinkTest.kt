package com.error1015.githubsrcreader.data.github

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SharedLinkTest {

    @Test
    fun `url inside chinese sentence`() {
        val text = "看看这个 https://github.com/error1015/GithubSrcReader 挺好用的"
        assertEquals("https://github.com/error1015/GithubSrcReader", SharedLink.firstLink(text))
    }

    @Test
    fun `markdown link is unwrapped`() {
        val text = "推荐 [GithubSrcReader](https://github.com/error1015/GithubSrcReader)"
        assertEquals("https://github.com/error1015/GithubSrcReader", SharedLink.firstLink(text))
    }

    @Test
    fun `trailing chinese punctuation is trimmed`() {
        assertEquals(
            "https://github.com/a/b",
            SharedLink.firstLink("https://github.com/a/b。"),
        )
        assertEquals(
            "https://github.com/a/b/tree/main",
            SharedLink.firstLink("路径见 https://github.com/a/b/tree/main，谢谢"),
        )
    }

    @Test
    fun `bare owner repo works when it is the whole text`() {
        assertEquals("error1015/GithubSrcReader", SharedLink.firstLink("  error1015/GithubSrcReader "))
    }

    @Test
    fun `scheme-less github url inside text`() {
        assertEquals(
            "github.com/a/b/tree/main",
            SharedLink.firstLink("仓库在 github.com/a/b/tree/main 这里"),
        )
    }

    @Test
    fun `query and hash are accepted`() {
        assertEquals(
            "https://github.com/a/b/blob/main/README.md?plain=1#L1",
            SharedLink.firstLink("https://github.com/a/b/blob/main/README.md?plain=1#L1"),
        )
    }

    @Test
    fun `git address is supported`() {
        assertEquals(
            "git@github.com:error1015/GithubSrcReader.git",
            SharedLink.firstLink("git@github.com:error1015/GithubSrcReader.git"),
        )
    }

    @Test
    fun `first valid link wins`() {
        val text = "旧 https://github.com/a/b 新 https://github.com/c/d"
        assertEquals("https://github.com/a/b", SharedLink.firstLink(text))
    }

    @Test
    fun `non github content returns null`() {
        assertNull(SharedLink.firstLink("今天天气不错"))
        assertNull(SharedLink.firstLink("https://example.com/foo/bar"))
        assertNull(SharedLink.firstLink(""))
    }
}
