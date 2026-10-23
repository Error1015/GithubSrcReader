package com.error1015.githubsrcreader.data.model

/**
 * 项目条目：GitHub 仓库或本地文件夹。
 *
 * 一条记录同时承担两种身份：
 * - 打开任何来源都会留下一条历史（[saved] = false），首页「最近打开」就是按时间取其中的一部分；
 * - 用户显式「加入项目库」后 [saved] = true，才会出现在项目页，可分组、可标星，
 *   并且**不会被历史裁剪掉**。
 */
data class Project(
    val id: String,
    val type: Type,
    val title: String,
    val subtitle: String,
    val owner: String? = null,
    val repo: String? = null,
    val ref: String? = null,
    val treeUri: String? = null,
    /** 是否已加入项目库；false 表示只在「最近打开」里。 */
    val saved: Boolean = false,
    /** 所属分组 id；null 表示未分组（仅对已加入项目库的条目有意义）。 */
    val groupId: String? = null,
    /** 标星：在首页与项目页都置顶显示。 */
    val starred: Boolean = false,
    /** 最近一次打开时间（毫秒）。 */
    val openedAt: Long = 0L,
    /** 首次加入项目库的时间（毫秒）。 */
    val addedAt: Long = 0L,
) {
    enum class Type { GITHUB, LOCAL }
}

/** 用户自建的项目分组，相当于文件夹。 */
data class ProjectGroup(
    val id: String,
    val name: String,
    val createdAt: Long = 0L,
)

/** 项目页的一段：一个分组，[group] 为 null 时表示「未分组」。 */
data class ProjectSection(
    val group: ProjectGroup?,
    val projects: List<Project>,
)
