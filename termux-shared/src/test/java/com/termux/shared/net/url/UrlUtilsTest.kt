package com.termux.shared.net.url

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plain-JVM tests for [UrlUtils] (java.net is fully usable on the JVM).
 */
class UrlUtilsTest {

    @Test
    fun `join url resolves relative destinations`() {
        assertEquals("https://a/b/c", UrlUtils.joinUrl("https://a/b/", "c", true))
        assertNull(UrlUtils.joinUrl(null, "c", false))
        assertNull(UrlUtils.joinUrl("::not a url::", "c", false))
    }

    @Test
    fun `get url rejects malformed input`() {
        assertEquals("https", UrlUtils.getUrl("https://a/b")!!.protocol)
        assertNull(UrlUtils.getUrl("::not a url::"))
        assertNull(UrlUtils.getUrl(null))
    }

    @Test
    fun `get url part covers every branch`() {
        val url = "https://user:pass@host.com:8080/p?q=1#frag"
        assertEquals("user:pass@host.com:8080", UrlUtils.getUrlPart(url, UrlUtils.UrlPart.AUTHORITY))
        assertEquals("/p?q=1", UrlUtils.getUrlPart(url, UrlUtils.UrlPart.FILE))
        assertEquals("host.com", UrlUtils.getUrlPart(url, UrlUtils.UrlPart.HOST))
        assertEquals("frag", UrlUtils.getUrlPart(url, UrlUtils.UrlPart.REF))
        assertEquals("frag", UrlUtils.getUrlPart(url, UrlUtils.UrlPart.FRAGMENT))
        assertEquals("/p", UrlUtils.getUrlPart(url, UrlUtils.UrlPart.PATH))
        assertEquals("8080", UrlUtils.getUrlPart(url, UrlUtils.UrlPart.PORT))
        assertEquals("-1", UrlUtils.getUrlPart("https://host.com/p", UrlUtils.UrlPart.PORT))
        assertEquals("https", UrlUtils.getUrlPart(url, UrlUtils.UrlPart.PROTOCOL))
        assertEquals("q=1", UrlUtils.getUrlPart(url, UrlUtils.UrlPart.QUERY))
        assertEquals("user:pass", UrlUtils.getUrlPart(url, UrlUtils.UrlPart.USER_INFO))
        assertNull(UrlUtils.getUrlPart("::not a url::", UrlUtils.UrlPart.HOST))
        assertNull(UrlUtils.getUrlPart(null, UrlUtils.UrlPart.HOST))
    }

    @Test
    fun `remove protocol strips known prefixes`() {
        assertEquals("a.com", UrlUtils.removeProtocol("https://www.a.com"))
        assertEquals("a.com", UrlUtils.removeProtocol("http://a.com"))
        assertEquals("a.com", UrlUtils.removeProtocol("www.a.com"))
        assertEquals("a.com", UrlUtils.removeProtocol("a.com"))
        assertNull(UrlUtils.removeProtocol(null))
    }

    @Test
    fun `urls equal ignores protocol and trailing slashes`() {
        assertTrue(UrlUtils.areUrlsEqual("https://www.a.com/x/", "http://a.com/x"))
        assertTrue(UrlUtils.areUrlsEqual(null, null))
        assertFalse(UrlUtils.areUrlsEqual(null, "http://a.com"))
        assertFalse(UrlUtils.areUrlsEqual("http://a.com", null))
        assertFalse(UrlUtils.areUrlsEqual("http://a.com/x", "http://a.com/y"))
    }
}
