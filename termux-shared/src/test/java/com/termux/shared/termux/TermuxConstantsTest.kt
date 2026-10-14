package com.termux.shared.termux

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Plain-JVM tests pinning [TermuxConstants] composed values. Descriptors
 * were diffed field-by-field against the Java baseline via javap.
 */
class TermuxConstantsTest {

    @Test
    fun `package names compose`() {
        assertEquals("com.termux", TermuxConstants.TERMUX_PACKAGE_NAME)
        assertEquals("com.termux.api", TermuxConstants.TERMUX_API_PACKAGE_NAME)
        assertEquals("com.termux.window", TermuxConstants.TERMUX_FLOAT_PACKAGE_NAME)
        assertEquals("com.termux.tasker", TermuxConstants.TERMUX_TASKER_PACKAGE_NAME)
        assertEquals("com.termux.gui", TermuxConstants.TERMUX_GUI_PACKAGE_NAME)
    }

    @Test
    fun `directory paths compose`() {
        assertEquals("/data/data/com.termux/files", TermuxConstants.TERMUX_FILES_DIR_PATH)
        assertEquals("/data/data/com.termux/files/usr", TermuxConstants.TERMUX_PREFIX_DIR_PATH)
        assertEquals("/data/data/com.termux/files/usr/bin", TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH)
        assertEquals("/data/data/com.termux/files/home", TermuxConstants.TERMUX_HOME_DIR_PATH)
        assertEquals(File("/data/data/com.termux/files/usr"), TermuxConstants.TERMUX_PREFIX_DIR)
    }

    @Test
    fun `plugin lists cover all seven plugins`() {
        val names = TermuxConstants.TERMUX_PLUGIN_APP_NAMES_LIST!!
        assertEquals(7, names.size)
        assertTrue(names.contains("Termux:API"))
        val packages = TermuxConstants.TERMUX_PLUGIN_APP_PACKAGE_NAMES_LIST!!
        assertEquals(7, packages.size)
        assertTrue(packages.contains("com.termux.api"))
    }

    @Test
    fun `notification ids and env roots match`() {
        assertEquals(1337, TermuxConstants.TERMUX_APP_NOTIFICATION_ID)
        assertEquals(1338, TermuxConstants.TERMUX_RUN_COMMAND_NOTIFICATION_ID)
        assertEquals("TERMUX", TermuxConstants.TERMUX_ENV_PREFIX_ROOT)
    }

    @Test
    fun `nested paths compose from outer constants`() {
        assertEquals(
            "/data/data/com.termux/files/apps/com.termux",
            TermuxConstants.TERMUX_APP.APPS_DIR_PATH
        )
        assertEquals(
            "/data/data/com.termux/files/apps/com.termux/termux-am/am.sock",
            TermuxConstants.TERMUX_APP.TERMUX_AM_SOCKET_FILE_PATH
        )
        assertEquals("com.termux.BuildConfig", TermuxConstants.TERMUX_APP.BUILD_CONFIG_CLASS_NAME)
    }
}
