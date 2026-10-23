package com.error1015.githubsrcreader.data.download

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.error1015.githubsrcreader.data.model.SourceException
import com.error1015.githubsrcreader.i18n.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 把文件保存到公共下载目录：`Download/Github Src Reader/<来源>/<原目录结构>/<文件名>`。
 *
 * 走 MediaStore.Downloads，因此**不需要任何存储权限**（API 29+），
 * 保存后系统「下载」应用与文件管理器里都能直接看到，也不会随卸载被清理。
 */
object FileDownloader {

    /** 下载根目录（位于公共 Download 下）。 */
    const val ROOT_FOLDER = "Github Src Reader"

    private val INVALID_CHARS = Regex("[\\\\/:*?\"<>|\\u0000-\\u001f]")

    /**
     * @param folder 来源目录名，例如 `error1015-GithubSrcReader`
     * @param path 文件在来源内的相对路径，用来还原目录结构
     * @return 保存后的可读路径，用于提示用户
     */
    suspend fun save(
        context: Context,
        folder: String,
        path: String,
        displayName: String,
        mimeType: String,
        bytes: ByteArray,
    ): String = withContext(Dispatchers.IO) {
        val relativeDir = relativeDirectory(folder, path)

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/$relativeDir")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw SourceException(Strings.downloadCreateFailed)

        try {
            val stream = resolver.openOutputStream(uri)
                ?: throw SourceException(Strings.downloadWriteFailed)
            stream.use { it.write(bytes) }

            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            // 写入失败时不要让半截文件留在下载目录里
            runCatching { resolver.delete(uri, null, null) }
            throw if (e is SourceException) e else SourceException(Strings.downloadFailed(e.message ?: Strings.commonUnknownError), e)
        }

        // 系统可能改写文件名（例如按 MIME 补扩展名），提示里用落盘后的真实名字
        "Download/$relativeDir${resolver.displayNameOf(uri) ?: displayName}"
    }

    /** 读取落盘后的真实文件名；查询失败时返回 null，由调用方回退。 */
    private fun ContentResolver.displayNameOf(uri: Uri): String? = runCatching {
        query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    /**
     * 计算相对 Download/ 的目录（末尾带 `/`），拆出来便于单测。
     * 结构：`Github Src Reader/<来源>/<原目录结构>/`
     */
    fun relativeDirectory(folder: String, path: String): String {
        val segments = buildList {
            add(ROOT_FOLDER)
            sanitize(folder).takeIf { it.isNotBlank() }?.let(::add)
            path.substringBeforeLast('/', "").split('/').forEach { part ->
                sanitize(part).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
        return segments.joinToString("/") + "/"
    }

    /** 去掉非法字符；`.` 与 `..` 这种会破坏层级的段直接丢弃（隐藏目录的 `.` 保留）。 */
    private fun sanitize(segment: String): String {
        val cleaned = segment.trim().replace(INVALID_CHARS, "_").trim()
        return if (cleaned == "." || cleaned == "..") "" else cleaned
    }
}
