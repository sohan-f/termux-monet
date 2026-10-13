package com.termux.shared.termux.shell

import com.termux.shared.termux.TermuxConstants
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Plain-JVM tests for [TermuxShellUtils.setupShellCommandArguments]
 * (pure java.io) and the [TermuxShellManager] static counters.
 */
class TermuxShellTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun writeFile(name: String, bytes: ByteArray): String {
        val file = tempFolder.newFile(name)
        file.writeBytes(bytes)
        return file.absolutePath
    }

    @Test
    fun `elf file executes directly`() {
        val path = writeFile("elfbin", byteArrayOf(0x7F, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte(), 0, 1))
        assertArrayEquals(arrayOf(path), TermuxShellUtils.setupShellCommandArguments(path, null))
    }

    @Test
    fun `plain script falls back to prefix sh`() {
        val path = writeFile("script.sh", "echo hello world this is long".toByteArray())
        assertArrayEquals(
            arrayOf(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/sh", path),
            TermuxShellUtils.setupShellCommandArguments(path, null)
        )
    }

    @Test
    fun `bin shebang is rewritten to prefix`() {
        val path = writeFile("Binscript", "#!/bin/sh\necho hi and more\n".toByteArray())
        val args = TermuxShellUtils.setupShellCommandArguments(path, arrayOf("a1"))
        assertEquals(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/sh", args[0])
        assertEquals(path, args[1])
        assertEquals("a1", args[2])
    }

    @Test
    fun `unreadable file executes directly with args kept`() {
        val path = tempFolder.root.absolutePath + "/does-not-exist"
        assertArrayEquals(
            arrayOf(path, "x"),
            TermuxShellUtils.setupShellCommandArguments(path, arrayOf("x"))
        )
    }

    @Test
    fun `shell id strictly increases`() {
        val first = TermuxShellManager.getNextShellId()
        assertTrue(TermuxShellManager.getNextShellId() > first)
    }

    @Test
    fun `app start counters increment monotonically`() {
        val shell1 = TermuxShellManager.getAndIncrementAppShellNumberSinceAppStart()
        assertEquals(shell1 + 1, TermuxShellManager.getAndIncrementAppShellNumberSinceAppStart())
        val sess1 = TermuxShellManager.getAndIncrementTerminalSessionNumberSinceAppStart()
        assertEquals(sess1 + 1, TermuxShellManager.getAndIncrementTerminalSessionNumberSinceAppStart())
    }
}
