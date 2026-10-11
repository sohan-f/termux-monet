package com.termux.shared.shell.command.environment

/**
 * The name/environment variable pair, with escaping info for `.env` files.
 */
open class ShellEnvironmentVariable @JvmOverloads constructor(
    /** The name for environment variable */
    @JvmField var name: String?,
    /** The value for environment variable */
    @JvmField var value: String?,
    /** If environment variable value is already escaped. */
    @JvmField var escaped: Boolean = false
) : Comparable<ShellEnvironmentVariable> {

    override fun compareTo(other: ShellEnvironmentVariable): Int {
        // The Java version dereferenced here and threw NPE for nulls.
        return name!!.compareTo(other.name!!)
    }
}
