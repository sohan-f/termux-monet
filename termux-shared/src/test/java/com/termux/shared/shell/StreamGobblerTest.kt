package com.termux.shared.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.ArrayList

class StreamGobblerTest {

    @Test
    fun `gobbles lines into string builder`() {
        val builder = StringBuilder()
        val gobbler = StreamGobbler("test", ByteArrayInputStream("a\nb\n".toByteArray()), builder, null)
        gobbler.start()
        gobbler.join(5000)
        assertEquals("a\nb\n", builder.toString())
    }

    @Test
    fun `gobbles lines into list`() {
        val list = ArrayList<String>()
        val gobbler = StreamGobbler("test", ByteArrayInputStream("a\nb\n".toByteArray()), list, null)
        gobbler.start()
        gobbler.join(5000)
        assertEquals(listOf("a", "b"), list)
    }

    @Test
    fun `line listener and closed listener fire`() {
        val seen = ArrayList<String>()
        var closed = false
        val gobbler = StreamGobbler("test", ByteArrayInputStream("x\n".toByteArray()),
            StreamGobbler.OnLineListener { seen.add(it) },
            StreamGobbler.OnStreamClosedListener { closed = true }, null)
        gobbler.start()
        gobbler.join(5000)
        assertEquals(listOf("x"), seen)
        assertTrue(closed)
    }

    @Test
    fun `suspend and resume bracket reading`() {
        val builder = StringBuilder()
        val gobbler = StreamGobbler("test", ByteArrayInputStream("a\n".toByteArray()), builder, null)
        gobbler.suspendGobbling()
        assertTrue(gobbler.isSuspended())
        gobbler.start()
        // Give the thread a chance to block in the paused state, then resume.
        Thread.sleep(200)
        gobbler.resumeGobbling()
        gobbler.join(5000)
        assertEquals("a\n", builder.toString())
    }
}
