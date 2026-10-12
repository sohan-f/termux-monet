package com.termux.shared.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarkdownUtilsTest {

    @Test
    fun `code for string handles null empty and plain`() {
        assertNull(MarkdownUtils.getMarkdownCodeForString(null, false))
        assertEquals("", MarkdownUtils.getMarkdownCodeForString("", false))
        assertEquals("`hi`", MarkdownUtils.getMarkdownCodeForString("hi", false))
        assertEquals("```\nhi\n```", MarkdownUtils.getMarkdownCodeForString("hi", true))
    }

    @Test
    fun `code fences grow past contained backticks`() {
        assertEquals("``a`b``", MarkdownUtils.getMarkdownCodeForString("a`b", false))
        assertEquals("`` `x``", MarkdownUtils.getMarkdownCodeForString("`x", false))
        assertEquals("``x` ``", MarkdownUtils.getMarkdownCodeForString("x`", false))
        assertEquals("``````\n```\n``````", MarkdownUtils.getMarkdownCodeForString("```", true))
    }

    @Test
    fun `max consecutive backticks`() {
        assertEquals(0, MarkdownUtils.getMaxConsecutiveBackTicksCount(null))
        assertEquals(0, MarkdownUtils.getMaxConsecutiveBackTicksCount(""))
        assertEquals(0, MarkdownUtils.getMaxConsecutiveBackTicksCount("abc"))
        assertEquals(2, MarkdownUtils.getMaxConsecutiveBackTicksCount("a`b``c"))
    }

    @Test
    fun `single line entries`() {
        assertEquals("**L**: `v`  ", MarkdownUtils.getSingleLineMarkdownStringEntry("L", "v", "-"))
        assertEquals("**L**: -  ", MarkdownUtils.getSingleLineMarkdownStringEntry("L", null, "-"))
        assertEquals("**L**: v  ", MarkdownUtils.getLiteralSingleLineMarkdownStringEntry("L", "v", "-"))
    }

    @Test
    fun `multi line entries`() {
        assertEquals("**L**:\n```\nv\n```\n", MarkdownUtils.getMultiLineMarkdownStringEntry("L", "v", "-"))
        assertEquals("**L**: -\n", MarkdownUtils.getMultiLineMarkdownStringEntry("L", null, "-"))
    }

    @Test
    fun `link escapes brackets and parens`() {
        assertEquals("[a](b)", MarkdownUtils.getLinkMarkdownString("a", "b"))
        assertEquals("[a\\]b](c\\)d)", MarkdownUtils.getLinkMarkdownString("a]b", "c)d"))
        assertEquals("label", MarkdownUtils.getLinkMarkdownString("label", null))
    }
}
