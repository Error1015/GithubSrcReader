package com.error1015.githubsrcreader.ui

import com.error1015.githubsrcreader.data.model.CodeSource
import com.error1015.githubsrcreader.data.model.FileContent
import com.error1015.githubsrcreader.data.model.FileEntry
import com.error1015.githubsrcreader.data.model.Loadable
import com.error1015.githubsrcreader.data.model.RecentSource
import com.error1015.githubsrcreader.data.model.RepoInfo
import com.error1015.githubsrcreader.data.model.ThemeMode
import com.error1015.githubsrcreader.data.model.UsageHit
import com.error1015.githubsrcreader.ui.code.CodeToken

data class HomeUiState(
    val urlInput: String = "",
    val error: String? = null,
    val busy: Boolean = false,
    val recents: List<RecentSource> = emptyList(),
)

data class BrowseUiState(
    val source: CodeSource? = null,
    val path: String = "",
    val entries: Loadable<List<FileEntry>> = Loadable.Idle,
    val repoInfo: RepoInfo? = null,
    val ref: String? = null,
)

data class ViewerUiState(
    val path: String = "",
    val fileName: String = "",
    val content: Loadable<FileContent> = Loadable.Idle,
    val tokens: List<CodeToken> = emptyList(),
    /** 从「查找引用」跳转过来时需要滚动到的行号。 */
    val targetLine: Int? = null,
) {
    val text: String? get() = (content as? Loadable.Success)?.value?.text
}

data class SettingsUiState(
    /** 用户正在输入的新令牌，仅在输入框里短暂存在，保存后立即清空。 */
    val tokenDraft: String = "",
    /** 是否已配置令牌——界面只显示状态，不回显明文。 */
    val hasToken: Boolean = false,
    val showLineNumbers: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val saved: Boolean = false,
    val message: String? = null,
)

/** 引用搜索范围。 */
enum class UsageScope(val label: String) {
    FILE("当前文件"),
    SOURCE("整个来源"),
}

data class UsageUiState(
    val query: String = "",
    val scope: UsageScope = UsageScope.FILE,
    val results: Loadable<List<UsageHit>> = Loadable.Idle,
    /** 当前文件里提取到的声明，query 为空时展示，点一下即可搜索。 */
    val symbols: List<UsageHit> = emptyList(),
    val progress: String? = null,
    val hint: String? = null,
)

sealed interface AppEvent {
    data object OpenBrowser : AppEvent
    data object OpenViewer : AppEvent

    /** 需要把用户带回首页（例如分享进来的文本没有可解析链接，要在首页显示提示）。 */
    data object ShowHome : AppEvent
}
