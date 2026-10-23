package com.error1015.githubsrcreader.data

import android.content.Context
import com.error1015.githubsrcreader.data.model.Project
import com.error1015.githubsrcreader.data.model.ProjectGroup
import com.error1015.githubsrcreader.data.model.ProjectLibrary
import com.error1015.githubsrcreader.data.model.ThemeMode
import com.error1015.githubsrcreader.i18n.AppLanguage
import com.error1015.githubsrcreader.data.security.SecretVault
import org.json.JSONArray
import org.json.JSONObject

/** 本地轻量持久化：Token（Keystore 加密）、显示偏好、项目库与分组。 */
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

    fun projects(): List<Project> {
        val raw = prefs.getString(KEY_PROJECTS, null) ?: return migrateLegacyRecents()
        return parseProjects(raw)
    }

    /** 读-改-写：项目库的所有变更都走这里，保证落盘结果与返回的列表一致。 */
    fun updateProjects(transform: (List<Project>) -> List<Project>): List<Project> {
        val updated = transform(projects())
        saveProjects(updated)
        return updated
    }

    fun groups(): List<ProjectGroup> {
        val raw = prefs.getString(KEY_GROUPS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.let(::toGroup)
            }
        }.getOrDefault(emptyList())
    }

    fun updateGroups(transform: (List<ProjectGroup>) -> List<ProjectGroup>): List<ProjectGroup> {
        val updated = transform(groups())
        val array = JSONArray()
        updated.forEach { array.put(fromGroup(it)) }
        prefs.edit().putString(KEY_GROUPS, array.toString()).apply()
        return updated
    }

    /** 删除分组：组内项目归到未分组，项目本身不删除。 */
    fun deleteGroup(groupId: String) {
        updateGroups { list -> list.filterNot { it.id == groupId } }
        updateProjects { ProjectLibrary.clearGroup(it, groupId) }
    }

    /**
     * 旧版本只存了「最近打开」（键 `recents`），这里一次性迁移成项目库。
     * 迁移后删掉旧键，避免重复处理。
     */
    private fun migrateLegacyRecents(): List<Project> {
        val raw = prefs.getString(KEY_RECENTS_LEGACY, null) ?: return emptyList()
        val migrated = runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val json = array.optJSONObject(index) ?: return@mapNotNull null
                val id = json.optString("id")
                val type = runCatching { Project.Type.valueOf(json.optString("type")) }.getOrNull()
                val openedAt = json.optLong("openedAt")
                if (id.isBlank() || type == null) null else Project(
                    id = id,
                    type = type,
                    title = json.optString("title"),
                    subtitle = json.optString("subtitle"),
                    owner = json.optString("owner").ifBlank { null },
                    repo = json.optString("repo").ifBlank { null },
                    ref = json.optString("ref").ifBlank { null },
                    treeUri = json.optString("treeUri").ifBlank { null },
                    // 旧版本没有 saved 字段，那时的条目都出现在项目页，迁移过来继续保留
                    saved = true,
                    openedAt = openedAt,
                    addedAt = openedAt,
                )
            }
        }.getOrDefault(emptyList())
        prefs.edit().remove(KEY_RECENTS_LEGACY).apply()
        if (migrated.isNotEmpty()) saveProjects(migrated)
        return migrated
    }

    private fun saveProjects(list: List<Project>) {
        val array = JSONArray()
        list.forEach { array.put(fromProject(it)) }
        prefs.edit().putString(KEY_PROJECTS, array.toString()).apply()
    }

    private fun parseProjects(raw: String): List<Project> = runCatching {
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let(::toProject)
        }
    }.getOrDefault(emptyList())

    private fun toProject(json: JSONObject): Project? {
        val id = json.optString("id")
        if (id.isBlank()) return null
        val type = runCatching { Project.Type.valueOf(json.optString("type")) }.getOrNull() ?: return null
        return Project(
            id = id,
            type = type,
            title = json.optString("title"),
            subtitle = json.optString("subtitle"),
            owner = json.optString("owner").ifBlank { null },
            repo = json.optString("repo").ifBlank { null },
            ref = json.optString("ref").ifBlank { null },
            treeUri = json.optString("treeUri").ifBlank { null },
            // 没有 saved 字段 = 旧版本数据，那时项目页展示全部条目，按「已入库」处理，避免升级后清空
            saved = if (json.has("saved")) json.optBoolean("saved") else true,
            groupId = json.optString("groupId").ifBlank { null },
            starred = json.optBoolean("starred", false),
            openedAt = json.optLong("openedAt"),
            addedAt = json.optLong("addedAt"),
        )
    }

    private fun fromProject(project: Project): JSONObject = JSONObject().apply {
        put("id", project.id)
        put("type", project.type.name)
        put("title", project.title)
        put("subtitle", project.subtitle)
        put("owner", project.owner.orEmpty())
        put("repo", project.repo.orEmpty())
        put("ref", project.ref.orEmpty())
        put("treeUri", project.treeUri.orEmpty())
        put("saved", project.saved)
        put("groupId", project.groupId.orEmpty())
        put("starred", project.starred)
        put("openedAt", project.openedAt)
        put("addedAt", project.addedAt)
    }

    private fun toGroup(json: JSONObject): ProjectGroup? {
        val id = json.optString("id")
        if (id.isBlank()) return null
        val name = json.optString("name").trim()
        if (name.isEmpty()) return null
        return ProjectGroup(id = id, name = name, createdAt = json.optLong("createdAt"))
    }

    private fun fromGroup(group: ProjectGroup): JSONObject = JSONObject().apply {
        put("id", group.id)
        put("name", group.name)
        put("createdAt", group.createdAt)
    }

    companion object {
        private const val PREFS_NAME = "github_src_reader"
        /** 旧版本明文键，仅用于迁移。 */
        private const val KEY_TOKEN_LEGACY = "token"
        /** Keystore 加密后的密文键。 */
        private const val KEY_TOKEN_CIPHER = "token_cipher"
        private const val KEY_RECENTS = "recents"
        /** 项目库（取代旧的「最近打开」）。 */
        private const val KEY_PROJECTS = "projects"
        private const val KEY_GROUPS = "groups"
        /** 旧版本键名，仅用于迁移。 */
        private const val KEY_RECENTS_LEGACY = KEY_RECENTS
        private const val KEY_LINE_NUMBERS = "line_numbers"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LANGUAGE = "language"
    }
}
