package com.termux.shared.shell

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShellUtilsTest {

    @Test
    fun `command args join executable and args`() {
        assertArrayEquals(arrayOf("/bin/sh", "-c", "x"), ShellUtils.setupShellCommandArguments("/bin/sh", arrayOf("-c", "x")))
    }

    @Test
    fun `command args tolerate null args`() {
        assertArrayEquals(arrayOf("/bin/sh"), ShellUtils.setupShellCommandArguments("/bin/sh", null))
    }

    @Test
    fun `executable basename strips directories`() {
        assertEquals("do-something.sh", ShellUtils.getExecutableBasename("/bin/do-something.sh"))
    }

    @Test
    fun `transcript of null session is null`() {
        assertNull(ShellUtils.getTerminalSessionTranscriptText(null, true, false))
    }
}
