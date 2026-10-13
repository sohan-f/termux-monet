package com.termux.shared.crash

import android.content.Context
import androidx.annotation.NonNull
import com.termux.shared.android.AndroidUtils
import com.termux.shared.errors.Error
import com.termux.shared.file.FileUtils
import com.termux.shared.logger.Logger
import com.termux.shared.markdown.MarkdownUtils
import java.nio.charset.Charset

/**
 * Catches uncaught exceptions and logs them.
 */
class CrashHandler private constructor(
    private val mContext: Context,
    private val mCrashHandlerClient: CrashHandlerClient,
    private val mDefaultUEH: Thread.UncaughtExceptionHandler,
    private val mIsDefaultHandler: Boolean
) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val LOG_TAG = "CrashUtils"

        /**
         * Set default uncaught crash handler for the app to {@link CrashHandler}.
         */
        @JvmStatic
        fun setDefaultCrashHandler(@NonNull context: Context, @NonNull crashHandlerClient: CrashHandlerClient) {
            if (Thread.getDefaultUncaughtExceptionHandler() !is CrashHandler) {
                Thread.setDefaultUncaughtExceptionHandler(CrashHandler(context, crashHandlerClient, true))
            }
        }

        /**
         * Set uncaught crash handler of current non-main thread to {@link CrashHandler}.
         */
        @JvmStatic
        fun setCrashHandler(@NonNull context: Context, @NonNull crashHandlerClient: CrashHandlerClient) {
            Thread.currentThread().uncaughtExceptionHandler = CrashHandler(context, crashHandlerClient, false)
        }

        /**
         * Get {@link CrashHandler} instance that can be set as uncaught crash handler of a non-main thread.
         */
        @JvmStatic
        fun getCrashHandler(@NonNull context: Context, @NonNull crashHandlerClient: CrashHandlerClient): CrashHandler {
            return CrashHandler(context, crashHandlerClient, false)
        }

        /**
         * Log a crash in the crash log file at path returned by {@link CrashHandlerClient#getCrashLogFilePath(Context)}.
         *
         * @param context The {@link Context} for operations.
         * @param crashHandlerClient The {@link CrashHandlerClient} implementation.
         * @param thread The {@link Thread} in which the crash happened.
         * @param throwable The {@link Throwable} thrown for the crash.
         */
        @JvmStatic
        fun logCrash(@NonNull context: Context, @NonNull crashHandlerClient: CrashHandlerClient, @NonNull thread: Thread, @NonNull throwable: Throwable) {
            Logger.logInfo(LOG_TAG, "logCrash() for " + thread + ": " + throwable.message)
            CrashHandler(context, crashHandlerClient, false).logCrash(thread, throwable)
        }
    }

    /**
     * Secondary constructor capturing the ambient default handler like the
     * Java version did in its constructor body.
     */
    private constructor(@NonNull context: Context, @NonNull crashHandlerClient: CrashHandlerClient, isDefaultHandler: Boolean) :
        this(context, crashHandlerClient, Thread.getDefaultUncaughtExceptionHandler(), isDefaultHandler)

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Logger.logInfo(LOG_TAG, "uncaughtException() for " + thread + ": " + throwable.message)
        logCrash(thread, throwable)
        // Don't stop the app if not on the main thread
        if (mIsDefaultHandler)
            mDefaultUEH.uncaughtException(thread, throwable)
    }

    fun logCrash(@NonNull thread: Thread, @NonNull throwable: Throwable) {
        if (!mCrashHandlerClient.onPreLogCrash(mContext, thread, throwable)) {
            logCrashToFile(mContext, mCrashHandlerClient, thread, throwable)
            mCrashHandlerClient.onPostLogCrash(mContext, thread, throwable)
        }
    }

    fun logCrashToFile(@NonNull context: Context, @NonNull crashHandlerClient: CrashHandlerClient, @NonNull thread: Thread, @NonNull throwable: Throwable) {
        val reportString = StringBuilder()
        reportString.append("## Crash Details\n")
        reportString.append("\n").append(MarkdownUtils.getSingleLineMarkdownStringEntry("Crash Thread", thread.toString(), "-"))
        reportString.append("\n").append(MarkdownUtils.getSingleLineMarkdownStringEntry("Crash Timestamp", AndroidUtils.getCurrentMilliSecondUTCTimeStamp(), "-"))
        reportString.append("\n\n").append(MarkdownUtils.getMultiLineMarkdownStringEntry("Crash Message", throwable.message, "-"))
        reportString.append("\n\n").append(Logger.getStackTracesMarkdownString("Stacktrace", Logger.getStackTracesStringArray(throwable)))
        val appInfoMarkdownString = crashHandlerClient.getAppInfoMarkdownString(context)
        if (!appInfoMarkdownString.isNullOrEmpty())
            reportString.append("\n\n").append(appInfoMarkdownString)
        reportString.append("\n\n").append(AndroidUtils.getDeviceInfoMarkdownString(context))
        // Log report string to logcat
        Logger.logError(reportString.toString())
        // Write report string to crash log file
        val error: Error? = FileUtils.writeTextToFile("crash log", crashHandlerClient.getCrashLogFilePath(context), Charset.defaultCharset(), reportString.toString(), false)
        if (error != null) {
            Logger.logErrorExtended(LOG_TAG, error.toString())
        }
    }

    interface CrashHandlerClient {

        /**
         * Called before {@link #logCrashToFile(Context, CrashHandlerClient, Thread, Throwable)} is called.
         *
         * @param context The {@link Context} passed to the {@link CrashHandler} constructor.
         * @param thread The {@link Thread} in which the crash happened.
         * @param throwable The {@link Throwable} thrown for the crash.
         * @return Should return {@code true} if crash has been handled and should not be logged,
         * otherwise {@code false}.
         */
        fun onPreLogCrash(context: Context?, thread: Thread?, throwable: Throwable?): Boolean

        /**
         * Called after {@link #logCrashToFile(Context, CrashHandlerClient, Thread, Throwable)} is called.
         *
         * @param context The {@link Context} passed to the {@link CrashHandler} constructor.
         * @param thread The {@link Thread} in which the crash happened.
         * @param throwable The {@link Throwable} thrown for the crash.
         */
        fun onPostLogCrash(context: Context?, thread: Thread?, throwable: Throwable?)

        /**
         * Get crash log file path.
         *
         * @param context The {@link Context} passed to the {@link CrashHandler} constructor.
         * @return Should return the crash log file path.
         */
        @NonNull
        fun getCrashLogFilePath(context: Context): String

        /**
         * Get app info markdown string to add to crash log.
         *
         * @param context The {@link Context} passed to the {@link CrashHandler} constructor.
         * @return Should return app info markdown string.
         */
        fun getAppInfoMarkdownString(context: Context): String?
    }
}
