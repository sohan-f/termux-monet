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
import com.termux.shared.termux.settings.preferences.TermuxPreferenceConstants.TERMUX_GUI_APP

class TermuxGUIAppSharedPreferences private constructor(@NonNull context: Context) : AppSharedPreferences(context, SharedPreferenceUtils.getPrivateSharedPreferences(context, TermuxConstants.TERMUX_GUI_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION), SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(context, TermuxConstants.TERMUX_GUI_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION)) {

    companion object {
        private const val LOG_TAG = "TermuxGUIAppSharedPreferences"

        /**
         * Get {@link TermuxGUIAppSharedPreferences}.
         *
         * @param context The {@link Context} to use to get the {@link Context} of the
         *                {@link TermuxConstants#TERMUX_GUI_PACKAGE_NAME}.
         * @return Returns the {@link TermuxGUIAppSharedPreferences}. This will {@code null} if an exception is raised.
         */
        @Nullable
        @JvmStatic
        fun build(@NonNull context: Context): TermuxGUIAppSharedPreferences? {
            val termuxGUIPackageContext = PackageUtils.getContextForPackage(context, TermuxConstants.TERMUX_GUI_PACKAGE_NAME)
                ?: return null
            return TermuxGUIAppSharedPreferences(termuxGUIPackageContext)
        }

        /**
         * Get {@link TermuxGUIAppSharedPreferences}.
         *
         * @param context The {@link Context} to use to get the {@link Context} of the
         *                {@link TermuxConstants#TERMUX_GUI_PACKAGE_NAME}.
         * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
         *                       be shown which when dismissed will exit the app.
         * @return Returns the {@link TermuxGUIAppSharedPreferences}. This will {@code null} if an exception is raised.
         */
        @JvmStatic
        fun build(@NonNull context: Context, exitAppOnError: Boolean): TermuxGUIAppSharedPreferences? {
            val termuxGUIPackageContext = TermuxUtils.getContextForPackageOrExitApp(context, TermuxConstants.TERMUX_GUI_PACKAGE_NAME, exitAppOnError)
                ?: return null
            return TermuxGUIAppSharedPreferences(termuxGUIPackageContext)
        }
    }

    fun getLogLevel(readFromFile: Boolean): Int {
        return if (readFromFile)
            SharedPreferenceUtils.getInt(mMultiProcessSharedPreferences, TERMUX_GUI_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL)
        else
            SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_GUI_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL)
    }

    fun setLogLevel(context: Context?, logLevel: Int, commitToFile: Boolean) {
        val newLogLevel = Logger.setLogLevel(context, logLevel)
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_GUI_APP.KEY_LOG_LEVEL, newLogLevel, commitToFile)
    }
}
