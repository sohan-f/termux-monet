package com.termux.shared.file.filesystem

import android.system.ErrnoException
import android.system.Os
import java.io.FileDescriptor
import java.io.IOException

class NativeDispatcher {

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun stat(filePath: String?, fileAttributes: FileAttributes) {
            validateFileExistence(filePath)
            try {
                // Null/empty already rejected above; !! keeps the stub-strict
                // non-null param while preserving the IOException behaviour.
                fileAttributes.loadFromStructStat(Os.stat(filePath!!))
            } catch (e: ErrnoException) {
                throw IOException("Failed to run Os.stat() on file at path \"" + filePath + "\": " + e.message)
            }
        }

        @JvmStatic
        @Throws(IOException::class)
        fun lstat(filePath: String?, fileAttributes: FileAttributes) {
            validateFileExistence(filePath)
            try {
                // See stat() for the !! rationale.
                fileAttributes.loadFromStructStat(Os.lstat(filePath!!))
            } catch (e: ErrnoException) {
                throw IOException("Failed to run Os.lstat() on file at path \"" + filePath + "\": " + e.message)
            }
        }

        @JvmStatic
        @Throws(IOException::class)
        fun fstat(fileDescriptor: FileDescriptor?, fileAttributes: FileAttributes) {
            validateFileDescriptor(fileDescriptor)
            try {
                // See stat() for the !! rationale.
                fileAttributes.loadFromStructStat(Os.fstat(fileDescriptor!!))
            } catch (e: ErrnoException) {
                throw IOException("Failed to run Os.fstat() on file descriptor \"" + fileDescriptor.toString() + "\": " + e.message)
            }
        }

        @JvmStatic
        @Throws(IOException::class)
        fun validateFileExistence(filePath: String?) {
            if (filePath == null || filePath.isEmpty())
                throw IOException("The path is null or empty")
            // Note: Java created an unused `new File(filePath)` here (with its
            // existence check commented out). Dropped to keep a zero-warning build.
        }

        @JvmStatic
        @Throws(IOException::class)
        fun validateFileDescriptor(fileDescriptor: FileDescriptor?) {
            if (fileDescriptor == null)
                throw IOException("The file descriptor is null")
            if (!fileDescriptor.valid())
                throw IOException("No such file descriptor: \"" + fileDescriptor.toString() + "\"")
        }
    }
}
