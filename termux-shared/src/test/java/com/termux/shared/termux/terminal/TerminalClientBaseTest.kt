package com.termux.shared.termux.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plain-JVM tests pinning the no-op defaults of the terminal client bases.
 */
class TerminalClientBaseTest {

    @Test
    fun `session client cursor style defaults to null`() {
        assertNull(TermuxTerminalSessionClientBase().getTerminalCursorStyle())
    }

    @Test
    fun `view client defaults match java no-ops`() {
        val client = TermuxTerminalViewClientBase()
        assertEquals(1.0f, client.onScale(2.0f))
        assertTrue(client.isTerminalViewSelected())
        assertFalse(client.shouldBackButtonBeMappedToEscape())
        assertFalse(client.shouldEnforceCharBasedInput())
        assertFalse(client.shouldUseCtrlSpaceWorkaround())
        assertFalse(client.onKeyDown(0, null, null))
        assertFalse(client.onKeyUp(0, null))
        assertFalse(client.onLongPress(null))
        assertFalse(client.readControlKey())
        assertFalse(client.readAltKey())
        assertFalse(client.readShiftKey())
        assertFalse(client.readFnKey())
        assertFalse(client.onCodePoint(0, false, null))
    }
}
