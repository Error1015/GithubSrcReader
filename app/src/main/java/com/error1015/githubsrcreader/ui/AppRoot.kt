package com.error1015.githubsrcreader.ui

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.error1015.githubsrcreader.ui.components.AppBottomBar
import com.error1015.githubsrcreader.ui.components.AppTab
import com.error1015.githubsrcreader.i18n.AppLanguage
import com.error1015.githubsrcreader.i18n.Strings
import com.error1015.githubsrcreader.ui.screens.BrowseScreen
import com.error1015.githubsrcreader.ui.screens.HomeScreen
import com.error1015.githubsrcreader.ui.screens.ProjectsScreen
import com.error1015.githubsrcreader.ui.screens.SettingsScreen
import com.error1015.githubsrcreader.ui.screens.UsagesScreen
import com.error1015.githubsrcreader.ui.screens.ViewerScreen
import com.error1015.githubsrcreader.ui.util.ExternalFiles
import com.error1015.githubsrcreader.ui.util.copyToClipboard
import top.yukonga.miuix.kmp.theme.MiuixTheme

object Routes {
    const val HOME = "home"
    const val PROJECTS = "projects"
    const val BROWSE = "browse"
    const val VIEWER = "viewer"
    const val USAGES = "usages"
    const val SETTINGS = "settings"
}

