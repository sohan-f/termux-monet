package com.termux.shared.view

import android.graphics.Point
import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewUtilsTest {

    // Note: android.jar stub constructors do not assign fields, so rects and
    // points are built with no-arg constructors plus direct field writes
    // (which is all the code under test touches).
    private fun rect(l: Int, t: Int, r: Int, b: Int) = Rect().apply {
        left = l
        top = t
        right = r
        bottom = b
    }

    @Test
    fun `rect above checks containment from above`() {
        val outer = rect(0, 0, 10, 10)
        val inside = rect(2, 2, 8, 8)
        assertTrue(ViewUtils.isRectAbove(outer, inside))
        // Empty base never contains.
        assertFalse(ViewUtils.isRectAbove(rect(0, 0, 0, 0), inside))
        // Extending below fails.
        assertFalse(ViewUtils.isRectAbove(outer, rect(2, 2, 8, 12)))
        // Extending left fails.
        assertFalse(ViewUtils.isRectAbove(outer, rect(-1, 2, 8, 8)))
    }

    @Test
    fun `rect and point strings`() {
        assertEquals("null", ViewUtils.toRectString(null))
        assertEquals("(0,0), (10,10)", ViewUtils.toRectString(rect(0, 0, 10, 10)))
        assertEquals("null", ViewUtils.toPointString(null))
        val point = Point().apply { x = 3; y = 4 }
        assertEquals("(3,4)", ViewUtils.toPointString(point))
    }
}
