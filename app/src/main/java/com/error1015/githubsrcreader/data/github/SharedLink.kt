package com.error1015.githubsrcreader.data.github

/**
 * 从系统分享进来的任意文本里挑出可解析的 GitHub 链接。
 *
 * 分享来源通常有三类：
 * - 浏览器 / GitHub 客户端：纯链接，可能带 `?tab=readme` 之类的尾巴；
 * - 聊天软件、笔记：夹在说明文字里的链接，可能紧跟中文与标点；
 * - Markdown：`[repo](https://github.com/a/b)` 这种包裹形式。
 *
 * 纯 Kotlin，无 Android 依赖，可直接单测。
 */
object SharedLink {

    /** 带协议或以 `git@` 开头的片段；中文与括号留给 [clean] 处理。 */
    private val urlRegex = Regex(
        pattern = "(?:https?://|git@)[^\\s<>\"'\\\\]+",
        option = RegexOption.IGNORE_CASE,
    )

    private const val LEADING = "（(<【[「『\"'*_~“"
    private const val TRAILING = "。，、；：！？.,;:!?）)】]」』>\"'*_~”"

    /**
     * 返回第一个可被 [GitHubUrlParser] 解析的链接文本；找不到返回 null。
     * 返回的是**原始候选串**（已去掉包装符号与尾随标点），可直接交给 openUrl。
     */
    fun firstLink(raw: String): String? {
        val text = raw.trim()
        if (text.isEmpty()) return null

        // 1. 整段就是一个链接，或 owner/repo 简写
        parseable(text)?.let { return it }

        // 2. 文本中出现的完整 URL（最常见：浏览器分享）
        for (match in urlRegex.findAll(text)) {
            parseable(match.value)?.let { return it }
        }

        // 3. 没有协议的 github.com/... 片段
        for (token in text.split(WHITESPACE)) {
            val candidate = clean(token)
            if (!candidate.contains("github.com", ignoreCase = true)) continue
            parseable(candidate)?.let { return it }
        }
        return null
    }

    private fun parseable(raw: String): String? {
        val candidate = clean(raw)
        if (candidate.isEmpty()) return null
        return if (GitHubUrlParser.parse(candidate) != null) candidate else null
    }

    /** 去掉首尾的括号、引号、标点和中文，并从说明文字里截出链接本身。 */
    private fun clean(raw: String): String {
        var text = raw.trim()
        // 带协议时只保留链接部分，否则 GitHubUrlParser 会忽略 "://" 之前的说明文字，
        // 却把说明文字留在返回值里
        urlRegex.find(text)?.let { text = it.value }
        while (text.isNotEmpty() && text.first() in LEADING) text = text.drop(1)
        while (text.isNotEmpty() && (text.last() in TRAILING || isCjk(text.last()))) {
            text = text.dropLast(1)
        }
        return text.trim()
    }

    private fun isCjk(char: Char): Boolean = char.code in 0x4E00..0x9FFF

    private val WHITESPACE = Regex("\\s+")
}
