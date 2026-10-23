package com.error1015.githubsrcreader.ui.code

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.error1015.githubsrcreader.data.search.ReferenceSearch

/** 代码正文字号与行高，行号列与跳转滚动都依赖它。 */
private const val CodeFontSizeSp = 13f
private const val CodeLineHeightSp = 19f

/** 双指缩放范围。 */
private const val MinZoom = 0.7f
private const val MaxZoom = 3.0f

/**
 * 只读代码视图：行号 + 双向滚动 + 语法着色，支持长按选择复制、双指缩放；
 * [targetLine] 用于从「查找引用」跳转后定位，[onWordDoubleTap] 双击标识符回调。
 *
 * 缩放比例由调用方持有（[zoom] / [onZoomChange]）：刷新内容时组合不重建，缩放自然保留；
 * 退出预览后状态随屏幕销毁，下次进入回到 1f。
 */
@Composable
fun CodeBlock(
    text: String,
    tokens: List<CodeToken>,
    modifier: Modifier = Modifier,
    showLineNumbers: Boolean = true,
    targetLine: Int? = null,
    zoom: Float = 1f,
    onZoomChange: (Float) -> Unit = {},
    onWordDoubleTap: ((String) -> Unit)? = null,
) {
    val colors = LocalCodeColors.current
    val density = LocalDensity.current

    val codeStyle = remember(zoom) {
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = (CodeFontSizeSp * zoom).sp,
            lineHeight = (CodeLineHeightSp * zoom).sp,
            letterSpacing = 0.sp,
        )
    }

    val annotated: AnnotatedString = remember(text, tokens, colors) {
        buildAnnotatedString {
            append(text)
            tokens.forEach { token ->
                if (token.start >= 0 && token.start < token.end && token.end <= text.length) {
                    addStyle(SpanStyle(color = colors.colorOf(token.kind)), token.start, token.end)
                }
            }
            // 彩虹括号：按嵌套层级取色，覆盖在原有 token 色之上
            val palette = colors.rainbow
            if (palette.isNotEmpty()) {
                RainbowBrackets.spans(text, tokens).forEach { bracket ->
                    addStyle(
                        SpanStyle(color = palette[bracket.level % palette.size]),
                        bracket.start,
                        bracket.end,
                    )
                }
            }
        }
    }

    val lineCount = remember(text) { countLines(text) }
    val numberText = remember(lineCount) {
        buildString {
            for (index in 1..lineCount) {
                append(index)
                if (index != lineCount) append('\n')
            }
        }
    }

    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    val lineHeightPx = with(density) { (CodeLineHeightSp * zoom).sp.toPx() }

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val currentZoom = rememberUpdatedState(zoom)

    // 跳转定位：滚到目标行上方两行处，保留上下文。
    // 这里**不**把 lineHeightPx 当 key：缩放时重跑会把视图拽回顶部/目标行，手感很差；
    // 换文件或指定新行号时才重新定位，用的还是当前缩放下的行高。
    LaunchedEffect(text, targetLine) {
        val offset = if (targetLine != null && targetLine > 0) {
            ((targetLine - 1) * lineHeightPx - lineHeightPx * 2f).coerceAtLeast(0f)
        } else {
            0f
        }
        verticalScroll.scrollTo(offset.toInt())
    }

    Box(
        modifier = modifier
            .background(colors.background)
            .pointerInput(Unit) {
                // 只在出现第二根手指时接管事件（Initial 阶段先于内层滚动容器处理），
                // 单指拖动依旧交给横向 / 纵向滚动，互不干扰。
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pressed = event.changes.any { it.pressed }
                        if (event.changes.size > 1) {
                            val zoomChange = event.calculateZoom()
                            if (zoomChange.isFinite() && zoomChange > 0f && zoomChange != 1f) {
                                val next = (currentZoom.value * zoomChange).coerceIn(MinZoom, MaxZoom)
                                if (next != currentZoom.value) onZoomChange(next)
                            }
                            // 消费掉多指事件，避免内层同时滚动造成抖动
                            event.changes.forEach { it.consume() }
                        }
                        if (!pressed) break
                    }
                }
            },
    ) {
        Row(modifier = Modifier.fillMaxSize().verticalScroll(verticalScroll)) {
            if (showLineNumbers) {
                BasicText(
                    text = numberText,
                    style = codeStyle.copy(color = colors.lineNumber, textAlign = TextAlign.End),
                    softWrap = false,
                    modifier = Modifier.padding(
                        start = 12.dp,
                        end = 10.dp,
                        top = 10.dp,
                        bottom = 24.dp,
                    ),
                )
            }
            Box(modifier = Modifier.weight(1f).horizontalScroll(horizontalScroll)) {
                SelectionContainer {
                    BasicText(
                        text = annotated,
                        style = codeStyle.copy(color = colors.plain),
                        softWrap = false,
                        onTextLayout = { layoutResult = it },
                        modifier = Modifier
                            .pointerInput(text, onWordDoubleTap) {
                                if (onWordDoubleTap != null) {
                                    detectTapGestures(
                                        onDoubleTap = { position ->
                                            val layout = layoutResult
                                            if (layout != null) {
                                                val word = ReferenceSearch.wordAt(
                                                    text = text,
                                                    offset = layout.getOffsetForPosition(position),
                                                )
                                                if (word != null) onWordDoubleTap(word)
                                            }
                                        },
                                    )
                                }
                            }
                            .padding(start = 4.dp, end = 24.dp, top = 10.dp, bottom = 24.dp),
                    )
                }
            }
        }
    }
}

private fun countLines(text: String): Int {
    if (text.isEmpty()) return 1
    var count = 1
    for (char in text) if (char == '\n') count++
    return count
}

fun CodeColors.colorOf(kind: TokenKind) = when (kind) {
    TokenKind.KEYWORD -> keyword
    TokenKind.STRING -> string
    TokenKind.COMMENT -> comment
    TokenKind.NUMBER -> number
    TokenKind.FUNCTION -> function
    TokenKind.TYPE -> type
    TokenKind.META -> meta
    TokenKind.PROPERTY -> property
}
