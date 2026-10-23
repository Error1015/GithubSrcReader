package com.error1015.githubsrcreader.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectLibraryTest {

    private fun project(
        id: String,
        openedAt: Long,
        starred: Boolean = false,
        saved: Boolean = false,
        groupId: String? = null,
        title: String = id,
    ) = Project(
        id = id,
        type = Project.Type.GITHUB,
        title = title,
        subtitle = "owner/$id",
        saved = saved,
        starred = starred,
        groupId = groupId,
        openedAt = openedAt,
        addedAt = openedAt,
    )

    @Test
    fun `starred projects come first then most recently opened`() {
        val list = listOf(
            project("a", 10),
            project("b", 30, starred = true),
            project("c", 20),
            project("d", 5, starred = true),
        )
        assertEquals(listOf("b", "d", "c", "a"), ProjectLibrary.ordered(list).map { it.id })
    }

    @Test
    fun `home recents keep pinned items and cap the size`() {
        val list = (1..5).map { project("p$it", it.toLong()) } + project("old", 0, starred = true)
        val recents = ProjectLibrary.recents(list, limit = 3)
        assertEquals(3, recents.size)
        // 置顶项一定在，剩下按时间取最新
        assertEquals(listOf("old", "p5", "p4"), recents.map { it.id })
    }

    @Test
    fun `sections keep empty groups and adopt orphaned projects`() {
        val group = ProjectGroup("g1", "组一", createdAt = 1)
        val later = ProjectGroup("g2", "组二", createdAt = 2)
        val list = listOf(
            project("a", 1, groupId = "g1"),
            project("b", 2, groupId = "g2"),
            project("orphan", 3, groupId = "deleted"),
            project("loose", 4),
        )
        val sections = ProjectLibrary.sections(list, listOf(later, group))

        assertEquals(listOf("g1", "g2", null), sections.map { it.group?.id })
        assertEquals(listOf("a"), sections[0].projects.map { it.id })
        // 指向已删除分组的项目归到未分组
        assertEquals(listOf("loose", "orphan"), sections[2].projects.map { it.id })
    }

    @Test
    fun `empty group still appears as its own section`() {
        val sections = ProjectLibrary.sections(emptyList(), listOf(ProjectGroup("g1", "空组", 1)))
        assertEquals(2, sections.size)
        assertEquals("g1", sections[0].group?.id)
        assertTrue(sections[0].projects.isEmpty())
        assertNull(sections[1].group)
    }

    @Test
    fun `upsert keeps star group and added time when reopening`() {
        val existing = listOf(project("a", openedAt = 10, starred = true, groupId = "g1"))
        val reopened = Project(
            id = "a",
            type = Project.Type.GITHUB,
            title = "a",
            subtitle = "owner/a · main",
            openedAt = 50,
        )
        val result = ProjectLibrary.upsert(existing, reopened).single()

        assertTrue(result.starred)
        assertEquals("g1", result.groupId)
        assertEquals(10L, result.addedAt)
        assertEquals(50L, result.openedAt)
        assertEquals("owner/a · main", result.subtitle)
    }

    @Test
    fun `upsert appends new project with timestamps`() {
        val incoming = project("new", openedAt = 0)
        val result = ProjectLibrary.upsert(emptyList(), incoming, now = 99).single()

        assertEquals(99L, result.openedAt)
        assertEquals(99L, result.addedAt)
        assertFalse(result.starred)
    }

    @Test
    fun `toggle star only touches the target`() {
        val list = listOf(project("a", 1), project("b", 2, starred = true))
        val result = ProjectLibrary.toggleStar(list, "a")

        assertTrue(result.first { it.id == "a" }.starred)
        assertTrue(result.first { it.id == "b" }.starred)

        val back = ProjectLibrary.toggleStar(result, "b")
        assertFalse(back.first { it.id == "b" }.starred)
    }

    @Test
    fun `move and clear group keep projects alive`() {
        val list = listOf(project("a", 1, groupId = "g1"), project("b", 2))
        assertEquals("g2", ProjectLibrary.moveTo(list, "a", "g2").first().groupId)
        assertNull(ProjectLibrary.moveTo(list, "a", null).first().groupId)

        val cleared = ProjectLibrary.clearGroup(list, "g1")
        assertEquals(2, cleared.size)
        assertNull(cleared.first { it.id == "a" }.groupId)
    }

    @Test
    fun `trim never drops saved projects`() {
        val saved = (1..3).map { project("s$it", it.toLong(), saved = true) }
        val history = (1..5).map { project("h$it", (100 + it).toLong()) }
        val trimmed = ProjectLibrary.trimmed(saved + history, maxHistory = 2)

        assertEquals(5, trimmed.size)
        assertEquals(
            setOf("s1", "s2", "s3", "h4", "h5"),
            trimmed.map { it.id }.toSet(),
        )
    }

    @Test
    fun `library only returns saved entries`() {
        val list = listOf(project("a", 1, saved = true), project("b", 2))
        assertEquals(listOf("a"), ProjectLibrary.library(list).map { it.id })
    }

    @Test
    fun `saveTo marks saved and assigns the group`() {
        val saved = ProjectLibrary.saveTo(listOf(project("a", 1)), "a", "g1", now = 50).single()

        assertTrue(saved.saved)
        assertEquals("g1", saved.groupId)
        // 已有加入时间就保留，只补空值
        assertEquals(1L, saved.addedAt)

        val fresh = Project(id = "b", type = Project.Type.LOCAL, title = "b", subtitle = "")
        assertEquals(50L, ProjectLibrary.saveTo(listOf(fresh), "b", null, now = 50).single().addedAt)
    }

    @Test
    fun `removing from library keeps history and group`() {
        val removed = ProjectLibrary
            .removeFromLibrary(listOf(project("a", 1, saved = true, groupId = "g1")), "a")
            .single()

        assertFalse(removed.saved)
        // 历史仍在，分组归属保留，重新加入时回到原分组
        assertEquals("g1", removed.groupId)
        assertEquals(1L, removed.openedAt)
    }

    @Test
    fun `reopening a saved project keeps it in the library`() {
        val existing = listOf(project("a", 10, saved = true, groupId = "g1"))
        val reopened = Project(
            id = "a",
            type = Project.Type.GITHUB,
            title = "a",
            subtitle = "owner/a · main",
            openedAt = 20,
        )
        val result = ProjectLibrary.upsert(existing, reopened).single()

        assertTrue(result.saved)
        assertEquals("g1", result.groupId)
    }

    @Test
    fun `removing a project leaves the rest untouched`() {
        val list = listOf(project("a", 1), project("b", 2))
        assertEquals(listOf("b"), ProjectLibrary.without(list, "a").map { it.id })
    }
}
