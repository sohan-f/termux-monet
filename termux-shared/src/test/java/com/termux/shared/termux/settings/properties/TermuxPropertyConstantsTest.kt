package com.termux.shared.termux.settings.properties

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plain-JVM tests pinning [TermuxPropertyConstants] values and maps.
 */
class TermuxPropertyConstantsTest {

    @Test
    fun `bell map covers all three behaviours`() {
        val map = TermuxPropertyConstants.MAP_BELL_BEHAVIOUR
        assertEquals(1, map["vibrate"])
        assertEquals(2, map["beep"])
        assertEquals(3, map["ignore"])
        assertNull(map["bogus"])
        assertEquals(1, TermuxPropertyConstants.DEFAULT_IVALUE_BELL_BEHAVIOUR)
    }

    @Test
    fun `cursor style map round-trips`() {
        val map = TermuxPropertyConstants.MAP_TERMINAL_CURSOR_STYLE
        assertEquals(map["block"], TermuxPropertyConstants.IVALUE_TERMINAL_CURSOR_STYLE_BLOCK)
        assertEquals(map["underline"], TermuxPropertyConstants.IVALUE_TERMINAL_CURSOR_STYLE_UNDERLINE)
        assertEquals(map["bar"], TermuxPropertyConstants.IVALUE_TERMINAL_CURSOR_STYLE_BAR)
    }

    @Test
    fun `night mode defaults to system`() {
        assertEquals(
            TermuxPropertyConstants.IVALUE_NIGHT_MODE_SYSTEM,
            TermuxPropertyConstants.DEFAULT_IVALUE_NIGHT_MODE
        )
        assertEquals("system", TermuxPropertyConstants.IVALUE_NIGHT_MODE_SYSTEM)
    }

    @Test
    fun `toolbar height bounds are sane floats`() {
        assertEquals(0.4f, TermuxPropertyConstants.IVALUE_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR_MIN)
        assertEquals(3f, TermuxPropertyConstants.IVALUE_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR_MAX)
        assertEquals(1f, TermuxPropertyConstants.DEFAULT_IVALUE_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR)
    }

    @Test
    fun `properties list contains representative keys`() {
        val list = TermuxPropertyConstants.TERMUX_APP_PROPERTIES_LIST
        assertTrue(list.contains(TermuxPropertyConstants.KEY_RUN_TERMUX_AM_SOCKET_SERVER))
        assertTrue(list.contains(TermuxPropertyConstants.KEY_NIGHT_MODE))
        assertTrue(list.contains(TermuxPropertyConstants.KEY_EXTRA_KEYS))
        assertTrue(list.contains(TermuxPropertyConstants.KEY_BACKGROUND_OVERLAY_COLOR))
        assertTrue(TermuxPropertyConstants.TERMUX_DEFAULT_TRUE_BOOLEAN_BEHAVIOUR_PROPERTIES_LIST.contains(TermuxPropertyConstants.KEY_RUN_TERMUX_AM_SOCKET_SERVER))
        assertTrue(TermuxPropertyConstants.TERMUX_DEFAULT_INVERETED_FALSE_BOOLEAN_BEHAVIOUR_PROPERTIES_LIST.isEmpty())
    }

    @Test
    fun `default extra keys embed expected rows`() {
        assertTrue(TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS.contains("KEYBOARD"))
        // The escaped $ must survive as a literal dollar in the value.
        assertTrue(TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS2.contains("'\$'"))
    }
}
