package com.error1015.githubsrcreader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.error1015.githubsrcreader.BuildConfig
import com.error1015.githubsrcreader.R
import com.error1015.githubsrcreader.data.model.ThemeMode
import com.error1015.githubsrcreader.i18n.AppLanguage
import com.error1015.githubsrcreader.i18n.Strings
import com.error1015.githubsrcreader.i18n.label
import com.error1015.githubsrcreader.ui.SettingsUiState
import com.error1015.githubsrcreader.ui.components.AppTitleHeader
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onTokenChange: (String) -> Unit,
    onSaveToken: () -> Unit,
    onClearToken: () -> Unit,
    onToggleLineNumbers: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    val colors = MiuixTheme.colorScheme

    // 输入框内部保留 TextFieldValue 以维持光标；保存后外部清空草稿时同步回来
    var field by remember { mutableStateOf(TextFieldValue("")) }
    LaunchedEffect(state.tokenDraft) {
        if (field.text != state.tokenDraft) {
            field = TextFieldValue(state.tokenDraft, TextRange(state.tokenDraft.length))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .statusBarsPadding(),
    ) {
        AppTitleHeader(
            title = stringResource(R.string.app_name),
            subtitle = Strings.settingsSubtitle,
            modifier = Modifier.padding(start = 24.dp, end = 20.dp, top = 14.dp, bottom = 10.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 132.dp),
        ) {
            SmallTitle(Strings.settingsAppearance)
            Card(
                modifier = Modifier.padding(horizontal = 16.dp),
                cornerRadius = 20.dp,
                insideMargin = PaddingValues(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val selected = state.themeMode == mode
                        Button(
                            onClick = { onThemeModeChange(mode) },
                            modifier = Modifier.weight(1f),
                            colors = if (selected) {
                                ButtonDefaults.buttonColorsPrimary()
                            } else {
                                ButtonDefaults.buttonColors()
                            },
                        ) {
                            Text(text = mode.label, fontSize = 13.sp)
                        }
                    }
                }
            }

            SmallTitle(Strings.settingsLanguage)
            Card(
                modifier = Modifier.padding(horizontal = 16.dp),
                cornerRadius = 20.dp,
                insideMargin = PaddingValues(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AppLanguage.entries.forEach { language ->
                        val selected = state.language == language
                        Button(
                            onClick = { onLanguageChange(language) },
                            modifier = Modifier.weight(1f),
                            colors = if (selected) {
                                ButtonDefaults.buttonColorsPrimary()
                            } else {
                                ButtonDefaults.buttonColors()
                            },
                        ) {
                            Text(text = language.label, fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
            }

            SmallTitle(Strings.settingsReading)
            Card(
                modifier = Modifier.padding(horizontal = 16.dp),
                cornerRadius = 20.dp,
                insideMargin = PaddingValues(0.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = Strings.settingsShowLineNumbers, fontSize = 15.sp)
                        Text(
                            text = Strings.settingsShowLineNumbersDesc,
                            fontSize = 11.sp,
                            color = colors.onSurfaceVariantSummary,
                        )
                    }
                    Switch(
                        checked = state.showLineNumbers,
                        onCheckedChange = onToggleLineNumbers,
                    )
                }
            }

            SmallTitle(Strings.settingsTokenSection)
            Card(
                modifier = Modifier.padding(horizontal = 16.dp),
                cornerRadius = 20.dp,
                insideMargin = PaddingValues(18.dp),
            ) {
                Text(
                    text = Strings.settingsTokenDesc,
                    fontSize = 12.sp,
                    color = colors.onSurfaceSecondary,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = Strings.settingsTokenStatus,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (state.hasToken) Strings.settingsTokenSaved else Strings.settingsTokenUnset,
                        fontSize = 12.sp,
                        color = if (state.hasToken) colors.primary else colors.onSurfaceVariantSummary,
                    )
                }
                Spacer(Modifier.height(12.dp))
                TextField(
                    value = field,
                    onValueChange = { updated ->
                        field = updated
                        onTokenChange(updated.text)
                    },
                    label = if (state.hasToken) {
                        Strings.settingsTokenReplace
                    } else {
                        Strings.settingsTokenPlaceholder
                    },
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    // 输入过程也按密码处理：掩码显示、关闭输入法联想
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onSaveToken,
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = Strings.actionSave, fontSize = 14.sp)
                    }
                    Button(
                        onClick = onClearToken,
                        enabled = state.hasToken,
                        colors = ButtonDefaults.buttonColors(),
                    ) {
                        Text(text = Strings.actionClear, fontSize = 14.sp)
                    }
                }
                state.message?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = message,
                        fontSize = 11.sp,
                        color = if (state.saved) colors.primary else colors.error,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = Strings.settingsTokenSecurity,
                    fontSize = 11.sp,
                    color = colors.onSurfaceVariantSummary,
                )
            }

            SmallTitle(Strings.settingsAbout)
            Card(
                modifier = Modifier.padding(horizontal = 16.dp),
                cornerRadius = 20.dp,
                insideMargin = PaddingValues(0.dp),
            ) {
                AboutRow(Strings.settingsAboutApp, stringResource(R.string.app_name))
                HorizontalDivider(modifier = Modifier.padding(start = 18.dp))
                AboutRow(
                    Strings.settingsAboutVersion,
                    "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                )
                HorizontalDivider(modifier = Modifier.padding(start = 18.dp))
                AboutRow(Strings.settingsAboutUi, "Miuix")
            }
        }
    }
}

@Composable
private fun AboutRow(title: String, value: String) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(text = value, fontSize = 13.sp, color = colors.onSurfaceVariantSummary)
    }
}
