package com.error1015.githubsrcreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.error1015.githubsrcreader.ui.code.CodeColors
import com.error1015.githubsrcreader.ui.code.LocalCodeColors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * 全局主题：直接使用 Miuix 自带的澎湃 (HyperOS) 配色与文字规范，
 * 同时把代码高亮色一并注入。
 */
@Composable
fun GithubSrcReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) darkColorScheme() else lightColorScheme()
    MiuixTheme(colors = colors) {
        CompositionLocalProvider(
            LocalCodeColors provides if (darkTheme) CodeColors.Dark else CodeColors.Light,
        ) {
            content()
        }
    }
}
