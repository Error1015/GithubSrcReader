package com.error1015.githubsrcreader.data.github

import com.error1015.githubsrcreader.data.model.FileEntry
import com.error1015.githubsrcreader.data.model.RepoInfo
import com.error1015.githubsrcreader.data.model.SourceException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 极简 GitHub REST 客户端：仓库信息、目录列表、文件内容、ref 存在性校验。
 *
 * Token 可选，仅保存在本机，用于提升 API 速率上限。
 */
class GitHubApi(private val tokenProvider: () -> String?) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(40, TimeUnit.SECONDS)
        .build()

    private val apiBase: HttpUrl = "https://api.github.com/".toHttpUrl()

    suspend fun repoInfo(owner: String, repo: String): RepoInfo {
        val body = fetch(urlOf("repos", owner, repo))
        val json = JSONObject(body)
        return RepoInfo(
            owner = owner,
            repo = repo,
            defaultBranch = json.optString("default_branch").ifBlank { "main" },
            description = if (json.isNull("description")) null else json.optString("description").ifBlank { null },
            stars = json.optInt("stargazers_count"),
            language = if (json.isNull("language")) null else json.optString("language").ifBlank { null },
        )
    }

    suspend fun listDirectory(owner: String, repo: String, path: String, ref: String?): List<FileEntry> {
        val url = contentsUrl(owner, repo, path, ref)
        val body = fetch(url).trimStart()
        return when {
            body.startsWith("[") -> {
                val array = JSONArray(body)
                (0 until array.length()).mapNotNull { index ->
                    array.optJSONObject(index)?.toEntry()
                }
            }
            body.startsWith("{") -> listOfNotNull(JSONObject(body).toEntry())
            else -> emptyList()
        }
    }

    suspend fun fileText(owner: String, repo: String, path: String, ref: String?): String =
        fetch(contentsUrl(owner, repo, path, ref), accept = RAW_ACCEPT)

    private fun contentsUrl(owner: String, repo: String, path: String, ref: String?): HttpUrl =
        if (path.isBlank()) {
            urlOf("repos", owner, repo, "contents", ref = ref)
        } else {
            urlOf("repos", owner, repo, "contents", path, ref = ref)
        }

    /** 校验 ref（分支 / 标签 / commit sha）是否存在。 */
    suspend fun refExists(owner: String, repo: String, ref: String): Boolean = try {
        fetch(urlOf("repos", owner, repo, "commits", ref))
        true
    } catch (_: SourceException) {
        false
    }

    fun hasToken(): Boolean = !tokenProvider().isNullOrBlank()

    /**
     * GitHub 代码搜索，返回命中文件路径。
     * 该接口要求鉴权（无 Token 会返回 401/403）。
     */
    suspend fun searchCodePaths(owner: String, repo: String, keyword: String, limit: Int = 20): List<String> {
        val url = apiBase.newBuilder()
            .addPathSegment("search")
            .addPathSegment("code")
            .addQueryParameter("q", "$keyword repo:$owner/$repo")
            .addQueryParameter("per_page", limit.coerceIn(1, 50).toString())
            .build()
        val body = fetch(url)
        val items = JSONObject(body).optJSONArray("items") ?: return emptyList()
        return (0 until items.length()).mapNotNull { index ->
            items.optJSONObject(index)?.optString("path")?.takeIf { it.isNotBlank() }
        }
    }

    private fun urlOf(vararg segments: String, ref: String? = null): HttpUrl {
        var pathSegments = segments.toList()
        // 目录路径整体作为一段加入，okhttp 会把其中的 "/" 正确编码为 %2F，GitHub 可正常解析。
        if (pathSegments.size >= 5) {
            val head = pathSegments.take(4)
            val path = pathSegments.drop(4).joinToString("/")
            pathSegments = head + path
        }
        val builder = apiBase.newBuilder()
        pathSegments.forEach { builder.addPathSegment(it) }
        if (!ref.isNullOrBlank()) builder.addQueryParameter("ref", ref)
        return builder.build()
    }

    private fun JSONObject.toEntry(): FileEntry? {
        val type = optString("type")
        val name = optString("name")
        if (name.isBlank()) return null
        val isDir = type == "dir"
        if (!isDir && type != "file" && type != "symlink") return null
        return FileEntry(
            name = name,
            path = optString("path", name),
            isDirectory = isDir,
            size = if (isDir) -1L else optLong("size", -1L),
        )
    }

    private fun request(url: HttpUrl, accept: String): Request =
        Request.Builder()
            .url(url)
            .header("Accept", accept)
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "GithubSrcReader-Android")
            .apply {
                tokenProvider()?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
            }
            .build()

    private suspend fun fetch(url: HttpUrl, accept: String = JSON_ACCEPT): String =
        withContext(Dispatchers.IO) {
            try {
                client.newCall(request(url, accept)).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        throw errorFor(response.code, body, response.header("X-RateLimit-Remaining"))
                    }
                    body
                }
            } catch (e: IOException) {
                throw SourceException("网络请求失败，请检查网络连接", e)
            }
        }

    private fun errorFor(code: Int, body: String, rateRemaining: String?): SourceException {
        val message = runCatching { JSONObject(body).optString("message") }.getOrNull().orEmpty()
        return when {
            code == 401 -> SourceException("Token 无效或已过期，请在设置中重新填写")
            code == 403 && rateRemaining == "0" ->
                SourceException("GitHub API 请求次数已用尽，请稍后重试或在设置中填入 Token")
            code == 403 -> SourceException("没有权限访问该资源（403）")
            code == 404 -> SourceException("仓库或路径不存在（404）")
            code >= 500 -> SourceException("GitHub 服务异常（$code），请稍后重试")
            else -> SourceException("GitHub 返回错误（$code）" + if (message.isNotBlank()) "：$message" else "")
        }
    }

    companion object {
        private const val JSON_ACCEPT = "application/vnd.github+json"
        private const val RAW_ACCEPT = "application/vnd.github.raw"
    }
}
