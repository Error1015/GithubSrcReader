package com.error1015.githubsrcreader

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.error1015.githubsrcreader.data.AppPreferences
import com.error1015.githubsrcreader.data.github.GitHubUrlParser
import com.error1015.githubsrcreader.data.model.ThemeMode
import com.error1015.githubsrcreader.i18n.AppLanguage
import com.error1015.githubsrcreader.i18n.Strings
import com.error1015.githubsrcreader.ui.AppRoot
import com.error1015.githubsrcreader.ui.AppViewModel
import com.error1015.githubsrcreader.ui.theme.GithubSrcReaderTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    private val prefs by lazy { AppPreferences(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 先把语言准备好，后面的界面文案（含 ViewModel 里的错误提示）才是正确语言
        Strings.apply(AppLanguage.resolve(prefs.language, Locale.getDefault()))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            handleIncomingIntent(intent)
        }
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            // 跟随应用内主题切换状态栏图标的深浅，避免手动切深色后图标看不清
            LaunchedEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        lightScrim = Color.TRANSPARENT,
                        darkScrim = Color.TRANSPARENT,
                    ) { darkTheme },
                )
            }

            GithubSrcReaderTheme(darkTheme = darkTheme, codeTheme = settings.codeTheme) {
                AppRoot(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    /**
     * 两种外部入口：
     * - ACTION_VIEW：在别的应用里“用 GithubSrcReader 打开” GitHub 链接；
     * - ACTION_SEND：在分享面板里把文本 / 链接分享过来（浏览器地址栏、聊天记录、Markdown 等）。
     */
    private fun handleIncomingIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                val url = intent.dataString ?: return
                if (GitHubUrlParser.parse(url) != null) {
                    viewModel.openUrl(url)
                }
            }

            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("text/") != true) return
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                    ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
                    ?: return
                if (text.isNotBlank()) {
                    viewModel.openSharedText(text)
                }
            }
        }
    }
}
