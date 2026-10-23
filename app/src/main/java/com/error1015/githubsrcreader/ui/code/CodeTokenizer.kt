package com.error1015.githubsrcreader.ui.code

import com.error1015.githubsrcreader.data.model.Language

enum class TokenKind {
    KEYWORD,
    STRING,
    COMMENT,
    NUMBER,
    FUNCTION,
    TYPE,
    META,
    PROPERTY,
}

/** 一段着色区间，[start] / [end] 为字符下标。 */
data class CodeToken(val start: Int, val end: Int, val kind: TokenKind)

/**
 * 轻量词法着色器：一次顺序扫描，输出着色区间。
 * 只追求“看起来对”，不处理模板语法、嵌套注释等边缘情况。
 */
object CodeTokenizer {

    fun tokenize(code: String, language: Language): List<CodeToken> {
        if (code.isEmpty()) return emptyList()
        return when {
            language.isMarkup -> markup(code)
            language == Language.MARKDOWN -> markdown(code)
            else -> generic(code, specOf(language))
        }
    }

    // ------------------------------------------------------------------ generic

    private class LangSpec(
        val keywords: Set<String>,
        val lineComments: List<String> = listOf("//"),
        val blockComments: List<Pair<String, String>> = listOf("/*" to "*/"),
        val metaChar: Char? = null,
        val metaAtLineStart: Boolean = false,
        val uppercaseIsType: Boolean = false,
        val propertyBeforeColon: Boolean = false,
    )

    private fun keywords(text: String): Set<String> = text.split(' ').filter { it.isNotEmpty() }.toSet()

    private fun specOf(language: Language): LangSpec = when (language) {
        Language.KOTLIN, Language.GROOVY -> LangSpec(
            keywords = keywords(
                "abstract actual annotation as break by catch class companion const constructor continue " +
                    "crossinline data delegate do dynamic else enum expect external final finally for fun get if " +
                    "import in infix init inline inner interface internal is lateinit noinline object open " +
                    "operator out override package private property protected public reified return sealed set " +
                    "super suspend tailrec this throw try typealias val var vararg when where while it " +
                    "true false null field"
            ),
            metaChar = '@',
            uppercaseIsType = true,
        )

        Language.JAVA -> LangSpec(
            keywords = keywords(
                "abstract assert boolean break byte case catch char class const continue default do double else " +
                    "enum extends final finally float for goto if implements import instanceof int interface long " +
                    "native new package private protected public record return sealed permits short static " +
                    "strictfp super switch synchronized this throw throws transient try var void volatile while " +
                    "true false null var"
            ),
            metaChar = '@',
            uppercaseIsType = true,
        )

        Language.PYTHON -> LangSpec(
            keywords = keywords(
                "and as assert async await break class continue def del elif else except False finally for from " +
                    "global if import in is lambda None nonlocal not or pass raise return True try while with " +
                    "yield match case self cls"
            ),
            lineComments = listOf("#"),
            blockComments = emptyList(),
            metaChar = '@',
            uppercaseIsType = true,
        )

        Language.JAVASCRIPT, Language.TYPESCRIPT, Language.DART, Language.LUA -> LangSpec(
            keywords = keywords(
                "break case catch class const continue debugger default delete do else export extends finally for " +
                    "function if import in instanceof let new of from as return super switch this throw try " +
                    "typeof var void while with yield async await static get set interface type enum implements " +
                    "declare namespace public private protected readonly abstract null undefined true false " +
                    "local end then elseif"
            ),
            uppercaseIsType = true,
        )

        Language.C, Language.CPP, Language.CSHARP, Language.SWIFT -> LangSpec(
            keywords = keywords(
                "auto break case char const continue default do double else enum extern float for goto if inline " +
                    "int long register return short signed sizeof static struct switch typedef union unsigned " +
                    "void volatile while class namespace public private protected template typename virtual " +
                    "using new delete try catch throw constexpr nullptr true false override final import func " +
                    "let var guard defer in where init self Self nil some any throws async await is as " +
                    "string bool decimal foreach base params out ref lock yield record"
            ),
            metaChar = '#',
            metaAtLineStart = true,
            uppercaseIsType = true,
        )

        Language.GO -> LangSpec(
            keywords = keywords(
                "break case chan const continue default defer else fallthrough for func go goto if import " +
                    "interface map package range return select struct switch type var nil true false iota"
            ),
            uppercaseIsType = true,
        )

        Language.RUST -> LangSpec(
            keywords = keywords(
                "as async await break const continue crate dyn else enum extern false fn for if impl in let loop " +
                    "match mod move mut pub ref return self Self static struct super trait true type unsafe use " +
                    "where while"
            ),
            uppercaseIsType = true,
        )

        Language.RUBY -> LangSpec(
            keywords = keywords(
                "def end if elsif else unless while until for in do class module begin rescue ensure return yield " +
                    "lambda proc self nil true false and or not then require attr_accessor attr_reader puts " +
                    "private public protected new"
            ),
            lineComments = listOf("#"),
            blockComments = emptyList(),
            metaChar = '@',
            uppercaseIsType = true,
        )

        Language.PHP -> LangSpec(
            keywords = keywords(
                "function class public private protected if else elseif for foreach while do switch case break " +
                    "continue return echo new static const use namespace try catch finally throw true false null " +
                    "array isset empty extends implements interface abstract final fn match"
            ),
            lineComments = listOf("//", "#"),
            uppercaseIsType = true,
        )

        Language.SHELL -> LangSpec(
            keywords = keywords(
                "if then else elif fi for while until do done case esac function in return export local readonly " +
                    "declare unset shift source echo cd exit set eval trap wait"
            ),
            lineComments = listOf("#"),
            blockComments = emptyList(),
            metaChar = '$',
            uppercaseIsType = false,
        )

        Language.SQL -> LangSpec(
            keywords = keywords(
                "select from where insert into values update set delete create table alter drop join inner left " +
                    "right outer on group by order having limit offset as and or not null primary key foreign " +
                    "index distinct union all case when then end exists count sum avg min max is like in default " +
                    "constraint references add column"
            ),
            lineComments = listOf("--"),
            blockComments = listOf("/*" to "*/"),
        )

        Language.JSON -> LangSpec(
            keywords = keywords("true false null"),
            lineComments = emptyList(),
            blockComments = emptyList(),
            propertyBeforeColon = true,
        )

        Language.YAML -> LangSpec(
            keywords = keywords("true false null yes no on off"),
            lineComments = listOf("#"),
            blockComments = emptyList(),
            propertyBeforeColon = true,
        )

        Language.TOML -> LangSpec(
            keywords = keywords("true false"),
            lineComments = listOf("#"),
            blockComments = emptyList(),
            propertyBeforeColon = true,
        )

        Language.CSS -> LangSpec(
            keywords = keywords("important media import keyframes from to and not only"),
            lineComments = emptyList(),
            blockComments = listOf("/*" to "*/"),
            propertyBeforeColon = true,
        )

        else -> LangSpec(keywords = emptySet(), lineComments = emptyList(), blockComments = emptyList())
    }

