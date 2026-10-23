package com.error1015.githubsrcreader.ui

import android.net.Uri
import com.error1015.githubsrcreader.data.model.CodeSource
import com.error1015.githubsrcreader.data.model.FileContent
import com.error1015.githubsrcreader.data.model.FileEntry
import com.error1015.githubsrcreader.data.model.Loadable
import com.error1015.githubsrcreader.data.model.Project
import com.error1015.githubsrcreader.data.model.ProjectGroup
import com.error1015.githubsrcreader.data.model.ProjectSection
import com.error1015.githubsrcreader.data.model.RepoInfo
import com.error1015.githubsrcreader.data.model.ThemeMode
import com.error1015.githubsrcreader.i18n.AppLanguage
import com.error1015.githubsrcreader.data.model.UsageHit
import com.error1015.githubsrcreader.ui.code.CodeToken

data class HomeUiState(
    val urlInput: String = "",
    val error: String? = null,
    val busy: Boolean = false,
    /** 最近打开：星标置顶，其余按打开时间。 */
    val recents: List<Project> = emptyList(),
    /** 供「加入项目库」时选择分组。 */
    val groups: List<ProjectGroup> = emptyList(),
)

/** 项目页：分组 + 未分组，组内星标置顶。 */
data class ProjectsUiState(
    val sections: List<ProjectSection> = emptyList(),
    val groups: List<ProjectGroup> = emptyList(),
) {
    val total: Int get() = sections.sumOf { it.projects.size }
    val isEmpty: Boolean get() = total == 0
}


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
    /** 正在把当前文件保存到下载目录。 */
    val downloading: Boolean = false,
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
    val language: AppLanguage = AppLanguage.SYSTEM,
    val saved: Boolean = false,
    val message: String? = null,
)

/** 引用搜索范围（显示名称见 `i18n/Labels.kt`）。 */
enum class UsageScope {
    FILE,
    SOURCE,
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

    /** 交给系统应用打开（图片等二进制文件）。 */
    data class OpenExternal(val uri: Uri, val mimeType: String) : AppEvent

    /** 轻量提示，用系统 Toast 呈现。 */
    data class ShowMessage(val text: String) : AppEvent
}
