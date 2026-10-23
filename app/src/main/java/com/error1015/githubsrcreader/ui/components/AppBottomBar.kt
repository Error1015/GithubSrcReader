package com.error1015.githubsrcreader.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 底部导航的三个分区。 */
enum class AppTab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "首页", MiuixIcons.Home),
    Browse("browse", "浏览", MiuixIcons.Folder),
    Settings("settings", "设置", MiuixIcons.Settings),
}

/** 悬浮底栏的外层圆角。 */
private val BarCornerRadius = 26.dp

/** 外层与内层之间的留白，内层圆角 = 外层圆角 - 留白，保证圆角同心对齐。 */
private val BarInset = 6.dp

/**
 * 悬浮底栏：外层与选中态使用同一套 squircle 曲线，圆角按同心圆规则计算。
 */
@Composable
fun AppBottomBar(
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    val innerCornerRadius = BarCornerRadius - BarInset

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(BarCornerRadius),
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.28f),
                    spotColor = Color.Black.copy(alpha = 0.28f),
                )
                .squircleBackground(color = colors.surfaceContainer, cornerRadius = BarCornerRadius)
                .padding(BarInset),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTab.entries.forEach { tab ->
                val isSelected = tab == selected
                val tint = if (isSelected) colors.primary else colors.onSurfaceSecondary.copy(alpha = 0.6f)
                val interactionSource = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .squircleBackground(
                            color = if (isSelected) colors.primary.copy(alpha = 0.12f) else Color.Transparent,
                            cornerRadius = innerCornerRadius,
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) { onSelect(tab) }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = tint,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = tab.label,
                        color = tint,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
