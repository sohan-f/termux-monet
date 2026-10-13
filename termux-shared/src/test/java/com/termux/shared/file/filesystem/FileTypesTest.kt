package com.termux.shared.file.filesystem

import com.termux.shared.errors.Errno
import com.termux.shared.file.FileUtilsErrno
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plain-JVM tests for the filesystem value cluster. Pins the SOCKET fix in
 * [FileTypes.convertFileTypeFlagsToNamesString] and parity of the
 * permission/key/errno helpers.
 */
class FileTypesTest {

    @Test
    fun `normal flags cover regular directory and symlink`() {
        assertEquals(
            FileType.REGULAR.getValue() or FileType.DIRECTORY.getValue() or FileType.SYMLINK.getValue(),
            FileTypes.FILE_TYPE_NORMAL_FLAGS
        )
    }

    @Test
    fun `convert flags includes socket`() {
        // Regression: SOCKET was missing from the conversion list, so socket
        // flags were silently dropped from the names string.
        val names = FileTypes.convertFileTypeFlagsToNamesString(
            FileType.REGULAR.getValue() or FileType.SOCKET.getValue()
        )
        assertEquals("regular,socket", names)
    }

    @Test
    fun `convert flags lists every known type`() {
        val all = FileType.REGULAR.getValue() or FileType.DIRECTORY.getValue() or
            FileType.SYMLINK.getValue() or FileType.SOCKET.getValue() or
            FileType.CHARACTER.getValue() or FileType.FIFO.getValue() or
            FileType.BLOCK.getValue() or FileType.UNKNOWN.getValue()
        assertEquals(
            "regular,directory,symlink,socket,character,fifo,block,unknown",
            FileTypes.convertFileTypeFlagsToNamesString(all)
        )
    }

    @Test
    fun `convert empty flags gives empty string`() {
        assertEquals("", FileTypes.convertFileTypeFlagsToNamesString(0))
    }

    @Test
    fun `getFileType of null path is no-exist`() {
        assertSame(FileType.NO_EXIST, FileTypes.getFileType(null, false))
        assertSame(FileType.NO_EXIST, FileTypes.getFileType("", true))
    }

    @Test
    fun `permissions string round-trips`() {
        val perms = FilePermissions.fromString("rwxr-x---")
        assertTrue(perms.contains(FilePermission.OWNER_READ))
        assertTrue(perms.contains(FilePermission.OWNER_WRITE))
        assertTrue(perms.contains(FilePermission.OWNER_EXECUTE))
        assertTrue(perms.contains(FilePermission.GROUP_READ))
        assertTrue(perms.contains(FilePermission.GROUP_EXECUTE))
        assertEquals(5, perms.size)
        assertEquals("rwxr-x---", FilePermissions.toString(perms))
    }

    @Test
    fun `file key equality is by dev and ino`() {
        // Same module, so the internal (package-private in Java)
        // constructor is directly accessible.
        val a = FileKey(7L, 42L)
        val b = FileKey(7L, 42L)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertEquals("(dev=7,ino=42)", a.toString())
    }

    @Test
    fun `file utils errno codes resolve uniquely`() {
        assertSame(
            FileUtilsErrno.ERRNO_FILE_NOT_FOUND_AT_PATH,
            Errno.valueOf("FileUtils Error", 150)
        )
        assertSame(
            FileUtilsErrno.ERRNO_FILE_NOT_EXECUTABLE_SHORT,
            Errno.valueOf("FileUtils Error", 406)
        )
    }

    @Test
    fun `file utils short mapping covers long variants`() {
        assertSame(
            FileUtilsErrno.ERRNO_FILE_NOT_FOUND_AT_PATH_SHORT,
            FileUtilsErrno.ERRNO_SHORT_MAPPING[FileUtilsErrno.ERRNO_FILE_NOT_FOUND_AT_PATH]
        )
        assertEquals(7, FileUtilsErrno.ERRNO_SHORT_MAPPING.size)
    }

    @Test
    fun `file utils error formats message`() {
        val error = FileUtilsErrno.ERRNO_FILE_NOT_FOUND_AT_PATH.getError("label", "/x")
        assertEquals("The label not found at path \"/x\".", error.getMessage())
    }

}
