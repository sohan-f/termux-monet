package com.termux.shared.termux.file

import com.termux.shared.termux.TermuxConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Plain-JVM tests for the pure-string helpers of [TermuxFileUtils].
 * Branches touching Android APIs are covered on-device instead.
 */
class TermuxFileUtilsTest {

    @Test
    fun `expand replaces prefix and home markers`() {
        val prefix = TermuxConstants.TERMUX_PREFIX_DIR_PATH
        val home = TermuxConstants.TERMUX_HOME_DIR_PATH
        assertEquals(prefix, TermuxFileUtils.getExpandedTermuxPath("\$PREFIX"))
        assertEquals("$prefix/bin/sh", TermuxFileUtils.getExpandedTermuxPath("\$PREFIX/bin/sh"))
        assertEquals(home, TermuxFileUtils.getExpandedTermuxPath("~/"))
        assertEquals("$home/x", TermuxFileUtils.getExpandedTermuxPath("~/x"))
        assertEquals("/other/x", TermuxFileUtils.getExpandedTermuxPath("/other/x"))
        assertNull(TermuxFileUtils.getExpandedTermuxPath(null))
    }

    @Test
    fun `unexpand restores markers`() {
        val prefix = TermuxConstants.TERMUX_PREFIX_DIR_PATH
        val home = TermuxConstants.TERMUX_HOME_DIR_PATH
        assertEquals("\$PREFIX/bin/sh", TermuxFileUtils.getUnExpandedTermuxPath("$prefix/bin/sh"))
        assertEquals("~/x", TermuxFileUtils.getUnExpandedTermuxPath("$home/x"))
        assertEquals("/other/x", TermuxFileUtils.getUnExpandedTermuxPath("/other/x"))
        assertNull(TermuxFileUtils.getUnExpandedTermuxPath(null))
    }

    @Test
    fun `expand and unexpand round-trip lists with nulls`() {
        val prefix = TermuxConstants.TERMUX_PREFIX_DIR_PATH
        assertEquals(
            listOf("$prefix/a", null, "/b"),
            TermuxFileUtils.getExpandedTermuxPaths(listOf("\$PREFIX/a", null, "/b"))
        )
        assertNull(TermuxFileUtils.getExpandedTermuxPaths(null))
        assertNull(TermuxFileUtils.getUnExpandedTermuxPaths(null))
    }

    @Test
    fun `canonical path expands then resolves`() {
        val prefix = TermuxConstants.TERMUX_PREFIX_DIR_PATH
        assertEquals("$prefix/bin", TermuxFileUtils.getCanonicalPath("\$PREFIX/bin/.", null, true))
        assertEquals("/x", TermuxFileUtils.getCanonicalPath("x", null, false))
    }

    @Test
    fun `matched parent path handles nulls and storage home`() {
        assertEquals(
            TermuxConstants.TERMUX_FILES_DIR_PATH,
            TermuxFileUtils.getMatchedAllowedTermuxWorkingDirectoryParentPathForPath(null)
        )
        assertEquals(
            TermuxConstants.TERMUX_FILES_DIR_PATH,
            TermuxFileUtils.getMatchedAllowedTermuxWorkingDirectoryParentPathForPath("")
        )
        val storageHome = TermuxConstants.TERMUX_STORAGE_HOME_DIR_PATH
        assertEquals(
            storageHome,
            TermuxFileUtils.getMatchedAllowedTermuxWorkingDirectoryParentPathForPath("$storageHome/Download/x")
        )
    }
}
