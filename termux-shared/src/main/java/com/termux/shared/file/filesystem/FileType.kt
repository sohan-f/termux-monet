package com.termux.shared.file.filesystem

/**
 * The {@link Enum} that defines file types.
 */
enum class FileType(private val typeName: String, private val typeValue: Int) {

    // 00000000
    NO_EXIST("no exist", 0),
    // 00000001
    REGULAR("regular", 1),
    // 00000010
    DIRECTORY("directory", 2),
    // 00000100
    SYMLINK("symlink", 4),
    // 00001000
    SOCKET("socket", 8),
    // 00010000
    CHARACTER("character", 16),
    // 00100000
    FIFO("fifo", 32),
    // 01000000
    BLOCK("block", 64),
    // 10000000
    UNKNOWN("unknown", 128);

    fun getName(): String {
        return typeName
    }

    fun getValue(): Int {
        return typeValue
    }
}
