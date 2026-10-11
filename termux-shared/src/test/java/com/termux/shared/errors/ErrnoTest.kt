package com.termux.shared.errors

import com.termux.shared.net.socket.local.LocalSocketErrno
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plain-JVM tests for the errno registry. In particular these pin the
 * duplicate-code fixes: every (type, code) pair must resolve to exactly one
 * entry, since [Errno.valueOf] backs error-code lookups.
 */
class ErrnoTest {

    @Test
    fun `invalid parameter keeps code 104`() {
        assertSame(FunctionErrno.ERRNO_INVALID_PARAMETER, Errno.valueOf("Function Error", 104))
    }

    @Test
    fun `not instance of moved to code 105`() {
        assertSame(FunctionErrno.ERRNO_PARAMETER_NOT_INSTANCE_OF, Errno.valueOf("Function Error", 105))
    }

    @Test
    fun `peer uid failed keeps code 158`() {
        assertSame(LocalSocketErrno.ERRNO_GET_CLIENT_SOCKET_PEER_UID_FAILED, Errno.valueOf("LocalSocket Error", 158))
    }

    @Test
    fun `peer uid invalid moved to code 159`() {
        assertSame(LocalSocketErrno.ERRNO_CLIENT_SOCKET_PEER_UID_INVALID, Errno.valueOf("LocalSocket Error", 159))
    }

    @Test
    fun `valueOf rejects nulls`() {
        assertNull(Errno.valueOf(null, 104))
        assertNull(Errno.valueOf("", 104))
        assertNull(Errno.valueOf("Function Error", null))
        assertNull(Errno.valueOf("No Such Type", 104))
    }

    @Test
    fun `getError formats message`() {
        val error = FunctionErrno.ERRNO_NULL_OR_EMPTY_PARAMETER.getError("label", "func")
        assertEquals("The label parameter passed to \"func\" is null or empty.", error.getMessage())
        assertEquals(Integer.valueOf(100), error.getCode())
    }

    @Test
    fun `getError with throwable attaches it`() {
        val throwable = RuntimeException("x")
        val error = FunctionErrno.ERRNO_NULL_OR_EMPTY_PARAMETER.getError(throwable, "label", "func")
        assertEquals(1, error.getThrowablesList().size)
        assertEquals("The label parameter passed to \"func\" is null or empty.", error.getMessage())
    }

    @Test
    fun `getError with bad format falls back to unformatted`() {
        // %3$s has no matching arg, so String.format throws and the raw
        // message plus args must be returned instead.
        val error = FunctionErrno.ERRNO_INVALID_PARAMETER.getError("label", "func")
        assertTrue(error.getMessage()!!.startsWith("The %1\$s parameter passed to"))
    }

    @Test
    fun `equalsErrorTypeAndCode matches type and code`() {
        val error = FunctionErrno.ERRNO_UNSET_PARAMETER.getError("a", "b")
        assertTrue(FunctionErrno.ERRNO_UNSET_PARAMETER.equalsErrorTypeAndCode(error))
        assertFalse(FunctionErrno.ERRNO_UNSET_PARAMETERS.equalsErrorTypeAndCode(error))
        assertFalse(FunctionErrno.ERRNO_UNSET_PARAMETER.equalsErrorTypeAndCode(null))
    }
}
