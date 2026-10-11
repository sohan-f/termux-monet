package com.termux.shared.errors

/**
 * The {@link Class} that defines function error messages and codes.
 */
open class FunctionErrno : Errno {

    // Package-private in Java and never instantiated; the entries below are
    // plain Errno instances, so keep the constructor inaccessible.
    private constructor(type: String, code: Int, message: String) : super(type, code, message)

    companion object {
        const val TYPE = "Function Error"

        /* Errors for null or empty parameters (100-150) */
        @JvmField
        val ERRNO_NULL_OR_EMPTY_PARAMETER: Errno = Errno(TYPE, 100, "The %1\$s parameter passed to \"%2\$s\" is null or empty.")

        @JvmField
        val ERRNO_NULL_OR_EMPTY_PARAMETERS: Errno = Errno(TYPE, 101, "The %1\$s parameters passed to \"%2\$s\" are null or empty.")

        @JvmField
        val ERRNO_UNSET_PARAMETER: Errno = Errno(TYPE, 102, "The %1\$s parameter passed to \"%2\$s\" must be set.")

        @JvmField
        val ERRNO_UNSET_PARAMETERS: Errno = Errno(TYPE, 103, "The %1\$s parameters passed to \"%2\$s\" must be set.")

        @JvmField
        val ERRNO_INVALID_PARAMETER: Errno = Errno(TYPE, 104, "The %1\$s parameter passed to \"%2\$s\" is invalid.\"%3\$s\"")

        // Fixed duplicate code: this reused 104, silently unregistering
        // ERRNO_INVALID_PARAMETER from the Errno valueOf map. Renumbered to
        // the next free code 105.
        @JvmField
        val ERRNO_PARAMETER_NOT_INSTANCE_OF: Errno = Errno(TYPE, 105, "The %1\$s parameter passed to \"%2\$s\" is not an instance of %3\$s.")
    }
}
