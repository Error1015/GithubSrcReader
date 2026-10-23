package com.error1015.githubsrcreader.data.github

/**
 * 解析 GitHub 链接。
 *
 * 支持的形式：
 * - `https://github.com/owner/repo`
 * - `https://github.com/owner/repo/tree/main/app/src`
 * - `https://github.com/owner/repo/blob/main/README.md`
 * - `https://raw.githubusercontent.com/owner/repo/main/README.md`
 * - `owner/repo`（简写）
 * - `git@github.com:owner/repo.git`
 *
 * 分支名可能带 `/`（如 `feature/foo`），因此 [ParsedUrl.candidates] 会按“最短 ref 优先”
 * 给出多组 `ref + path` 组合，由上层通过 GitHub API 校验哪一组真实存在。
 */
object GitHubUrlParser {

    data class RefPath(val ref: String?, val path: String)

    data class ParsedUrl(
        val owner: String,
        val repo: String,
        val candidates: List<RefPath>,
        /** URL 是否直接指向某个文件（blob / raw）。 */
        val pointsToFile: Boolean,
    )

    private val domainRegex = Regex("^[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+$")
    private val repoPartRegex = Regex("^[A-Za-z0-9._-]+$")

    private val githubHosts = setOf("github.com", "www.github.com", "m.github.com")
    private val rawHosts = setOf("raw.githubusercontent.com", "raw.github.com")

    fun parse(input: String): ParsedUrl? {
        var text = input.trim()
        if (text.isEmpty()) return null

        if (text.startsWith("git@github.com:")) {
            text = text.removePrefix("git@github.com:")
        }
        val schemeIndex = text.indexOf("://")
        if (schemeIndex >= 0) text = text.substring(schemeIndex + 3)

        text = text.substringBefore('#').substringBefore('?')
        text = text.trim('/')
        if (text.isEmpty()) return null

        val segments = text.split('/').filter { it.isNotBlank() }
        if (segments.size < 2) return null

        val head = segments.first().lowercase()
        val isKnownHost = head in githubHosts || head in rawHosts
        val looksLikeHost = head.contains('.') && domainRegex.matches(head)

        // 不是 GitHub 域名又不是简写形式，直接判为无法识别。
        if (looksLikeHost && !isKnownHost) return null

        val rest = if (isKnownHost || looksLikeHost) segments.drop(1) else segments
        if (rest.size < 2) return null

        val owner = rest[0]
        val repo = rest[1].removeSuffix(".git").removeSuffix(".git".uppercase())
        if (!repoPartRegex.matches(owner) || !repoPartRegex.matches(repo)) return null

        val tail = rest.drop(2)

        if (head in rawHosts) {
            return ParsedUrl(owner, repo, buildCandidates(tail), pointsToFile = tail.size > 1)
        }

        if (tail.isEmpty()) {
            return ParsedUrl(owner, repo, listOf(RefPath(null, "")), pointsToFile = false)
        }

        return when (tail.first().lowercase()) {
            "tree" -> ParsedUrl(owner, repo, buildCandidates(tail.drop(1)), pointsToFile = false)
            "blob", "raw", "blame", "edit" ->
                ParsedUrl(owner, repo, buildCandidates(tail.drop(1)), pointsToFile = true)
            // issues / pulls / releases 等页面统一落到仓库根目录
            else -> ParsedUrl(owner, repo, listOf(RefPath(null, "")), pointsToFile = false)
        }
    }

    /** 依次尝试 `ref` 为前 1..n 段，剩余部分作为路径。 */
    private fun buildCandidates(segments: List<String>): List<RefPath> {
        if (segments.isEmpty()) return listOf(RefPath(null, ""))
        return (1..segments.size).map { i ->
            RefPath(
                ref = segments.take(i).joinToString("/"),
                path = segments.drop(i).joinToString("/"),
            )
        }
    }
}
