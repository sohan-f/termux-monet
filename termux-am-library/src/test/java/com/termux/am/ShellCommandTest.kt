package com.termux.am

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class ShellCommandTest {

    private fun cmd(vararg args: String): ShellCommand =
        ShellCommand().apply { init(args as Array<String>, 0) }

    @Test
    fun `options and args are consumed in order`() {
        val c = cmd("-a", "ACTION", "-d", "http://x")
        assertEquals("-a", c.getNextOption())
        assertEquals("ACTION", c.getNextArg())
        assertEquals("-d", c.getNextOption())
        assertEquals("http://x", c.getNextArg())
        assertNull(c.getNextOption())
        assertNull(c.getNextArg())
    }

    @Test
    fun `combined short option splits value`() {
        val c = cmd("-R2")
        assertEquals("-R", c.getNextOption())
        assertEquals("2", c.getNextArg())
    }

    @Test
    fun `non-option returns null without consuming`() {
        val c = cmd("pos", "-a")
        assertNull(c.getNextOption())
        assertEquals("pos", c.getNextArg())
        assertEquals("-a", c.getNextOption())
    }

    @Test
    fun `-- separator ends options`() {
        val c = cmd("--", "-a")
        assertNull(c.getNextOption())
        assertEquals("-a", c.getNextArg())
    }

    @Test
    fun `peek does not consume`() {
        val c = cmd("-a", "x")
        assertEquals("-a", c.peekNextArg())
        assertEquals("-a", c.getNextOption())
        assertEquals("x", c.peekNextArg())
        assertEquals("x", c.getNextArg())
    }

    @Test
    fun `required arg throws naming previous`() {
        val c = cmd("-R")
        assertEquals("-R", c.getNextOption())
        try {
            c.getNextArgRequired()
            fail("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Argument expected after \"-R\"", e.message)
        }
    }

    @Test
    fun `option after combined value throws`() {
        val c = cmd("-R2", "-a")
        assertEquals("-R", c.getNextOption())
        try {
            c.getNextOption()
            fail("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("No argument expected after \"-R2\"", e.message)
        }
    }
}
