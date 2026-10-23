package com.error1015.githubsrcreader.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class AppLanguageTest {

    @Test
    fun `system follows the device locale`() {
        assertEquals(AppLanguage.ZH, AppLanguage.resolve(AppLanguage.SYSTEM, Locale.SIMPLIFIED_CHINESE))
        assertEquals(AppLanguage.ZH, AppLanguage.resolve(AppLanguage.SYSTEM, Locale.TRADITIONAL_CHINESE))
        assertEquals(AppLanguage.EN, AppLanguage.resolve(AppLanguage.SYSTEM, Locale.US))
        assertEquals(AppLanguage.EN, AppLanguage.resolve(AppLanguage.SYSTEM, Locale.JAPAN))
    }

    @Test
    fun `explicit choice overrides the device locale`() {
        assertEquals(AppLanguage.EN, AppLanguage.resolve(AppLanguage.EN, Locale.SIMPLIFIED_CHINESE))
        assertEquals(AppLanguage.ZH, AppLanguage.resolve(AppLanguage.ZH, Locale.US))
    }

    @Test
    fun `tag lookup only matches registered languages`() {
        assertEquals(AppLanguage.ZH, AppLanguage.fromTag("zh"))
        assertEquals(AppLanguage.EN, AppLanguage.fromTag("EN"))
        assertNull(AppLanguage.fromTag("ja"))
        assertNull(AppLanguage.fromTag(null))
    }

    @Test
    fun `unknown or missing preference falls back to system`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromName(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromName("FR"))
        assertEquals(AppLanguage.ZH, AppLanguage.fromName("ZH"))
    }
}
