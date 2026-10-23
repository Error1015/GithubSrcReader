package com.error1015.githubsrcreader.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.error1015.githubsrcreader.i18n.Strings
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 贴住点击位置的浮层（类似 KernelSU 的长按菜单）：
 * 菜单、输入、确认三种形态共用同一套定位与动效，避免弹「屏幕正中的大对话框」。
 */

/** 触发点在窗口坐标系里的位置与尺寸，用于决定浮层出现在哪一侧。 */
data class PopupAnchor(
    val x: Int,
    val y: Int,
    val width: Int = 0,
    val height: Int = 0,
)

/** 记录某个控件在窗口里的位置；控件挂 [anchorTo] 后点开浮层即可拿到锚点。 */
@Stable
class AnchorHandle {
    private val state = mutableStateOf(PopupAnchor(0, 0))
    val anchor: PopupAnchor get() = state.value

    internal fun update(value: PopupAnchor) {
        state.value = value
    }
}

@Composable
fun rememberAnchorHandle(): AnchorHandle = remember { AnchorHandle() }

/** 在控件上记录窗口位置，供浮层定位。 */
fun Modifier.anchorTo(handle: AnchorHandle): Modifier = onGloballyPositioned { coordinates ->
    val position = coordinates.positionInWindow()
    handle.update(
        PopupAnchor(
            x = position.x.toInt(),
            y = position.y.toInt(),
            width = coordinates.size.width,
            height = coordinates.size.height,
        ),
    )
}

private val MenuCornerRadius = 18.dp
private val BubbleCornerRadius = 22.dp

/** 浮层靠左还是靠右展开（决定动画的缩放原点）。 */
private fun PopupAnchor.expandsFromLeft(screenWidthPx: Int): Boolean =
    x + width / 2 <= screenWidthPx / 2

/**
 * 定位：水平左对齐锚点、放不下就右对齐；垂直优先下方，下方空间不足则翻到上方，
 * 最后再整体夹紧在窗口内，避免被状态栏 / 屏幕边缘截断。
 *
 * 抽成纯函数便于单测（浮层位置算错会跑到屏幕外，这类问题在真机上很难复现）。
 */
internal fun popupOffset(
    anchor: PopupAnchor,
    windowSize: IntSize,
    popupSize: IntSize,
    marginPx: Int,
    gapPx: Int,
): IntOffset {
    val maxX = (windowSize.width - popupSize.width - marginPx).coerceAtLeast(marginPx)
    val spaceRight = windowSize.width - marginPx - anchor.x
    val spaceLeft = anchor.x + anchor.width - marginPx
    val x = when {
        // 默认左边缘对齐触发控件
        popupSize.width <= spaceRight -> anchor.x
        // 右侧放不下就右边缘对齐，菜单会朝左边展开
        popupSize.width <= spaceLeft -> anchor.x + anchor.width - popupSize.width
        // 两侧都放不下（超宽菜单）：先按左对齐再夹紧
        else -> anchor.x
    }.coerceIn(marginPx, maxX)

    val below = anchor.y + anchor.height + gapPx
    val above = anchor.y - popupSize.height - gapPx
    val maxY = (windowSize.height - popupSize.height - marginPx).coerceAtLeast(marginPx)
    val fitsBelow = below + popupSize.height <= windowSize.height - marginPx
    val y = (if (fitsBelow) below else above).coerceIn(marginPx, maxY)
    return IntOffset(x, y)
}

private class AnchorPopupPositionProvider(
    private val anchor: PopupAnchor,
    private val marginPx: Int,
    private val gapPx: Int,
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = popupOffset(anchor, windowSize, popupContentSize, marginPx, gapPx)
}

@Composable
private fun AnchoredPopup(
    anchor: PopupAnchor,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val marginPx = with(density) { 8.dp.roundToPx() }
    val gapPx = with(density) { 6.dp.roundToPx() }
    val provider = remember(anchor, marginPx, gapPx) {
        AnchorPopupPositionProvider(anchor, marginPx, gapPx)
    }
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }

    // 从锚点位置轻轻「长出来」：缩放 + 淡入，比直接闪现自然
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(durationMillis = 140)) }
    val fromLeft = anchor.expandsFromLeft(screenWidthPx)
    val latestDismiss by rememberUpdatedState(onDismiss)

    Popup(
        popupPositionProvider = provider,
        onDismissRequest = { latestDismiss() },
        properties = PopupProperties(focusable = true),
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                val value = progress.value
                alpha = value
                val scale = 0.92f + 0.08f * value
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(if (fromLeft) 0f else 1f, 0f)
            },
        ) {
            content()
        }
    }
}