    private fun generic(code: String, spec: LangSpec): List<CodeToken> {
        val tokens = ArrayList<CodeToken>()
        val length = code.length
        var i = 0
        while (i < length) {
            val ch = code[i]
            if (ch == '\n' || ch == '\r' || ch == ' ' || ch == '\t') {
                i++
                continue
            }

            val lineComment = spec.lineComments.firstOrNull { code.startsWith(it, i) }
            if (lineComment != null) {
                var end = code.indexOf('\n', i)
                if (end < 0) end = length
                tokens += CodeToken(i, end, TokenKind.COMMENT)
                i = end
                continue
            }

            val blockComment = spec.blockComments.firstOrNull { code.startsWith(it.first, i) }
            if (blockComment != null) {
                val close = code.indexOf(blockComment.second, i + blockComment.first.length)
                val end = if (close < 0) length else close + blockComment.second.length
                tokens += CodeToken(i, end, TokenKind.COMMENT)
                i = end
                continue
            }

            if (spec.metaChar != null && ch == spec.metaChar && (!spec.metaAtLineStart || atLineStart(code, i))) {
                var end = i + 1
                while (end < length && (code[end].isLetterOrDigit() || code[end] == '_' || code[end] == '.')) end++
                if (end > i + 1) {
                    tokens += CodeToken(i, end, TokenKind.META)
                    i = end
                    continue
                }
            }

            if (ch == '"' || ch == '\'' || ch == '`') {
                val triple = code.startsWith("$ch$ch$ch", i)
                val quote = if (triple) "$ch$ch$ch" else ch.toString()
                var end = i + quote.length
                while (end < length) {
                    if (code[end] == '\\') {
                        end += 2
                        continue
                    }
                    if (code.startsWith(quote, end)) {
                        end += quote.length
                        break
                    }
                    if (quote.length == 1 && code[end] == '\n') break
                    end++
                }
                val safeEnd = minOf(end, length)
                val kind = if (spec.propertyBeforeColon && nextMeaningful(code, safeEnd) == ':') {
                    TokenKind.PROPERTY
                } else {
                    TokenKind.STRING
                }
                tokens += CodeToken(i, safeEnd, kind)
                i = safeEnd
                continue
            }

            if (ch.isDigit()) {
                val end = consumeNumber(code, i)
                tokens += CodeToken(i, end, TokenKind.NUMBER)
                i = end
                continue
            }

            if (ch.isLetter() || ch == '_' || ch == '$') {
                var end = i + 1
                while (end < length && (code[end].isLetterOrDigit() || code[end] == '_' || code[end] == '$')) end++
                val word = code.substring(i, end)
                val kind = when {
                    spec.keywords.contains(word) -> TokenKind.KEYWORD
                    spec.propertyBeforeColon && nextMeaningful(code, end) == ':' -> TokenKind.PROPERTY
                    nextMeaningful(code, end) == '(' -> TokenKind.FUNCTION
                    spec.uppercaseIsType && word.first().isUpperCase() -> TokenKind.TYPE
                    else -> null
                }
                if (kind != null) tokens += CodeToken(i, end, kind)
                i = end
                continue
            }

            i++
        }
        return tokens
    }

