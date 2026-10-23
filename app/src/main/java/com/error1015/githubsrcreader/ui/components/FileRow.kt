package com.error1015.githubsrcreader.ui.components

import com.error1015.githubsrcreader.i18n.Strings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.error1015.githubsrcreader.data.model.FileEntry
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 文件 / 目录行，用于浏览页列表。 */
@Composable
fun FileRow(
    entry: FileEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntryBadge(entry)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = colors.onSurface,
            )
            val hint = if (entry.isDirectory) Strings.commonFolder else formatSize(entry.size)
            Text(
                text = hint,
                fontSize = 11.sp,
                color = colors.onSurfaceVariantSummary,
            )
        }
        if (entry.isDirectory) {
            Icon(
                imageVector = MiuixIcons.ChevronForward,
                contentDescription = null,
                tint = colors.onSurfaceVariantActions,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** 左侧圆角徽标：目录用图标，文件用扩展名。 */
@Composable
fun EntryBadge(entry: FileEntry, modifier: Modifier = Modifier) {
    val colors = MiuixTheme.colorScheme
    val isDir = entry.isDirectory
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(
                if (isDir) colors.primary.copy(alpha = 0.12f) else colors.surfaceContainerHigh.copy(alpha = 0.7f),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isDir) {
            Icon(
                imageVector = MiuixIcons.Folder,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(
                text = extensionLabel(entry.name),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onSurfaceContainerVariant,
            )
        }
    }
}

/** 空状态提示。 */
@Composable
fun EmptyHint(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title, fontSize = 15.sp, color = colors.onSurfaceSecondary)
        Spacer(Modifier.height(6.dp))
        Text(text = subtitle, fontSize = 12.sp, color = colors.onSurfaceVariantSummary)
        if (action != null) {
            Spacer(Modifier.height(18.dp))
            action()
        }
    }
}

private fun extensionLabel(name: String): String {
    val ext = name.substringAfterLast('.', "").uppercase()
    if (ext.isEmpty()) return "TXT"
    return ext.take(4)
}

fun formatSize(bytes: Long): String {
    if (bytes < 0) return ""
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    return String.format("%.1f GB", mb / 1024.0)
}
