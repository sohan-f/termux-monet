package com.termux.shared.termux.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plain-JVM tests for [TermuxUrlUtils] (java.util.regex is fully usable here).
 */
class TermuxUrlUtilsTest {

    @Test
    fun `regex is lazily created once`() {
        assertSame(TermuxUrlUtils.getUrlMatchRegex(), TermuxUrlUtils.getUrlMatchRegex())
        assertNotNull(TermuxUrlUtils.URL_MATCH_REGEX)
    }

    @Test
    fun `extract urls finds http and file urls`() {
        val found = TermuxUrlUtils.extractUrls("see https://termux.com/x and file:///sdcard/a.txt ok")
        assertTrue(found.contains("https://termux.com/x"))
        assertTrue(found.contains("file:///sdcard/a.txt"))
    }

    @Test
    fun `extract urls ignores plain text`() {
        assertTrue(TermuxUrlUtils.extractUrls("no urls here").isEmpty())
    }
}
