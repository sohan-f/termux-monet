package com.termux.terminal

import android.util.Log
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter

class Logger {

    companion object {
        // android.util.Log takes a nullable tag but a non-null message (null message throws
        // NPE inside println_native), so `orEmpty()`/`!!` preserve the Java pass-through behaviour.
        @JvmStatic
        fun logError(client: TerminalSessionClient?, logTag: String?, message: String?) {
            if (client != null)
                client.logError(logTag, message)
            else
                Log.e(logTag.orEmpty(), message!!)
        }

        @JvmStatic
        fun logWarn(client: TerminalSessionClient?, logTag: String?, message: String?) {
            if (client != null)
                client.logWarn(logTag, message)
            else
                Log.w(logTag.orEmpty(), message!!)
        }

        @JvmStatic
        fun logInfo(client: TerminalSessionClient?, logTag: String?, message: String?) {
            if (client != null)
                client.logInfo(logTag, message)
            else
                Log.i(logTag.orEmpty(), message!!)
        }

        @JvmStatic
        fun logDebug(client: TerminalSessionClient?, logTag: String?, message: String?) {
            if (client != null)
                client.logDebug(logTag, message)
            else
                Log.d(logTag.orEmpty(), message!!)
        }

        @JvmStatic
        fun logVerbose(client: TerminalSessionClient?, logTag: String?, message: String?) {
            if (client != null)
                client.logVerbose(logTag, message)
            else
                Log.v(logTag.orEmpty(), message!!)
        }

        @JvmStatic
        fun logStackTraceWithMessage(client: TerminalSessionClient?, tag: String?, message: String?, throwable: Throwable?) {
            logError(client, tag, getMessageAndStackTraceString(message, throwable))
        }

        @JvmStatic
        fun getMessageAndStackTraceString(message: String?, throwable: Throwable?): String? {
            if (message == null && throwable == null)
                return null
            else if (message != null && throwable != null)
                return message + ":\n" + getStackTraceString(throwable)
            else if (throwable == null)
                return message
            else
                return getStackTraceString(throwable)
        }

        @JvmStatic
        fun getStackTraceString(throwable: Throwable?): String? {
            if (throwable == null)
                return null
            var stackTraceString: String? = null
            try {
                val errors = StringWriter()
                val pw = PrintWriter(errors)
                throwable.printStackTrace(pw)
                pw.close()
                stackTraceString = errors.toString()
                errors.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return stackTraceString
        }
    }
}
