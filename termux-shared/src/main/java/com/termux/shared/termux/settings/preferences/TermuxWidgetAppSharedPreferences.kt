package com.termux.shared.termux.settings.preferences

import android.content.Context
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.termux.shared.android.PackageUtils
import com.termux.shared.logger.Logger
import com.termux.shared.settings.preferences.AppSharedPreferences
import com.termux.shared.settings.preferences.SharedPreferenceUtils
import com.termux.shared.termux.TermuxConstants
import com.termux.shared.termux.TermuxUtils
import com.termux.shared.termux.settings.preferences.TermuxPreferenceConstants.TERMUX_WIDGET_APP
import java.util.UUID

class TermuxWidgetAppSharedPreferences private constructor(@NonNull context: Context) : AppSharedPreferences(context, SharedPreferenceUtils.getPrivateSharedPreferences(context, TermuxConstants.TERMUX_WIDGET_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION), SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(context, TermuxConstants.TERMUX_WIDGET_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION)) {

    companion object {
        private const val LOG_TAG = "TermuxWidgetAppSharedPreferences"

        /**
         * Get {@link TermuxWidgetAppSharedPreferences}.
         *
         * @param context The {@link Context} to use to get the {@link Context} of the
         *                {@link TermuxConstants#TERMUX_WIDGET_PACKAGE_NAME}.
         * @return Returns the {@link TermuxWidgetAppSharedPreferences}. This will {@code null} if an exception is raised.
         */
        @Nullable
        @JvmStatic
        fun build(@NonNull context: Context): TermuxWidgetAppSharedPreferences? {
            val termuxWidgetPackageContext = PackageUtils.getContextForPackage(context, TermuxConstants.TERMUX_WIDGET_PACKAGE_NAME)
                ?: return null
            return TermuxWidgetAppSharedPreferences(termuxWidgetPackageContext)
        }

        /**
         * Get the {@link TermuxWidgetAppSharedPreferences}.
         *
         * @param context The {@link Context} to use to get the {@link Context} of the
         *                {@link TermuxConstants#TERMUX_WIDGET_PACKAGE_NAME}.
         * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
         *                       be shown which when dismissed will exit the app.
         * @return Returns the {@link TermuxWidgetAppSharedPreferences}. This will {@code null} if an exception is raised.
         */
        @JvmStatic
        fun build(@NonNull context: Context, exitAppOnError: Boolean): TermuxWidgetAppSharedPreferences? {
            val termuxWidgetPackageContext = TermuxUtils.getContextForPackageOrExitApp(context, TermuxConstants.TERMUX_WIDGET_PACKAGE_NAME, exitAppOnError)
                ?: return null
            return TermuxWidgetAppSharedPreferences(termuxWidgetPackageContext)
        }

        @JvmStatic
        fun getGeneratedToken(@NonNull context: Context): String? {
            val preferences = build(context, true)
                ?: return null
            return preferences.getGeneratedToken()
        }
    }

    fun getGeneratedToken(): String {
        var token = SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_WIDGET_APP.KEY_TOKEN, null, true)
        if (token == null) {
            token = UUID.randomUUID().toString()
            SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_WIDGET_APP.KEY_TOKEN, token, true)
        }
        return token
    }

    fun getLogLevel(readFromFile: Boolean): Int {
        return if (readFromFile)
            SharedPreferenceUtils.getInt(mMultiProcessSharedPreferences, TERMUX_WIDGET_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL)
        else
            SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_WIDGET_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL)
    }

    fun setLogLevel(context: Context?, logLevel: Int, commitToFile: Boolean) {
        val newLogLevel = Logger.setLogLevel(context, logLevel)
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_WIDGET_APP.KEY_LOG_LEVEL, newLogLevel, commitToFile)
    }
}
