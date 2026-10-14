package com.termux.shared.termux.settings.properties

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Properties

/**
 * Plain-JVM tests for the pure dispatch/conversion helpers of
 * [TermuxSharedProperties]. Instance paths need files/Android and are
 * covered on-device instead.
 */
class TermuxSharedPropertiesTest {

    @Test
    fun `bell behaviour maps known values and defaults`() {
        assertEquals(1, TermuxSharedProperties.getBellBehaviourInternalPropertyValueFromValue("vibrate"))
        assertEquals(2, TermuxSharedProperties.getBellBehaviourInternalPropertyValueFromValue("BEEP"))
        assertEquals(3, TermuxSharedProperties.getBellBehaviourInternalPropertyValueFromValue("ignore"))
        assertEquals(1, TermuxSharedProperties.getBellBehaviourInternalPropertyValueFromValue("bogus"))
        assertEquals(1, TermuxSharedProperties.getBellBehaviourInternalPropertyValueFromValue(null))
    }

    @Test
    fun `int ranges clamp to defaults`() {
        assertEquals(5, TermuxSharedProperties.getTerminalMarginHorizontalInternalPropertyValueFromValue("5"))
        assertEquals(0, TermuxSharedProperties.getTerminalMarginHorizontalInternalPropertyValueFromValue("101"))
        assertEquals(0, TermuxSharedProperties.getTerminalMarginHorizontalInternalPropertyValueFromValue(null))
        assertEquals(7, TermuxSharedProperties.getDeleteTMPDIRFilesOlderThanXDaysOnExitInternalPropertyValueFromValue("7"))
        assertEquals(3, TermuxSharedProperties.getDeleteTMPDIRFilesOlderThanXDaysOnExitInternalPropertyValueFromValue("100001"))
        assertEquals(2.5f, TermuxSharedProperties.getTerminalToolbarHeightScaleFactorInternalPropertyValueFromValue("2.5"))
    }

    @Test
    fun `cursor style maps and string helpers fall back`() {
        assertEquals(
            TermuxPropertyConstants.IVALUE_TERMINAL_CURSOR_STYLE_BAR,
            TermuxSharedProperties.getTerminalCursorStyleInternalPropertyValueFromValue("bar")
        )
        assertEquals("back", TermuxSharedProperties.getBackKeyBehaviourInternalPropertyValueFromValue("back"))
        assertEquals("back", TermuxSharedProperties.getBackKeyBehaviourInternalPropertyValueFromValue(null))
        assertEquals("system", TermuxSharedProperties.getNightModeInternalPropertyValueFromValue("system"))
        assertEquals("system", TermuxSharedProperties.getNightModeInternalPropertyValueFromValue("bogus"))
        assertEquals("show/hide", TermuxSharedProperties.getSoftKeyboardToggleBehaviourInternalPropertyValueFromValue(null))
        assertEquals("virtual", TermuxSharedProperties.getVolumeKeysBehaviourInternalPropertyValueFromValue(null))
    }

    @Test
    fun `session shortcut parsing accepts ctrl plus one or two chars`() {
        assertEquals(99, TermuxSharedProperties.getCodePointForSessionShortcuts("k", "ctrl+c"))
        // Note: the value is lowercased first, so 'A' yields 'a' (97).
        assertEquals(97, TermuxSharedProperties.getCodePointForSessionShortcuts("k", "CTRL+A"))
        assertNull(TermuxSharedProperties.getCodePointForSessionShortcuts(null, "ctrl+c"))
        assertNull(TermuxSharedProperties.getCodePointForSessionShortcuts("k", null))
        assertNull(TermuxSharedProperties.getCodePointForSessionShortcuts("k", "alt+c"))
        assertNull(TermuxSharedProperties.getCodePointForSessionShortcuts("k", "ctrl+abc"))
        assertNull(TermuxSharedProperties.getCodePointForSessionShortcuts("k", "ctrl"))
    }

    @Test
    fun `dispatch routes keys to converters`() {
        assertEquals(1, TermuxSharedProperties.getInternalTermuxPropertyValueFromValue(null, TermuxPropertyConstants.KEY_BELL_BEHAVIOUR, "vibrate"))
        assertEquals(true, TermuxSharedProperties.getInternalTermuxPropertyValueFromValue(null, TermuxPropertyConstants.KEY_HIDE_SOFT_KEYBOARD_ON_STARTUP, "true"))
        assertEquals(false, TermuxSharedProperties.getInternalTermuxPropertyValueFromValue(null, TermuxPropertyConstants.KEY_HIDE_SOFT_KEYBOARD_ON_STARTUP, null))
        assertEquals("direct", TermuxSharedProperties.getInternalTermuxPropertyValueFromValue(null, "unknown-key", "direct"))
        assertNull(TermuxSharedProperties.getInternalTermuxPropertyValueFromValue(null, "unknown-key", null))
        assertNull(TermuxSharedProperties.getInternalTermuxPropertyValueFromValue(null, null, "x"))
        assertEquals(99, TermuxSharedProperties.getInternalTermuxPropertyValueFromValue(null, TermuxPropertyConstants.KEY_SHORTCUT_CREATE_SESSION, "ctrl+c"))
    }

    @Test
    fun `black-ui migration maps to night mode once`() {
        val properties = Properties()
        properties["keep"] = "1"
        properties[TermuxPropertyConstants.KEY_USE_BLACK_UI] = "true"
        val migrated = TermuxSharedProperties.replaceUseBlackUIProperty(properties)
        assertNull(migrated.getProperty(TermuxPropertyConstants.KEY_USE_BLACK_UI))
        assertEquals("true", migrated.getProperty(TermuxPropertyConstants.KEY_NIGHT_MODE))
        assertEquals("1", migrated.getProperty("keep"))
        // Existing night mode wins over the migration.
        val properties2 = Properties()
        properties2[TermuxPropertyConstants.KEY_USE_BLACK_UI] = "true"
        properties2[TermuxPropertyConstants.KEY_NIGHT_MODE] = "false"
        assertEquals("false", TermuxSharedProperties.replaceUseBlackUIProperty(properties2).getProperty(TermuxPropertyConstants.KEY_NIGHT_MODE))
        // Invalid values migrate the removal but map to nothing.
        val properties3 = Properties()
        properties3[TermuxPropertyConstants.KEY_USE_BLACK_UI] = "bogus"
        val migrated3 = TermuxSharedProperties.replaceUseBlackUIProperty(properties3)
        assertNull(migrated3.getProperty(TermuxPropertyConstants.KEY_USE_BLACK_UI))
        assertNull(migrated3.getProperty(TermuxPropertyConstants.KEY_NIGHT_MODE))
    }

    @Test
    fun `working directory falls back for missing paths`() {
        val missing = TermuxSharedProperties.getDefaultWorkingDirectoryInternalPropertyValueFromValue("/definitely/not/here")
        assertEquals(TermuxPropertyConstants.DEFAULT_IVALUE_DEFAULT_WORKING_DIRECTORY, missing)
        assertEquals(TermuxPropertyConstants.DEFAULT_IVALUE_DEFAULT_WORKING_DIRECTORY, TermuxSharedProperties.getDefaultWorkingDirectoryInternalPropertyValueFromValue(null))
        val tmp = System.getProperty("java.io.tmpdir")
        assertEquals(tmp, TermuxSharedProperties.getDefaultWorkingDirectoryInternalPropertyValueFromValue(tmp))
        assertFalse(TermuxSharedProperties.getExtraKeysInternalPropertyValueFromValue(null).isNullOrEmpty())
    }
}
