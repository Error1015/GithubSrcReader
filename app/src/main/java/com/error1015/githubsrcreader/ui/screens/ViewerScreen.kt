package com.error1015.githubsrcreader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.error1015.githubsrcreader.data.model.FileContent
import com.error1015.githubsrcreader.data.model.Loadable
import com.error1015.githubsrcreader.i18n.Strings
import com.error1015.githubsrcreader.ui.ViewerUiState
import com.error1015.githubsrcreader.ui.code.CodeBlock
import com.error1015.githubsrcreader.ui.components.AppIcons
import com.error1015.githubsrcreader.ui.components.EmptyHint
import com.error1015.githubsrcreader.ui.components.formatSize
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 窗口高度低于该值（dp）就按紧凑布局处理：横屏手机、分屏、平板小窗都在这个量级，
 * 竖屏手机（≈780dp+）不会命中。紧凑布局省掉路径行与元信息，把纵向空间让给代码。
 */
private const val CompactScreenHeightDp = 480

@Composable
fun ViewerScreen(
    state: ViewerUiState,
    showLineNumbers: Boolean,
    onBack: () -> Unit,
    onReload: () -> Unit,
    onToggleLineNumbers: () -> Unit,
    onCopy: (String) -> Unit,
    onFindUsages: (String) -> Unit,
    onDownload: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val content = state.content

    // 缩放提升到屏幕这一层：刷新只是替换内容、组合不重建，缩放能保留下来；
    // 退出预览（屏幕被销毁）或换一个文件时重新从 1f 开始。
    var zoom by remember(state.path) { mutableFloatStateOf(1f) }

    // 横屏 / 分屏时窗口很矮，工具栏与元信息会吃掉大量代码区，切到紧凑布局
    val compact = LocalConfiguration.current.screenHeightDp < CompactScreenHeightDp

    Column(modifier = Modifier.fillMaxSize().background(colors.surface)) {
        SmallTopAppBar(
            title = state.fileName.ifBlank { Strings.viewerDefaultTitle },
            // 路径放不进 subtitle：它没有单行/省略号约束，长路径会换行把工具栏撑高；
            // 改放 bottomContent，固定一行、中间省略。紧凑布局下这行也省掉。
            bottomContent = { if (!compact) PathLine(state.path) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = Strings.actionBack,
                        tint = colors.onSurface,
                        modifier = Modifier.size(22.dp),
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = onDownload,
                    enabled = !state.downloading,
                    minWidth = 36.dp,
                ) {
                    if (state.downloading) {
                        CircularProgressIndicator(size = 18.dp)
                    } else {
                        Icon(
                            imageVector = AppIcons.Download,
                            contentDescription = Strings.viewerSaveToDownloads,
                            tint = colors.onSurface,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
                IconButton(onClick = { onFindUsages("") }, minWidth = 36.dp) {
                    Icon(
                        imageVector = MiuixIcons.Search,
                        contentDescription = Strings.viewerFindUsages,
                        tint = colors.onSurface,
                        modifier = Modifier.size(19.dp),
                    )
                }
                if (content is Loadable.Success) {
                    IconButton(onClick = { onCopy(content.value.text) }, minWidth = 36.dp) {
                        Icon(
                            imageVector = MiuixIcons.Copy,
                            contentDescription = Strings.actionCopyAll,
                            tint = colors.onSurface,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
                IconButton(onClick = onToggleLineNumbers, minWidth = 36.dp) {
                    Icon(
                        imageVector = MiuixIcons.ListView,
                        contentDescription = Strings.viewerLineNumbers,
                        tint = if (showLineNumbers) colors.primary else colors.onSurfaceVariantActions,
                        modifier = Modifier.size(19.dp),
                    )
                }
                IconButton(onClick = onReload, minWidth = 36.dp) {
                    Icon(
                        imageVector = MiuixIcons.Refresh,
                        contentDescription = Strings.actionReload,
                        tint = colors.onSurface,
                        modifier = Modifier.size(19.dp),
                    )
                }
            },
        )

        when (content) {
            is Loadable.Loading, Loadable.Idle -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is Loadable.Failure -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    EmptyHint(
                        title = Strings.viewerOpenFailed,
                        subtitle = content.message,
                        action = {
                            Button(onClick = onReload, colors = ButtonDefaults.buttonColorsPrimary()) {
                                Text(text = Strings.actionRetry, fontSize = 14.sp)
                            }
                        },
                    )
                }
            }

            is Loadable.Success -> {
                val file = content.value
                // 紧凑布局里只保留真正有信息量的提示（截断 / 跳转到指定行）
                if (!compact || file.truncated || state.targetLine != null) {
                    FileMeta(file = file, targetLine = state.targetLine)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(
                            start = if (compact) 6.dp else 12.dp,
                            end = if (compact) 6.dp else 12.dp,
                            top = if (compact) 0.dp else 4.dp,
                            bottom = if (compact) 6.dp else 12.dp,
                        )
                        .clip(RoundedCornerShape(if (compact) 12.dp else 18.dp)),
                ) {
                    CodeBlock(
                        text = file.text,
                        tokens = state.tokens,
                        showLineNumbers = showLineNumbers,
                        targetLine = state.targetLine,
                        zoom = zoom,
                        onZoomChange = { zoom = it },
                        onWordDoubleTap = onFindUsages,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun PathLine(path: String) {
    if (path.isBlank()) return
    Text(
        text = path,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        fontSize = 11.sp,
        maxLines = 1,
        overflow = TextOverflow.MiddleEllipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 2.dp),
    )
}

@Composable
private fun FileMeta(file: FileContent, targetLine: Int?) {
    val colors = MiuixTheme.colorScheme
    val lineCount = remember(file.text) { file.text.count { it == '\n' } + 1 }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(colors.tertiaryContainer)
                .padding(horizontal = 7.dp, vertical = 3.dp),
        ) {
            Text(text = file.language.displayName, fontSize = 10.sp, color = colors.onTertiaryContainer)
        }
        Text(text = Strings.viewerLineCount(lineCount), fontSize = 11.sp, color = colors.onSurfaceVariantSummary)
        if (file.sizeBytes > 0) {
            Text(text = formatSize(file.sizeBytes), fontSize = 11.sp, color = colors.onSurfaceVariantSummary)
        }
        if (targetLine != null) {
            Text(
                text = Strings.viewerTargetLine(targetLine),
                fontSize = 11.sp,
                color = colors.primary,
                maxLines = 1,
            )
        }
        if (file.truncated) {
            Text(
                text = Strings.viewerTruncated,
                fontSize = 11.sp,
                color = colors.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
