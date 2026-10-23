package com.error1015.githubsrcreader.i18n

import java.util.Locale

/**
 * 应用内语言。
 *
 * @param tag BCP-47 语言标签，对应 `i18n/texts` 里的一张文案表；SYSTEM 为 null
 * @param endonym 语言名，按惯例用该语言自身书写（简体中文 / English / 日本語），不参与翻译
 *
 * 新增一种语言时这里加一个枚举值，编译器会强制你补上 `i18n.label` 与文案表登记。
 */
enum class AppLanguage(val tag: String?, val endonym: String) {
    SYSTEM(null, ""),
    ZH("zh", "简体中文"),
    EN("en", "English"),
    ;

    companion object {
        fun fromName(raw: String?): AppLanguage = entries.firstOrNull { it.name == raw } ?: SYSTEM

        /** 按 BCP-47 标签找语言（"zh" / "en" …），找不到返回 null。 */
        fun fromTag(tag: String?): AppLanguage? =
            tag?.let { value -> entries.firstOrNull { it.tag.equals(value, ignoreCase = true) } }

        /**
         * SYSTEM 时跟随系统语言：命中已支持的语言就用它，否则英文。
         * 这里**不写死语言列表**，新增语言后系统语言自动生效。
         */
        fun resolve(preference: AppLanguage, system: Locale): AppLanguage = when (preference) {
            SYSTEM -> fromTag(system.language) ?: EN
            else -> preference
        }
    }
}
