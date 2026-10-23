package com.error1015.githubsrcreader.i18n

import com.error1015.githubsrcreader.i18n.texts.EnTexts
import com.error1015.githubsrcreader.i18n.texts.ZhTexts
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StringsTest {

    @After
    fun reset() {
        // 全局文案表是有状态的，避免影响其它用例
        Strings.apply(AppLanguage.ZH)
    }

    // ------------------------------------------------------------ 表结构

    @Test
    fun `every declared key exists in every language table`() {
        val expected = K.allKeys.toSet()
        assertEquals("K 里有重复键", expected.size, K.allKeys.size)
        assertEquals("中文表缺词条或多词条", expected, ZhTexts.keys)
        assertEquals("英文表缺词条或多词条", expected, EnTexts.keys)
    }

    @Test
    fun `no blank translations`() {
        listOf("zh" to ZhTexts, "en" to EnTexts).forEach { (lang, table) ->
            table.forEach { (key, value) ->
                assertTrue("$lang 的 $key 是空文案", value.isNotBlank())
            }
        }
    }

    @Test
    fun `english table holds no chinese`() {
        EnTexts.forEach { (key, value) ->
            assertTrue("英文表 $key 里还有中文：$value", value.none { it.code in 0x4E00..0x9FFF })
        }
    }

    // ------------------------------------------------------------ 取值

    @Test
    fun `same key returns the text of current language`() {
        Strings.apply(AppLanguage.ZH)
        assertEquals("首页", Strings.tabHome)
        assertEquals("查找引用", Strings.usagesTitle)

        Strings.apply(AppLanguage.EN)
        assertEquals("Home", Strings.tabHome)
        assertEquals("Find usages", Strings.usagesTitle)
    }

    @Test
    fun `placeholder entries are formatted in both languages`() {
        Strings.apply(AppLanguage.ZH)
        assertEquals("第 12 行", Strings.viewerTargetLine(12))
        assertEquals("12 行", Strings.viewerLineCount(12))
        assertEquals("没有找到「foo」的引用", Strings.usagesNotFound("foo"))
        assertEquals("已扫描 3 / 10", Strings.usagesScanned("3 / 10"))
        assertEquals("GitHub 返回错误（404）：Not Found", Strings.apiError(404, "Not Found"))

        Strings.apply(AppLanguage.EN)
        assertEquals("Line 12", Strings.viewerTargetLine(12))
        assertEquals("12 lines", Strings.viewerLineCount(12))
        assertEquals("No usages of “foo” found", Strings.usagesNotFound("foo"))
    }

    @Test
    fun `missing entries fall back to english then to the key`() {
        assertEquals("Home", resolveText(K.tabHome, emptyMap()))
        assertEquals("no.such.key", resolveText("no.such.key", emptyMap()))
        assertEquals("自定义", resolveText(K.tabHome, mapOf(K.tabHome to "自定义")))
    }
}
