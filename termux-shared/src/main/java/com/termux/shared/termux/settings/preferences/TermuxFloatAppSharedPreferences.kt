package com.termux.shared.termux.settings.preferences

import android.content.Context
import android.os.Build
import android.view.Display
import android.view.WindowManager
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.termux.shared.android.PackageUtils
import com.termux.shared.data.DataUtils
import com.termux.shared.logger.Logger
import com.termux.shared.settings.preferences.AppSharedPreferences
import com.termux.shared.settings.preferences.SharedPreferenceUtils
import com.termux.shared.termux.TermuxConstants
import com.termux.shared.termux.TermuxUtils
import com.termux.shared.termux.settings.preferences.TermuxPreferenceConstants.TERMUX_FLOAT_APP

class TermuxFloatAppSharedPreferences private constructor(@NonNull context: Context) : AppSharedPreferences(context, SharedPreferenceUtils.getPrivateSharedPreferences(context, TermuxConstants.TERMUX_FLOAT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION), SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(context, TermuxConstants.TERMUX_FLOAT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION)) {

    private var MIN_FONTSIZE = 0

    private var MAX_FONTSIZE = 0

    private var DEFAULT_FONTSIZE = 0

    init {
        setFontVariables(context)
    }

    companion object {
        private const val LOG_TAG = "TermuxFloatAppSharedPreferences"

        /**
         * Get {@link TermuxFloatAppSharedPreferences}.
         *
         * @param context The {@link Context} to use to get the {@link Context} of the
         *                {@link TermuxConstants#TERMUX_FLOAT_PACKAGE_NAME}.
         * @return Returns the {@link TermuxFloatAppSharedPreferences}. This will {@code null} if an exception is raised.
         */
        @Nullable
        @JvmStatic
        fun build(@NonNull context: Context): TermuxFloatAppSharedPreferences? {
            val termuxFloatPackageContext = PackageUtils.getContextForPackage(context, TermuxConstants.TERMUX_FLOAT_PACKAGE_NAME)
                ?: return null
            return TermuxFloatAppSharedPreferences(termuxFloatPackageContext)
        }

        /**
         * Get {@link TermuxFloatAppSharedPreferences}.
         *
         * @param context The {@link Context} to use to get the {@link Context} of the
         *                {@link TermuxConstants#TERMUX_FLOAT_PACKAGE_NAME}.
         * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
         *                       be shown which when dismissed will exit the app.
         * @return Returns the {@link TermuxFloatAppSharedPreferences}. This will {@code null} if an exception is raised.
         */
        @JvmStatic
        fun build(@NonNull context: Context, exitAppOnError: Boolean): TermuxFloatAppSharedPreferences? {
            val termuxFloatPackageContext = TermuxUtils.getContextForPackageOrExitApp(context, TermuxConstants.TERMUX_FLOAT_PACKAGE_NAME, exitAppOnError)
                ?: return null
            return TermuxFloatAppSharedPreferences(termuxFloatPackageContext)
        }
    }

    fun getWindowX(): Int {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_FLOAT_APP.KEY_WINDOW_X, 200)
    }

    fun setWindowX(value: Int) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_FLOAT_APP.KEY_WINDOW_X, value, false)
    }

    fun getWindowY(): Int {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_FLOAT_APP.KEY_WINDOW_Y, 200)
    }

    fun setWindowY(value: Int) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_FLOAT_APP.KEY_WINDOW_Y, value, false)
    }

    fun getWindowWidth(): Int {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_FLOAT_APP.KEY_WINDOW_WIDTH, 500)
    }

    fun setWindowWidth(value: Int) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_FLOAT_APP.KEY_WINDOW_WIDTH, value, false)
    }

    fun getWindowHeight(): Int {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_FLOAT_APP.KEY_WINDOW_HEIGHT, 500)
    }

    fun setWindowHeight(value: Int) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_FLOAT_APP.KEY_WINDOW_HEIGHT, value, false)
    }

    fun setFontVariables(context: Context?) {
        val sizes = TermuxAppSharedPreferences.getDefaultFontSizes(context)
        DEFAULT_FONTSIZE = sizes[0]
        MIN_FONTSIZE = sizes[1]
        MAX_FONTSIZE = sizes[2]
    }

    @Suppress("DEPRECATION") // getDefaultDisplay must stay: the pre-R fallback path
    private fun getDisplayIdAsString(): String {
        val context = getContext()
        val display: Display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            // See TermuxAppSharedPreferences.getDisplayIdAsString for the cast rationale.
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        }
        val d = display.displayId
        return if (d == Display.DEFAULT_DISPLAY)
            ""
        else
            d.toString()
    }

    fun getFontSize(): Int {
        val fontSize = SharedPreferenceUtils.getIntStoredAsString(mSharedPreferences, TERMUX_FLOAT_APP.KEY_FONTSIZE + getDisplayIdAsString(), DEFAULT_FONTSIZE)
        return DataUtils.clamp(fontSize, MIN_FONTSIZE, MAX_FONTSIZE)
    }

    fun setFontSize(value: Int) {
        SharedPreferenceUtils.setIntStoredAsString(mSharedPreferences, TERMUX_FLOAT_APP.KEY_FONTSIZE + getDisplayIdAsString(), value, false)
    }

    fun changeFontSize(increase: Boolean) {
        var fontSize = getFontSize()
        fontSize += (if (increase) 1 else -1) * 2
        fontSize = Math.max(MIN_FONTSIZE, Math.min(fontSize, MAX_FONTSIZE))
        setFontSize(fontSize)
    }

    fun getLogLevel(readFromFile: Boolean): Int {
        return if (readFromFile)
            SharedPreferenceUtils.getInt(mMultiProcessSharedPreferences, TERMUX_FLOAT_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL)
        else
            SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_FLOAT_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL)
    }

    fun setLogLevel(context: Context?, logLevel: Int, commitToFile: Boolean) {
        val newLogLevel = Logger.setLogLevel(context, logLevel)
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_FLOAT_APP.KEY_LOG_LEVEL, newLogLevel, commitToFile)
    }

    fun isTerminalViewKeyLoggingEnabled(readFromFile: Boolean): Boolean {
        return if (readFromFile)
            SharedPreferenceUtils.getBoolean(mMultiProcessSharedPreferences, TERMUX_FLOAT_APP.KEY_TERMINAL_VIEW_KEY_LOGGING_ENABLED, TERMUX_FLOAT_APP.DEFAULT_VALUE_TERMINAL_VIEW_KEY_LOGGING_ENABLED)
        else
            SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_FLOAT_APP.KEY_TERMINAL_VIEW_KEY_LOGGING_ENABLED, TERMUX_FLOAT_APP.DEFAULT_VALUE_TERMINAL_VIEW_KEY_LOGGING_ENABLED)
    }

    fun setTerminalViewKeyLoggingEnabled(value: Boolean, commitToFile: Boolean) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_FLOAT_APP.KEY_TERMINAL_VIEW_KEY_LOGGING_ENABLED, value, commitToFile)
    }
}
