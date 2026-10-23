package com.error1015.githubsrcreader.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.error1015.githubsrcreader.data.model.Project
import com.error1015.githubsrcreader.data.model.ProjectGroup
import com.error1015.githubsrcreader.data.model.ProjectSection
import com.error1015.githubsrcreader.i18n.Strings
import com.error1015.githubsrcreader.ui.ProjectsUiState
import com.error1015.githubsrcreader.ui.components.AnchorHandle
import com.error1015.githubsrcreader.ui.components.AppIcons
import com.error1015.githubsrcreader.ui.components.AppTitleHeader
import com.error1015.githubsrcreader.ui.components.EmptyHint
import com.error1015.githubsrcreader.ui.components.PopupAnchor
import com.error1015.githubsrcreader.ui.components.PopupConfirm
import com.error1015.githubsrcreader.ui.components.PopupMenu
import com.error1015.githubsrcreader.ui.components.PopupMenuItem
import com.error1015.githubsrcreader.ui.components.PopupPrompt
import com.error1015.githubsrcreader.ui.components.ProjectRow
import com.error1015.githubsrcreader.ui.components.anchorTo
import com.error1015.githubsrcreader.ui.components.rememberAnchorHandle
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 项目页：把所有仓库 / 本地文件夹按分组归档，星标置顶。
 * 数据与首页「最近打开」共用一份，标星与删除两处同步。
 *
 * 所有菜单与输入框都是**贴着触发点弹出**的浮层（`ui/components/Popups.kt`），
 * 不再使用居中的大对话框。
 */
@Composable
fun ProjectsScreen(
    state: ProjectsUiState,
    onAddByLink: (String) -> Unit,
    onAddByFolder: (Uri) -> Unit,
    onOpenProject: (Project) -> Unit,
    onToggleStar: (String) -> Unit,
    onCreateGroup: (String) -> Unit,
    onRenameGroup: (String, String) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onMoveProject: (String, String?) -> Unit,
    onRemoveProject: (String) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    var popup by remember { mutableStateOf<ProjectsPopup?>(null) }
    val addAnchor = rememberAnchorHandle()

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) onAddByFolder(uri)
    }

    // 「未分组」为空、也没有任何分组时不占位置
    val sections = remember(state.sections) {
        state.sections.filter { it.group != null || it.projects.isNotEmpty() }
    }
    val groups = remember(state.sections) { state.sections.mapNotNull { it.group } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        AppTitleHeader(
            title = Strings.tabProjects,
            subtitle = Strings.projectsSubtitle,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            actions = {
                IconButton(
                    onClick = { popup = ProjectsPopup.AddMenu(addAnchor.anchor) },
                    modifier = Modifier.anchorTo(addAnchor),
                    minWidth = 44.dp,
                ) {
                    Icon(
                        imageVector = AppIcons.Plus,
                        contentDescription = Strings.projectsAdd,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            },
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 140.dp),
        ) {
            if (state.isEmpty) {
                item {
                    Card(cornerRadius = 20.dp, insideMargin = PaddingValues(0.dp)) {
                        EmptyHint(
                            title = Strings.projectsEmptyTitle,
                            subtitle = Strings.projectsEmptyDesc,
                        )
                    }
                }
            }

            items(sections, key = { it.group?.id ?: "ungrouped" }) { section ->
                SectionBlock(
                    section = section,
                    onOpenProject = onOpenProject,
                    onToggleStar = onToggleStar,
                    onGroupMenu = { group, anchor -> popup = ProjectsPopup.GroupMenu(group, anchor) },
                    onProjectMenu = { project, anchor -> popup = ProjectsPopup.ProjectMenu(project, anchor) },
                )
            }
        }
    }

    when (val current = popup) {
        null -> Unit

        is ProjectsPopup.AddMenu -> PopupMenu(
            anchor = current.anchor,
            items = listOf(
                PopupMenuItem(Strings.projectsAddByLink) {
                    popup = ProjectsPopup.AddLink(current.anchor)
                },
                PopupMenuItem(Strings.projectsAddFromFolder) {
                    popup = null
                    folderPicker.launch(null)
                },
                PopupMenuItem(Strings.projectsNewGroup) {
                    popup = ProjectsPopup.NewGroup(current.anchor)
                },
            ),
            onDismiss = { popup = null },
        )

        is ProjectsPopup.AddLink -> PopupPrompt(
            anchor = current.anchor,
            title = Strings.projectsAddByLink,
            label = Strings.projectsAddLinkLabel,
            confirmText = Strings.actionConfirm,
            onConfirm = { text ->
                onAddByLink(text)
                popup = null
            },
            onDismiss = { popup = null },
        )

        is ProjectsPopup.NewGroup -> PopupPrompt(
            anchor = current.anchor,
            title = Strings.projectsNewGroup,
            label = Strings.projectsGroupName,
            confirmText = Strings.actionCreate,
            onConfirm = { name ->
                onCreateGroup(name)
                popup = null
            },
            onDismiss = { popup = null },
        )

        is ProjectsPopup.GroupMenu -> PopupMenu(
            anchor = current.anchor,
            items = listOf(
                PopupMenuItem(Strings.actionRename) {
                    popup = ProjectsPopup.RenameGroup(current.group, current.anchor)
                },
                PopupMenuItem(Strings.actionDelete, destructive = true) {
                    popup = ProjectsPopup.DeleteGroup(current.group, current.anchor)
                },
            ),
            onDismiss = { popup = null },
        )

        is ProjectsPopup.RenameGroup -> PopupPrompt(
            anchor = current.anchor,
            title = Strings.actionRename,
            label = Strings.projectsGroupName,
            confirmText = Strings.actionConfirm,
            initial = current.group.name,
            onConfirm = { name ->
                onRenameGroup(current.group.id, name)
                popup = null
            },
            onDismiss = { popup = null },
        )

        is ProjectsPopup.DeleteGroup -> PopupConfirm(
            anchor = current.anchor,
            title = Strings.dialogDeleteGroupTitle,
            message = Strings.dialogDeleteGroupMessage,
            confirmText = Strings.actionDelete,
            onConfirm = {
                onDeleteGroup(current.group.id)
                popup = null
            },
            onDismiss = { popup = null },
            destructive = true,
        )

        is ProjectsPopup.ProjectMenu -> PopupMenu(
            anchor = current.anchor,
            items = listOf(
                PopupMenuItem(Strings.projectsMoveTo) {
                    popup = ProjectsPopup.MoveProject(current.project, current.anchor)
                },
                PopupMenuItem(Strings.actionRemove, destructive = true) {
                    popup = ProjectsPopup.RemoveProject(current.project, current.anchor)
                },
            ),
            onDismiss = { popup = null },
        )

        is ProjectsPopup.MoveProject -> PopupMenu(
            anchor = current.anchor,
            items = listOf(
                PopupMenuItem(Strings.projectsUngrouped) {
                    onMoveProject(current.project.id, null)
                    popup = null
                },
            ) + groups.map { group ->
                PopupMenuItem(group.name) {
                    onMoveProject(current.project.id, group.id)
                    popup = null
                }
            },
            onDismiss = { popup = null },
        )

        is ProjectsPopup.RemoveProject -> PopupConfirm(
            anchor = current.anchor,
            title = Strings.dialogRemoveProjectTitle,
            message = Strings.dialogRemoveProjectMessage,
            confirmText = Strings.actionRemove,
            onConfirm = {
                onRemoveProject(current.project.id)
                popup = null
            },
            onDismiss = { popup = null },
            destructive = true,
        )
    }
}

