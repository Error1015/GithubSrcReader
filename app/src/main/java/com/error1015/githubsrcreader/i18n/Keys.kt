package com.error1015.githubsrcreader.i18n

/**
 * 文案键。新增文案：① 这里加一个键；② 各语言表（texts/）补上对应值。
 * 忘了补表会被 `StringsTest` 的完整性校验拦下。
 */
internal object K {
    const val actionBack = "actionBack"
    const val actionRetry = "actionRetry"
    const val actionCopyAll = "actionCopyAll"
    const val actionReload = "actionReload"
    const val actionSave = "actionSave"
    const val actionClear = "actionClear"
    const val actionSearch = "actionSearch"
    const val actionDeleteRecord = "actionDeleteRecord"
    const val actionGoHome = "actionGoHome"
    const val actionOpen = "actionOpen"
    const val actionPaste = "actionPaste"
    const val commonRoot = "commonRoot"
    const val commonFolder = "commonFolder"
    const val commonCurrentSource = "commonCurrentSource"
    const val commonLocalFolder = "commonLocalFolder"
    const val commonLoadFailed = "commonLoadFailed"
    const val commonUnknownError = "commonUnknownError"
    const val commonRetryLater = "commonRetryLater"
    const val commonNoAppToOpen = "commonNoAppToOpen"
    const val tabHome = "tabHome"
    const val tabBrowse = "tabBrowse"
    const val tabSettings = "tabSettings"
    const val homeTagline = "homeTagline"
    const val homeGithubLink = "homeGithubLink"
    const val homeParsing = "homeParsing"
    const val homeLinkHint = "homeLinkHint"
    const val homeOpenFolder = "homeOpenFolder"
    const val homeOpenFolderDesc = "homeOpenFolderDesc"
    const val homeRecent = "homeRecent"
    const val homeRecentEmptyTitle = "homeRecentEmptyTitle"
    const val homeRecentEmptyDesc = "homeRecentEmptyDesc"
    const val homeTokenHint = "homeTokenHint"
    const val browseDefaultTitle = "browseDefaultTitle"
    const val browseNoSource = "browseNoSource"
    const val browseRefresh = "browseRefresh"
    const val browseEmptyDirTitle = "browseEmptyDirTitle"
    const val browseEmptyDirDesc = "browseEmptyDirDesc"
    const val browseNoRepoTitle = "browseNoRepoTitle"
    const val browseNoRepoDesc = "browseNoRepoDesc"
    const val viewerDefaultTitle = "viewerDefaultTitle"
    const val viewerSaveToDownloads = "viewerSaveToDownloads"
    const val viewerFindUsages = "viewerFindUsages"
    const val viewerLineNumbers = "viewerLineNumbers"
    const val viewerOpenFailed = "viewerOpenFailed"
    const val viewerTruncated = "viewerTruncated"
    const val viewerLineCount = "viewerLineCount"
    const val viewerTargetLine = "viewerTargetLine"
    const val usagesTitle = "usagesTitle"
    const val usagesQueryLabel = "usagesQueryLabel"
    const val usagesTip = "usagesTip"
    const val usagesIdleTitle = "usagesIdleTitle"
    const val usagesIdleDesc = "usagesIdleDesc"
    const val usagesSymbolsTitle = "usagesSymbolsTitle"
    const val usagesStartTitle = "usagesStartTitle"
    const val usagesBlockedTitle = "usagesBlockedTitle"
    const val usagesFailedTitle = "usagesFailedTitle"
    const val usagesNotFoundDesc = "usagesNotFoundDesc"
    const val usagesDeclaration = "usagesDeclaration"
    const val usagesReference = "usagesReference"
    const val usagesScope = "usagesScope"
    const val usagesScanned = "usagesScanned"
    const val usagesNotFound = "usagesNotFound"
    const val usagesHitCount = "usagesHitCount"
    const val usageScopeFile = "usageScopeFile"
    const val usageScopeSource = "usageScopeSource"
    const val settingsSubtitle = "settingsSubtitle"
    const val settingsAppearance = "settingsAppearance"
    const val settingsLanguage = "settingsLanguage"
    const val settingsReading = "settingsReading"
    const val settingsShowLineNumbers = "settingsShowLineNumbers"
    const val settingsShowLineNumbersDesc = "settingsShowLineNumbersDesc"
    const val settingsCodeTheme = "settingsCodeTheme"
    const val settingsCodeThemeDesc = "settingsCodeThemeDesc"
    const val codeThemeFollowApp = "codeThemeFollowApp"
    const val codeThemeOneDarkPro = "codeThemeOneDarkPro"
    const val settingsTokenSection = "settingsTokenSection"
    const val settingsTokenDesc = "settingsTokenDesc"
    const val settingsTokenStatus = "settingsTokenStatus"
    const val settingsTokenSaved = "settingsTokenSaved"
    const val settingsTokenUnset = "settingsTokenUnset"
    const val settingsTokenReplace = "settingsTokenReplace"
    const val settingsTokenSecurity = "settingsTokenSecurity"
    const val settingsAbout = "settingsAbout"
    const val settingsAboutApp = "settingsAboutApp"
    const val settingsAboutVersion = "settingsAboutVersion"
    const val settingsAboutUi = "settingsAboutUi"
    const val themeSystem = "themeSystem"
    const val themeLight = "themeLight"
    const val themeDark = "themeDark"
    const val languageSystem = "languageSystem"
    const val messageLoading = "messageLoading"
    const val messageSavedTo = "messageSavedTo"
    const val messageTokenRequired = "messageTokenRequired"
    const val messageTokenSaved = "messageTokenSaved"
    const val messageTokenSaveFailed = "messageTokenSaveFailed"
    const val messageTokenCleared = "messageTokenCleared"
    const val errorEnterLink = "errorEnterLink"
    const val errorUnrecognizedLink = "errorUnrecognizedLink"
    const val errorSharedWithoutLink = "errorSharedWithoutLink"
    const val errorRecentInvalid = "errorRecentInvalid"
    const val errorFolderInvalid = "errorFolderInvalid"
    const val errorReadFile = "errorReadFile"
    const val errorNoFileOpen = "errorNoFileOpen"
    const val errorNoSourceOpen = "errorNoSourceOpen"
    const val errorNoNetwork = "errorNoNetwork"
    const val errorTimeout = "errorTimeout"
    const val errorNetwork = "errorNetwork"
    const val errorNetworkFailed = "errorNetworkFailed"
    const val errorEmptyBody = "errorEmptyBody"
    const val errorFileTooLarge = "errorFileTooLarge"
    const val apiBadToken = "apiBadToken"
    const val apiRateLimited = "apiRateLimited"
    const val apiForbidden = "apiForbidden"
    const val apiNotFound = "apiNotFound"
    const val apiServerError = "apiServerError"
    const val apiError = "apiError"
    const val apiErrorShort = "apiErrorShort"
    const val errorFolderPermission = "errorFolderPermission"
    const val errorNotFolder = "errorNotFolder"
    const val errorUnnamed = "errorUnnamed"
    const val errorFileMissing = "errorFileMissing"
    const val errorIsFolder = "errorIsFolder"
    const val errorBinaryFile = "errorBinaryFile"
    const val hintNoCrossFileSearch = "hintNoCrossFileSearch"
    const val hintSearchNeedsToken = "hintSearchNeedsToken"
    const val downloadCreateFailed = "downloadCreateFailed"
    const val downloadWriteFailed = "downloadWriteFailed"
    const val downloadFailed = "downloadFailed"
    const val actionStar = "actionStar"
    const val actionUnstar = "actionUnstar"
    const val actionRename = "actionRename"
    const val actionDelete = "actionDelete"
    const val actionRemove = "actionRemove"
    const val actionCancel = "actionCancel"
    const val actionConfirm = "actionConfirm"
    const val actionCreate = "actionCreate"
    const val tabProjects = "tabProjects"
    const val projectsSubtitle = "projectsSubtitle"
    const val projectsAdd = "projectsAdd"
    const val projectsAddByLink = "projectsAddByLink"
    const val projectsAddFromFolder = "projectsAddFromFolder"
    const val projectsAddLinkLabel = "projectsAddLinkLabel"
    const val projectsNewGroup = "projectsNewGroup"
    const val projectsGroupName = "projectsGroupName"
    const val projectsUngrouped = "projectsUngrouped"
    const val projectsEmptyTitle = "projectsEmptyTitle"
    const val projectsEmptyDesc = "projectsEmptyDesc"
    const val projectsGroupEmpty = "projectsGroupEmpty"
    const val projectsCount = "projectsCount"
    const val projectsMoveTo = "projectsMoveTo"
    const val projectsGroupCreated = "projectsGroupCreated"
    const val projectsGroupRenamed = "projectsGroupRenamed"
    const val projectsGroupDeleted = "projectsGroupDeleted"
    const val projectsAdded = "projectsAdded"
    const val projectsMoved = "projectsMoved"
    const val projectsRemoved = "projectsRemoved"
    const val dialogDeleteGroupTitle = "dialogDeleteGroupTitle"
    const val dialogDeleteGroupMessage = "dialogDeleteGroupMessage"
    const val dialogRemoveProjectTitle = "dialogRemoveProjectTitle"
    const val dialogRemoveProjectMessage = "dialogRemoveProjectMessage"

