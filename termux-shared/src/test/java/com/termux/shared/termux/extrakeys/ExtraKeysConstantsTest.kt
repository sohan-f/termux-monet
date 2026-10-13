package com.termux.shared.termux.extrakeys

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plain-JVM tests pinning the [ExtraKeysConstants] data tables.
 */
class ExtraKeysConstantsTest {

    @Test
    fun `android keycode constants are real, not stubs`() {
        assertEquals(62, KeyEvent.KEYCODE_SPACE)
        assertEquals(66, KeyEvent.KEYCODE_ENTER)
    }

    @Test
    fun `key code map covers core keys`() {
        val map = ExtraKeysConstants.PRIMARY_KEY_CODES_FOR_STRINGS
        assertEquals(KeyEvent.KEYCODE_SPACE, map["SPACE"])
        assertEquals(KeyEvent.KEYCODE_ESCAPE, map["ESC"])
        assertEquals(KeyEvent.KEYCODE_ENTER, map["ENTER"])
        assertEquals(KeyEvent.KEYCODE_DPAD_UP, map["UP"])
        assertEquals(KeyEvent.KEYCODE_F12, map["F12"])
        assertEquals(27, map.size)
    }

    @Test
    fun `aliases resolve`() {
        val aliases = ExtraKeysConstants.CONTROL_CHARS_ALIASES
        assertEquals("ESC", aliases.get("ESCAPE", "ESCAPE"))
        assertEquals("CTRL", aliases.get("CONTROL", "CONTROL"))
        assertEquals("PGUP", aliases.get("PAGE UP", "PAGE UP"))
        assertEquals("\\", aliases.get("BACKSLASH", "BACKSLASH"))
        assertEquals("NOPE", aliases.get("NOPE", "NOPE"))
    }

    @Test
    fun `display maps compose`() {
        assertEquals("←", ExtraKeysConstants.EXTRA_KEY_DISPLAY_MAPS.CLASSIC_ARROWS_DISPLAY["LEFT"])
        // FULL_ISO includes classic + well-known + less-known + nicer + unknown ISO sets.
        val full = ExtraKeysConstants.EXTRA_KEY_DISPLAY_MAPS.FULL_ISO_CHAR_DISPLAY
        assertEquals("←", full["LEFT"])
        assertEquals("↲", full["ENTER"])
        assertEquals("⇱", full["HOME"])
        assertEquals("―", full["-"])
        assertEquals("⎈", full["CTRL"])
        assertTrue(ExtraKeysConstants.PRIMARY_REPETITIVE_KEYS.containsAll(listOf("UP", "DOWN", "LEFT", "RIGHT")))
    }
}
