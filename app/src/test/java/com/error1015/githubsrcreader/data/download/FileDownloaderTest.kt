package com.error1015.githubsrcreader.data.download

import org.junit.Assert.assertEquals
import org.junit.Test

class FileDownloaderTest {

    @Test
    fun `keeps original directory structure`() {
        assertEquals(
            "Github Src Reader/error1015-GithubSrcReader/app/src/main/",
            FileDownloader.relativeDirectory(
                folder = "error1015-GithubSrcReader",
                path = "app/src/main/Main.kt",
            ),
        )
    }

    @Test
    fun `root level file has no extra directory`() {
        assertEquals(
            "Github Src Reader/error1015-GithubSrcReader/",
            FileDownloader.relativeDirectory("error1015-GithubSrcReader", "README.md"),
        )
    }

    @Test
    fun `hidden directories keep their dot`() {
        assertEquals(
            "Github Src Reader/repo/.github/workflows/",
            FileDownloader.relativeDirectory("repo", ".github/workflows/ci.yml"),
        )
    }

    @Test
    fun `illegal characters and dot segments are removed`() {
        assertEquals(
            "Github Src Reader/owner_repo/a_b/",
            FileDownloader.relativeDirectory("owner/repo", "a:b/../file.kt"),
        )
    }

    @Test
    fun `blank folder falls back to path only`() {
        assertEquals(
            "Github Src Reader/src/",
            FileDownloader.relativeDirectory("   ", "src/App.kt"),
        )
    }
}
