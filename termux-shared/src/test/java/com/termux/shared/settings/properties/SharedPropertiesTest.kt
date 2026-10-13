package com.termux.shared.settings.properties

import com.google.common.collect.ImmutableBiMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.HashMap
import java.util.Properties

/**
 * Plain-JVM tests for the pure helpers of [SharedProperties]. Anything that
 * touches files or Android goes through the manual on-device tests instead.
 */
class SharedPropertiesTest {

    @Test
    fun `boolean maps resolve both cases case-insensitively`() {
        assertEquals(true, SharedProperties.getBooleanValueForStringValue("true"))
        assertEquals(true, SharedProperties.getBooleanValueForStringValue("TRUE"))
        assertEquals(false, SharedProperties.getBooleanValueForStringValue("false"))
        assertEquals(false, SharedProperties.getBooleanValueForStringValue("False"))
        assertNull(SharedProperties.getBooleanValueForStringValue("yes"))
        assertNull(SharedProperties.getBooleanValueForStringValue(null))
    }

    @Test
    fun `boolean with default falls back on invalid`() {
        assertTrue(SharedProperties.getBooleanValueForStringValue("k", "true", false, false, null))
        assertFalse(SharedProperties.getBooleanValueForStringValue("k", "bogus", false, false, null))
        assertTrue(SharedProperties.getBooleanValueForStringValue("k", null, true, false, null))
        assertFalse(SharedProperties.getInvertedBooleanValueForStringValue("k", "true", true, false, null))
        assertTrue(SharedProperties.getInvertedBooleanValueForStringValue("k", "false", true, false, null))
    }

    @Test
    fun `range check keeps in-range values`() {
        assertEquals(5, SharedProperties.getDefaultIfNotInRange("k", 5, 0, 0, 10, false, false, null))
        assertEquals(0, SharedProperties.getDefaultIfNotInRange("k", 99, 0, 0, 10, false, false, null))
        assertEquals(2.5f, SharedProperties.getDefaultIfNotInRange("k", 2.5f, 0f, 0f, 10f, false, false, null))
        // NaN comparisons are always false, so NaN passes straight through (Java parity).
        assertTrue(SharedProperties.getDefaultIfNotInRange("k", Float.NaN, 0f, 0f, 10f, false, false, null).isNaN())
    }

    @Test
    fun `default-if helpers fall back correctly`() {
        assertEquals("a", SharedProperties.getDefaultIfNull("a", "b"))
        assertEquals("b", SharedProperties.getDefaultIfNull(null, "b"))
        assertNull(SharedProperties.getDefaultIfNull(null, null))
        assertEquals("a", SharedProperties.getDefaultIfNullOrEmpty("a", "b"))
        assertEquals("b", SharedProperties.getDefaultIfNullOrEmpty("", "b"))
        assertEquals("b", SharedProperties.getDefaultIfNullOrEmpty(null, "b"))
        assertEquals("abc", SharedProperties.toLowerCase("ABC"))
        assertNull(SharedProperties.toLowerCase(null))
    }

    @Test
    fun `put to map only accepts null primitives and strings`() {
        val map = HashMap<String, Any?>()
        assertTrue(SharedProperties.putToMap(map, "s", "v"))
        assertTrue(SharedProperties.putToMap(map, "i", 3))
        assertTrue(SharedProperties.putToMap(map, "b", true))
        assertTrue(SharedProperties.putToMap(map, "n", null))
        assertTrue(map.containsKey("n"))
        assertFalse(SharedProperties.putToMap(map, "o", Object()))
        assertFalse(SharedProperties.putToMap(map, null, "v"))
        assertFalse(SharedProperties.putToMap(null, "k", "v"))
        assertEquals("v", map["s"])
    }

    @Test
    fun `put to properties stores and removes`() {
        val properties = Properties()
        assertTrue(SharedProperties.putToProperties(properties, "k", "v"))
        assertEquals("v", properties.getProperty("k"))
        assertTrue(SharedProperties.putToProperties(properties, "k", null))
        assertNull(properties.getProperty("k"))
        assertFalse(SharedProperties.putToProperties(properties, null, "v"))
        assertFalse(SharedProperties.putToProperties(null, "k", "v"))
    }

    @Test
    fun `copies duplicate contents and tolerate nulls`() {
        assertNull(SharedProperties.getPropertiesCopy(null))
        assertNull(SharedProperties.getMapCopy(null))
        val properties = Properties()
        properties["a"] = "1"
        val propertiesCopy = SharedProperties.getPropertiesCopy(properties)!!
        assertEquals("1", propertiesCopy.getProperty("a"))
        propertiesCopy["a"] = "2"
        assertEquals("1", properties.getProperty("a"))
        val mapCopy = SharedProperties.getMapCopy(mapOf("a" to 1))!!
        assertEquals(1, mapCopy["a"])
    }

    @Test
    fun `default-if-not-in-map returns mapped or default`() {
        val map: ImmutableBiMap<String, Boolean> = SharedProperties.MAP_GENERIC_BOOLEAN
        assertEquals(true, SharedProperties.getDefaultIfNotInMap("k", map, "true", false, false, null))
        assertEquals(false, SharedProperties.getDefaultIfNotInMap("k", map, "bogus", false, false, null))
        assertEquals(true, SharedProperties.getDefaultIfNotInMap("k", map, null, true, false, null))
    }
}
