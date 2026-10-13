package com.termux.shared.file

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plain-JVM tests for the pure-string helpers of [FileUtils]. Anything that
 * touches file metadata goes through android.system.Os and can only run
 * on-device (see the manual FileUtilsTests).
 */
class FileUtilsTest {

    @Test
    fun `normalizePath collapses slashes and dot segments`() {
        assertEquals("/a/b", FileUtils.normalizePath("//a//b/"))
        assertEquals("/a/b", FileUtils.normalizePath("/a/./b"))
        assertNull(FileUtils.normalizePath(null))
    }

    @Test
    fun `sanitizeFileName replaces specials`() {
        assertEquals("a_b_c", FileUtils.sanitizeFileName("a/b:c", false, false))
        assertEquals("a_b", FileUtils.sanitizeFileName("A B", true, true))
        assertNull(FileUtils.sanitizeFileName(null, true, true))
    }

    @Test
    fun `permission string validation accepts only 3-char rwx form`() {
        assertTrue(FileUtils.isValidPermissionString("rwx"))
        assertTrue(FileUtils.isValidPermissionString("r-x"))
        assertTrue(FileUtils.isValidPermissionString("---"))
        assertFalse(FileUtils.isValidPermissionString(null))
        assertFalse(FileUtils.isValidPermissionString(""))
        assertFalse(FileUtils.isValidPermissionString("rwxrwxrwx"))
        assertFalse(FileUtils.isValidPermissionString("rw"))
        assertFalse(FileUtils.isValidPermissionString("abc"))
    }

    @Test
    fun `dirname and basename split on last slash`() {
        assertEquals("/a", FileUtils.getFileDirname("/a/b"))
        assertNull(FileUtils.getFileDirname("b"))
        assertNull(FileUtils.getFileDirname(null))
        assertEquals("b", FileUtils.getFileBasename("/a/b"))
        assertEquals("b", FileUtils.getFileBasename("b"))
        assertNull(FileUtils.getFileBasename(null))
        assertEquals("b", FileUtils.getFileBasenameWithoutExtension("/a/b.txt"))
        assertEquals("b", FileUtils.getFileBasenameWithoutExtension("b"))
        assertNull(FileUtils.getFileBasenameWithoutExtension(null))
    }

    @Test
    fun `canonical path prefixes non-absolute paths`() {
        assertEquals("/x", FileUtils.getCanonicalPath("/x", null))
        assertEquals("/p/x", FileUtils.getCanonicalPath("x", "/p"))
        assertEquals("/x", FileUtils.getCanonicalPath("x", null))
    }

    @Test
    fun `path in dir paths matches canonical prefix`() {
        assertTrue(FileUtils.isPathInDirPaths("/a/b/c", listOf("/a/b"), false))
        assertTrue(FileUtils.isPathInDirPaths("/a/b/c", listOf("/a/b"), true))
        assertFalse(FileUtils.isPathInDirPaths("/a/b", listOf("/a/b"), true))
        assertFalse(FileUtils.isPathInDirPaths("/a/b", listOf("/a/b"), false))
        assertFalse(FileUtils.isPathInDirPaths("/a/bc", listOf("/a/b"), false))
        assertFalse(FileUtils.isPathInDirPaths(null, listOf("/a"), false))
        assertFalse(FileUtils.isPathInDirPaths("/a/b", null, false))
        assertFalse(FileUtils.isPathInDirPaths("/a/b", listOf(), false))
        assertTrue(FileUtils.isPathInDirPath("/a/b/c", "/a/b", false))
        assertFalse(FileUtils.isPathInDirPath("/a/b/c", null, false))
    }

    @Test
    fun `disallowed file type error formats all four placeholders`() {
        // Regression pin for copyOrMoveFile passing only 3 args to this
        // 4-placeholder message, which made String.format throw and reduced
        // the error to the unformatted fallback.
        val error = FileUtilsErrno.ERRNO_FILE_NOT_AN_ALLOWED_FILE_TYPE.getError(
            "label source file meant to be copied", "/src", "regular", "directory"
        )
        assertEquals(
            "The label source file meant to be copied found at path \"/src\" " +
                "of type \"regular\" is not one of allowed file types \"directory\".",
            error.getMessage()
        )
    }

    @Test
    fun `short error mapping shortens long file errors`() {
        val long = FileUtilsErrno.ERRNO_FILE_NOT_FOUND_AT_PATH.getError("mylabel", "/x").setLabel("mylabel")
        val short = FileUtils.getShortFileUtilsError(long)
        assertEquals("The mylabel not found at path.", short.getMessage())
        // Without a label the default "file" is used, matching Java behaviour.
        val unlabelled = FileUtils.getShortFileUtilsError(
            FileUtilsErrno.ERRNO_FILE_NOT_FOUND_AT_PATH.getError("mylabel", "/x")
        )
        assertEquals("The file not found at path.", unlabelled.getMessage())
        // Non-FileUtils errors pass through untouched.
        val other = com.termux.shared.errors.FunctionErrno.ERRNO_UNSET_PARAMETER.getError("a", "b")
        assertEquals(other, FileUtils.getShortFileUtilsError(other))
    }
}
