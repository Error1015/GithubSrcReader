package com.error1015.githubsrcreader.data.model

import android.net.Uri
import com.error1015.githubsrcreader.i18n.Strings

/**
 * 统一的代码来源抽象：GitHub 仓库或本地文件夹都实现同一套浏览 / 读取接口。
 */
interface CodeSource {
    /** 一级标题，例如仓库名或文件夹名。 */
    val title: String

    /** 下载到本地时使用的目录名，默认用标题。 */
    val downloadFolder: String get() = title

    /** 副标题，例如 owner/repo · 分支。 */
    val subtitle: String

    /** 是否支持「整个来源」范围内的引用搜索。 */
    val supportsRepositorySearch: Boolean get() = false

    /** 不支持全来源搜索时的原因提示。 */
    val repositorySearchHint: String? get() = Strings.hintNoCrossFileSearch

    suspend fun list(path: String): List<FileEntry>

    suspend fun readText(path: String): FileContent

    /**
     * 读取二进制内容（图片、PDF 等非文本文件）。
     * 不支持时返回 null；调用方应改用 [directUri]。
     */
    suspend fun readBytes(path: String): ByteArray? = null

    /**
     * 能直接交给系统应用打开的本地 Uri（如 SAF 文档 Uri），避免拷贝。
     * 远程来源返回 null。
     */
    suspend fun directUri(path: String): Uri? = null

    /**
     * 在整个来源里查找 [name] 的引用。
     *
     * @param onProgress 已扫描 / 总数，便于界面显示进度
     */
    suspend fun searchReferences(
        name: String,
        onProgress: (scanned: Int, total: Int) -> Unit = { _, _ -> },
    ): List<UsageHit> = emptyList()
}

/** 数据层可预期的错误，直接用于界面提示。 */
class SourceException(message: String, cause: Throwable? = null) : Exception(message, cause)