@Composable
fun AppRoot(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route ?: Routes.HOME

    val home by viewModel.home.collectAsStateWithLifecycle()
    val browse by viewModel.browse.collectAsStateWithLifecycle()
    val viewer by viewModel.viewer.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val usage by viewModel.usage.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // 打开「查找引用」：word 为空时由用户自己输入，非空则直接搜索该标识符
    val openUsages: (String) -> Unit = { word ->
        viewModel.openUsages(word)
        navController.navigate(Routes.USAGES) { launchSingleTop = true }
    }

    // 切换底栏分区。
    //
    // 这里**故意不用** saveState / restoreState：四个分区是同一个图里的平级 destination，
    // 而 Navigation 的 popUpTo(saveState = true) 会把被弹掉的浏览页状态挂到目标 destination
    // （这里是起始页「首页」）的 id 下，下次带 restoreState 头回首页时又把它当成「首页的栈」
    // 恢复上去 —— 表现就是点了首页还停在浏览页、首页和浏览页来回切不动，直到某些状态变化
    // 让这张映射表被清掉才恢复。各页面状态本来就在 ViewModel 里（`_home` / `_browse`），
    // 不需要靠这套机制存，去掉反而更稳。
    val selectTab: (AppTab) -> Unit = { tab ->
        // 慢网络下还没返回的「打开项目 / 链接」会发 OpenBrowser 把人拽回浏览页，先取消
        viewModel.cancelPendingOpen()
        // 不判断「已经在该分区」：route 只是渲染用的快照，靠它做守卫反而可能把切换卡死；
        // popUpTo + launchSingleTop 本身已经是幂等的
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id)
            launchSingleTop = true
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AppEvent.OpenBrowser -> navController.navigate(Routes.BROWSE) { launchSingleTop = true }
                AppEvent.OpenViewer -> navController.navigate(Routes.VIEWER) { launchSingleTop = true }
                AppEvent.ShowHome -> navController.navigate(Routes.HOME) {
                    popUpTo(Routes.HOME) { inclusive = true }
                    launchSingleTop = true
                }

                is AppEvent.OpenExternal -> {
                    val view = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(event.uri, event.mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val opened = runCatching { context.startActivity(view) }.isSuccess
                    if (!opened) {
                        Toast.makeText(context, Strings.commonNoAppToOpen, Toast.LENGTH_SHORT).show()
                    }
                }

                is AppEvent.ShowMessage ->
                    Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MiuixTheme.colorScheme.surface)) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    state = home,
                    onUrlChange = viewModel::onUrlChange,
                    onSubmit = viewModel::submitUrl,
                    onFolderPicked = { uri -> viewModel.openLocalTree(uri) },
                    onOpenProject = viewModel::openProject,
                    onToggleStar = viewModel::toggleProjectStar,
                    onRemoveProject = viewModel::removeProject,
                    onSaveToLibrary = viewModel::saveProjectToLibrary,
                    onRemoveFromLibrary = viewModel::removeProjectFromLibrary,
                )
            }
            composable(Routes.PROJECTS) {
                ProjectsScreen(
                    state = projects,
                    onAddByLink = viewModel::addProjectByLink,
                    onAddByFolder = viewModel::addProjectByFolder,
                    onOpenProject = viewModel::openProject,
                    onToggleStar = viewModel::toggleProjectStar,
                    onCreateGroup = viewModel::createGroup,
                    onRenameGroup = viewModel::renameGroup,
                    onDeleteGroup = viewModel::deleteGroup,
                    onMoveProject = viewModel::moveProject,
                    onRemoveProject = viewModel::removeProjectFromLibrary,
                )
            }
            composable(Routes.BROWSE) {
                BrowseScreen(
                    state = browse,
                    onNavigateUp = viewModel::navigateUp,
                    onBack = { navController.popBackStack() },
                    onReload = viewModel::reloadDirectory,
                    onOpenEntry = { entry ->
                        when {
                            entry.isDirectory -> viewModel.openDirectory(entry)
                            // 图片不按文本打开，交给系统查看器
                            ExternalFiles.isImage(entry.name) -> viewModel.openExternalFile(entry)
                            else -> {
                                viewModel.openFile(entry)
                                navController.navigate(Routes.VIEWER) { launchSingleTop = true }
                            }
                        }
                    },
                    onGotoPath = viewModel::loadDirectory,
                    onGoHome = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.VIEWER) {
                ViewerScreen(
                    state = viewer,
                    showLineNumbers = settings.showLineNumbers,
                    onBack = { navController.popBackStack() },
                    onReload = viewModel::reloadFile,
                    onToggleLineNumbers = { viewModel.setShowLineNumbers(!settings.showLineNumbers) },
                    onCopy = { context.copyToClipboard(it) },
                    onFindUsages = openUsages,
                    onDownload = viewModel::downloadCurrentFile,
                )
            }
            composable(Routes.USAGES) {
                UsagesScreen(
                    state = usage,
                    subtitle = viewer.fileName.ifBlank { browse.source?.title ?: Strings.commonCurrentSource },
                    onBack = { navController.popBackStack() },
                    onQueryChange = viewModel::onUsageQueryChange,
                    onSearch = viewModel::runUsageSearch,
                    onScopeChange = viewModel::onUsageScopeChange,
                    onPickSymbol = { name -> viewModel.openUsages(name) },
                    onOpenHit = { hit ->
                        viewModel.openUsageHit(hit)
                        navController.navigate(Routes.VIEWER) {
                            popUpTo(Routes.VIEWER) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    state = settings,
                    onTokenChange = viewModel::onTokenDraftChange,
                    onSaveToken = viewModel::saveToken,
                    onClearToken = viewModel::clearToken,
                    onToggleLineNumbers = viewModel::setShowLineNumbers,
                    onThemeModeChange = viewModel::setThemeMode,
                    onLanguageChange = { language ->
                        viewModel.setLanguage(language)
                        // 文案来自全局文案表，重建一次让整棵树重新取词
                        (context as? Activity)?.recreate()
                    },
                    onCodeThemeChange = viewModel::setCodeTheme,
                )
            }
        }

        if (route != Routes.VIEWER && route != Routes.USAGES) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    // 底栏是悬浮的，必须压在页面内容之上（转场动画期间也一样）
                    .zIndex(1f),
            ) {
                AppBottomBar(
                    selected = when (route) {
                        Routes.PROJECTS -> AppTab.Projects
                        Routes.BROWSE -> AppTab.Browse
                        Routes.SETTINGS -> AppTab.Settings
                        else -> AppTab.Home
                    },
                    onSelect = selectTab,
                )
            }
        }
    }
}
