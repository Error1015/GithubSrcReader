package com.error1015.githubsrcreader.data.model

/**
 * 统一的代码来源抽象：GitHub 仓库或本地文件夹都实现同一套浏览 / 读取接口。
 */
interface CodeSource {
    /** 一级标题，例如仓库名或文件夹名。 */
    val title: String

    /** 副标题，例如 owner/repo · 分支。 */
    val subtitle: String

    /** 是否支持「整个来源」范围内的引用搜索。 */
    val supportsRepositorySearch: Boolean get() = false

    /** 不支持全来源搜索时的原因提示。 */
    val repositorySearchHint: String? get() = "当前来源不支持跨文件搜索"

    suspend fun list(path: String): List<FileEntry>

    suspend fun readText(path: String): FileContent

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

/** 最近打开的来源记录，用于首页列表与持久化。 */
data class RecentSource(
    val id: String,
    val type: Type,
    val title: String,
    val subtitle: String,
    val owner: String? = null,
    val repo: String? = null,
    val ref: String? = null,
    val treeUri: String? = null,
    val openedAt: Long = 0L,
) {
    enum class Type { GITHUB, LOCAL }
}
