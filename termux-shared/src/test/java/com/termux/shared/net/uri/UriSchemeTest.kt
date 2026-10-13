package com.termux.shared.net.uri

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Plain-JVM tests pinning the [UriScheme] constant values. The [UriUtils]
 * helpers need android.net.Uri and are covered on-device instead.
 */
class UriSchemeTest {

    @Test
    fun `scheme values match their specs`() {
        assertEquals("android.resource", UriScheme.SCHEME_ANDROID_RESOURCE)
        assertEquals("content", UriScheme.SCHEME_CONTENT)
        assertEquals("file", UriScheme.SCHEME_FILE)
        assertEquals("http", UriScheme.SCHEME_HTTP)
        assertEquals("https", UriScheme.SCHEME_HTTPS)
    }
}