@Composable
private fun SectionBlock(
    section: ProjectSection,
    onOpenProject: (Project) -> Unit,
    onToggleStar: (String) -> Unit,
    onGroupMenu: (ProjectGroup, PopupAnchor) -> Unit,
    onProjectMenu: (Project, PopupAnchor) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val groupAnchor = rememberAnchorHandle()

    Column(modifier = Modifier.padding(top = 6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 2.dp, top = 6.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = section.group?.name ?: Strings.projectsUngrouped,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onSurfaceVariantSummary,
                maxLines = 1,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = Strings.projectsCount(section.projects.size),
                fontSize = 11.sp,
                color = colors.onSurfaceVariantSummary,
            )
            Spacer(Modifier.weight(1f))
            section.group?.let { group ->
                MoreButton(
                    iconSize = 16.dp,
                    minSize = 32.dp,
                    anchorHandle = groupAnchor,
                    onClick = { anchor -> onGroupMenu(group, anchor) },
                )
            }
        }

        Card(cornerRadius = 20.dp, insideMargin = PaddingValues(0.dp)) {
            if (section.projects.isEmpty()) {
                Text(
                    text = Strings.projectsGroupEmpty,
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariantSummary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                )
            } else {
                section.projects.forEachIndexed { index, project ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(start = 62.dp))
                    }
                    // 用 key 隔开每一项：锚点靠 remember 保存，不加 key 时同一 call site 的
                    // remember 槽位会被所有行共用，浮层会跑到别的行去
                    key(project.id) {
                        ProjectRow(
                            project = project,
                            onClick = { onOpenProject(project) },
                            onToggleStar = { onToggleStar(project.id) },
                            trailing = {
                                MoreButton(
                                    iconSize = 18.dp,
                                    minSize = 36.dp,
                                    onClick = { anchor -> onProjectMenu(project, anchor) },
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

/** 「更多」按钮：自带锚点，点开时把控件位置交给浮层。 */
@Composable
private fun MoreButton(
    iconSize: Dp,
    minSize: Dp,
    onClick: (PopupAnchor) -> Unit,
    anchorHandle: AnchorHandle = rememberAnchorHandle(),
) {
    val colors = MiuixTheme.colorScheme
    IconButton(
        onClick = { onClick(anchorHandle.anchor) },
        modifier = Modifier.anchorTo(anchorHandle),
        minWidth = minSize,
        minHeight = minSize,
    ) {
        Icon(
            imageVector = MiuixIcons.More,
            contentDescription = Strings.actionMore,
            tint = colors.onSurfaceVariantActions,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** 项目页所有浮层的状态；锚点随状态一起传递，浮层才会出现在触发点旁边。 */
private sealed interface ProjectsPopup {
    data class AddMenu(val anchor: PopupAnchor) : ProjectsPopup
    data class AddLink(val anchor: PopupAnchor) : ProjectsPopup
    data class NewGroup(val anchor: PopupAnchor) : ProjectsPopup
    data class GroupMenu(val group: ProjectGroup, val anchor: PopupAnchor) : ProjectsPopup
    data class RenameGroup(val group: ProjectGroup, val anchor: PopupAnchor) : ProjectsPopup
    data class DeleteGroup(val group: ProjectGroup, val anchor: PopupAnchor) : ProjectsPopup
    data class ProjectMenu(val project: Project, val anchor: PopupAnchor) : ProjectsPopup
    data class MoveProject(val project: Project, val anchor: PopupAnchor) : ProjectsPopup
    data class RemoveProject(val project: Project, val anchor: PopupAnchor) : ProjectsPopup
}
