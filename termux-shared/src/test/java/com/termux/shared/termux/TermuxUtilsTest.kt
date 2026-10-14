package com.termux.shared.termux

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Plain-JVM tests for the pure helpers of [TermuxUtils]. Everything needing
 * a Context is covered on-device instead.
 */
class TermuxUtilsTest {

    @Test
    fun `apk release resolves known digests case-insensitively`() {
        assertEquals(
            TermuxConstants.APK_RELEASE_FDROID,
            TermuxUtils.getAPKRelease(TermuxConstants.APK_RELEASE_FDROID_SIGNING_CERTIFICATE_SHA256_DIGEST.lowercase())
        )
        assertEquals(
            TermuxConstants.APK_RELEASE_GITHUB,
            TermuxUtils.getAPKRelease(TermuxConstants.APK_RELEASE_GITHUB_SIGNING_CERTIFICATE_SHA256_DIGEST)
        )
        assertEquals(
            TermuxConstants.APK_RELEASE_GOOGLE_PLAYSTORE,
            TermuxUtils.getAPKRelease(TermuxConstants.APK_RELEASE_GOOGLE_PLAYSTORE_SIGNING_CERTIFICATE_SHA256_DIGEST)
        )
        assertEquals(
            TermuxConstants.APK_RELEASE_TERMUX_DEVS,
            TermuxUtils.getAPKRelease(TermuxConstants.APK_RELEASE_TERMUX_DEVS_SIGNING_CERTIFICATE_SHA256_DIGEST)
        )
    }

    @Test
    fun `apk release falls back for unknown and null`() {
        assertEquals("Unknown", TermuxUtils.getAPKRelease("00:11:22"))
        assertEquals("null", TermuxUtils.getAPKRelease(null))
    }
}
