package com.termux.shared.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidUtilsTest {

    @Test
    fun `system property with android api`() {
        assertEquals(System.getProperty("os.version"), AndroidUtils.getSystemPropertyWithAndroidAPI("os.version"))
        assertNull(AndroidUtils.getSystemPropertyWithAndroidAPI("no.such.prop.xyz"))
    }

    @Test
    fun `property markdown entries`() {
        assertEquals("**K**: `v`  ", AndroidUtils.getPropertyMarkdown("K", "v"))
        assertEquals("**K**: -  ", AndroidUtils.getPropertyMarkdown("K", null))
        assertEquals("**K**: v  ", AndroidUtils.getLiteralPropertyMarkdown("K", "v"))
    }

    @Test
    fun `append if set skips null empty and rel`() {
        val sb = StringBuilder()
        AndroidUtils.appendPropertyToMarkdownIfSet(sb, "K", null)
        AndroidUtils.appendPropertyToMarkdownIfSet(sb, "K", "")
        AndroidUtils.appendPropertyToMarkdownIfSet(sb, "K", "REL")
        assertEquals("", sb.toString())
        AndroidUtils.appendPropertyToMarkdownIfSet(sb, "K", "v")
        assertEquals("\n**K**: `v`  ", sb.toString())
    }

    @Test
    fun `append always appends`() {
        val sb = StringBuilder()
        AndroidUtils.appendPropertyToMarkdown(sb, "K", null)
        assertEquals("\n**K**: -  ", sb.toString())
    }

    @Test
    fun `timestamps are formatted`() {
        assertTrue(AndroidUtils.getCurrentTimeStamp().matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} \\S+")))
        assertTrue(AndroidUtils.getCurrentMilliSecondUTCTimeStamp().matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\S+")))
        assertTrue(AndroidUtils.getCurrentMilliSecondLocalTimeStamp().matches(Regex("\\d{4}-\\d{2}-\\d{2}_\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{3}")))
    }
}
