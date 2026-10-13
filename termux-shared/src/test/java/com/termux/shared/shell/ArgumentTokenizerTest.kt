package com.termux.shared.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ArgumentTokenizerTest {

    @Test
    fun `empty and blank give no tokens`() {
        assertEquals(emptyList<String>(), ArgumentTokenizer.tokenize(""))
        assertEquals(emptyList<String>(), ArgumentTokenizer.tokenize("   "))
    }

    @Test
    fun `plain split on whitespace`() {
        assertEquals(listOf("a", "b"), ArgumentTokenizer.tokenize("a b"))
        assertEquals(listOf("a", "b"), ArgumentTokenizer.tokenize("  a  b  "))
    }

    @Test
    fun `quotes group whitespace`() {
        assertEquals(listOf("a b", "c"), ArgumentTokenizer.tokenize("'a b' c"))
        assertEquals(listOf("a b"), ArgumentTokenizer.tokenize("\"a b\""))
        assertEquals(listOf("abc"), ArgumentTokenizer.tokenize("a\"b\"c"))
    }

    @Test
    fun `single quotes keep double quotes literal`() {
        assertEquals(listOf("a\"b"), ArgumentTokenizer.tokenize("'a\"b'"))
    }

    @Test
    fun `backslash escapes outside quotes`() {
        assertEquals(listOf("a b"), ArgumentTokenizer.tokenize("a\\ b"))
    }

    @Test
    fun `trailing lone backslash is kept`() {
        assertEquals(listOf("a\\"), ArgumentTokenizer.tokenize("a\\"))
    }

    @Test
    fun `trailing backslash inside quotes throws`() {
        try {
            ArgumentTokenizer.tokenize("\"abc\\")
            fail("expected index out of bounds")
        } catch (e: IndexOutOfBoundsException) {
            // Preserved Java charAt() behaviour.
        }
    }

    @Test
    fun `escaped quote inside quotes`() {
        assertEquals(listOf("a\"b"), ArgumentTokenizer.tokenize("\"a\\\"b\""))
    }

    @Test
    fun `non-quote escape keeps backslash`() {
        assertEquals(listOf("a\\nb"), ArgumentTokenizer.tokenize("\"a\\nb\""))
    }

    @Test
    fun `unterminated quote is lenient`() {
        assertEquals(listOf("abc"), ArgumentTokenizer.tokenize("\"abc"))
        assertEquals(listOf("abc"), ArgumentTokenizer.tokenize("'abc"))
    }

    @Test
    fun `stringify wraps in quotes`() {
        assertEquals(listOf("\"a\"", "\"b\""), ArgumentTokenizer.tokenize("a b", true))
    }

    @Test
    fun `stringify escapes newline`() {
        assertEquals(listOf("\"a\\nb\""), ArgumentTokenizer.tokenize("\"a\nb\"", true))
    }

    @Test
    fun `stringify escapes backslash and quote`() {
        // Double backslash tokenizes to one; stringify re-escapes it.
        assertEquals(listOf("\"a\\\\b\""), ArgumentTokenizer.tokenize("a\\\\b", true))
        // A bare quote toggles quoting instead of landing in the token.
        assertEquals(listOf("\"ab\""), ArgumentTokenizer.tokenize("a\"b", true))
        // An escaped quote inside quotes survives and is re-escaped.
        assertEquals(listOf("\"a\\\"b\""), ArgumentTokenizer.tokenize("\"a\\\"b\"", true))
    }
}
