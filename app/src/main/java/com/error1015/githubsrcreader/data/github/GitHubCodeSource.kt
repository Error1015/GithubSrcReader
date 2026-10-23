package com.error1015.githubsrcreader.data.github

import com.error1015.githubsrcreader.data.TextReader
import com.error1015.githubsrcreader.data.model.CodeSource
import com.error1015.githubsrcreader.data.model.FileContent
import com.error1015.githubsrcreader.data.model.FileEntry
import com.error1015.githubsrcreader.data.model.Language
import com.error1015.githubsrcreader.data.model.UsageHit
import com.error1015.githubsrcreader.data.model.sortedForDisplay
import com.error1015.githubsrcreader.data.search.ReferenceSearch
import com.error1015.githubsrcreader.data.stripBom
import com.error1015.githubsrcreader.i18n.Strings

/** GitHub 云端仓库来源。 */
class GitHubCodeSource(
    private val api: GitHubApi,
    val owner: String,
    val repo: String,
    val ref: String,
) : CodeSource {

    override val title: String = repo
    override val subtitle: String = "$owner/$repo · $ref"

    /** 不同仓库下载到同一目录会互相覆盖，所以带上 owner。 */
    override val downloadFolder: String = "$owner-$repo"

    override val supportsRepositorySearch: Boolean get() = api.hasToken()

    override val repositorySearchHint: String?
        get() = if (api.hasToken()) {
            null
        } else {
            Strings.hintSearchNeedsToken
        }

    override suspend fun list(path: String): List<FileEntry> =
        api.listDirectory(owner, repo, path, ref).sortedForDisplay()

    override suspend fun readText(path: String): FileContent {
        val raw = api.fileText(owner, repo, path, ref).stripBom()
        val trimmed = TextReader.trimLines(raw)
        val truncated = trimmed.truncated || raw.length >= TextReader.MAX_BYTES
        return FileContent(
            path = path,
            text = trimmed.text,
            language = Language.fromPath(path),
            truncated = truncated,
            sizeBytes = raw.length.toLong(),
        )
    }

    override suspend fun readBytes(path: String): ByteArray = api.fileBytes(owner, repo, path, ref)

    /**
     * 全仓库搜索：先用 /search/code 拿到命中文件，再逐个抓取文件内容定位到具体行，
     * 这样结果与本地来源保持一致（带行号与上下文行）。
     */
    override suspend fun searchReferences(
        name: String,
        onProgress: (scanned: Int, total: Int) -> Unit,
    ): List<UsageHit> {
        val paths = api.searchCodePaths(owner, repo, name, limit = MAX_SEARCH_FILES)
        val hits = ArrayList<UsageHit>()
        paths.forEachIndexed { index, path ->
            onProgress(index + 1, paths.size)
            val text = runCatching { api.fileText(owner, repo, path, ref) }.getOrNull() ?: return@forEachIndexed
            hits += ReferenceSearch.searchInText(
                text = text.stripBom(),
                name = name,
                language = Language.fromPath(path),
                path = path,
            )
        }
        return hits.sortedWith(compareBy({ it.path }, { it.line }))
    }

    private companion object {
        const val MAX_SEARCH_FILES = 20
    }
}
