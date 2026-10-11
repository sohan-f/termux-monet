package com.termux.shared.shell.am

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmSocketServerTest {

    @Test
    fun `null and empty commands parse to nothing without error`() {
        val empty = ArrayList<String>()
        assertNull(AmSocketServer.parseAmCommand(null, empty))
        assertTrue(empty.isEmpty())

        val alsoEmpty = ArrayList<String>()
        assertNull(AmSocketServer.parseAmCommand("", alsoEmpty))
        assertTrue(alsoEmpty.isEmpty())
    }

    @Test
    fun `plain command is split on whitespace`() {
        val list = ArrayList<String>()
        assertNull(AmSocketServer.parseAmCommand("start -a android.intent.action.VIEW", list))
        assertEquals(listOf("start", "-a", "android.intent.action.VIEW"), list)
    }

    @Test
    fun `quoted sections survive as single args`() {
        val list = ArrayList<String>()
        assertNull(AmSocketServer.parseAmCommand("broadcast -a \"a b\" --es k \"v v\"", list))
        assertEquals(listOf("broadcast", "-a", "a b", "--es", "k", "v v"), list)
    }

    @Test
    fun `unterminated quote is leniently accepted`() {
        // The tokenizer never rejects this; the rest becomes the arg.
        // Same behaviour as the Java version, documented here.
        val list = ArrayList<String>()
        assertNull(AmSocketServer.parseAmCommand("broadcast -a \"abc", list))
        assertEquals(listOf("broadcast", "-a", "abc"), list)
    }

    @Test
    fun `backslash at end of quoted section is an error`() {
        val list = ArrayList<String>()
        val error = AmSocketServer.parseAmCommand("broadcast -a \"abc\\", list)
        assertNotNull(error)
        assertTrue(error!!.isStateFailed())
    }
}
