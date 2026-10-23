package com.error1015.githubsrcreader.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalFilesTest {

    @Test
    fun `image extensions are detected case insensitively`() {
        assertTrue(ExternalFiles.isImage("logo.png"))
        assertTrue(ExternalFiles.isImage("photo.JPG"))
        assertTrue(ExternalFiles.isImage("anim.gif"))
        assertTrue(ExternalFiles.isImage("shot.webp"))
        assertTrue(ExternalFiles.isImage("IMG_0001.heic"))
    }

    @Test
    fun `code and extensionless files are not images`() {
        assertFalse(ExternalFiles.isImage("Main.kt"))
        assertFalse(ExternalFiles.isImage("build.gradle.kts"))
        assertFalse(ExternalFiles.isImage("README"))
        assertFalse(ExternalFiles.isImage("LICENSE.md"))
    }

    @Test
    fun `mime types follow the extension`() {
        assertEquals("image/png", ExternalFiles.mimeTypeOf("a.png"))
        assertEquals("image/jpeg", ExternalFiles.mimeTypeOf("a.jpeg"))
        assertEquals("image/gif", ExternalFiles.mimeTypeOf("a.gif"))
        assertEquals("image/webp", ExternalFiles.mimeTypeOf("a.WEBP"))
        assertEquals("image/heic", ExternalFiles.mimeTypeOf("a.heif"))
        assertEquals("image/*", ExternalFiles.mimeTypeOf("a.bin"))
    }

    /** 只认系统表：能查到就用查到的，查不到必须回退 octet-stream，否则系统会给文件名补扩展名。 */
    private val systemMime: (String) -> String? = { ext ->
        when (ext) {
            "txt" -> "text/plain"
            "md" -> "text/markdown"
            "png" -> "image/png"
            else -> null
        }
    }

    @Test
    fun `save mime follows the system table`() {
        assertEquals("text/plain", ExternalFiles.mimeForSave("notes.txt", systemMime))
        assertEquals("text/markdown", ExternalFiles.mimeForSave("README.md", systemMime))
        assertEquals("image/png", ExternalFiles.mimeForSave("logo.PNG", systemMime))
    }

    @Test
    fun `unknown extensions fall back so the original name survives`() {
        assertEquals("application/octet-stream", ExternalFiles.mimeForSave("Main.kt", systemMime))
        assertEquals("application/octet-stream", ExternalFiles.mimeForSave("build.gradle.kts", systemMime))
        assertEquals("application/octet-stream", ExternalFiles.mimeForSave("Dockerfile", systemMime))
    }
}
