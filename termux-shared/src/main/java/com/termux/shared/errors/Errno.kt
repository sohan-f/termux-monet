package com.termux.shared.errors

import android.app.Activity
import androidx.annotation.NonNull
import com.termux.shared.logger.Logger
import java.util.HashMap

/**
 * The {@link Class} that defines error messages and codes.
 */
open class Errno {

    /**
     * The errno type.
     */
    @JvmField
    protected val type: String

    /**
     * The errno code.
     */
    @JvmField
    protected val code: Int

    /**
     * The errno message.
     */
    @JvmField
    protected val message: String

    constructor(@NonNull type: String, code: Int, @NonNull message: String) {
        this.type = type
        this.code = code
        this.message = message
        map[type + ":" + code] = this
    }

    @NonNull
    override fun toString(): String {
        return "type=$type, code=$code, message=\"$message\""
    }

    @NonNull
    fun getType(): String {
        return type
    }

    fun getCode(): Int {
        return code
    }

    @NonNull
    fun getMessage(): String {
        return message
    }

    fun getError(): Error {
        return Error(getType(), getCode(), getMessage())
    }

    fun getError(vararg args: Any?): Error {
        return try {
            Error(getType(), getCode(), String.format(getMessage(), *args))
        } catch (e: Exception) {
            Logger.logWarn(LOG_TAG, "Exception raised while calling String.format() for error message of errno " + this + " with args" + args.contentToString() + "\n" + e.message)
            // Return unformatted message as a backup
            Error(getType(), getCode(), getMessage() + ": " + args.contentToString())
        }
    }

    fun getError(throwable: Throwable?, vararg args: Any?): Error {
        return if (throwable == null)
            getError(*args)
        else
            getError(listOf(throwable), *args)
    }

    fun getError(throwablesList: List<Throwable>?, vararg args: Any?): Error {
        return try {
            if (throwablesList == null)
                Error(getType(), getCode(), String.format(getMessage(), *args))
            else
                Error(getType(), getCode(), String.format(getMessage(), *args), throwablesList)
        } catch (e: Exception) {
            Logger.logWarn(LOG_TAG, "Exception raised while calling String.format() for error message of errno " + this + " with args" + args.contentToString() + "\n" + e.message)
            // Return unformatted message as a backup
            Error(getType(), getCode(), getMessage() + ": " + args.contentToString(), throwablesList)
        }
    }

    fun equalsErrorTypeAndCode(error: Error?): Boolean {
        if (error == null)
            return false
        return type == error.getType() && code == error.getCode()
    }

    companion object {
        private val map: HashMap<String, Errno> = HashMap()

        const val TYPE = "Error"

        @JvmField
        val ERRNO_SUCCESS: Errno = Errno(TYPE, Activity.RESULT_OK, "Success")

        @JvmField
        val ERRNO_CANCELLED: Errno = Errno(TYPE, Activity.RESULT_CANCELED, "Cancelled")

        @JvmField
        val ERRNO_MINOR_FAILURES: Errno = Errno(TYPE, Activity.RESULT_FIRST_USER, "Minor failure")

        @JvmField
        val ERRNO_FAILED: Errno = Errno(TYPE, Activity.RESULT_FIRST_USER + 1, "Failed")

        private const val LOG_TAG = "Errno"

        /**
         * Get the {@link Errno} of a specific type and code.
         *
         * @param type The unique type of the {@link Errno}.
         * @param code The unique code of the {@link Errno}.
         */
        @JvmStatic
        fun valueOf(type: String?, code: Int?): Errno? {
            if (type == null || type.isEmpty() || code == null)
                return null
            return map[type + ":" + code]
        }
    }
}
