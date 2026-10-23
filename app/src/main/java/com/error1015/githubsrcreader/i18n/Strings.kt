/*
 * 文案系统结构：
 * - `Keys.kt` 声明所有键；
 * - `texts/ZhTexts.kt`、`texts/EnTexts.kt` 是各语言的键值表；
 * - 本文件把键包装成 `Strings.xxx`，调用点只认这里。
 *
 * 新增一种语言只要三步，**调用点一行都不用改**：
 * 1. `AppLanguage` 加枚举值，如 `JA("ja", "日本語")`；
 * 2. 新建 `texts/JaTexts.kt`，结构与 `EnTexts.kt` 相同（可只翻译一部分，缺的回退英文）；
 * 3. 在 [Strings.tables] 里登记 `"ja" to JaTexts`。
 *
 * 取值顺序：当前语言 → 英文 → 键名本身。
 * 不用 `values-xx/strings.xml` 的原因：数据层（GitHubApi / LocalCodeSource / FileDownloader）
 * 也要产出本地化错误提示，但它们没有 Context；全局表还能在切换语言后立刻生效。
 */

package com.error1015.githubsrcreader.i18n

import com.error1015.githubsrcreader.i18n.texts.EnTexts
import com.error1015.githubsrcreader.i18n.texts.ZhTexts

/** 按「当前语言 → 英文 → 键名」取值，抽出来便于单测。 */
internal fun resolveText(key: String, table: Map<String, String>): String =
    table[key] ?: EnTexts[key] ?: ZhTexts[key] ?: key

/** 按当前语言取文案；用 [apply] 切换语言。 */
object Strings {
    private val tables: Map<String, Map<String, String>> = mapOf(
        "zh" to ZhTexts,
        "en" to EnTexts,
    )

    private var table: Map<String, String> = ZhTexts

    /** 由 Activity 在创建时（以及切换语言后）调用。 */
    fun apply(language: AppLanguage) {
        table = tables[language.tag] ?: EnTexts
    }

    private fun raw(key: String): String = resolveText(key, table)

    private fun format(key: String, vararg args: Any): String {
        var text = raw(key)
        args.forEachIndexed { index, arg -> text = text.replace("{$index}", arg.toString()) }
        return text
    }

    /** 唯一不需要翻译的占位提示。 */
    const val settingsTokenPlaceholder: String = "ghp_xxxxxxxx"