    private fun consumeNumber(code: String, start: Int): Int {
        val length = code.length
        var end = start
        var seenDot = false
        val hex = code.startsWith("0x", start) || code.startsWith("0X", start)
        while (end < length) {
            val ch = code[end]
            when {
                ch.isDigit() -> end++
                ch == '_' -> end++
                ch == '.' -> {
                    if (seenDot) return end
                    seenDot = true
                    end++
                }
                hex && ch.lowercaseChar() in 'a'..'f' -> end++
                ch in "xXbBeE" -> end++
                ch in "fFlLuUdD" -> end++
                else -> return end
            }
        }
        return end
    }

    private fun atLineStart(code: String, index: Int): Boolean {
        var i = index - 1
        while (i >= 0) {
            val ch = code[i]
            if (ch == '\n') return true
            if (ch != ' ' && ch != '\t' && ch != '\r') return false
            i--
        }
        return true
    }

    private fun nextMeaningful(code: String, from: Int): Char? {
        var i = from
        while (i < code.length) {
            val ch = code[i]
            if (ch != ' ' && ch != '\t' && ch != '\r') return ch
            i++
        }
        return null
    }

    // ------------------------------------------------------------------ markup

    private fun markup(code: String): List<CodeToken> {
        val tokens = ArrayList<CodeToken>()
        val length = code.length
        var i = 0
        while (i < length) {
            if (code.startsWith("<!--", i)) {
                val close = code.indexOf("-->", i + 4)
                val end = if (close < 0) length else close + 3
                tokens += CodeToken(i, end, TokenKind.COMMENT)
                i = end
                continue
            }
            if (code[i] == '<') {
                val close = code.indexOf('>', i)
                val end = if (close < 0) length else close + 1
                var j = i + 1
                if (j < end && (code[j] == '/' || code[j] == '!' || code[j] == '?')) j++
                val nameStart = j
                while (j < end && (code[j].isLetterOrDigit() || code[j] in "-_:.")) j++
                if (j > nameStart) tokens += CodeToken(nameStart, j, TokenKind.KEYWORD)
                while (j < end) {
                    when {
                        code[j] == '"' || code[j] == '\'' -> {
                            val quote = code[j]
                            var k = j + 1
                            while (k < end && code[k] != quote) k++
                            tokens += CodeToken(j, minOf(k + 1, end), TokenKind.STRING)
                            j = k + 1
                        }

                        code[j].isLetter() -> {
                            var k = j
                            while (k < end && (code[k].isLetterOrDigit() || code[k] in "-_:.")) k++
                            tokens += CodeToken(j, k, TokenKind.PROPERTY)
                            j = k
                        }

                        else -> j++
                    }
                }
                i = end
                continue
            }
            i++
        }
        return tokens
    }

    // ---------------------------------------------------------------- markdown

    private fun markdown(code: String): List<CodeToken> {
        val tokens = ArrayList<CodeToken>()
        var lineStart = 0
        while (lineStart <= code.length) {
            var lineEnd = code.indexOf('\n', lineStart)
            if (lineEnd < 0) lineEnd = code.length
            val line = code.substring(lineStart, lineEnd)
            val trimmed = line.trimStart()
            when {
                trimmed.startsWith("```") -> tokens += CodeToken(lineStart, lineEnd, TokenKind.COMMENT)
                trimmed.startsWith("#") -> tokens += CodeToken(lineStart, lineEnd, TokenKind.KEYWORD)
                else -> {
                    var i = lineStart
                    while (i < lineEnd) {
                        when {
                            code[i] == '`' -> {
                                val close = code.indexOf('`', i + 1)
                                if (close in (i + 1) until lineEnd) {
                                    tokens += CodeToken(i, close + 1, TokenKind.STRING)
                                    i = close + 1
                                } else {
                                    i++
                                }
                            }

                            code[i] == ']' && i + 1 < lineEnd && code[i + 1] == '(' -> {
                                val close = code.indexOf(')', i)
                                if (close in (i + 2) until lineEnd) {
                                    tokens += CodeToken(i + 2, close, TokenKind.FUNCTION)
                                    i = close + 1
                                } else {
                                    i++
                                }
                            }

                            else -> i++
                        }
                    }
                }
            }
            if (lineEnd >= code.length) break
            lineStart = lineEnd + 1
        }
        return tokens
    }
}
