package com.error1015.githubsrcreader.ui.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
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

    /** 系统 MimeTypeMap 认不出扩展名时的兜底类型（也是 MediaStore 下载集合的默认类型）。 */
    private const val FALLBACK_MIME = "application/octet-stream"

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

    /**
     * 保存到下载目录时使用的 MIME。
     *
     * 必须返回「系统 MimeTypeMap 为这个扩展名认可的 MIME」：MediaProvider 在插入时会把
     * MIME 反推成规范扩展名（AOSP `FileUtils.splitFileName`），一旦它和文件名里的扩展名对不上
     * 就直接追加，于是 `Main.kt` 配上 `text/plain` 会存成 `Main.kt.txt`。
     * 系统不认识的扩展名（kt/kts/gradle…）一律回退 [FALLBACK_MIME]，此时 MediaProvider
     * 认为扩展名未知，会原样保留文件名。
     *
     * @param mimeLookup 扩展名 → MIME 的查询函数，默认查系统表，测试时可替换
     */
    fun mimeForSave(
        fileName: String,
        mimeLookup: (String) -> String? = { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) },
    ): String {
        val ext = extensionOf(fileName)
        if (ext.isEmpty()) return FALLBACK_MIME
        return mimeLookup(ext) ?: FALLBACK_MIME
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
