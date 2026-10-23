package com.error1015.githubsrcreader.ui

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.error1015.githubsrcreader.ui.components.AppBottomBar
import com.error1015.githubsrcreader.ui.components.AppTab
import com.error1015.githubsrcreader.ui.screens.BrowseScreen
import com.error1015.githubsrcreader.ui.screens.HomeScreen
import com.error1015.githubsrcreader.ui.screens.SettingsScreen
import com.error1015.githubsrcreader.ui.screens.UsagesScreen
import com.error1015.githubsrcreader.ui.screens.ViewerScreen
import com.error1015.githubsrcreader.ui.util.copyToClipboard
import top.yukonga.miuix.kmp.theme.MiuixTheme

object Routes {
    const val HOME = "home"
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

    val context = LocalContext.current

    // 打开「查找引用」：word 为空时由用户自己输入，非空则直接搜索该标识符
    val openUsages: (String) -> Unit = { word ->
        viewModel.openUsages(word)
        navController.navigate(Routes.USAGES) { launchSingleTop = true }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AppEvent.OpenBrowser -> navController.navigate(Routes.BROWSE) { launchSingleTop = true }
                AppEvent.OpenViewer -> navController.navigate(Routes.VIEWER) { launchSingleTop = true }
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
                    onOpenRecent = viewModel::openRecent,
                    onRemoveRecent = viewModel::removeRecent,
                )
            }
            composable(Routes.BROWSE) {
                BrowseScreen(
                    state = browse,
                    onNavigateUp = viewModel::navigateUp,
                    onBack = { navController.popBackStack() },
                    onReload = viewModel::reloadDirectory,
                    onOpenEntry = { entry ->
                        if (entry.isDirectory) {
                            viewModel.openDirectory(entry)
                        } else {
                            viewModel.openFile(entry)
                            navController.navigate(Routes.VIEWER) { launchSingleTop = true }
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
                )
            }
            composable(Routes.USAGES) {
                UsagesScreen(
                    state = usage,
                    subtitle = viewer.fileName.ifBlank { browse.source?.title ?: "当前来源" },
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
                )
            }
        }

        if (route != Routes.VIEWER && route != Routes.USAGES) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            ) {
                AppBottomBar(
                    selected = when (route) {
                        Routes.BROWSE -> AppTab.Browse
                        Routes.SETTINGS -> AppTab.Settings
                        else -> AppTab.Home
                    },
                    onSelect = { tab ->
                        navController.navigate(tab.route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }
    }
}
