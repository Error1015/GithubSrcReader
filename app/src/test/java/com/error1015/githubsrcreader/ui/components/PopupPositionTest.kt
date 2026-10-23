package com.error1015.githubsrcreader.ui.components

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

/** 浮层定位的纯计算：跑偏会在真机上表现为「菜单跑到屏幕外」，这里先把边界情况钉死。 */
class PopupPositionTest {

    private val window = IntSize(1080, 2340)
    private val popup = IntSize(600, 400)
    private val margin = 24
    private val gap = 16

    private fun offset(anchor: PopupAnchor) =
        popupOffset(anchor, window, popup, margin, gap)

    @Test
    fun `opens below the anchor when there is room`() {
        val result = offset(PopupAnchor(x = 100, y = 300, width = 120, height = 120))
        assertEquals(IntOffset(100, 300 + 120 + gap), result)
    }

    @Test
    fun `flips above when the anchor is near the bottom`() {
        val anchor = PopupAnchor(x = 100, y = 2000, width = 120, height = 120)
        val result = offset(anchor)
        assertEquals(2000 - 400 - gap, result.y)
    }

    @Test
    fun `aligns to the right edge when the menu would overflow`() {
        // 触发点在右侧：菜单右边缘与锚点右边缘对齐
        val anchor = PopupAnchor(x = 900, y = 400, width = 120, height = 120)
        val result = offset(anchor)
        assertEquals(900 + 120 - 600, result.x)
    }

    @Test
    fun `never leaves the window`() {
        // 极端情况：锚点在角落且菜单几乎占满屏幕
        val hugePopup = IntSize(window.width - 10, window.height - 10)
        val result = popupOffset(
            anchor = PopupAnchor(x = window.width - 4, y = window.height - 4, width = 4, height = 4),
            windowSize = window,
            popupSize = hugePopup,
            marginPx = margin,
            gapPx = gap,
        )
        assertEquals(margin, result.x)
        assertEquals(margin, result.y)
    }

    @Test
    fun `anchor at the very left stays at the margin`() {
        val result = offset(PopupAnchor(x = 0, y = 500, width = 80, height = 80))
        assertEquals(margin, result.x)
    }
}
