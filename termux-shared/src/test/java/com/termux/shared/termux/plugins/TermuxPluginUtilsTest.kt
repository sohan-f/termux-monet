package com.termux.shared.termux.plugins

import com.termux.shared.shell.command.ExecutionCommand
import com.termux.shared.termux.TermuxConstants
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Plain-JVM tests for the pure wiring helpers of [TermuxPluginUtils].
 */
class TermuxPluginUtilsTest {

    @Test
    fun `pending intent variables use termux service extras`() {
        val command = ExecutionCommand()
        TermuxPluginUtils.setPluginResultPendingIntentVariables(command)
        val config = command.resultConfig
        assertEquals(TermuxConstants.TERMUX_APP.TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE, config.resultBundleKey)
        assertEquals(TermuxConstants.TERMUX_APP.TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT, config.resultStdoutKey)
        assertEquals(TermuxConstants.TERMUX_APP.TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE, config.resultExitCodeKey)
        assertEquals(TermuxConstants.TERMUX_APP.TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_ERRMSG, config.resultErrmsgKey)
    }

    @Test
    fun `directory variables canonicalize and default basename`() {
        val command = ExecutionCommand()
        // Under the storage-home branch so no Android APIs are touched
        // (getExternalStorageDirectory() is null on plain JVM).
        val storageHome = com.termux.shared.termux.TermuxConstants.TERMUX_STORAGE_HOME_DIR_PATH
        command.resultConfig.resultDirectoryPath = "$storageHome/./plugindir"
        command.resultConfig.resultSingleFile = true
        command.resultConfig.resultFileBasename = null
        command.executable = "/bin/echo"
        TermuxPluginUtils.setPluginResultDirectoryVariables(command)
        // Canonical path resolves the /./ segment (java.io.File is pure JVM).
        assertEquals("$storageHome/plugindir", command.resultConfig.resultDirectoryPath)
        assertEquals(storageHome, command.resultConfig.resultDirectoryAllowedParentPath)
        val basename = command.resultConfig.resultFileBasename!!
        assertEquals("echo", basename.substringBefore("-"))
        assertEquals(".log", basename.substring(basename.length - 4))
    }
}
