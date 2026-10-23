package com.error1015.githubsrcreader.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.error1015.githubsrcreader.i18n.label
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 底部导航的四个分区（显示名称见 `i18n/Labels.kt`）。 */
enum class AppTab(val route: String, val icon: ImageVector) {
    Home("home", MiuixIcons.Home),
    Projects("projects", MiuixIcons.ListView),
    Browse("browse", MiuixIcons.Folder),
    Settings("settings", MiuixIcons.Settings),
}

/** 悬浮底栏的外层圆角。 */
private val BarCornerRadius = 26.dp

/** 外层与内层之间的留白，内层圆角 = 外层圆角 - 留白，保证圆角同心对齐。 */
private val BarInset = 6.dp

/**
 * 分区的视觉高度。点按区域不是这个尺寸——每个分区都撑满整条栏的高度（见下方 `fillMaxHeight`），
 * 否则外层留白（上下各 [BarInset]）会成为点不到的死区，摸上去像「底栏点了没反应」。
 */
private val BarItemHeight = 40.dp

/**
 * 悬浮底栏：外层与选中态使用同一套 squircle 曲线，圆角按同心圆规则计算。
 *
 * 整条栏（含留白与分区之间的间距）都在可点范围内：视觉上的胶囊只有 [BarItemHeight] 高，
 * 但每个分区的点击热区从栏顶一直延伸到栏底，间距也算在热区里。
 */
@Composable
fun AppBottomBar(
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    val innerCornerRadius = BarCornerRadius - BarInset
    val interactions = remember { AppTab.entries.associateWith { MutableInteractionSource() } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .height(BarItemHeight + BarInset * 2)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(BarCornerRadius),
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.28f),
                    spotColor = Color.Black.copy(alpha = 0.28f),
                )
                .squircleBackground(color = colors.surfaceContainer, cornerRadius = BarCornerRadius)
                // 留白处也吃掉点击：底栏悬在列表上方，漏下去会误开文件
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(horizontal = BarInset),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTab.entries.forEach { tab ->
                val isSelected = tab == selected
                val tint = if (isSelected) colors.primary else colors.onSurfaceSecondary.copy(alpha = 0.6f)
                Box(
                    modifier = Modifier
                        // 热区撑满整条栏，并且把分区之间的间距也包进来，避免夹缝点不到
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = interactions.getValue(tab),
                            indication = null,
                        ) { onSelect(tab) }
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = Modifier
                            .height(BarItemHeight)
                            .squircleBackground(
                                color = if (isSelected) colors.primary.copy(alpha = 0.12f) else Color.Transparent,
                                cornerRadius = innerCornerRadius,
                            )
                            // 四个分区时只给选中项展开文字，英文下也放得下
                            .padding(horizontal = if (isSelected) 14.dp else 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = tint,
                            modifier = Modifier.size(22.dp),
                        )
                        if (isSelected) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = tab.label,
                                color = tint,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}
