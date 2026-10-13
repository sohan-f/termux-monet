package com.termux.shared.settings.preferences

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [SharedPreferences] is an interface, so its contract is faked on the JVM.
 * Missing keys return defaults and wrong types throw ClassCastException,
 * mirroring the framework.
 */
class FakeSharedPreferences(private val map: MutableMap<String, Any?> = HashMap()) : SharedPreferences {

    override fun getAll(): MutableMap<String, *> = map

    override fun getString(key: String?, defValue: String?): String? {
        if (!map.containsKey(key)) return defValue
        return map[key] as String?
    }

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? {
        if (!map.containsKey(key)) return defValues
        return map[key] as Set<String>?
    }

    override fun getInt(key: String?, defValue: Int): Int {
        if (!map.containsKey(key)) return defValue
        return map[key] as Int
    }

    override fun getLong(key: String?, defValue: Long): Long {
        if (!map.containsKey(key)) return defValue
        return map[key] as Long
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        if (!map.containsKey(key)) return defValue
        return map[key] as Float
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        if (!map.containsKey(key)) return defValue
        return map[key] as Boolean
    }

    override fun contains(key: String?): Boolean = map.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor()

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
    }

    inner class FakeEditor : SharedPreferences.Editor {
        override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply { map[key!!] = value }
        override fun putStringSet(key: String?, values: Set<String>?): SharedPreferences.Editor = apply { map[key!!] = values }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply { map[key!!] = value }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply { map[key!!] = value }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply { map[key!!] = value }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply { map[key!!] = value }
        override fun remove(key: String?): SharedPreferences.Editor = apply { map.remove(key) }
        override fun clear(): SharedPreferences.Editor = apply { map.clear() }
        override fun commit(): Boolean = true
        override fun apply() {
        }
    }
}

class SharedPreferenceUtilsTest {

    @Test
    fun `getters return defaults on missing and empty`() {
        val prefs = FakeSharedPreferences()
        assertEquals(true, SharedPreferenceUtils.getBoolean(prefs, "k", true))
        assertTrue(SharedPreferenceUtils.getFloat(prefs, "k", 1.5f) == 1.5f)
        assertEquals(7, SharedPreferenceUtils.getInt(prefs, "k", 7))
        assertEquals(9L, SharedPreferenceUtils.getLong(prefs, "k", 9L))
        assertEquals("d", SharedPreferenceUtils.getString(prefs, "k", "d", false))
        assertNull(SharedPreferenceUtils.getString(prefs, "k", null, false))
        assertNull(SharedPreferenceUtils.getStringSet(prefs, "k", null))
    }

    @Test
    fun `null prefs return defaults and ignore sets`() {
        assertEquals(true, SharedPreferenceUtils.getBoolean(null, "k", true))
        assertEquals(7, SharedPreferenceUtils.getInt(null, "k", 7))
        SharedPreferenceUtils.setInt(null, "k", 1, false)
        assertEquals(3, SharedPreferenceUtils.getAndIncrementInt(null, "k", 3, false, null))
    }

    @Test
    fun `wrong types fall back to defaults`() {
        val prefs = FakeSharedPreferences(hashMapOf<String, Any?>("k" to 5))
        assertEquals("d", SharedPreferenceUtils.getString(prefs, "k", "d", false))
        assertEquals(true, SharedPreferenceUtils.getBoolean(prefs, "k", true))
    }

    @Test
    fun `set and get roundtrip`() {
        val prefs = FakeSharedPreferences()
        SharedPreferenceUtils.setBoolean(prefs, "b", true, false)
        SharedPreferenceUtils.setFloat(prefs, "f", 1.5f, true)
        SharedPreferenceUtils.setInt(prefs, "i", 7, false)
        SharedPreferenceUtils.setLong(prefs, "l", 9L, false)
        SharedPreferenceUtils.setString(prefs, "s", "v", false)
        SharedPreferenceUtils.setStringSet(prefs, "ss", setOf("a"), false)
        assertEquals(true, SharedPreferenceUtils.getBoolean(prefs, "b", false))
        assertTrue(SharedPreferenceUtils.getFloat(prefs, "f", 0f) == 1.5f)
        assertEquals(7, SharedPreferenceUtils.getInt(prefs, "i", 0))
        assertEquals(9L, SharedPreferenceUtils.getLong(prefs, "l", 0L))
        assertEquals("v", SharedPreferenceUtils.getString(prefs, "s", "d", false))
        assertEquals(setOf("a"), SharedPreferenceUtils.getStringSet(prefs, "ss", null))
    }

    @Test
    fun `increment returns before-value and stores plus-one`() {
        val prefs = FakeSharedPreferences()
        assertEquals(0, SharedPreferenceUtils.getAndIncrementInt(prefs, "k", 0, false, null))
        assertEquals(1, SharedPreferenceUtils.getAndIncrementInt(prefs, "k", 0, false, null))
        assertEquals(2, SharedPreferenceUtils.getInt(prefs, "k", -1))
    }

    @Test
    fun `increment resets negatives when asked`() {
        val prefs = FakeSharedPreferences(hashMapOf<String, Any?>("k" to -5))
        assertEquals(10, SharedPreferenceUtils.getAndIncrementInt(prefs, "k", 0, false, 10))
        assertEquals(11, SharedPreferenceUtils.getInt(prefs, "k", -1))
    }

    @Test
    fun `increment wraps at max int like java`() {
        val prefs = FakeSharedPreferences(hashMapOf<String, Any?>("k" to Int.MAX_VALUE))
        assertEquals(Int.MAX_VALUE, SharedPreferenceUtils.getAndIncrementInt(prefs, "k", 0, false, null))
        assertEquals(Int.MIN_VALUE, SharedPreferenceUtils.getInt(prefs, "k", 0))
    }

    @Test
    fun `int stored as string`() {
        val prefs = FakeSharedPreferences()
        SharedPreferenceUtils.setIntStoredAsString(prefs, "k", 42, false)
        assertEquals(42, SharedPreferenceUtils.getIntStoredAsString(prefs, "k", 0))
        SharedPreferenceUtils.setIntStoredAsString(prefs, "bad", 0, false)
        prefs.edit().putString("bad", "xx").commit()
        assertEquals(0, SharedPreferenceUtils.getIntStoredAsString(prefs, "bad", 0))
        assertEquals(0, SharedPreferenceUtils.getIntStoredAsString(prefs, "missing", 0))
    }

    @Test
    fun `empty string uses default when asked`() {
        val prefs = FakeSharedPreferences(hashMapOf<String, Any?>("k" to ""))
        assertEquals("d", SharedPreferenceUtils.getString(prefs, "k", "d", true))
        assertEquals("", SharedPreferenceUtils.getString(prefs, "k", "d", false))
    }
}
