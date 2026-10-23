package com.error1015.githubsrcreader.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.error1015.githubsrcreader.data.AppPreferences
import com.error1015.githubsrcreader.data.github.GitHubApi
import com.error1015.githubsrcreader.data.github.GitHubCodeSource
import com.error1015.githubsrcreader.data.github.GitHubUrlParser
import com.error1015.githubsrcreader.data.github.SharedLink
import com.error1015.githubsrcreader.data.local.LocalCodeSource
import com.error1015.githubsrcreader.data.model.CodeSource
import com.error1015.githubsrcreader.data.model.FileEntry
import com.error1015.githubsrcreader.data.model.Loadable
import com.error1015.githubsrcreader.data.model.RecentSource
import com.error1015.githubsrcreader.data.model.SourceException
import com.error1015.githubsrcreader.data.model.Language
import com.error1015.githubsrcreader.data.model.ThemeMode
import com.error1015.githubsrcreader.data.model.UsageHit
import com.error1015.githubsrcreader.data.search.ReferenceSearch
import com.error1015.githubsrcreader.ui.code.CodeTokenizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application)
    private val api = GitHubApi { prefs.token }

    private var currentSource: CodeSource? = null

    private val _home = MutableStateFlow(HomeUiState(recents = prefs.recentSources()))
    val home = _home.asStateFlow()

    private val _browse = MutableStateFlow(BrowseUiState())
    val browse = _browse.asStateFlow()

    private val _viewer = MutableStateFlow(ViewerUiState())
    val viewer = _viewer.asStateFlow()

    private val _settings = MutableStateFlow(
        SettingsUiState(
            hasToken = prefs.hasToken,
            showLineNumbers = prefs.showLineNumbers,
            themeMode = prefs.themeMode,
        ),
    )
    val settings = _settings.asStateFlow()

    private val _usage = MutableStateFlow(UsageUiState())
    val usage = _usage.asStateFlow()

    private var usageJob: Job? = null

    private val _events = Channel<AppEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // ------------------------------------------------------------------ 首页

    fun onUrlChange(value: String) {
        _home.update { it.copy(urlInput = value, error = null) }
    }

    fun submitUrl() = openUrl(_home.value.urlInput)

    fun openUrl(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) {
            _home.update { it.copy(error = "请输入 GitHub 链接") }
            return
        }
        val parsed = GitHubUrlParser.parse(text)
        if (parsed == null) {
            _home.update { it.copy(error = "无法识别该链接，支持 github.com/owner/repo 或直接输入 owner/repo") }
            return
        }

        _home.update { it.copy(urlInput = text, busy = true, error = null) }
        viewModelScope.launch {
            try {
                val info = api.repoInfo(parsed.owner, parsed.repo)
                val (ref, path) = resolveTarget(parsed, info.defaultBranch)
                val source = GitHubCodeSource(api, parsed.owner, parsed.repo, ref)
                currentSource = source

                prefs.addRecent(
                    RecentSource(
                        id = "github:${parsed.owner}/${parsed.repo}".lowercase(),
                        type = RecentSource.Type.GITHUB,
                        title = parsed.repo,
                        subtitle = "${parsed.owner}/${parsed.repo} · $ref",
                        owner = parsed.owner,
                        repo = parsed.repo,
                        ref = ref,
                        openedAt = System.currentTimeMillis(),
                    ),
                )
                _home.update { it.copy(busy = false, error = null, recents = prefs.recentSources()) }

                if (parsed.pointsToFile && path.isNotBlank()) {
                    val parent = path.substringBeforeLast('/', "")
                    _browse.value = BrowseUiState(source = source, path = parent, repoInfo = info, ref = ref)
                    loadDirectory(parent)
                    openFile(path, path.substringAfterLast('/'))
                    _events.send(AppEvent.OpenViewer)
                } else {
                    _browse.value = BrowseUiState(source = source, path = path, repoInfo = info, ref = ref)
                    loadDirectory(path)
                    _events.send(AppEvent.OpenBrowser)
                }
            } catch (e: Exception) {
                _home.update { it.copy(busy = false, error = e.friendlyMessage()) }
            }
        }
    }

    /**
     * 处理系统「分享」进来的文本：里面夹带的 GitHub 链接会被挑出来直接打开；
     * 找不到就回到首页并给出提示，不会静默失败。
     */
    fun openSharedText(raw: String) {
        val link = SharedLink.firstLink(raw)
        if (link == null) {
            _home.update {
                it.copy(error = "分享的内容里没有可解析的 GitHub 链接", busy = false)
            }
            viewModelScope.launch { _events.send(AppEvent.ShowHome) }
            return
        }
        openUrl(link)
    }

    fun openRecent(recent: RecentSource) {
        when (recent.type) {
            RecentSource.Type.GITHUB -> {
                val owner = recent.owner
                val repo = recent.repo
                if (owner.isNullOrBlank() || repo.isNullOrBlank()) {
                    _home.update { it.copy(error = "该记录已失效，请重新输入链接") }
                    return
                }
                openUrl("https://github.com/$owner/$repo")
            }

            RecentSource.Type.LOCAL -> {
                val uri = recent.treeUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
                if (uri == null) {
                    _home.update { it.copy(error = "该文件夹记录已失效，请重新选择") }
                    return
                }
                openLocalTree(uri, recent.title)
            }
        }
    }

    fun removeRecent(id: String) {
        prefs.removeRecent(id)
        _home.update { it.copy(recents = prefs.recentSources()) }
    }

    fun openLocalTree(uri: Uri, nameOverride: String? = null) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            try {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                val name = nameOverride?.takeIf { it.isNotBlank() }
                    ?: LocalCodeSource.displayName(context, uri)
                val source = LocalCodeSource(context, uri, name)
                currentSource = source
                prefs.addRecent(
                    RecentSource(
                        id = "local:$uri",
                        type = RecentSource.Type.LOCAL,
                        title = name,
                        subtitle = "本地文件夹",
                        treeUri = uri.toString(),
                        openedAt = System.currentTimeMillis(),
                    ),
                )
                _home.update { it.copy(recents = prefs.recentSources(), error = null, busy = false) }
                _browse.value = BrowseUiState(source = source, path = "")
                loadDirectory("")
                _events.send(AppEvent.OpenBrowser)
            } catch (e: Exception) {
                _home.update { it.copy(error = e.friendlyMessage()) }
            }
        }
    }

    // ------------------------------------------------------------------ 浏览

    fun loadDirectory(path: String) {
        val source = currentSource ?: return
        _browse.update { it.copy(path = path, entries = Loadable.Loading) }
        viewModelScope.launch {
            try {
                val entries = source.list(path)
                _browse.update { state ->
                    if (state.path == path) state.copy(entries = Loadable.Success(entries)) else state
                }
            } catch (e: Exception) {
                _browse.update { state ->
                    if (state.path == path) state.copy(entries = Loadable.Failure(e.friendlyMessage())) else state
                }
            }
        }
    }

    fun reloadDirectory() = loadDirectory(_browse.value.path)

    fun navigateUp(): Boolean {
        val path = _browse.value.path
        if (path.isEmpty()) return false
        loadDirectory(path.substringBeforeLast('/', ""))
        return true
    }

    fun openDirectory(entry: FileEntry) {
        if (!entry.isDirectory) return
        loadDirectory(entry.path)
    }

    // ------------------------------------------------------------------ 阅读

    fun openFile(entry: FileEntry) = openFile(entry.path, entry.name)

    fun openFile(path: String, name: String, targetLine: Int? = null) {
        val source = currentSource ?: return
        _viewer.value = ViewerUiState(
            path = path,
            fileName = name,
            content = Loadable.Loading,
            targetLine = targetLine,
        )
        viewModelScope.launch {
            try {
                val content = source.readText(path)
                val tokens = if (content.text.length <= HIGHLIGHT_LIMIT) {
                    withContext(Dispatchers.Default) {
                        CodeTokenizer.tokenize(content.text, content.language)
                    }
                } else {
                    emptyList()
                }
                _viewer.update {
                    it.copy(content = Loadable.Success(content), tokens = tokens)
                }
            } catch (e: Exception) {
                _viewer.update { it.copy(content = Loadable.Failure(e.friendlyMessage()), tokens = emptyList()) }
            }
        }
    }

    fun reloadFile() {
        val state = _viewer.value
        if (state.path.isNotBlank()) openFile(state.path, state.fileName, state.targetLine)
    }

    // ------------------------------------------------------------------ 查找引用

    /** 打开「查找引用」：带上当前文件的声明列表，[query] 非空时立即搜索。 */
    fun openUsages(query: String) {
        val content = (_viewer.value.content as? Loadable.Success)?.value
        _usage.update {
            it.copy(
                query = query,
                scope = UsageScope.FILE,
                symbols = emptyList(),
                results = Loadable.Idle,
                progress = null,
                hint = null,
            )
        }
        if (content != null) {
            viewModelScope.launch {
                val symbols = withContext(Dispatchers.Default) {
                    ReferenceSearch.declarationsInText(content.text, content.language, content.path)
                }
                // 期间可能已切到别的文件，避免串台
                if (_viewer.value.path == content.path) {
                    _usage.update { it.copy(symbols = symbols) }
                }
            }
        }
        if (query.isNotBlank()) runUsageSearch()
    }

    fun onUsageQueryChange(value: String) {
        _usage.update { it.copy(query = value) }
    }

    fun onUsageScopeChange(scope: UsageScope) {
        _usage.update { it.copy(scope = scope) }
        if (_usage.value.query.isNotBlank()) runUsageSearch()
    }

    fun runUsageSearch() {
        val state = _usage.value
        val name = state.query.trim()
        if (name.isEmpty()) {
            _usage.update { it.copy(results = Loadable.Idle) }
            return
        }

        if (state.scope == UsageScope.FILE) {
            val content = (_viewer.value.content as? Loadable.Success)?.value
            if (content == null) {
                _usage.update { it.copy(results = Loadable.Failure("还没有打开任何文件")) }
                return
            }
            _usage.update { it.copy(results = Loadable.Loading, hint = null) }
            viewModelScope.launch {
                val hits = withContext(Dispatchers.Default) {
                    ReferenceSearch.searchInText(content.text, name, content.language, content.path)
                }
                _usage.update { it.copy(results = Loadable.Success(hits)) }
            }
            return
        }

        val source = currentSource
        if (source == null) {
            _usage.update { it.copy(results = Loadable.Failure("还没有打开任何来源")) }
            return
        }
        if (!source.supportsRepositorySearch) {
            _usage.update {
                it.copy(
                    results = Loadable.Failure(source.repositorySearchHint ?: "当前来源不支持跨文件搜索"),
                    hint = source.repositorySearchHint,
                )
            }
            return
        }

        _usage.update { it.copy(results = Loadable.Loading, hint = null, progress = null) }
        usageJob?.cancel()
        usageJob = viewModelScope.launch {
            try {
                val hits = source.searchReferences(name) { scanned, total ->
                    _usage.update { it.copy(progress = if (total > 0) "$scanned / $total" else null) }
                }
                _usage.update { it.copy(results = Loadable.Success(hits), progress = null) }
            } catch (e: Exception) {
                _usage.update { it.copy(results = Loadable.Failure(e.friendlyMessage()), progress = null) }
            }
        }
    }

    /** 点击搜索结果：同一文件内直接定位，跨文件则打开对应文件并跳到该行。 */
    fun openUsageHit(hit: UsageHit) {
        if (_viewer.value.path == hit.path) {
            _viewer.update { it.copy(targetLine = hit.line) }
        } else {
            openFile(hit.path, hit.path.substringAfterLast('/'), hit.line)
        }
    }

    // ------------------------------------------------------------------ 设置

    fun onTokenDraftChange(value: String) {
        _settings.update { it.copy(tokenDraft = value, saved = false, message = null) }
    }

    /** 保存令牌：落盘即加密，界面不回显明文，保存后立即清空草稿。 */
    fun saveToken() {
        val draft = _settings.value.tokenDraft.trim()
        if (draft.isEmpty()) {
            _settings.update { it.copy(saved = false, message = "请先输入 GitHub Token") }
            return
        }
        prefs.token = draft
        val stored = prefs.hasToken
        _settings.update {
            it.copy(
                tokenDraft = "",
                hasToken = stored,
                saved = stored,
                message = if (stored) "已加密保存到本机" else "保存失败，请稍后重试",
            )
        }
    }

    fun clearToken() {
        prefs.token = ""
        _settings.update {
            it.copy(tokenDraft = "", hasToken = false, saved = true, message = "已清除令牌")
        }
    }

    fun setShowLineNumbers(enabled: Boolean) {
        prefs.showLineNumbers = enabled
        _settings.update { it.copy(showLineNumbers = enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.themeMode = mode
        _settings.update { it.copy(themeMode = mode) }
    }

    // ------------------------------------------------------------------ 内部

    private suspend fun resolveTarget(
        parsed: GitHubUrlParser.ParsedUrl,
        defaultBranch: String,
    ): Pair<String, String> {
        val candidates = parsed.candidates
        if (candidates.size <= 1) {
            val only = candidates.firstOrNull()
            return (only?.ref ?: defaultBranch) to (only?.path.orEmpty())
        }
        for (candidate in candidates) {
            val ref = candidate.ref ?: return defaultBranch to candidate.path
            if (api.refExists(parsed.owner, parsed.repo, ref)) return ref to candidate.path
        }
        val first = candidates.first()
        return (first.ref ?: defaultBranch) to first.path
    }

    private fun Throwable.friendlyMessage(): String = when (this) {
        is SourceException -> message ?: "加载失败"
        is UnknownHostException -> "无法连接网络，请检查网络设置"
        is SocketTimeoutException -> "请求超时，请稍后重试"
        is IOException -> "网络异常，请稍后重试"
        else -> message ?: "未知错误"
    }

    companion object {
        /** 超过这个字符数不再做语法着色，避免长文件卡顿。 */
        private const val HIGHLIGHT_LIMIT = 400_000
    }
}
