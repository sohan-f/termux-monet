package com.termux.shared.termux.extrakeys

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Plain-JVM tests for the static helpers of [ExtraKeysView]. The view
 * itself (including the long-press-delay fix) needs Android and is
 * covered on-device instead.
 */
class ExtraKeysViewTest {

    @Test
    fun `maximum length finds widest row`() {
        assertEquals(3, ExtraKeysView.maximumLength(arrayOf(arrayOf(1), arrayOf(1, 2, 3), arrayOf(1, 2))))
        assertEquals(0, ExtraKeysView.maximumLength(arrayOf()))
    }
}
