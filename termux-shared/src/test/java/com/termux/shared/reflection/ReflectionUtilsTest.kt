package com.termux.shared.reflection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReflectionUtilsTest {

    class Fixture {
        @Suppress("unused")
        private val secret = "shh"

        @JvmField
        var count = 0

        @Suppress("unused")
        private fun add(a: Int, b: Int): Int = a + b

        @Suppress("unused")
        private fun bump() {
            count++
        }
    }

    @Test
    fun `hidden api bypass flag starts bypassed on old sdks`() {
        // Unit-test SDK_INT is 0 (< P), so no bypass attempt or mutation happens.
        assertTrue(ReflectionUtils.areHiddenAPIReflectionRestrictionsBypassed())
        ReflectionUtils.bypassHiddenAPIReflectionRestrictions()
        assertTrue(ReflectionUtils.areHiddenAPIReflectionRestrictionsBypassed())
    }

    @Test
    fun `result constructors map fields correctly`() {
        // Regression test: the Java constructors assigned success to value.
        val fieldResult = ReflectionUtils.FieldInvokeResult(true, "x")
        assertTrue(fieldResult.success)
        assertEquals("x", fieldResult.value)
        val methodResult = ReflectionUtils.MethodInvokeResult(false, null)
        assertFalse(methodResult.success)
        assertNull(methodResult.value)
    }

    @Test
    fun `field lookup and invoke`() {
        assertNotNull(ReflectionUtils.getDeclaredField(Fixture::class.java, "secret"))
        assertNull(ReflectionUtils.getDeclaredField(Fixture::class.java, "missing"))
        val ok = ReflectionUtils.invokeField(Fixture::class.java, "secret", Fixture())
        assertTrue(ok.success)
        assertEquals("shh", ok.value)
        val missing = ReflectionUtils.invokeField(Fixture::class.java, "missing", Fixture())
        assertFalse(missing.success)
    }

    @Test
    fun `method lookup invoke and void invoke`() {
        val add = ReflectionUtils.getDeclaredMethod(Fixture::class.java, "add", Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!)
        assertNotNull(add)
        assertNull(ReflectionUtils.getDeclaredMethod(Fixture::class.java, "missing"))
        val sum = ReflectionUtils.invokeMethod(add!!, Fixture(), 2, 3)
        assertTrue(sum.success)
        assertEquals(5, sum.value)
        val fixture = Fixture()
        assertTrue(ReflectionUtils.invokeVoidMethod(ReflectionUtils.getDeclaredMethod(Fixture::class.java, "bump")!!, fixture))
        assertEquals(1, fixture.count)
        assertFalse(ReflectionUtils.invokeVoidMethod(add, Fixture(), "wrong"))
    }

    @Test
    fun `constructor lookup and invoke`() {
        val ctor = ReflectionUtils.getConstructor(Fixture::class.java)
        assertNotNull(ctor)
        assertNull(ReflectionUtils.getConstructor("no.such.Class"))
        val instance = ReflectionUtils.invokeConstructor(ctor!!)
        assertTrue(instance is Fixture)
        assertNull(ReflectionUtils.invokeConstructor(ctor, "wrong"))
    }
}