    const val actionMore = "actionMore"

    const val actionRemoveFromLibrary = "actionRemoveFromLibrary"
    const val projectsAddToLibrary = "projectsAddToLibrary"

    const val projectsParsing = "projectsParsing"

    /** 所有键，供测试校验各语言表是否齐全。 */
    val allKeys: List<String> = listOf(
        actionBack, actionRetry, actionCopyAll, actionReload,
        actionSave, actionClear, actionSearch, actionDeleteRecord,
        actionGoHome, actionOpen, actionPaste, commonRoot,
        commonFolder, commonCurrentSource, commonLocalFolder, commonLoadFailed,
        commonUnknownError, commonRetryLater, commonNoAppToOpen, tabHome,
        tabBrowse, tabSettings, homeTagline, homeGithubLink,
        homeParsing, homeLinkHint, homeOpenFolder, homeOpenFolderDesc,
        homeRecent, homeRecentEmptyTitle, homeRecentEmptyDesc, homeTokenHint,
        browseDefaultTitle, browseNoSource, browseRefresh, browseEmptyDirTitle,
        browseEmptyDirDesc, browseNoRepoTitle, browseNoRepoDesc, viewerDefaultTitle,
        viewerSaveToDownloads, viewerFindUsages, viewerLineNumbers, viewerOpenFailed,
        viewerTruncated, viewerLineCount, viewerTargetLine, usagesTitle,
        usagesQueryLabel, usagesTip, usagesIdleTitle, usagesIdleDesc,
        usagesSymbolsTitle, usagesStartTitle, usagesBlockedTitle, usagesFailedTitle,
        usagesNotFoundDesc, usagesDeclaration, usagesReference, usagesScope,
        usagesScanned, usagesNotFound, usagesHitCount, usageScopeFile,
        usageScopeSource, settingsSubtitle, settingsAppearance, settingsLanguage,
        settingsReading, settingsShowLineNumbers, settingsShowLineNumbersDesc, settingsCodeTheme,
        settingsCodeThemeDesc, codeThemeFollowApp, codeThemeOneDarkPro, settingsTokenSection,
        settingsTokenDesc, settingsTokenStatus, settingsTokenSaved, settingsTokenUnset,
        settingsTokenReplace, settingsTokenSecurity, settingsAbout, settingsAboutApp,
        settingsAboutVersion, settingsAboutUi, themeSystem, themeLight,
        themeDark, languageSystem, messageLoading, messageSavedTo,
        messageTokenRequired, messageTokenSaved, messageTokenSaveFailed, messageTokenCleared,
        errorEnterLink, errorUnrecognizedLink, errorSharedWithoutLink, errorRecentInvalid,
        errorFolderInvalid, errorReadFile, errorNoFileOpen, errorNoSourceOpen,
        errorNoNetwork, errorTimeout, errorNetwork, errorNetworkFailed,
        errorEmptyBody, errorFileTooLarge, apiBadToken, apiRateLimited,
        apiForbidden, apiNotFound, apiServerError, apiError,
        apiErrorShort, errorFolderPermission, errorNotFolder, errorUnnamed,
        errorFileMissing, errorIsFolder, errorBinaryFile, hintNoCrossFileSearch,
        hintSearchNeedsToken, downloadCreateFailed, downloadWriteFailed, downloadFailed,
        actionStar, actionUnstar, actionRename, actionDelete,
        actionRemove, actionCancel, actionConfirm, actionCreate,
        tabProjects, projectsSubtitle, projectsAdd, projectsAddByLink,
        projectsAddFromFolder, projectsAddLinkLabel, projectsNewGroup, projectsGroupName,
        projectsUngrouped, projectsEmptyTitle, projectsEmptyDesc, projectsGroupEmpty,
        projectsCount, projectsMoveTo, projectsGroupCreated, projectsGroupRenamed,
        projectsGroupDeleted, projectsAdded, projectsMoved, projectsRemoved,
        dialogDeleteGroupTitle, dialogDeleteGroupMessage, dialogRemoveProjectTitle, dialogRemoveProjectMessage,
        actionMore,
        actionRemoveFromLibrary,
        projectsAddToLibrary,
        projectsParsing,
    )
}