    val actionBack: String get() = raw(K.actionBack)
    val actionRetry: String get() = raw(K.actionRetry)
    val actionCopyAll: String get() = raw(K.actionCopyAll)
    val actionReload: String get() = raw(K.actionReload)
    val actionSave: String get() = raw(K.actionSave)
    val actionClear: String get() = raw(K.actionClear)
    val actionSearch: String get() = raw(K.actionSearch)
    val actionDeleteRecord: String get() = raw(K.actionDeleteRecord)
    val actionGoHome: String get() = raw(K.actionGoHome)
    val actionOpen: String get() = raw(K.actionOpen)
    val actionPaste: String get() = raw(K.actionPaste)
    val commonRoot: String get() = raw(K.commonRoot)
    val commonFolder: String get() = raw(K.commonFolder)
    val commonCurrentSource: String get() = raw(K.commonCurrentSource)
    val commonLocalFolder: String get() = raw(K.commonLocalFolder)
    val commonLoadFailed: String get() = raw(K.commonLoadFailed)
    val commonUnknownError: String get() = raw(K.commonUnknownError)
    val commonRetryLater: String get() = raw(K.commonRetryLater)
    val commonNoAppToOpen: String get() = raw(K.commonNoAppToOpen)
    val tabHome: String get() = raw(K.tabHome)
    val tabBrowse: String get() = raw(K.tabBrowse)
    val tabSettings: String get() = raw(K.tabSettings)
    val homeTagline: String get() = raw(K.homeTagline)
    val homeGithubLink: String get() = raw(K.homeGithubLink)
    val homeParsing: String get() = raw(K.homeParsing)
    val homeLinkHint: String get() = raw(K.homeLinkHint)
    val homeOpenFolder: String get() = raw(K.homeOpenFolder)
    val homeOpenFolderDesc: String get() = raw(K.homeOpenFolderDesc)
    val homeRecent: String get() = raw(K.homeRecent)
    val homeRecentEmptyTitle: String get() = raw(K.homeRecentEmptyTitle)
    val homeRecentEmptyDesc: String get() = raw(K.homeRecentEmptyDesc)
    val homeTokenHint: String get() = raw(K.homeTokenHint)
    val browseDefaultTitle: String get() = raw(K.browseDefaultTitle)
    val browseNoSource: String get() = raw(K.browseNoSource)
    val browseRefresh: String get() = raw(K.browseRefresh)
    val browseEmptyDirTitle: String get() = raw(K.browseEmptyDirTitle)
    val browseEmptyDirDesc: String get() = raw(K.browseEmptyDirDesc)
    val browseNoRepoTitle: String get() = raw(K.browseNoRepoTitle)
    val browseNoRepoDesc: String get() = raw(K.browseNoRepoDesc)
    val viewerDefaultTitle: String get() = raw(K.viewerDefaultTitle)
    val viewerSaveToDownloads: String get() = raw(K.viewerSaveToDownloads)
    val viewerFindUsages: String get() = raw(K.viewerFindUsages)
    val viewerLineNumbers: String get() = raw(K.viewerLineNumbers)
    val viewerOpenFailed: String get() = raw(K.viewerOpenFailed)
    val viewerTruncated: String get() = raw(K.viewerTruncated)
    val usagesTitle: String get() = raw(K.usagesTitle)
    val usagesQueryLabel: String get() = raw(K.usagesQueryLabel)
    val usagesTip: String get() = raw(K.usagesTip)
    val usagesIdleTitle: String get() = raw(K.usagesIdleTitle)
    val usagesIdleDesc: String get() = raw(K.usagesIdleDesc)
    val usagesSymbolsTitle: String get() = raw(K.usagesSymbolsTitle)
    val usagesStartTitle: String get() = raw(K.usagesStartTitle)
    val usagesBlockedTitle: String get() = raw(K.usagesBlockedTitle)
    val usagesFailedTitle: String get() = raw(K.usagesFailedTitle)
    val usagesNotFoundDesc: String get() = raw(K.usagesNotFoundDesc)
    val usagesDeclaration: String get() = raw(K.usagesDeclaration)
    val usagesReference: String get() = raw(K.usagesReference)
    val usagesScope: String get() = raw(K.usagesScope)
    val usageScopeFile: String get() = raw(K.usageScopeFile)
    val usageScopeSource: String get() = raw(K.usageScopeSource)
    val settingsSubtitle: String get() = raw(K.settingsSubtitle)
    val settingsAppearance: String get() = raw(K.settingsAppearance)
    val settingsLanguage: String get() = raw(K.settingsLanguage)
    val settingsReading: String get() = raw(K.settingsReading)
    val settingsShowLineNumbers: String get() = raw(K.settingsShowLineNumbers)
    val settingsShowLineNumbersDesc: String get() = raw(K.settingsShowLineNumbersDesc)
    val settingsTokenSection: String get() = raw(K.settingsTokenSection)
    val settingsTokenDesc: String get() = raw(K.settingsTokenDesc)
    val settingsTokenStatus: String get() = raw(K.settingsTokenStatus)
    val settingsTokenSaved: String get() = raw(K.settingsTokenSaved)
    val settingsTokenUnset: String get() = raw(K.settingsTokenUnset)
    val settingsTokenReplace: String get() = raw(K.settingsTokenReplace)
    val settingsTokenSecurity: String get() = raw(K.settingsTokenSecurity)
    val settingsAbout: String get() = raw(K.settingsAbout)
    val settingsAboutApp: String get() = raw(K.settingsAboutApp)
    val settingsAboutVersion: String get() = raw(K.settingsAboutVersion)
    val settingsAboutUi: String get() = raw(K.settingsAboutUi)
    val themeSystem: String get() = raw(K.themeSystem)
    val themeLight: String get() = raw(K.themeLight)
    val themeDark: String get() = raw(K.themeDark)
    val languageSystem: String get() = raw(K.languageSystem)
    val messageTokenRequired: String get() = raw(K.messageTokenRequired)
    val messageTokenSaved: String get() = raw(K.messageTokenSaved)
    val messageTokenSaveFailed: String get() = raw(K.messageTokenSaveFailed)
    val messageTokenCleared: String get() = raw(K.messageTokenCleared)
    val errorEnterLink: String get() = raw(K.errorEnterLink)
    val errorUnrecognizedLink: String get() = raw(K.errorUnrecognizedLink)
    val errorSharedWithoutLink: String get() = raw(K.errorSharedWithoutLink)
    val errorRecentInvalid: String get() = raw(K.errorRecentInvalid)
    val errorFolderInvalid: String get() = raw(K.errorFolderInvalid)
    val errorReadFile: String get() = raw(K.errorReadFile)
    val errorNoFileOpen: String get() = raw(K.errorNoFileOpen)
    val errorNoSourceOpen: String get() = raw(K.errorNoSourceOpen)
    val errorNoNetwork: String get() = raw(K.errorNoNetwork)
    val errorTimeout: String get() = raw(K.errorTimeout)
    val errorNetwork: String get() = raw(K.errorNetwork)
    val errorNetworkFailed: String get() = raw(K.errorNetworkFailed)
    val errorEmptyBody: String get() = raw(K.errorEmptyBody)
    val errorFileTooLarge: String get() = raw(K.errorFileTooLarge)
    val apiBadToken: String get() = raw(K.apiBadToken)
    val apiRateLimited: String get() = raw(K.apiRateLimited)
    val apiForbidden: String get() = raw(K.apiForbidden)
    val apiNotFound: String get() = raw(K.apiNotFound)
    val errorFolderPermission: String get() = raw(K.errorFolderPermission)
    val errorNotFolder: String get() = raw(K.errorNotFolder)
    val errorUnnamed: String get() = raw(K.errorUnnamed)
    val errorFileMissing: String get() = raw(K.errorFileMissing)
    val errorIsFolder: String get() = raw(K.errorIsFolder)
    val errorBinaryFile: String get() = raw(K.errorBinaryFile)
    val hintNoCrossFileSearch: String get() = raw(K.hintNoCrossFileSearch)
    val hintSearchNeedsToken: String get() = raw(K.hintSearchNeedsToken)
    val downloadCreateFailed: String get() = raw(K.downloadCreateFailed)
    val downloadWriteFailed: String get() = raw(K.downloadWriteFailed)

