package com.error1015.githubsrcreader.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 项目内自绘的少量图标：Miuix 图标集只有 21 个，缺少下载、星标、加号这类常用符号，
 * 与其引入整包 material-icons（约 10MB），不如在这里画几个 ImageVector。
 */
object AppIcons {

    /** 下载：向下箭头 + 底部托盘。 */
    val Download: ImageVector by lazy {
        ImageVector.Builder(
            name = "Download",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 9f)
                horizontalLineToRelative(-4f)
                verticalLineTo(3f)
                horizontalLineTo(9f)
                verticalLineToRelative(6f)
                horizontalLineTo(5f)
                lineToRelative(7f, 7f)
                lineToRelative(7f, -7f)
                close()
                moveTo(5f, 18f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(14f)
                verticalLineToRelative(-2f)
                close()
            }
        }.build()
    }

    /** 空心五角星（未标星）。 */
    val Star: ImageVector by lazy {
        ImageVector.Builder(
            name = "Star",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            starPath(
                stroke = SolidColor(Color.Black),
                fill = null,
            )
        }.build()
    }

    /** 实心五角星（已标星 / 置顶）。 */
    val StarFilled: ImageVector by lazy {
        ImageVector.Builder(
            name = "StarFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            starPath(stroke = null, fill = SolidColor(Color.Black))
        }.build()
    }

    /** 加号。 */
    val Plus: ImageVector by lazy {
        ImageVector.Builder(
            name = "Plus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(11f, 5f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(6f)
                horizontalLineToRelative(6f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(-6f)
                verticalLineToRelative(6f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(-6f)
                horizontalLineTo(5f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(6f)
                close()
            }
        }.build()
    }

    /** 五角星轮廓：外径 9.2、内径 3.75，中心略偏下，视觉上更居中。 */
    private fun ImageVector.Builder.starPath(
        stroke: SolidColor?,
        fill: SolidColor?,
    ) = path(
        fill = fill,
        stroke = stroke,
        strokeLineWidth = if (stroke != null) 1.7f else 0f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        moveTo(12.00f, 3.00f)
        lineTo(14.20f, 9.17f)
        lineTo(20.75f, 9.36f)
        lineTo(15.57f, 13.36f)
        lineTo(17.41f, 19.64f)
        lineTo(12.00f, 15.95f)
        lineTo(6.59f, 19.64f)
        lineTo(8.43f, 13.36f)
        lineTo(3.25f, 9.36f)
        lineTo(9.80f, 9.17f)
        close()
    }
}
