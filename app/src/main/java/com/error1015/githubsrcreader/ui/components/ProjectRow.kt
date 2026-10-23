package com.error1015.githubsrcreader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.error1015.githubsrcreader.data.model.Project
import com.error1015.githubsrcreader.i18n.Strings
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.CloudFill
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 项目 / 最近打开共用的一行：图标 + 标题 + 标星 + 尾部操作。
 * 首页尾部放「删除」，项目页尾部放「更多」。
 */
@Composable
fun ProjectRow(
    project: Project,
    onClick: () -> Unit,
    onToggleStar: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 2.dp),
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
                    imageVector = if (project.type == Project.Type.GITHUB) MiuixIcons.CloudFill else MiuixIcons.Folder,
                    contentDescription = null,
                    tint = colors.onTertiaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = project.title, fontSize = 15.sp, maxLines = 1)
                Text(
                    text = project.subtitle,
                    fontSize = 11.sp,
                    color = colors.onSurfaceVariantSummary,
                    maxLines = 1,
                )
            }
        }

        IconButton(onClick = onToggleStar, minWidth = 36.dp, minHeight = 36.dp) {
            Icon(
                imageVector = if (project.starred) AppIcons.StarFilled else AppIcons.Star,
                contentDescription = if (project.starred) Strings.actionUnstar else Strings.actionStar,
                tint = if (project.starred) colors.primary else colors.onSurfaceVariantActions,
                modifier = Modifier.size(18.dp),
            )
        }
        trailing()
    }
}
