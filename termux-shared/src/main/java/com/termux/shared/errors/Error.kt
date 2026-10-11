package com.termux.shared.errors

import android.content.Context
import androidx.annotation.NonNull
import com.termux.shared.logger.Logger
import com.termux.shared.markdown.MarkdownUtils
import java.io.Serializable
import java.util.ArrayList
import java.util.Collections

class Error : Serializable {

    /**
     * The optional error label.
     */
    private var _label: String? = null

    /**
     * The error type.
     */
    private var _type: String? = null

    /**
     * The error code.
     */
    private var _code: Int = 0

    /**
     * The error message.
     */
    private var _message: String? = null

    /**
     * The error exceptions.
     */
    private var _throwablesList: List<Throwable>? = ArrayList()

    constructor() {
        initError(null, null, null, null)
    }

    constructor(type: String?, code: Int?, message: String?, throwablesList: List<Throwable>?) {
        initError(type, code, message, throwablesList)
    }

    constructor(type: String?, code: Int?, message: String?, throwable: Throwable?) {
        initError(type, code, message, Collections.singletonList(throwable))
    }

    constructor(type: String?, code: Int?, message: String?) {
        initError(type, code, message, null)
    }

    constructor(code: Int?, message: String?, throwablesList: List<Throwable>?) {
        initError(null, code, message, throwablesList)
    }

    constructor(code: Int?, message: String?, throwable: Throwable?) {
        initError(null, code, message, Collections.singletonList(throwable))
    }

    constructor(code: Int?, message: String?) {
        initError(null, code, message, null)
    }

    constructor(message: String?, throwable: Throwable?) {
        initError(null, null, message, Collections.singletonList(throwable))
    }

    constructor(message: String?, throwablesList: List<Throwable>?) {
        initError(null, null, message, throwablesList)
    }

    constructor(message: String?) {
        initError(null, null, message, null)
    }

    private fun initError(type: String?, code: Int?, message: String?, throwablesList: List<Throwable>?) {
        _type = if (type != null && type.isNotEmpty())
            type
        else
            Errno.TYPE
        _code = if (code != null && code > Errno.ERRNO_SUCCESS.getCode())
            code
        else
            Errno.ERRNO_SUCCESS.getCode()
        _message = message
        if (throwablesList != null)
            _throwablesList = throwablesList
    }

    fun setLabel(label: String?): Error {
        _label = label
        return this
    }

    fun getLabel(): String? {
        return _label
    }

    fun getType(): String? {
        return _type
    }

    fun getCode(): Int? {
        return _code
    }

    fun getMessage(): String? {
        return _message
    }

    fun prependMessage(message: String?) {
        if (message != null && isStateFailed())
            _message = message + _message
    }

    fun appendMessage(message: String?) {
        if (message != null && isStateFailed())
            _message = _message + message
    }

    fun getThrowablesList(): List<Throwable> {
        // _throwablesList is null only if setStateFailed() was passed a null
        // list, in which case the Java version threw NPE here as well.
        return Collections.unmodifiableList(_throwablesList!!)
    }

    @Synchronized
    fun setStateFailed(@NonNull error: Error): Boolean {
        return setStateFailed(error.getType(), error.getCode()!!, error.getMessage(), null)
    }

    @Synchronized
    fun setStateFailed(@NonNull error: Error, throwable: Throwable?): Boolean {
        return setStateFailed(error.getType(), error.getCode()!!, error.getMessage(), Collections.singletonList(throwable))
    }

    @Synchronized
    fun setStateFailed(@NonNull error: Error, throwablesList: List<Throwable>?): Boolean {
        return setStateFailed(error.getType(), error.getCode()!!, error.getMessage(), throwablesList)
    }

    @Synchronized
    fun setStateFailed(code: Int, message: String?): Boolean {
        return setStateFailed(_type, code, message, null)
    }

    @Synchronized
    fun setStateFailed(code: Int, message: String?, throwable: Throwable?): Boolean {
        return setStateFailed(_type, code, message, Collections.singletonList(throwable))
    }

    @Synchronized
    fun setStateFailed(code: Int, message: String?, throwablesList: List<Throwable>?): Boolean {
        return setStateFailed(_type, code, message, throwablesList)
    }

    @Synchronized
    fun setStateFailed(type: String?, code: Int, message: String?, throwablesList: List<Throwable>?): Boolean {
        _message = message
        _throwablesList = throwablesList
        if (type != null && type.isNotEmpty())
            _type = type
        return if (code > Errno.ERRNO_SUCCESS.getCode()) {
            _code = code
            true
        } else {
            Logger.logWarn(LOG_TAG, "Ignoring invalid error code value \"" + code + "\". Force setting it to RESULT_CODE_FAILED \"" + Errno.ERRNO_FAILED.getCode() + "\"")
            _code = Errno.ERRNO_FAILED.getCode()
            false
        }
    }

