package com.termux.shared.net.socket.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plain-JVM tests for [PeerCred]. The fields are set by native code via JNI
 * field lookup and by [PeerCred.fillPeerCred] (which needs a Context and is
 * not covered here); these tests pin the pure formatting logic and the
 * JNI-visible field defaults.
 */
class PeerCredTest {

    @Test
    fun `fields default to unset`() {
        // The constructor is module-internal like the old package-private
        // one; tests run in the same module.
        val cred = PeerCred()
        assertEquals(-1, cred.pid)
        assertEquals(-1, cred.uid)
        assertEquals(-1, cred.gid)
        assertNull(cred.pname)
        assertNull(cred.uname)
        assertNull(cred.gname)
        assertNull(cred.cmdline)
    }

    @Test
    fun `process string uses name when set`() {
        val cred = newCred(pid = 5, pname = "sh")
        assertEquals("5 (sh)", cred.getProcessString())
    }

    @Test
    fun `process string falls back to pid`() {
        val cred = newCred(pid = -1)
        assertEquals("-1", cred.getProcessString())
    }

    @Test
    fun `user and group strings use names when set`() {
        val cred = newCred(uid = 100, uname = "u0", gid = 200, gname = "g0")
        assertEquals("100 (u0)", cred.getUserString())
        assertEquals("200 (g0)", cred.getGroupString())
    }

    @Test
    fun `minimal string combines all three`() {
        val cred = newCred(pid = 5, pname = "sh", uid = 100, uname = "u0", gid = 200, gname = "g0")
        assertEquals("process=5 (sh), user=100 (u0), group=200 (g0)", cred.getMinimalString())
    }

    @Test
    fun `log string contains entries`() {
        val cred = newCred(pid = 5, pname = "sh", uid = 100, uname = "u0", gid = 200, gname = "g0", cmdline = "a b")
        val log = cred.getLogString()
        assertTrue(log.contains("5 (sh)"))
        assertTrue(log.contains("100 (u0)"))
        assertTrue(log.contains("200 (g0)"))
        assertTrue(log.contains("a b"))
    }

    @Test
    fun `static helpers handle null`() {
        assertEquals("null", PeerCred.getPeerCredLogString(null))
        assertEquals("null", PeerCred.getPeerCredMarkdownString(null))
    }

    private fun newCred(
        pid: Int = -1,
        pname: String? = null,
        uid: Int = -1,
        uname: String? = null,
        gid: Int = -1,
        gname: String? = null,
        cmdline: String? = null
    ): PeerCred {
        val cred = PeerCred()
        cred.pid = pid
        cred.pname = pname
        cred.uid = uid
        cred.uname = uname
        cred.gid = gid
        cred.gname = gname
        cred.cmdline = cmdline
        return cred
    }
}
