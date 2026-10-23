package com.error1015.githubsrcreader.ui.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * 处理需要交给系统应用打开的文件（目前是图片）：
 * 判断类型、给出 MIME，远程内容先写缓存再用 FileProvider 暴露成 content://。
 */
object ExternalFiles {

    private const val CACHE_DIR = "external"
    private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L
    private val UNSAFE = Regex("[^A-Za-z0-9._-]")

    private val IMAGE_EXTENSIONS =
        setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "heic", "heif", "avif")

    /** 下载保存时按文本处理的扩展名（代码、配置、文档）。 */
    private val TEXT_EXTENSIONS = setOf(
        "txt", "md", "markdown", "json", "xml", "yaml", "yml", "toml", "ini", "conf",
        "properties", "gradle", "kts", "kt", "java", "py", "js", "mjs", "ts", "tsx", "jsx",
        "c", "h", "cpp", "hpp", "cc", "cs", "go", "rs", "rb", "php", "swift", "dart", "lua",
        "sh", "bash", "bat", "ps1", "sql", "html", "htm", "css", "scss", "vue", "svelte",
        "pro", "gitignore", "editorconfig", "cmake", "dockerfile",
    )

    fun isImage(fileName: String): Boolean = extensionOf(fileName) in IMAGE_EXTENSIONS

    fun mimeTypeOf(fileName: String): String = when (extensionOf(fileName)) {
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "bmp" -> "image/bmp"
        "heic", "heif" -> "image/heic"
        "avif" -> "image/avif"
        else -> "image/*"
    }

    /** 保存到下载目录时使用的 MIME，避免全落成 application/octet-stream。 */
    fun mimeForSave(fileName: String): String {
        val ext = extensionOf(fileName)
        return when {
            ext in IMAGE_EXTENSIONS -> mimeTypeOf(fileName)
            ext in TEXT_EXTENSIONS -> "text/plain"
            ext == "pdf" -> "application/pdf"
            ext == "zip" -> "application/zip"
            ext == "apk" -> "application/vnd.android.package-archive"
            else -> "application/octet-stream"
        }
    }

    /**
     * 把远程文件写入缓存并返回可分享的 content:// Uri。
     * 每次打开顺带清理 24 小时前的旧文件，避免缓存无限增长。
     */
    fun writeCache(context: Context, fileName: String, bytes: ByteArray): Uri {
        val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
        val deadline = System.currentTimeMillis() - CACHE_TTL_MS
        dir.listFiles()?.forEach { file ->
            if (file.lastModified() < deadline) file.delete()
        }
        val safeName = fileName.replace(UNSAFE, "_").ifBlank { "image" }
        val file = File(dir, "${System.currentTimeMillis()}_$safeName")
        file.writeBytes(bytes)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun extensionOf(fileName: String): String =
        fileName.substringAfterLast('.', "").lowercase()
}
