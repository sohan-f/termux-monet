package com.termux.shared.termux.extrakeys

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Plain-JVM tests for the [SpecialButton] registry.
 */
class SpecialButtonTest {

    @Test
    fun `registry resolves the four built-ins`() {
        assertSame(SpecialButton.CTRL, SpecialButton.valueOf("CTRL"))
        assertSame(SpecialButton.ALT, SpecialButton.valueOf("ALT"))
        assertSame(SpecialButton.SHIFT, SpecialButton.valueOf("SHIFT"))
        assertSame(SpecialButton.FN, SpecialButton.valueOf("FN"))
    }

    @Test
    fun `unknown keys resolve to null`() {
        assertNull(SpecialButton.valueOf("NOPE"))
        assertNull(SpecialButton.valueOf(null))
    }

    @Test
    fun `key and name round-trip`() {
        assertEquals("CTRL", SpecialButton.CTRL.getKey())
        assertEquals("CTRL", SpecialButton.CTRL.toString())
    }
}
