package com.termux.shared.theme

import androidx.appcompat.app.AppCompatDelegate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class NightModeTest {

    @Test
    fun `entries carry names and appcompat modes`() {
        assertEquals("true", NightMode.TRUE.getName())
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, NightMode.TRUE.getMode())
        assertEquals("false", NightMode.FALSE.getName())
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, NightMode.FALSE.getMode())
        assertEquals("system", NightMode.SYSTEM.getName())
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, NightMode.SYSTEM.getMode())
    }

    @Test
    fun `modeOf resolves and falls back`() {
        assertSame(NightMode.TRUE, NightMode.modeOf("true"))
        assertNull(NightMode.modeOf("bogus"))
        assertNull(NightMode.modeOf(null))
        assertSame(NightMode.FALSE, NightMode.modeOf("bogus", NightMode.FALSE))
        assertSame(NightMode.TRUE, NightMode.modeOf("true", NightMode.FALSE))
    }

    @Test
    fun `app night mode defaults to system`() {
        NightMode.setAppNightMode(null)
        assertSame(NightMode.SYSTEM, NightMode.getAppNightMode())
    }

    @Test
    fun `app night mode can be set`() {
        NightMode.setAppNightMode("false")
        assertSame(NightMode.FALSE, NightMode.getAppNightMode())
        NightMode.setAppNightMode("system")
        assertSame(NightMode.SYSTEM, NightMode.getAppNightMode())
    }

    @Test
    fun `invalid app night mode keeps previous`() {
        NightMode.setAppNightMode("false")
        NightMode.setAppNightMode("bogus")
        assertSame(NightMode.FALSE, NightMode.getAppNightMode())
        NightMode.setAppNightMode("system")
    }
}
