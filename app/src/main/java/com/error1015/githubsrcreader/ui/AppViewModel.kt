package com.error1015.githubsrcreader.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.error1015.githubsrcreader.data.AppPreferences
import com.error1015.githubsrcreader.data.download.FileDownloader
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
import com.error1015.githubsrcreader.i18n.AppLanguage
import com.error1015.githubsrcreader.i18n.Strings
import com.error1015.githubsrcreader.ui.code.CodeTokenizer
import com.error1015.githubsrcreader.ui.util.ExternalFiles
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
            language = prefs.language,
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
            _home.update { it.copy(error = Strings.errorEnterLink) }
            return
        }
        val parsed = GitHubUrlParser.parse(text)
        if (parsed == null) {
            _home.update { it.copy(error = Strings.errorUnrecognizedLink) }
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
                it.copy(error = Strings.errorSharedWithoutLink, busy = false)
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
                    _home.update { it.copy(error = Strings.errorRecentInvalid) }
                    return
                }
                openUrl("https://github.com/$owner/$repo")
            }

            RecentSource.Type.LOCAL -> {
                val uri = recent.treeUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
                if (uri == null) {
                    _home.update { it.copy(error = Strings.errorFolderInvalid) }
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
                        subtitle = Strings.commonLocalFolder,
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

    /**
     * 打开图片等非文本文件：不读成文本，直接交给系统图片查看器。
     * 本地文件用 SAF Uri 直达；远程文件先下载到缓存再通过 FileProvider 暴露。
     */
    fun openExternalFile(entry: FileEntry) {
        val source = currentSource ?: return
        viewModelScope.launch {
            try {
                val mime = ExternalFiles.mimeTypeOf(entry.name)
                source.directUri(entry.path)?.let { uri ->
                    _events.send(AppEvent.OpenExternal(uri, mime))
                    return@launch
                }
                _events.send(AppEvent.ShowMessage(Strings.messageLoading(entry.name)))
                val bytes = source.readBytes(entry.path)
                    ?: throw SourceException(Strings.errorReadFile)
                val uri = withContext(Dispatchers.IO) {
                    ExternalFiles.writeCache(getApplication<Application>(), entry.name, bytes)
                }
                _events.send(AppEvent.OpenExternal(uri, mime))
            } catch (e: Exception) {
                _events.send(AppEvent.ShowMessage(e.friendlyMessage()))
            }
        }
    }

    /**
     * 把当前文件保存到公共下载目录 Download/Github Src Reader/<来源>/<原路径>。
     * 走 MediaStore，不需要存储权限；文本文件也取原始字节，避免保存到被截断的预览内容。
     */
    fun downloadCurrentFile() {
        val source = currentSource ?: return
        val state = _viewer.value
        val path = state.path
        if (path.isBlank() || state.downloading) return

        viewModelScope.launch {
            _viewer.update { it.copy(downloading = true) }
            try {
                val bytes = source.readBytes(path)
                    ?: (_viewer.value.text?.toByteArray() ?: throw SourceException(Strings.errorReadFile))
                val name = path.substringAfterLast('/')
                val savedTo = withContext(Dispatchers.IO) {
                    FileDownloader.save(
                        context = getApplication<Application>(),
                        folder = source.downloadFolder,
                        path = path,
                        displayName = name,
                        mimeType = ExternalFiles.mimeForSave(name),
                        bytes = bytes,
                    )
                }
                _events.send(AppEvent.ShowMessage(Strings.messageSavedTo(savedTo)))
            } catch (e: Exception) {
                _events.send(AppEvent.ShowMessage(e.friendlyMessage()))
            } finally {
                _viewer.update { it.copy(downloading = false) }
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
                _usage.update { it.copy(results = Loadable.Failure(Strings.errorNoFileOpen)) }
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
            _usage.update { it.copy(results = Loadable.Failure(Strings.errorNoSourceOpen)) }
            return
        }
        if (!source.supportsRepositorySearch) {
            _usage.update {
                it.copy(
                    results = Loadable.Failure(source.repositorySearchHint ?: Strings.hintNoCrossFileSearch),
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
            _settings.update { it.copy(saved = false, message = Strings.messageTokenRequired) }
            return
        }
        prefs.token = draft
        val stored = prefs.hasToken
        _settings.update {
            it.copy(
                tokenDraft = "",
                hasToken = stored,
                saved = stored,
                message = if (stored) Strings.messageTokenSaved else Strings.messageTokenSaveFailed,
            )
        }
    }

    fun clearToken() {
        prefs.token = ""
        _settings.update {
            it.copy(tokenDraft = "", hasToken = false, saved = true, message = Strings.messageTokenCleared)
        }
    }

    fun setShowLineNumbers(enabled: Boolean) {
        prefs.showLineNumbers = enabled
        _settings.update { it.copy(showLineNumbers = enabled) }
    }

    /** 切换语言：写入偏好后由界面重建 Activity 生效。 */
    fun setLanguage(value: AppLanguage) {
        prefs.language = value
        _settings.update { it.copy(language = value) }
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
        is SourceException -> message ?: Strings.commonLoadFailed
        is UnknownHostException -> Strings.errorNoNetwork
        is SocketTimeoutException -> Strings.errorTimeout
        is IOException -> Strings.errorNetwork
        else -> message ?: Strings.commonUnknownError
    }

    companion object {
        /** 超过这个字符数不再做语法着色，避免长文件卡顿。 */
        private const val HIGHLIGHT_LIMIT = 400_000
    }
}
