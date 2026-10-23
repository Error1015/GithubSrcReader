package com.error1015.githubsrcreader.data

import android.content.Context
import com.error1015.githubsrcreader.data.model.RecentSource
import com.error1015.githubsrcreader.data.model.ThemeMode
import com.error1015.githubsrcreader.i18n.AppLanguage
import com.error1015.githubsrcreader.data.security.SecretVault
import org.json.JSONArray
import org.json.JSONObject

/** 本地轻量持久化：Token（Keystore 加密）、显示偏好、最近打开记录。 */
class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var tokenCache: String? = null

    /**
     * GitHub Token：内存里是明文（请求要用），落盘一律是 Keystore 加密后的密文。
     * 首次读取会自动把旧版本的明文 token 迁移成密文并删除明文。
     */
    var token: String
        get() = tokenCache ?: synchronized(this) {
            tokenCache ?: readToken().also { tokenCache = it }
        }
        set(value) = writeToken(value)

    /** 是否已配置令牌——UI 只需要知道这个，不接触明文。 */
    val hasToken: Boolean
        get() = token.isNotBlank()

    var showLineNumbers: Boolean
        get() = prefs.getBoolean(KEY_LINE_NUMBERS, true)
        set(value) = prefs.edit().putBoolean(KEY_LINE_NUMBERS, value).apply()

    var language: AppLanguage
        get() = AppLanguage.fromName(prefs.getString(KEY_LANGUAGE, null))
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value.name).apply()

    var themeMode: ThemeMode
        get() {
            val raw = prefs.getString(KEY_THEME_MODE, null)
            return ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.SYSTEM
        }
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value.name).apply()

    private fun readToken(): String {
        val cipherText = prefs.getString(KEY_TOKEN_CIPHER, null)
        if (!cipherText.isNullOrBlank()) {
            val plain = SecretVault.decrypt(cipherText)
            if (plain != null) return plain
            // 密钥失效或密文损坏：清掉，避免每次启动都失败
            prefs.edit().remove(KEY_TOKEN_CIPHER).apply()
            return ""
        }

        // 旧版本把 token 明文存在 KEY_TOKEN_LEGACY：迁移后立刻删掉明文
        val legacy = prefs.getString(KEY_TOKEN_LEGACY, null)?.trim().orEmpty()
        if (legacy.isEmpty()) return ""
        writeToken(legacy)
        return legacy
    }

    private fun writeToken(value: String) {
        val trimmed = value.trim()
        val editor = prefs.edit().remove(KEY_TOKEN_LEGACY)
        if (trimmed.isEmpty()) {
            editor.remove(KEY_TOKEN_CIPHER)
        } else {
            val encrypted = SecretVault.encrypt(trimmed)
            // 加密不可用时宁可不保存，也不退回明文
            if (encrypted != null) editor.putString(KEY_TOKEN_CIPHER, encrypted)
        }
        editor.apply()
        tokenCache = trimmed
    }

    fun recentSources(): List<RecentSource> {
        val raw = prefs.getString(KEY_RECENTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.let(::toRecent)
            }
        }.getOrDefault(emptyList())
    }

    fun addRecent(source: RecentSource) {
        val updated = (listOf(source) + recentSources().filterNot { it.id == source.id })
            .take(MAX_RECENTS)
        saveRecents(updated)
    }

    fun removeRecent(id: String) {
        saveRecents(recentSources().filterNot { it.id == id })
    }

    private fun saveRecents(list: List<RecentSource>) {
        val array = JSONArray()
        list.forEach { array.put(fromRecent(it)) }
        prefs.edit().putString(KEY_RECENTS, array.toString()).apply()
    }

    private fun toRecent(json: JSONObject): RecentSource? {
        val id = json.optString("id")
        if (id.isBlank()) return null
        val type = runCatching { RecentSource.Type.valueOf(json.optString("type")) }.getOrNull() ?: return null
        return RecentSource(
            id = id,
            type = type,
            title = json.optString("title"),
            subtitle = json.optString("subtitle"),
            owner = json.optString("owner").ifBlank { null },
            repo = json.optString("repo").ifBlank { null },
            ref = json.optString("ref").ifBlank { null },
            treeUri = json.optString("treeUri").ifBlank { null },
            openedAt = json.optLong("openedAt"),
        )
    }

    private fun fromRecent(source: RecentSource): JSONObject = JSONObject().apply {
        put("id", source.id)
        put("type", source.type.name)
        put("title", source.title)
        put("subtitle", source.subtitle)
        put("owner", source.owner.orEmpty())
        put("repo", source.repo.orEmpty())
        put("ref", source.ref.orEmpty())
        put("treeUri", source.treeUri.orEmpty())
        put("openedAt", source.openedAt)
    }

    companion object {
        private const val PREFS_NAME = "github_src_reader"
        /** 旧版本明文键，仅用于迁移。 */
        private const val KEY_TOKEN_LEGACY = "token"
        /** Keystore 加密后的密文键。 */
        private const val KEY_TOKEN_CIPHER = "token_cipher"
        private const val KEY_RECENTS = "recents"
        private const val KEY_LINE_NUMBERS = "line_numbers"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LANGUAGE = "language"
        private const val MAX_RECENTS = 12
    }
}
