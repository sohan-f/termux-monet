package com.termux.shared.activity

import com.termux.shared.errors.Errno
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Plain-JVM tests for the activity errno registry.
 */
class ActivityErrnoTest {

    @Test
    fun `start activity failure keeps code 100`() {
        assertSame(ActivityErrno.ERRNO_START_ACTIVITY_FAILED_WITH_EXCEPTION, Errno.valueOf("Activity Error", 100))
    }

    @Test
    fun `start for result failure keeps code 101`() {
        assertSame(ActivityErrno.ERRNO_START_ACTIVITY_FOR_RESULT_FAILED_WITH_EXCEPTION, Errno.valueOf("Activity Error", 101))
    }

    @Test
    fun `null context keeps code 102`() {
        assertSame(ActivityErrno.ERRNO_STARTING_ACTIVITY_WITH_NULL_CONTEXT, Errno.valueOf("Activity Error", 102))
    }

    @Test
    fun `null context error formats activity name`() {
        val error = ActivityErrno.ERRNO_STARTING_ACTIVITY_WITH_NULL_CONTEXT.getError("com.example.Foo")
        assertEquals("Cannot start \"com.example.Foo\" activity with null Context", error.getMessage())
    }
}
