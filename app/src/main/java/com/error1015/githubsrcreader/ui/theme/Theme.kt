package com.error1015.githubsrcreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.error1015.githubsrcreader.data.model.CodeTheme
import com.error1015.githubsrcreader.ui.code.CodeColors
import com.error1015.githubsrcreader.ui.code.LocalCodeColors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * 全局主题：直接使用 Miuix 自带的澎湃 (HyperOS) 配色与文字规范，
 * 同时把代码高亮色一并注入。
 *
 * 代码配色独立于界面明暗：选 One Dark Pro 时即使界面是浅色，代码块也保持暗色（和编辑器习惯一致）。
 */
@Composable
fun GithubSrcReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    codeTheme: CodeTheme = CodeTheme.FOLLOW_APP,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) darkColorScheme() else lightColorScheme()
    val codeColors = when (codeTheme) {
        CodeTheme.FOLLOW_APP -> if (darkTheme) CodeColors.Dark else CodeColors.Light
        CodeTheme.ONE_DARK_PRO -> CodeColors.OneDarkPro
    }
    MiuixTheme(colors = colors) {
        CompositionLocalProvider(LocalCodeColors provides codeColors) {
            content()
        }
    }
}
