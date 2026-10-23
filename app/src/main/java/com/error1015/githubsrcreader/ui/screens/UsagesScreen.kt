package com.error1015.githubsrcreader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.error1015.githubsrcreader.data.model.Loadable
import com.error1015.githubsrcreader.data.model.UsageHit
import com.error1015.githubsrcreader.ui.UsageScope
import com.error1015.githubsrcreader.ui.UsageUiState
import com.error1015.githubsrcreader.ui.components.EmptyHint
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun UsagesScreen(
    state: UsageUiState,
    subtitle: String,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onScopeChange: (UsageScope) -> Unit,
    onPickSymbol: (String) -> Unit,
    onOpenHit: (UsageHit) -> Unit,
) {
    val colors = MiuixTheme.colorScheme

    Column(modifier = Modifier.fillMaxSize().background(colors.surface)) {
        SmallTopAppBar(
            title = "查找引用",
            subtitle = subtitle,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = "返回",
                        tint = colors.onSurface,
                        modifier = Modifier.size(22.dp),
                    )
                }
            },
        )

        Card(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            cornerRadius = 20.dp,
            insideMargin = PaddingValues(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    label = "类名 / 方法名 / 字段名",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onSearch,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Text(text = "搜索", fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                UsageScope.entries.forEach { scope ->
                    Button(
                        onClick = { onScopeChange(scope) },
                        colors = if (state.scope == scope) {
                            ButtonDefaults.buttonColorsPrimary()
                        } else {
                            ButtonDefaults.buttonColors()
                        },
                        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(text = scope.label, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.weight(1f))
                state.progress?.let {
                    Text(text = it, fontSize = 11.sp, color = colors.onSurfaceVariantSummary)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "提示：阅读页双击标识符可直接查找引用",
                fontSize = 11.sp,
                color = colors.onSurfaceVariantSummary,
            )
            state.hint?.let { hint ->
                Spacer(Modifier.height(8.dp))
                Text(text = hint, fontSize = 11.sp, color = colors.error)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surfaceContainer),
        ) {
            if (state.query.isBlank()) {
                if (state.symbols.isEmpty()) {
                    EmptyHint(
                        title = "输入类名 / 方法名 / 字段名",
                        subtitle = "也可以回到代码里双击标识符直接查找引用",
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        item {
                            Text(
                                text = "本文件符号 · 点击即可查找引用",
                                fontSize = 11.sp,
                                color = colors.onSurfaceVariantSummary,
                                modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp),
                            )
                        }
                        items(state.symbols, key = { "sym:${it.line}:${it.column}" }) { symbol ->
                            val symbolName = symbolNameOf(symbol)
                            UsageRow(
                                hit = symbol,
                                name = symbolName,
                                onClick = { onPickSymbol(symbolName) },
                            )
                        }
                    }
                }
            } else {
                when (val results = state.results) {
                    is Loadable.Idle -> {
                        EmptyHint(
                            title = "点「搜索」开始查找",
                            subtitle = "当前范围：${state.scope.label}",
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    is Loadable.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                state.progress?.let {
                                    Spacer(Modifier.height(10.dp))
                                    Text(text = "已扫描 $it", fontSize = 11.sp, color = colors.onSurfaceVariantSummary)
                                }
                            }
                        }
                    }

                    is Loadable.Failure -> {
                        val blocked = state.hint != null
                        EmptyHint(
                            title = if (blocked) "全仓库搜索暂不可用" else "搜索失败",
                            subtitle = state.hint ?: results.message,
                            modifier = Modifier.align(Alignment.Center),
                            action = if (blocked) {
                                null
                            } else {
                                {
                                    Button(onClick = onSearch, colors = ButtonDefaults.buttonColorsPrimary()) {
                                        Text(text = "重试", fontSize = 14.sp)
                                    }
                                }
                            },
                        )
                    }

                    is Loadable.Success -> {
                        val hits = results.value
                        if (hits.isEmpty()) {
                            EmptyHint(
                                title = "没有找到「${state.query}」的引用",
                                subtitle = "试试切换到「整个来源」，或检查拼写",
                                modifier = Modifier.align(Alignment.Center),
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 16.dp),
                            ) {
                                if (state.scope == UsageScope.SOURCE) {
                                    hits.groupBy { it.path }.forEach { (path, group) ->
                                        item(key = "h:$path") {
                                            PathHeader(path = path, count = group.size)
                                        }
                                        items(group, key = { "${it.path}:${it.line}:${it.column}" }) { hit ->
                                            UsageRow(
                                                hit = hit,
                                                name = state.query,
                                                onClick = { onOpenHit(hit) },
                                            )
                                        }
                                    }
                                } else {
                                    items(hits, key = { "${it.line}:${it.column}" }) { hit ->
                                        UsageRow(
                                            hit = hit,
                                            name = state.query,
                                            onClick = { onOpenHit(hit) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PathHeader(path: String, count: Int) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = path,
            fontSize = 11.sp,
            color = colors.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(text = "$count 处", fontSize = 11.sp, color = colors.onSurfaceVariantActions)
    }
}

@Composable
private fun UsageRow(
    hit: UsageHit,
    name: String,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val codeStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 14.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = hit.line.toString(),
            fontSize = 11.sp,
            color = colors.onSurfaceVariantActions,
            textAlign = TextAlign.End,
            modifier = Modifier.width(34.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = highlightMatch(hit.lineText.trimStart(), name, colors.primary),
                style = codeStyle.copy(color = colors.onSurface),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (hit.isDeclaration) {
                        colors.primary.copy(alpha = 0.12f)
                    } else {
                        colors.surfaceContainerHigh.copy(alpha = 0.6f)
                    },
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                text = if (hit.isDeclaration) "声明" else "引用",
                fontSize = 10.sp,
                color = if (hit.isDeclaration) colors.primary else colors.onSurfaceContainerVariant,
            )
        }
    }
}

/** 取符号名：声明行的末段标识符。 */
private fun symbolNameOf(hit: UsageHit): String {
    val line = hit.lineText
    val index = hit.column - 1
    if (index in line.indices) {
        var end = index
        while (end < line.length && (line[end].isLetterOrDigit() || line[end] == '_' || line[end] == '$')) end++
        val word = line.substring(index, end)
        if (word.isNotEmpty()) return word
    }
    return line.trim().substringAfterLast(' ')
}

private fun highlightMatch(line: String, name: String, color: Color): AnnotatedString {
    if (name.isEmpty()) return AnnotatedString(line)
    return buildAnnotatedString {
        append(line)
        var index = line.indexOf(name)
        while (index >= 0) {
            addStyle(SpanStyle(color = color, fontWeight = FontWeight.Medium), index, index + name.length)
            index = line.indexOf(name, index + name.length)
        }
    }
}
