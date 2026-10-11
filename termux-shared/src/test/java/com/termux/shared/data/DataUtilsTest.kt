package com.termux.shared.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DataUtilsTest {

    @Test
    fun `truncate returns short text as-is`() {
        assertNull(DataUtils.getTruncatedCommandOutput(null, 10, false, false, false))
        assertEquals("abc", DataUtils.getTruncatedCommandOutput("abc", 10, false, false, false))
        assertEquals("abc", DataUtils.getTruncatedCommandOutput("abc", -1, false, false, true))
    }

    @Test
    fun `truncate from end keeps head`() {
        assertEquals("ab", DataUtils.getTruncatedCommandOutput("abcdef", 2, true, false, false))
    }

    @Test
    fun `truncate from start keeps tail`() {
        assertEquals("ef", DataUtils.getTruncatedCommandOutput("abcdef", 2, false, false, false))
    }

    @Test
    fun `truncate snaps to newline`() {
        assertEquals("c\nd", DataUtils.getTruncatedCommandOutput("ab\nc\nd", 4, false, true, false))
    }

    @Test
    fun `truncate adds prefix within budget`() {
        // Prefix "(truncated) " is 12 chars, so budget 14 leaves 2.
        assertEquals("(truncated) ef", DataUtils.getTruncatedCommandOutput("abcdef", 14, false, false, true))
    }

    @Test
    fun `replace substrings in array`() {
        val array = arrayOf("a,b", "c")
        DataUtils.replaceSubStringsInStringArrayItems(array, ",", ";")
        assertArrayEquals(arrayOf("a;b", "c"), array)
    }

    @Test
    fun `replace tolerates null and empty arrays`() {
        DataUtils.replaceSubStringsInStringArrayItems(null, ",", ";")
        DataUtils.replaceSubStringsInStringArrayItems(emptyArray(), ",", ";")
    }

    @Test
    fun `replace null element throws like java`() {
        @Suppress("UNCHECKED_CAST")
        val array = arrayOf("a", null) as Array<String>
        try {
            DataUtils.replaceSubStringsInStringArrayItems(array, ",", ";")
            fail("expected NullPointerException")
        } catch (e: NullPointerException) {
            // Preserved Java behaviour.
        }
    }

    @Test
    fun `parse numbers with defaults`() {
        assertTrue(DataUtils.getFloatFromString("1.5", 0f) == 1.5f)
        assertTrue(DataUtils.getFloatFromString("xx", 0f) == 0f)
        assertTrue(DataUtils.getFloatFromString(null, 0f) == 0f)
        assertEquals(5, DataUtils.getIntFromString("5", 0))
        assertEquals(0, DataUtils.getIntFromString("xx", 0))
        assertEquals(0, DataUtils.getIntFromString(null, 0))
    }

    @Test
    fun `string from integer honours nulls`() {
        assertEquals("5", DataUtils.getStringFromInteger(5, "d"))
        assertEquals("d", DataUtils.getStringFromInteger(null, "d"))
        assertNull(DataUtils.getStringFromInteger(null, null))
    }

    @Test
    fun `bytes to hex`() {
        assertEquals("00FFAB", DataUtils.bytesToHex(byteArrayOf(0x00, 0xFF.toByte(), 0xAB.toByte())))
        assertEquals("", DataUtils.bytesToHex(byteArrayOf()))
    }

    @Test
    fun `clamp and ranged`() {
        assertEquals(5, DataUtils.clamp(5, 0, 10))
        assertEquals(0, DataUtils.clamp(-5, 0, 10))
        assertEquals(10, DataUtils.clamp(15, 0, 10))
        assertTrue(DataUtils.rangedOrDefault(5f, 0f, 0f, 10f) == 5f)
        assertTrue(DataUtils.rangedOrDefault(15f, 0f, 0f, 10f) == 0f)
    }

    @Test
    fun `ranged keeps NaN`() {
        // NaN fails both comparisons, so the value itself is returned.
        assertTrue(DataUtils.rangedOrDefault(Float.NaN, 0f, 0f, 10f).isNaN())
    }

    @Test
    fun `indents handle null empty and multiline`() {
        assertNull(DataUtils.getSpaceIndentedString(null, 1))
        assertEquals("", DataUtils.getSpaceIndentedString("", 1))
        assertEquals("    a\n    b", DataUtils.getSpaceIndentedString("a\nb", 1))
        assertEquals("\ta", DataUtils.getTabIndentedString("a", 1))
        assertEquals("        a", DataUtils.getSpaceIndentedString("a", 2))
    }

    @Test
    fun `defaults`() {
        assertEquals("v", DataUtils.getDefaultIfNull("v", "d"))
        assertEquals("d", DataUtils.getDefaultIfNull(null, "d"))
        assertEquals("v", DataUtils.getDefaultIfUnset("v", "d"))
        assertEquals("d", DataUtils.getDefaultIfUnset("", "d"))
        assertEquals("d", DataUtils.getDefaultIfUnset(null, "d"))
        assertTrue(DataUtils.isNullOrEmpty(null))
        assertTrue(DataUtils.isNullOrEmpty(""))
        assertTrue(!DataUtils.isNullOrEmpty("x"))
    }

    @Test
    fun `serialized size`() {
        assertEquals(0, DataUtils.getSerializedSize(null))
        assertTrue(DataUtils.getSerializedSize("hello") > 0)
        assertEquals(-1, DataUtils.getSerializedSize(BadSerializable()))
    }

    class NonSerializable

    class BadSerializable : java.io.Serializable {
        @Suppress("unused")
        val bad: NonSerializable = NonSerializable()
    }
}
