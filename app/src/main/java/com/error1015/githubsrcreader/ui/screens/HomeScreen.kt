package com.error1015.githubsrcreader.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.error1015.githubsrcreader.R
import com.error1015.githubsrcreader.data.model.Project
import com.error1015.githubsrcreader.data.model.ProjectGroup
import com.error1015.githubsrcreader.i18n.Strings
import com.error1015.githubsrcreader.ui.HomeUiState
import com.error1015.githubsrcreader.ui.components.AppTitleHeader
import com.error1015.githubsrcreader.ui.components.EmptyHint
import com.error1015.githubsrcreader.ui.components.PopupAnchor
import com.error1015.githubsrcreader.ui.components.PopupMenu
import com.error1015.githubsrcreader.ui.components.PopupMenuItem
import com.error1015.githubsrcreader.ui.components.ProjectRow
import com.error1015.githubsrcreader.ui.components.anchorTo
import com.error1015.githubsrcreader.ui.components.rememberAnchorHandle
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
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.FolderFill
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HomeScreen(
    state: HomeUiState,
    onUrlChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onFolderPicked: (Uri) -> Unit,
    onOpenProject: (Project) -> Unit,
    onToggleStar: (String) -> Unit,
    onRemoveProject: (String) -> Unit,
    onSaveToLibrary: (String, String?) -> Unit,
    onRemoveFromLibrary: (String) -> Unit,
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
                subtitle = Strings.homeTagline,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
            )

            Card(
                cornerRadius = 20.dp,
                insideMargin = PaddingValues(18.dp),
            ) {
                Text(
                    text = Strings.homeGithubLink,
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
                        Text(text = Strings.actionPaste, fontSize = 14.sp)
                    }
                    Button(
                        onClick = onSubmit,
                        enabled = !state.busy,
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = if (state.busy) Strings.homeParsing else Strings.actionOpen, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = Strings.homeLinkHint,
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
                        Text(text = Strings.homeOpenFolder, fontSize = 15.sp)
                        Text(
                            text = Strings.homeOpenFolderDesc,
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

            SmallTitle(Strings.homeRecent)

            Card(
                cornerRadius = 20.dp,
                insideMargin = PaddingValues(0.dp),
            ) {
                if (state.recents.isEmpty()) {
                    EmptyHint(
                        title = Strings.homeRecentEmptyTitle,
                        subtitle = Strings.homeRecentEmptyDesc,
                    )
                } else {
                    state.recents.forEachIndexed { index, project ->
                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(start = 62.dp))
                        }
                        // key 隔开每一行：加入项目库的按钮要按行记住锚点
                        key(project.id) {
                            ProjectRow(
                                project = project,
                                onClick = { onOpenProject(project) },
                                onToggleStar = { onToggleStar(project.id) },
                                trailing = {
                                    LibraryButton(
                                        project = project,
                                        groups = state.groups,
                                        onSave = onSaveToLibrary,
                                        onRemove = onRemoveFromLibrary,
                                    )
                                    IconButton(
                                        onClick = { onRemoveProject(project.id) },
                                        minWidth = 36.dp,
                                        minHeight = 36.dp,
                                    ) {
                                        Icon(
                                            imageVector = MiuixIcons.Delete,
                                            contentDescription = Strings.actionDeleteRecord,
                                            tint = colors.onSurfaceVariantActions,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = Strings.homeTokenHint,
                fontSize = 11.sp,
                color = colors.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

/**
 * 「加入项目库」按钮：点开贴着按钮的菜单选分组。
 * 已在库里时按钮变成实心文件夹，菜单里多一项「从项目库移除」。
 */
@Composable
private fun LibraryButton(
    project: Project,
    groups: List<ProjectGroup>,
    onSave: (String, String?) -> Unit,
    onRemove: (String) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val handle = rememberAnchorHandle()
    var menuAnchor by remember { mutableStateOf<PopupAnchor?>(null) }

    IconButton(
        onClick = { menuAnchor = handle.anchor },
        modifier = Modifier.anchorTo(handle),
        minWidth = 36.dp,
        minHeight = 36.dp,
    ) {
        Icon(
            imageVector = if (project.saved) MiuixIcons.FolderFill else MiuixIcons.AddFolder,
            contentDescription = if (project.saved) Strings.actionRemoveFromLibrary else Strings.projectsAddToLibrary,
            tint = if (project.saved) colors.primary else colors.onSurfaceVariantActions,
            modifier = Modifier.size(18.dp),
        )
    }

    menuAnchor?.let { anchor ->
        PopupMenu(
            anchor = anchor,
            title = if (project.saved) Strings.projectsMoveTo else Strings.projectsAddToLibrary,
            items = buildList {
                if (project.saved) {
                    add(
                        PopupMenuItem(Strings.actionRemoveFromLibrary, destructive = true) {
                            onRemove(project.id)
                            menuAnchor = null
                        },
                    )
                }
                add(
                    PopupMenuItem(Strings.projectsUngrouped) {
                        onSave(project.id, null)
                        menuAnchor = null
                    },
                )
                groups.forEach { group ->
                    add(
                        PopupMenuItem(group.name) {
                            onSave(project.id, group.id)
                            menuAnchor = null
                        },
                    )
                }
            },
            onDismiss = { menuAnchor = null },
        )
    }
}
