package com.error1015.githubsrcreader.data.model

/** 目录或文件条目，GitHub 与本地文件夹共用同一模型。 */
data class FileEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = -1L,
)

/** 读取到的文本文件内容。 */
data class FileContent(
    val path: String,
    val text: String,
    val language: Language,
    val truncated: Boolean = false,
    val sizeBytes: Long = -1L,
)

/** GitHub 仓库元信息。 */
data class RepoInfo(
    val owner: String,
    val repo: String,
    val defaultBranch: String,
    val description: String? = null,
    val stars: Int = 0,
    val language: String? = null,
) {
    val fullName: String get() = "$owner/$repo"
}

/** 简单的加载状态封装。 */
sealed interface Loadable<out T> {
    data object Idle : Loadable<Nothing>
    data object Loading : Loadable<Nothing>
    data class Success<T>(val value: T) : Loadable<T>
    data class Failure(val message: String) : Loadable<Nothing>
}

/** 目录优先、再按名称排序。 */
fun List<FileEntry>.sortedForDisplay(): List<FileEntry> = sortedWith(
    compareByDescending<FileEntry> { it.isDirectory }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
)
