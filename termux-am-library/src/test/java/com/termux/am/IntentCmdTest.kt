package com.termux.am

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntentCmdTest {

    private fun parse(vararg args: String) =
        IntentCmd.parseCommandArgs(ShellCommand().apply { init(args as Array<String>, 0) }, null)

    @Test
    @Suppress("DEPRECATION")
    fun `int extra is stored as Serializable like javac resolves it`() {
        val intent = parse("--ei", "k", "5", "com.example")
        assertEquals(5, intent.getIntExtra("k", -1))
        assertTrue(intent.extras!!.getSerializable("k") is java.io.Serializable)
    }

    @Test
    fun `null string extra is stored under String overload`() {
        val intent = parse("--esn", "k", "com.example")
        assertTrue(intent.hasExtra("k"))
        assertNull(intent.getStringExtra("k"))
    }

    @Test
    fun `boolean extra accepts true t false f and numbers`() {
        for (v in listOf("true", "TRUE", "t"))
            assertEquals("expected true for $v", true, parse("--ez", "k", v, "com.example").getBooleanExtra("k", false))
        for (v in listOf("false", "f", "0"))
            assertEquals("expected false for $v", false, parse("--ez", "k", v, "com.example").getBooleanExtra("k", true))
        assertEquals(true, parse("--ez", "k", "7", "com.example").getBooleanExtra("k", false))
    }

    @Test
    fun `invalid boolean extra throws`() {
        try {
            parse("--ez", "k", "yes", "com.example")
            fail("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Invalid boolean value: yes", e.message)
        }
    }

    @Test
    fun `int array extra is parsed`() {
        val intent = parse("--eia", "k", "1,2,3", "com.example")
        assertArrayEquals(intArrayOf(1, 2, 3), intent.getIntArrayExtra("k"))
    }

    @Test
    fun `escaped commas survive in string array extra`() {
        val intent = parse("--esa", "k", "a\\,b,c", "com.example")
        assertArrayEquals(arrayOf("a,b", "c"), intent.getStringArrayExtra("k"))
    }

    @Test
    fun `unknown option throws`() {
        try {
            parse("--bogus", "com.example")
            fail("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Unknown option: --bogus", e.message)
        }
    }

    @Test
    fun `no intent supplied throws`() {
        try {
            parse()
            fail("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("No intent supplied", e.message)
        }
    }

    @Test
    fun `bad component name throws`() {
        try {
            parse("-n", "not a component!", "com.example")
            fail("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.startsWith("Bad component name:"))
        }
    }

    @Test
    fun `selector intent is attached via referential identity`() {
        val intent = parse("--selector", "-a", "FOO", "com.example")
        assertEquals("FOO", intent.selector!!.action)
        assertEquals("com.example", intent.getPackage())
    }
}
