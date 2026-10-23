package com.error1015.githubsrcreader.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.error1015.githubsrcreader.data.model.FileEntry
import com.error1015.githubsrcreader.data.model.Loadable
import com.error1015.githubsrcreader.ui.BrowseUiState
import com.error1015.githubsrcreader.ui.components.EmptyHint
import com.error1015.githubsrcreader.ui.components.FileRow
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun BrowseScreen(
    state: BrowseUiState,
    onNavigateUp: () -> Boolean,
    onBack: () -> Unit,
    onReload: () -> Unit,
    onOpenEntry: (FileEntry) -> Unit,
    onGotoPath: (String) -> Unit,
    onGoHome: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val source = state.source

    BackHandler(enabled = source != null) {
        if (!onNavigateUp()) onBack()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SmallTopAppBar(
            title = source?.title ?: "浏览",
            subtitle = source?.subtitle ?: "还没有打开任何来源",
            navigationIcon = {
                IconButton(onClick = { if (!onNavigateUp()) onBack() }) {
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = "返回",
                        tint = colors.onSurface,
                        modifier = Modifier.size(22.dp),
                    )
                }
            },
            actions = {
                if (source != null) {
                    IconButton(onClick = onReload) {
                        Icon(
                            imageVector = MiuixIcons.Refresh,
                            contentDescription = "刷新",
                            tint = colors.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            },
        )

        if (source == null) {
            CardEmpty(onGoHome)
            return@Column
        }

        Breadcrumb(path = state.path, onGotoPath = onGotoPath)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surfaceContainer),
        ) {
            when (val entries = state.entries) {
                is Loadable.Loading, Loadable.Idle -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is Loadable.Failure -> {
                    EmptyHint(
                        title = "加载失败",
                        subtitle = entries.message,
                        modifier = Modifier.align(Alignment.Center),
                        action = {
                            Button(
                                onClick = onReload,
                                colors = ButtonDefaults.buttonColorsPrimary(),
                            ) {
                                Text(text = "重试", fontSize = 14.sp)
                            }
                        },
                    )
                }

                is Loadable.Success -> {
                    if (entries.value.isEmpty()) {
                        EmptyHint(
                            title = "这个文件夹是空的",
                            subtitle = "换一个目录看看",
                            modifier = Modifier.align(Alignment.Center),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 132.dp),
                        ) {
                            itemsIndexed(entries.value, key = { _, entry -> entry.path }) { index, entry ->
                                if (index > 0) {
                                    HorizontalDivider(modifier = Modifier.padding(start = 64.dp))
                                }
                                FileRow(entry = entry, onClick = { onOpenEntry(entry) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardEmpty(onGoHome: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyHint(
            title = "还没有打开任何仓库",
            subtitle = "先回到首页解析一个 GitHub 链接，或选择本地文件夹",
            action = {
                Button(onClick = onGoHome, colors = ButtonDefaults.buttonColorsPrimary()) {
                    Text(text = "去首页", fontSize = 14.sp)
                }
            },
        )
    }
}

@Composable
private fun Breadcrumb(path: String, onGotoPath: (String) -> Unit) {
    val colors = MiuixTheme.colorScheme
    val segments = path.split('/').filter { it.isNotBlank() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "根目录",
            fontSize = 12.sp,
            color = if (segments.isEmpty()) colors.primary else colors.onSurfaceVariantSummary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onGotoPath("") }
                .padding(horizontal = 4.dp, vertical = 2.dp),
        )
        var accumulated = ""
        segments.forEach { segment ->
            accumulated = if (accumulated.isEmpty()) segment else "$accumulated/$segment"
            val target = accumulated
            Text(
                text = "/",
                fontSize = 12.sp,
                color = colors.onSurfaceVariantActions,
            )
            Text(
                text = segment,
                fontSize = 12.sp,
                color = if (segment == segments.last()) colors.onSurface else colors.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onGotoPath(target) }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}
