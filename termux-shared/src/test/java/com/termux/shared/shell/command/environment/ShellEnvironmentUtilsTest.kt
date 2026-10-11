package com.termux.shared.shell.command.environment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellEnvironmentUtilsTest {

    @Test
    fun `environ entries are name=value and invalid ones skipped`() {
        val map = HashMap<String, String>()
        map["B"] = "2"
        map["A"] = "1"
        map["1BAD"] = "x"
        assertEquals(setOf("A=1", "B=2"), ShellEnvironmentUtils.convertEnvironmentToEnviron(map).toSet())
    }

    @Test
    fun `dotenv escapes quotes backticks backslashes and dollars`() {
        val list = arrayListOf(
            ShellEnvironmentVariable("Q", "a\"b"),
            ShellEnvironmentVariable("B", "a\\b"),
            ShellEnvironmentVariable("D", "a\$b"),
            ShellEnvironmentVariable("T", "a`b"),
            ShellEnvironmentVariable("P", "plain"),
            ShellEnvironmentVariable("E", "a\"b", true)
        )
        val out = ShellEnvironmentUtils.convertEnvironmentToDotEnvFile(list)
        assertTrue(out.contains("export Q=\"a\\\"b\"\n"))
        assertTrue(out.contains("export B=\"a\\\\b\"\n"))
        assertTrue(out.contains("export D=\"a\\\$b\"\n"))
        assertTrue(out.contains("export T=\"a\\`b\"\n"))
        assertTrue(out.contains("export P=\"plain\"\n"))
        // Already escaped values pass through untouched.
        assertTrue(out.contains("export E=\"a\"b\"\n"))
    }

    @Test
    fun `dotenv output is sorted by name`() {
        val list = arrayListOf(
            ShellEnvironmentVariable("B", "2"),
            ShellEnvironmentVariable("A", "1")
        )
        val out = ShellEnvironmentUtils.convertEnvironmentToDotEnvFile(list)
        assertTrue(out.indexOf("export A=") < out.indexOf("export B="))
    }

    @Test
    fun `name validation rejects bad names`() {
        assertTrue(ShellEnvironmentUtils.isValidEnvironmentVariableName("ABC_123"))
        assertFalse(ShellEnvironmentUtils.isValidEnvironmentVariableName(null))
        assertFalse(ShellEnvironmentUtils.isValidEnvironmentVariableName(""))
        assertFalse(ShellEnvironmentUtils.isValidEnvironmentVariableName("1ABC"))
        assertFalse(ShellEnvironmentUtils.isValidEnvironmentVariableName("A B"))
        assertFalse(ShellEnvironmentUtils.isValidEnvironmentVariableName("A\u0000B"))
    }

    @Test
    fun `value validation rejects null and nul bytes`() {
        assertTrue(ShellEnvironmentUtils.isValidEnvironmentVariableValue("a b"))
        assertFalse(ShellEnvironmentUtils.isValidEnvironmentVariableValue(null))
        assertFalse(ShellEnvironmentUtils.isValidEnvironmentVariableValue("a\u0000b"))
    }

    @Test
    fun `map conversion defaults escaped to false`() {
        val map = hashMapOf("K" to "V")
        val list = ShellEnvironmentUtils.convertEnvironmentMapToEnvironmentVariableList(map)
        assertEquals(1, list.size)
        assertEquals("K", list[0].name)
        assertEquals("V", list[0].value)
        assertEquals(false, list[0].escaped)
    }

    @Test
    fun `variables compare by name`() {
        val a = ShellEnvironmentVariable("A", "1")
        val b = ShellEnvironmentVariable("B", "2")
        assertTrue(a.compareTo(b) < 0)
        assertTrue(b.compareTo(a) > 0)
        assertEquals(0, a.compareTo(ShellEnvironmentVariable("A", "9")))
    }
}
