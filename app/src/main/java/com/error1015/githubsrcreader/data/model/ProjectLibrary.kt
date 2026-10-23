package com.error1015.githubsrcreader.data.model

/**
 * 项目库的纯逻辑（无 Android 依赖，可单测）：
 * 排序（星标置顶）、分组切分、加入/移出项目库、历史裁剪。
 *
 * 一条 [Project] 同时是「历史记录」和「项目库条目」：看 [Project.saved]。
 * 历史会自动裁剪，已加入项目库的条目永远保留。
 */
object ProjectLibrary {

    /** 首页「最近打开」最多展示的条数。 */
    const val HOME_RECENTS = 12

    /** 历史记录（未加入项目库的条目）最多保留的条数，超出后丢弃最久未打开的。 */
    const val HISTORY_LIMIT = 60

    /** 星标优先，其次按最近打开时间倒序。 */
    fun ordered(projects: List<Project>): List<Project> = projects.sortedWith(
        compareByDescending<Project> { it.starred }
            .thenByDescending { it.openedAt },
    )

    /** 首页列表：置顶（星标）在最前，其余按打开时间。 */
    fun recents(projects: List<Project>, limit: Int = HOME_RECENTS): List<Project> =
        ordered(projects).take(limit)

    /** 项目库：只包含用户显式加入过的条目。 */
    fun library(projects: List<Project>): List<Project> = projects.filter { it.saved }

    /**
     * 切分成项目页的若干段：先按创建时间排各分组，最后是「未分组」。
     * 空分组也会保留（便于作为收纳目标），指向已删除分组的项目归到未分组。
     *
     * 传进来的应当已经是项目库条目（[library]）。
     */
    fun sections(projects: List<Project>, groups: List<ProjectGroup>): List<ProjectSection> {
        val known = groups.mapTo(mutableSetOf()) { it.id }
        val grouped = groups
            .sortedWith(compareBy({ it.createdAt }, { it.name }))
            .map { group -> ProjectSection(group, ordered(projects.filter { it.groupId == group.id })) }
        val ungrouped = projects.filter { it.groupId == null || it.groupId !in known }
        return grouped + ProjectSection(null, ordered(ungrouped))
    }

    /**
     * 打开或添加：同 id 已存在时保留原有的星标、分组、加入状态与加入时间，
     * 只刷新标题、分支、时间——这样重新打开不会把收藏状态冲掉。
     */
    fun upsert(
        projects: List<Project>,
        incoming: Project,
        now: Long = incoming.openedAt,
    ): List<Project> {
        val existing = projects.firstOrNull { it.id == incoming.id }
        if (existing == null) {
            val added = incoming.copy(
                addedAt = if (incoming.addedAt > 0L) incoming.addedAt else now,
                openedAt = if (incoming.openedAt > 0L) incoming.openedAt else now,
            )
            return trimmed(projects + added)
        }
        val updated = projects.map { current ->
            if (current.id != incoming.id) current
            else incoming.copy(
                saved = incoming.saved || existing.saved,
                groupId = existing.groupId,
                starred = existing.starred,
                addedAt = existing.addedAt,
                openedAt = maxOf(incoming.openedAt, current.openedAt),
            )
        }
        return trimmed(updated)
    }

    /** 加入项目库（已加入时相当于换分组）。 */
    fun saveTo(projects: List<Project>, id: String, groupId: String?, now: Long = 0L): List<Project> =
        projects.map { project ->
            if (project.id != id) project
            else project.copy(
                saved = true,
                groupId = groupId,
                addedAt = if (project.addedAt > 0L) project.addedAt else now,
            )
        }

    /** 移出项目库：只取消收藏，历史记录与分组归属都保留，重新加入时回到原分组。 */
    fun removeFromLibrary(projects: List<Project>, id: String): List<Project> =
        projects.map { if (it.id == id) it.copy(saved = false) else it }

    fun toggleStar(projects: List<Project>, id: String): List<Project> =
        projects.map { if (it.id == id) it.copy(starred = !it.starred) else it }

    fun without(projects: List<Project>, id: String): List<Project> =
        projects.filterNot { it.id == id }

    fun moveTo(projects: List<Project>, id: String, groupId: String?): List<Project> =
        projects.map { if (it.id == id) it.copy(groupId = groupId) else it }

    /** 删除分组时把组内项目归到未分组，项目本身保留。 */
    fun clearGroup(projects: List<Project>, groupId: String): List<Project> =
        projects.map { if (it.groupId == groupId) it.copy(groupId = null) else it }

    /**
     * 只裁剪历史：已加入项目库的条目原样保留，
     * 未加入的按打开时间留最新的 [maxHistory] 条。
     */
    fun trimmed(projects: List<Project>, maxHistory: Int = HISTORY_LIMIT): List<Project> {
        val saved = projects.filter { it.saved }
        val history = projects.filterNot { it.saved }
        if (history.size <= maxHistory) return projects
        return saved + history.sortedByDescending { it.openedAt }.take(maxHistory)
    }
}
