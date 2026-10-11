package com.termux.am

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.ByteArrayOutputStream
import java.io.PrintStream

@RunWith(RobolectricTestRunner::class)
class AmTest {

    private fun runAm(vararg args: String): String {
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        val am = Am(PrintStream(out), PrintStream(err), RuntimeEnvironment.getApplication())
        am.run(args as Array<String>)
        return out.toString() + err.toString()
    }

    private fun countOccurrences(haystack: String, needle: String) =
        haystack.split(needle).size - 1

    @Test
    fun `repeat flag launches exactly count times`() {
        val out = runAm("start", "-R", "3", "-a", "android.intent.action.VIEW", "-d", "https://example.com")
        assertEquals(3, countOccurrences(out, "Starting: "))
    }

    @Test
    fun `no repeat flag launches once`() {
        val out = runAm("start", "-a", "android.intent.action.VIEW", "-d", "https://example.com")
        assertEquals(1, countOccurrences(out, "Starting: "))
    }

    @Test
    fun `unknown command reports error`() {
        val out = runAm("bogusop")
        assertTrue(out.contains("Error: unknown command 'bogusop'"))
    }
}
