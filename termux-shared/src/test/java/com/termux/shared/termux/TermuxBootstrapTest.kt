package com.termux.shared.termux

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plain-JVM tests for the [TermuxBootstrap] package manager/variant enums.
 */
class TermuxBootstrapTest {

    @Test
    fun `manager lookup resolves apt only`() {
        assertEquals(TermuxBootstrap.PackageManager.APT, TermuxBootstrap.PackageManager.managerOf("apt"))
        assertNull(TermuxBootstrap.PackageManager.managerOf("tapm"))
        assertNull(TermuxBootstrap.PackageManager.managerOf(""))
        assertNull(TermuxBootstrap.PackageManager.managerOf(null))
    }

    @Test
    fun `variant lookup resolves both variants`() {
        assertEquals(TermuxBootstrap.PackageVariant.APT_ANDROID_7, TermuxBootstrap.PackageVariant.variantOf("apt-android-7"))
        assertEquals(TermuxBootstrap.PackageVariant.APT_ANDROID_5, TermuxBootstrap.PackageVariant.variantOf("apt-android-5"))
        assertNull(TermuxBootstrap.PackageVariant.variantOf("tapm-android-7"))
        assertNull(TermuxBootstrap.PackageVariant.variantOf(null))
    }

    @Test
    fun `names and equality matchers agree`() {
        assertEquals("apt", TermuxBootstrap.PackageManager.APT.getName())
        assertEquals("apt-android-7", TermuxBootstrap.PackageVariant.APT_ANDROID_7.getName())
        assertTrue(TermuxBootstrap.PackageManager.APT.equalsManager("apt"))
        assertFalse(TermuxBootstrap.PackageManager.APT.equalsManager("tapm"))
        assertFalse(TermuxBootstrap.PackageManager.APT.equalsManager(null))
        assertTrue(TermuxBootstrap.PackageVariant.APT_ANDROID_5.equalsVariant("apt-android-5"))
        assertFalse(TermuxBootstrap.PackageVariant.APT_ANDROID_5.equalsVariant(null))
    }
}