    val actionStar: String get() = raw(K.actionStar)
    val actionUnstar: String get() = raw(K.actionUnstar)
    val actionRename: String get() = raw(K.actionRename)
    val actionDelete: String get() = raw(K.actionDelete)
    val actionRemove: String get() = raw(K.actionRemove)
    val actionCancel: String get() = raw(K.actionCancel)
    val actionConfirm: String get() = raw(K.actionConfirm)
    val actionCreate: String get() = raw(K.actionCreate)
    val tabProjects: String get() = raw(K.tabProjects)
    val projectsSubtitle: String get() = raw(K.projectsSubtitle)
    val projectsAdd: String get() = raw(K.projectsAdd)
    val projectsAddByLink: String get() = raw(K.projectsAddByLink)
    val projectsAddFromFolder: String get() = raw(K.projectsAddFromFolder)
    val projectsAddLinkLabel: String get() = raw(K.projectsAddLinkLabel)
    val projectsNewGroup: String get() = raw(K.projectsNewGroup)
    val projectsGroupName: String get() = raw(K.projectsGroupName)
    val projectsUngrouped: String get() = raw(K.projectsUngrouped)
    val projectsEmptyTitle: String get() = raw(K.projectsEmptyTitle)
    val projectsEmptyDesc: String get() = raw(K.projectsEmptyDesc)
    val projectsGroupEmpty: String get() = raw(K.projectsGroupEmpty)
    val projectsMoveTo: String get() = raw(K.projectsMoveTo)
    val projectsGroupCreated: String get() = raw(K.projectsGroupCreated)
    val projectsGroupRenamed: String get() = raw(K.projectsGroupRenamed)
    val projectsGroupDeleted: String get() = raw(K.projectsGroupDeleted)
    val projectsAdded: String get() = raw(K.projectsAdded)
    val projectsMoved: String get() = raw(K.projectsMoved)
    val projectsRemoved: String get() = raw(K.projectsRemoved)
    val dialogDeleteGroupTitle: String get() = raw(K.dialogDeleteGroupTitle)
    val dialogDeleteGroupMessage: String get() = raw(K.dialogDeleteGroupMessage)
    val dialogRemoveProjectTitle: String get() = raw(K.dialogRemoveProjectTitle)
    val dialogRemoveProjectMessage: String get() = raw(K.dialogRemoveProjectMessage)

    val actionMore: String get() = raw(K.actionMore)

    val actionRemoveFromLibrary: String get() = raw(K.actionRemoveFromLibrary)
    val projectsAddToLibrary: String get() = raw(K.projectsAddToLibrary)

    val projectsParsing: String get() = raw(K.projectsParsing)

    fun projectsCount(total: Int): String = format(K.projectsCount, total)
    fun viewerLineCount(count: Int): String = format(K.viewerLineCount, count)
    fun viewerTargetLine(line: Int): String = format(K.viewerTargetLine, line)
    fun usagesScanned(progress: String): String = format(K.usagesScanned, progress)
    fun usagesNotFound(query: String): String = format(K.usagesNotFound, query)
    fun usagesHitCount(count: Int): String = format(K.usagesHitCount, count)
    fun messageLoading(name: String): String = format(K.messageLoading, name)
    fun messageSavedTo(path: String): String = format(K.messageSavedTo, path)
    fun apiServerError(code: Int): String = format(K.apiServerError, code)
    fun downloadFailed(reason: String): String = format(K.downloadFailed, reason)

    /** 带 GitHub 原始错误信息；没有详情时用短文案。 */
    fun apiError(code: Int, detail: String): String =
        if (detail.isBlank()) format(K.apiErrorShort, code) else format(K.apiError, code, detail)
}
