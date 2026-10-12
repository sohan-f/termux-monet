package com.termux.shared.logger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoggerTest {

    @Test
    fun `message and stack trace combinations`() {
        assertNull(Logger.getMessageAndStackTraceString(null, null))
        assertEquals("m", Logger.getMessageAndStackTraceString("m", null))
        val trace = Logger.getMessageAndStackTraceString(null, RuntimeException("x"))
        assertTrue(trace!!.contains("RuntimeException: x"))
        val both = Logger.getMessageAndStackTraceString("m", RuntimeException("x"))
        assertTrue(both!!.startsWith("m:\n"))
        assertTrue(both.contains("RuntimeException: x"))
    }

    @Test
    fun `message and stack traces combinations`() {
        assertNull(Logger.getMessageAndStackTracesString(null, null))
        assertNull(Logger.getMessageAndStackTracesString(null, emptyList()))
        assertEquals("m", Logger.getMessageAndStackTracesString("m", null))
        assertEquals("m", Logger.getMessageAndStackTracesString("m", emptyList()))
        val both = Logger.getMessageAndStackTracesString("m", listOf(RuntimeException("x")))
        assertTrue(both!!.startsWith("m:\n"))
        assertTrue(both.contains("RuntimeException: x"))
        val tracesOnly = Logger.getMessageAndStackTracesString(null, listOf(RuntimeException("x")))
        assertTrue(tracesOnly!!.contains("RuntimeException: x"))
    }

    @Test
    fun `stack trace string`() {
        assertNull(Logger.getStackTraceString(null))
        val trace = Logger.getStackTraceString(RuntimeException("boom"))
        assertTrue(trace!!.contains("java.lang.RuntimeException: boom"))
        assertTrue(trace.contains("at "))
    }

    @Test
    fun `stack traces array`() {
        assertNull(Logger.getStackTracesStringArray(null as List<Throwable>?))
        val array = Logger.getStackTracesStringArray(listOf(RuntimeException("a"), RuntimeException("b")))!!
        assertEquals(2, array.size)
        assertTrue(array[0]!!.contains("RuntimeException: a"))
        assertTrue(array[1]!!.contains("RuntimeException: b"))
    }

    @Test
    fun `stack traces log string`() {
        assertEquals("StackTraces: -", Logger.getStackTracesString(null, null))
        assertEquals("L -", Logger.getStackTracesString("L", emptyArray()))
        val two = Logger.getStackTracesString(null, arrayOf("t1", "t2"))
        assertTrue(two.contains("Stacktrace 2"))
        assertTrue(two.contains("```\nt1\n```"))
    }

    @Test
    fun `stack traces markdown string`() {
        assertEquals("### StackTraces\n\n`-`\n##\n", Logger.getStackTracesMarkdownString(null, null))
        val one = Logger.getStackTracesMarkdownString("L", arrayOf("t1"))
        assertTrue(one.startsWith("### L"))
        assertTrue(one.contains("```\nt1\n```"))
        assertTrue(one.endsWith("\n##\n"))
    }

    @Test
    fun `log string entries`() {
        assertEquals("L: `v`", Logger.getSingleLineLogStringEntry("L", "v", "-"))
        assertEquals("L: -", Logger.getSingleLineLogStringEntry("L", null, "-"))
        assertEquals("L:\n```\nv\n```\n", Logger.getMultiLineLogStringEntry("L", "v", "-"))
        assertEquals("L: -", Logger.getMultiLineLogStringEntry("L", null, "-"))
    }

    @Test
    fun `log levels array`() {
        assertEquals(listOf("0", "1", "2", "3"), Logger.getLogLevelsArray().toList())
    }

    @Test
    fun `full tag`() {
        assertEquals("Logger", Logger.getFullTag("Logger"))
        assertEquals("Logger.X", Logger.getFullTag("X"))
        assertEquals("Logger.null", Logger.getFullTag(null))
    }

    @Test
    fun `log level validity`() {
        assertFalse(Logger.isLogLevelValid(null))
        assertFalse(Logger.isLogLevelValid(-1))
        assertTrue(Logger.isLogLevelValid(0))
        assertTrue(Logger.isLogLevelValid(3))
        assertFalse(Logger.isLogLevelValid(4))
    }

    @Test
    fun `custom log level gating at default normal level`() {
        // CURRENT_LOG_LEVEL defaults to NORMAL (1); these do not mutate it.
        assertFalse(Logger.shouldEnableLoggingForCustomLogLevel(null))
        assertFalse(Logger.shouldEnableLoggingForCustomLogLevel(0))
        assertTrue(Logger.shouldEnableLoggingForCustomLogLevel(2))
        // Invalid levels fall back to VERBOSE, which passes.
        assertTrue(Logger.shouldEnableLoggingForCustomLogLevel(99))
    }
}