/** 菜单项：标签 + 是否危险操作（用错误色）。 */
data class PopupMenuItem(
    val label: String,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

/** 贴着点击位置弹出的选项菜单。[title] 可作为分组标题显示在顶部。 */
@Composable
fun PopupMenu(
    anchor: PopupAnchor,
    items: List<PopupMenuItem>,
    onDismiss: () -> Unit,
    title: String? = null,
) {
    val colors = MiuixTheme.colorScheme
    AnchoredPopup(anchor = anchor, onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(min = 176.dp, max = 264.dp)
                .heightIn(max = 340.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(MenuCornerRadius),
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.22f),
                    spotColor = Color.Black.copy(alpha = 0.22f),
                )
                .background(colors.surfaceContainerHigh, RoundedCornerShape(MenuCornerRadius))
                .verticalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariantSummary,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 8.dp),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp, vertical = 0.dp))
            }
            items.forEach { item ->
                Text(
                    text = item.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (item.destructive) colors.error else colors.onSurface,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { item.onClick() }
                        .padding(horizontal = 18.dp, vertical = 11.dp),
                )
            }
        }
    }
}

/** 贴着触发点弹出的输入浮层（新建分组、重命名、添加链接）。 */
@Composable
fun PopupPrompt(
    anchor: PopupAnchor,
    title: String,
    label: String,
    confirmText: String,
    initial: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    var value by remember { mutableStateOf(initial) }
    val focusRequester = remember { FocusRequester() }

    AnchoredPopup(anchor = anchor, onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .width(284.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(BubbleCornerRadius),
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.24f),
                    spotColor = Color.Black.copy(alpha = 0.24f),
                )
                .background(colors.surfaceContainerHigh, RoundedCornerShape(BubbleCornerRadius))
                .padding(start = 16.dp, end = 16.dp, top = 15.dp, bottom = 8.dp),
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface,
                maxLines = 1,
            )
            Spacer(Modifier.height(10.dp))
            TextField(
                value = value,
                onValueChange = { value = it },
                label = label,
                useLabelAsPlaceholder = true,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
            // 弹出后直接聚焦输入框（必须放在浮层内容里，太早调用时输入框还没挂到窗口上）
            LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                PopupTextAction(
                    text = Strings.actionCancel,
                    onClick = onDismiss,
                )
                PopupTextAction(
                    text = confirmText,
                    primary = true,
                    enabled = value.isNotBlank(),
                    onClick = { onConfirm(value) },
                )
            }
        }
    }
}

/** 贴着触发点弹出的二次确认浮层。 */
@Composable
fun PopupConfirm(
    anchor: PopupAnchor,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    val colors = MiuixTheme.colorScheme
    AnchoredPopup(anchor = anchor, onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(BubbleCornerRadius),
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.24f),
                    spotColor = Color.Black.copy(alpha = 0.24f),
                )
                .background(colors.surfaceContainerHigh, RoundedCornerShape(BubbleCornerRadius))
                .padding(start = 16.dp, end = 16.dp, top = 15.dp, bottom = 8.dp),
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(text = message, fontSize = 12.sp, color = colors.onSurfaceVariantSummary)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                PopupTextAction(
                    text = Strings.actionCancel,
                    onClick = onDismiss,
                )
                PopupTextAction(
                    text = confirmText,
                    primary = true,
                    danger = destructive,
                    onClick = onConfirm,
                )
            }
        }
    }
}

/** 浮层里的文字按钮，比实体按钮轻，不会把气泡撑得笨重。 */
@Composable
private fun PopupTextAction(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
    minWidth: Dp = 0.dp,
) {
    val colors = MiuixTheme.colorScheme
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            color = Color.Transparent,
            disabledColor = Color.Transparent,
            contentColor = when {
                danger -> colors.error
                primary -> colors.primary
                else -> colors.onSurfaceSecondary
            },
            disabledContentColor = colors.disabledOnSurface,
        ),
        cornerRadius = 12.dp,
        minWidth = minWidth,
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (primary) FontWeight.Medium else FontWeight.Normal,
        )
    }
}
