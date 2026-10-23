package com.error1015.githubsrcreader.data.model

/**
 * 代码区域的配色方案。只影响阅读页的代码块（背景 + 语法高亮 + 彩虹括号），
 * 界面其余部分始终跟随应用外观设置。
 */
enum class CodeTheme {
    /** 跟随应用明暗，用默认的 GitHub 风配色。 */
    FOLLOW_APP,

    /** One Dark Pro：Atom / VS Code 上最经典的暗色代码配色。 */
    ONE_DARK_PRO,
    ;

    companion object {
        fun fromName(raw: String?): CodeTheme = entries.firstOrNull { it.name == raw } ?: FOLLOW_APP
    }
}
