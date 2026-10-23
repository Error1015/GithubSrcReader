package com.error1015.githubsrcreader.i18n

import com.error1015.githubsrcreader.data.model.ThemeMode
import com.error1015.githubsrcreader.ui.UsageScope
import com.error1015.githubsrcreader.ui.components.AppTab

/** 枚举值的本地化显示名（枚举本身不掺文案，便于数据层复用）。 */

val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.SYSTEM -> Strings.themeSystem
        ThemeMode.LIGHT -> Strings.themeLight
        ThemeMode.DARK -> Strings.themeDark
    }

val UsageScope.label: String
    get() = when (this) {
        UsageScope.FILE -> Strings.usageScopeFile
        UsageScope.SOURCE -> Strings.usageScopeSource
    }

val AppTab.label: String
    get() = when (this) {
        AppTab.Home -> Strings.tabHome
        AppTab.Browse -> Strings.tabBrowse
        AppTab.Settings -> Strings.tabSettings
    }

/** “跟随系统”需要翻译，具体语言名用其自身的写法。 */
val AppLanguage.label: String
    get() = if (this == AppLanguage.SYSTEM) Strings.languageSystem else endonym
