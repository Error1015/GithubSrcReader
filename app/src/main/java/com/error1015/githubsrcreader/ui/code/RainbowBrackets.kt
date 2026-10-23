package com.error1015.githubsrcreader.ui.code

/**
 * 一个括号字符的着色信息：[start, end) 为它在文本中的位置，[level] 为嵌套层级（从 0 开始）。
 * 同一对括号（开 / 闭）的 [level] 相同，因此颜色一致。
 */
data class BracketSpan(val start: Int, val end: Int, val level: Int)

/**
 * 彩虹括号扫描器：一趟线性扫描算出每个 `() [] {}` 的嵌套层级。
 *
 * 纯 Kotlin、无 Android 依赖，可直接单测。字符串与注释里的括号会被跳过
 * （依赖 [CodeTokenizer] 给出的 token 区间），避免 `"("`、`// )` 这类误判。
 */
object RainbowBrackets {

    /** 括号太多就放弃着色，避免超长文件生成过多 SpanStyle 拖慢排版。 */
    private const val MaxBrackets = 20_000

    fun spans(text: String, tokens: List<CodeToken>): List<BracketSpan> {
        if (text.isEmpty()) return emptyList()

        // 标记出字符串 / 注释覆盖的字符，扫描时直接跳过
        val skipped = BooleanArray(text.length)
        for (token in tokens) {
            if (token.kind != TokenKind.STRING && token.kind != TokenKind.COMMENT) continue
            val from = token.start.coerceIn(0, text.length)
            val to = token.end.coerceIn(from, text.length)
            for (i in from until to) skipped[i] = true
        }

        val result = ArrayList<BracketSpan>()
        var depth = 0
        for (i in text.indices) {
            if (skipped[i]) continue
            when (text[i]) {
                '(', '[', '{' -> {
                    result += BracketSpan(i, i + 1, depth)
                    depth++
                }

                ')', ']', '}' -> {
                    // 多余的右括号按层级 0 处理，不出现负层级
                    if (depth > 0) depth--
                    result += BracketSpan(i, i + 1, depth)
                }

                else -> continue
            }
            if (result.size > MaxBrackets) return emptyList()
        }
        return result
    }
}
