package com.error1015.githubsrcreader.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.error1015.githubsrcreader.R
import com.error1015.githubsrcreader.data.model.RecentSource
import com.error1015.githubsrcreader.ui.HomeUiState
import com.error1015.githubsrcreader.ui.components.AppTitleHeader
import com.error1015.githubsrcreader.ui.components.EmptyHint
import com.error1015.githubsrcreader.ui.util.readClipboard
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddFolder
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.CloudFill
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HomeScreen(
    state: HomeUiState,
    onUrlChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onFolderPicked: (Uri) -> Unit,
    onOpenRecent: (RecentSource) -> Unit,
    onRemoveRecent: (String) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) onFolderPicked(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 132.dp),
        ) {
            AppTitleHeader(
                title = stringResource(R.string.app_name),
                subtitle = "解析 GitHub 链接，或打开本地文件夹阅读代码",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
            )

            Card(
                cornerRadius = 20.dp,
                insideMargin = PaddingValues(18.dp),
            ) {
                Text(
                    text = "GitHub 链接",
                    fontSize = 13.sp,
                    color = colors.onSurfaceSecondary,
                )
                Spacer(Modifier.height(10.dp))
                TextField(
                    value = state.urlInput,
                    onValueChange = onUrlChange,
                    label = "github.com/owner/repo/tree/main",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.error?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(text = message, fontSize = 12.sp, color = colors.error)
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { context.readClipboard()?.let(onUrlChange) },
                        colors = ButtonDefaults.buttonColors(),
                    ) {
                        Text(text = "粘贴", fontSize = 14.sp)
                    }
                    Button(
                        onClick = onSubmit,
                        enabled = !state.busy,
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = if (state.busy) "解析中…" else "打开", fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "支持仓库 / 分支 / 目录 / 文件链接，也可直接输入 owner/repo",
                    fontSize = 11.sp,
                    color = colors.onSurfaceVariantSummary,
                )
            }

            Spacer(Modifier.height(12.dp))

            Card(
                cornerRadius = 20.dp,
                insideMargin = PaddingValues(16.dp),
                onClick = { folderPicker.launch(null) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.AddFolder,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "打开本地文件夹", fontSize = 15.sp)
                        Text(
                            text = "通过系统文件选择器授权读取",
                            fontSize = 11.sp,
                            color = colors.onSurfaceVariantSummary,
                        )
                    }
                    Icon(
                        imageVector = MiuixIcons.ChevronForward,
                        contentDescription = null,
                        tint = colors.onSurfaceVariantActions,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            SmallTitle("最近打开")

            Card(
                cornerRadius = 20.dp,
                insideMargin = PaddingValues(0.dp),
            ) {
                if (state.recents.isEmpty()) {
                    EmptyHint(
                        title = "还没有打开过任何仓库",
                        subtitle = "从上面的链接或本地文件夹开始",
                    )
                } else {
                    state.recents.forEachIndexed { index, recent ->
                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(start = 62.dp))
                        }
                        RecentRow(
                            recent = recent,
                            onClick = { onOpenRecent(recent) },
                            onRemove = { onRemoveRecent(recent.id) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "提示：在设置中填入 GitHub Token 可提高 API 请求上限",
                fontSize = 11.sp,
                color = colors.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

@Composable
private fun RecentRow(
    recent: RecentSource,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(colors.tertiaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (recent.type == RecentSource.Type.GITHUB) MiuixIcons.CloudFill else MiuixIcons.Folder,
                contentDescription = null,
                tint = colors.onTertiaryContainer,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 2.dp),
        ) {
            Text(text = recent.title, fontSize = 15.sp, maxLines = 1)
            Text(
                text = recent.subtitle,
                fontSize = 11.sp,
                color = colors.onSurfaceVariantSummary,
                maxLines = 1,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = MiuixIcons.Delete,
                contentDescription = "删除记录",
                tint = colors.onSurfaceVariantActions,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