    fun isStateFailed(): Boolean {
        return _code > Errno.ERRNO_SUCCESS.getCode()
    }

    @NonNull
    override fun toString(): String {
        return getErrorLogString(this)
    }

    fun logErrorAndShowToast(context: Context?, logTag: String?) {
        Logger.logErrorExtended(logTag, getErrorLogString())
        Logger.showToast(context, getMinimalErrorLogString(), true)
    }

    fun getErrorLogString(): String {
        val logString = StringBuilder()
        logString.append(getCodeString())
        logString.append("\n").append(getTypeAndMessageLogString())
        if (_throwablesList != null && _throwablesList!!.isNotEmpty())
            logString.append("\n").append(geStackTracesLogString())
        return logString.toString()
    }

    fun getMinimalErrorLogString(): String {
        val logString = StringBuilder()
        logString.append(getCodeString())
        logString.append(getTypeAndMessageLogString())
        return logString.toString()
    }

    fun getMinimalErrorString(): String {
        val logString = StringBuilder()
        logString.append("(").append(getCode()).append(") ")
        logString.append(getType()).append(": ").append(getMessage())
        return logString.toString()
    }

    fun getErrorMarkdownString(): String {
        val markdownString = StringBuilder()
        markdownString.append(MarkdownUtils.getSingleLineMarkdownStringEntry("Error Code", getCode(), "-"))
        markdownString.append("\n").append(MarkdownUtils.getMultiLineMarkdownStringEntry(if (Errno.TYPE == getType()) "Error Message" else "Error Message (" + getType() + ")", _message, "-"))
        if (_throwablesList != null && _throwablesList!!.isNotEmpty())
            markdownString.append("\n\n").append(geStackTracesMarkdownString())
        return markdownString.toString()
    }

    fun getCodeString(): String {
        return Logger.getSingleLineLogStringEntry("Error Code", _code, "-")
    }

    fun getTypeAndMessageLogString(): String {
        return Logger.getMultiLineLogStringEntry(if (Errno.TYPE == _type) "Error Message" else "Error Message (" + _type + ")", _message, "-")
    }

    fun geStackTracesLogString(): String {
        return Logger.getStackTracesString("StackTraces:", Logger.getStackTracesStringArray(_throwablesList))
    }

    fun geStackTracesMarkdownString(): String {
        return Logger.getStackTracesMarkdownString("StackTraces", Logger.getStackTracesStringArray(_throwablesList))
    }

    companion object {
        private const val LOG_TAG = "Error"

        /**
         * Log the {@link Error} and show a toast for the minimal {@link String} for the {@link Error}.
         *
         * @param context The {@link Context} for operations.
         * @param logTag The log tag to use for logging.
         * @param error The {@link Error} to convert.
         */
        @JvmStatic
        fun logErrorAndShowToast(context: Context?, logTag: String?, error: Error?) {
            if (error == null)
                return
            error.logErrorAndShowToast(context, logTag)
        }

        /**
         * Get a log friendly {@link String} for {@link Error} error parameters.
         *
         * @param error The {@link Error} to convert.
         * @return Returns the log friendly {@link String}.
         */
        @JvmStatic
        fun getErrorLogString(error: Error?): String {
            if (error == null)
                return "null"
            return error.getErrorLogString()
        }

        /**
         * Get a minimal log friendly {@link String} for {@link Error} error parameters.
         *
         * @param error The {@link Error} to convert.
         * @return Returns the log friendly {@link String}.
         */
        @JvmStatic
        fun getMinimalErrorLogString(error: Error?): String {
            if (error == null)
                return "null"
            return error.getMinimalErrorLogString()
        }

        /**
         * Get a minimal {@link String} for {@link Error} error parameters.
         *
         * @param error The {@link Error} to convert.
         * @return Returns the {@link String}.
         */
        @JvmStatic
        fun getMinimalErrorString(error: Error?): String {
            if (error == null)
                return "null"
            return error.getMinimalErrorString()
        }

        /**
         * Get a markdown {@link String} for {@link Error}.
         *
         * @param error The {@link Error} to convert.
         * @return Returns the markdown {@link String}.
         */
        @JvmStatic
        fun getErrorMarkdownString(error: Error?): String {
            if (error == null)
                return "null"
            return error.getErrorMarkdownString()
        }
    }
}
