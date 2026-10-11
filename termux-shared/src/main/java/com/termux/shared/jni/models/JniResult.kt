package com.termux.shared.jni.models

import androidx.annotation.Keep
import androidx.annotation.NonNull
import com.termux.shared.logger.Logger

/**
 * A class that can be used to return result for JNI calls with support for multiple fields to easily
 * return success and error states.
 *
 * https://docs.oracle.com/javase/7/docs/technotes/guides/jni/spec/functions.html
 * https://developer.android.com/training/articles/perf-jni
 */
@Keep
class JniResult {

    /**
     * The return value for the JNI call.
     * This should be 0 for success.
     */
    @JvmField
    var retval: Int = 0

    /**
     * The errno value for any failed native system or library calls if {@link #retval} does not equal 0.
     * This should be 0 if no errno was set.
     *
     * https://manpages.debian.org/testing/manpages-dev/errno.3.en.html
     */
    @JvmField
    var errno: Int = 0

    /**
     * The error message for the failure if {@link #retval} does not equal 0.
     * The message will contain errno message returned by strerror() if errno was set.
     *
     * https://manpages.debian.org/testing/manpages-dev/strerror.3.en.html
     */
    @JvmField
    var errmsg: String? = null

    /**
     * Optional additional int data that needs to be returned by JNI call, like bytes read on success.
     */
    @JvmField
    var intData: Int = 0

    /**
     * Create an new instance of {@link JniResult}.
     *
     * @param retval The {@link #retval} value.
     * @param errno The {@link #errno} value.
     * @param errmsg The {@link #errmsg} value.
     */
    constructor(retval: Int, errno: Int, errmsg: String?) {
        this.retval = retval
        this.errno = errno
        this.errmsg = errmsg
    }

    /**
     * Create an new instance of {@link JniResult}.
     *
     * Note: the exact JVM signature {@code (IILjava/lang/String;I)V} is looked
     * up by name from native code ({@code local-socket.cpp#getJniResult}) and
     * must be preserved.
     *
     * @param retval The {@link #retval} value.
     * @param errno The {@link #errno} value.
     * @param errmsg The {@link #errmsg} value.
     * @param intData The {@link #intData} value.
     */
    constructor(retval: Int, errno: Int, errmsg: String?, intData: Int) : this(retval, errno, errmsg) {
        this.intData = intData
    }

    /**
     * Create an new instance of {@link JniResult} from a {@link Throwable} with {@link #retval} -1.
     *
     * @param message The error message.
     * @param throwable The {@link Throwable} value.
     */
    constructor(message: String?, throwable: Throwable?) : this(-1, 0, Logger.getMessageAndStackTraceString(message, throwable))

    /**
     * Get error {@link String} for {@link JniResult}.
     */
    @NonNull
    fun getErrorString(): String {
        val logString = StringBuilder()
        logString.append(Logger.getSingleLineLogStringEntry("Retval", retval, "-"))
        if (errno != 0)
            logString.append("\n").append(Logger.getSingleLineLogStringEntry("Errno", errno, "-"))
        if (!errmsg.isNullOrEmpty())
            logString.append("\n").append(Logger.getMultiLineLogStringEntry("Errmsg", errmsg, "-"))
        return logString.toString()
    }

    companion object {
        /**
         * Get error {@link String} for {@link JniResult}.
         *
         * @param result The {@link JniResult} to get error from.
         * @return Returns the error {@link String}.
         */
        @JvmStatic
        @NonNull
        fun getErrorString(result: JniResult?): String {
            if (result == null)
                return "null"
            return result.getErrorString()
        }
    }
}
