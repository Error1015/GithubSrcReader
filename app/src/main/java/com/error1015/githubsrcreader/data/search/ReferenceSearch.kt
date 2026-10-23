package com.error1015.githubsrcreader.data.search

import com.error1015.githubsrcreader.data.model.Language
import com.error1015.githubsrcreader.data.model.UsageHit
import com.error1015.githubsrcreader.ui.code.CodeTokenizer
import com.error1015.githubsrcreader.ui.code.TokenKind

/**
 * 轻量「查找引用」实现：基于词法 token 做标识符匹配，
 *
 * - 跳过注释与字符串里的同名文本；
 * - 根据前一个词法单元判断该处是「声明」还是「引用」。
 *
 * 不构建真正的符号表，因此是启发式结果，和 IDE 的精确解析不同，但对阅读源码足够用。
 */
object ReferenceSearch {

    private val declarationKeywords = setOf(
        "fun", "def", "function", "func", "fn", "class", "interface", "object", "struct",
        "enum", "record", "trait", "typealias", "alias", "type", "val", "var", "let",
        "const", "const val", "void", "int", "long", "short", "double", "float", "char",
        "boolean", "bool", "byte", "string", "auto", "static", "final", "public",
        "private", "protected", "internal", "inline", "override", "suspend", "async",
        "export", "default", "get", "set", "field", "property", "macro",
    )

    /** 在单个文件文本中查找 [name] 的所有出现。 */
    fun searchInFile(text: String, path: String, name: String): List<UsageHit> =
        searchInText(text, name, Language.fromPath(path), path)

    fun searchInText(text: String, name: String, language: Language, path: String): List<UsageHit> {
        if (name.isBlank() || text.isEmpty()) return emptyList()

        val lineStarts = lineStartsOf(text)
        val tokens = CodeTokenizer.tokenize(text, language)
        val skipped = tokens.filter { it.kind == TokenKind.COMMENT || it.kind == TokenKind.STRING }
        val skipStarts = IntArray(skipped.size) { skipped[it].start }
        val skipEnds = IntArray(skipped.size) { skipped[it].end }

        val hits = ArrayList<UsageHit>()
        var index = text.indexOf(name)
        while (index >= 0) {
            val end = index + name.length
            if (isWordBoundary(text, index, end) && !inRanges(skipStarts, skipEnds, index)) {
                val lineIndex = lineIndexOf(lineStarts, index)
                val lineStart = lineStarts[lineIndex]
                var lineEnd = text.indexOf('\n', lineStart)
                if (lineEnd < 0) lineEnd = text.length
                hits += UsageHit(
                    path = path,
                    line = lineIndex + 1,
                    column = index - lineStart + 1,
                    lineText = text.substring(lineStart, lineEnd).trimEnd(),
                    isDeclaration = isDeclaration(text, index, lineStart),
                )
            }
            index = text.indexOf(name, index + 1)
        }
        return hits
    }

    /** 提取文件里的声明（类 / 方法 / 字段等），用于「本文件符号」列表。 */
    fun declarationsInText(text: String, language: Language, path: String): List<UsageHit> {
        if (text.isEmpty()) return emptyList()
        val lineStarts = lineStartsOf(text)
        val result = ArrayList<UsageHit>()
        val seen = HashSet<Int>()

        CodeTokenizer.tokenize(text, language).forEach { token ->
            if (token.kind != TokenKind.KEYWORD) return@forEach
            val word = text.substring(token.start, token.end)
            if (word !in declarationKeywords) return@forEach

            var i = token.end
            while (i < text.length && (text[i] == ' ' || text[i] == '\t')) i++
            while (i < text.length && text[i] == '@') {
                while (i < text.length && !text[i].isWhitespace()) i++
                while (i < text.length && (text[i] == ' ' || text[i] == '\t')) i++
            }
            if (i >= text.length || !isIdentifierStart(text[i])) return@forEach

            var end = i + 1
            while (end < text.length && isIdentifierPart(text[end])) end++
            val name = text.substring(i, end)
            if (name in declarationKeywords || !seen.add(i)) return@forEach

            val lineIndex = lineIndexOf(lineStarts, i)
            val lineStart = lineStarts[lineIndex]
            var lineEnd = text.indexOf('\n', lineStart)
            if (lineEnd < 0) lineEnd = text.length
            result += UsageHit(
                path = path,
                line = lineIndex + 1,
                column = i - lineStart + 1,
                lineText = text.substring(lineStart, lineEnd).trimEnd(),
                isDeclaration = true,
            )
        }
        return result
    }

    /** 取 [offset] 处的单词，用于双击选中。 */
    fun wordAt(text: String, offset: Int): String? {
        if (text.isEmpty()) return null
        var index = offset.coerceIn(0, text.length - 1)
        if (!isIdentifierPart(text[index])) {
            index -= 1
            if (index < 0 || !isIdentifierPart(text[index])) return null
        }
        var start = index
        var end = index + 1
        while (start > 0 && isIdentifierPart(text[start - 1])) start--
        while (end < text.length && isIdentifierPart(text[end])) end++
        val word = text.substring(start, end)
        if (word.isEmpty() || !isIdentifierStart(word.first())) return null
        return word
    }

    // ------------------------------------------------------------------ 内部

    private fun isIdentifierStart(char: Char): Boolean = char.isLetter() || char == '_' || char == '$'

    private fun isIdentifierPart(char: Char): Boolean = char.isLetterOrDigit() || char == '_' || char == '$'

    private fun isWordBoundary(text: String, start: Int, end: Int): Boolean {
        val before = if (start == 0) null else text[start - 1]
        val after = if (end >= text.length) null else text[end]
        val okBefore = before == null || !isIdentifierPart(before)
        val okAfter = after == null || !isIdentifierPart(after)
        return okBefore && okAfter
    }

    private fun inRanges(starts: IntArray, ends: IntArray, position: Int): Boolean {
        var low = 0
        var high = starts.size - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            when {
                position < starts[mid] -> high = mid - 1
                position >= ends[mid] -> low = mid + 1
                else -> return true
            }
        }
        return false
    }

    private fun isDeclaration(text: String, offset: Int, lineStart: Int): Boolean {
        var i = offset - 1
        while (i >= lineStart && (text[i] == ' ' || text[i] == '\t')) i--
        if (i < lineStart) return false
        val end = i + 1
        while (i >= lineStart && isIdentifierPart(text[i])) i--
        val previous = text.substring(i + 1, end)
        if (previous.isEmpty()) return false
        return previous in declarationKeywords || previous.first().isUpperCase()
    }

    private fun lineStartsOf(text: String): IntArray {
        var starts = IntArray(32)
        var count = 1
        starts[0] = 0
        for (i in text.indices) {
            if (text[i] == '\n') {
                if (count == starts.size) starts = starts.copyOf(count * 2)
                starts[count] = i + 1
                count++
            }
        }
        return if (count == starts.size) starts else starts.copyOf(count)
    }

    private fun lineIndexOf(lineStarts: IntArray, offset: Int): Int {
        var low = 0
        var high = lineStarts.size - 1
        while (low < high) {
            val mid = (low + high + 1) ushr 1
            if (lineStarts[mid] <= offset) low = mid else high = mid - 1
        }
        return low
    }
}
