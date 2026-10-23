package com.error1015.githubsrcreader.ui.code

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** 代码配色，跟随明暗主题。 */
@Immutable
data class CodeColors(
    val background: Color,
    val plain: Color,
    val keyword: Color,
    val string: Color,
    val comment: Color,
    val number: Color,
    val function: Color,
    val type: Color,
    val meta: Color,
    val property: Color,
    val lineNumber: Color,
    /** 彩虹括号色板，按嵌套层级循环取色。 */
    val rainbow: List<Color> = emptyList(),
) {
    companion object {
        private val RainbowLight = listOf(
            Color(0xFFBF8700),
            Color(0xFFA626A4),
            Color(0xFF1565C0),
            Color(0xFF2E7D32),
            Color(0xFFC2410C),
            Color(0xFF00838F),
        )

        private val RainbowDark = listOf(
            Color(0xFFE5C07B),
            Color(0xFFC678DD),
            Color(0xFF61AFEF),
            Color(0xFF98C379),
            Color(0xFFD19A66),
            Color(0xFF56B6C2),
        )

        val Light = CodeColors(
            background = Color(0xFFFBFBFC),
            plain = Color(0xFF1F2328),
            keyword = Color(0xFFCF222E),
            string = Color(0xFF0A3069),
            comment = Color(0xFF7D8590),
            number = Color(0xFF0550AE),
            function = Color(0xFF8250DF),
            type = Color(0xFF953800),
            meta = Color(0xFF116329),
            property = Color(0xFF0A3069),
            lineNumber = Color(0xFFB0B6BF),
            rainbow = RainbowLight,
        )

        val Dark = CodeColors(
            background = Color(0xFF15161A),
            plain = Color(0xFFE6EDF3),
            keyword = Color(0xFFFF7B72),
            string = Color(0xFFA5D6FF),
            comment = Color(0xFF8B949E),
            number = Color(0xFF79C0FF),
            function = Color(0xFFD2A8FF),
            type = Color(0xFFFFA657),
            meta = Color(0xFF7EE787),
            property = Color(0xFFA5D6FF),
            lineNumber = Color(0xFF4C5058),
            rainbow = RainbowDark,
        )
    }
}

val LocalCodeColors = staticCompositionLocalOf { CodeColors.Light }
