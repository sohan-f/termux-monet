package com.termux.shared.theme

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatDelegate
import com.termux.shared.logger.Logger

/**
 * The modes used by to decide night mode for themes.
 */
enum class NightMode(displayName: String, modeValue: Int) {

    /**
     * Night theme should be enabled.
     */
    TRUE("true", AppCompatDelegate.MODE_NIGHT_YES),

    /**
     * Dark theme should be enabled.
     */
    FALSE("false", AppCompatDelegate.MODE_NIGHT_NO),

    /**
     * Use night or dark theme depending on system night mode.
     * https://developer.android.com/guide/topics/resources/providing-resources#NightQualifier
     */
    SYSTEM("system", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

    private val displayName: String = displayName

    private val modeValue: Int = modeValue

    fun getName(): String {
        return displayName
    }

    @AppCompatDelegate.NightMode
    fun getMode(): Int {
        return modeValue
    }

    companion object {
        private const val LOG_TAG = "NightMode"

        /**
         * The current app wide night mode used by various libraries. Defaults to {@link #SYSTEM}.
         * Stored in the companion object like the original static field so it is
         * shared by all entries instead of per-entry state.
         */
        private var appNightMode: NightMode? = null

        /**
         * Get {@link NightMode} for {@code name} if found, otherwise {@code null}.
         */
        @Nullable
        @JvmStatic
        fun modeOf(name: String?): NightMode? {
            for (v in NightMode.values()) {
                if (v.displayName == name) {
                    return v
                }
            }
            return null
        }

        /**
         * Get {@link NightMode} for {@code name} if found, otherwise {@code def}.
         */
        @NonNull
        @JvmStatic
        fun modeOf(@Nullable name: String?, @NonNull def: NightMode): NightMode {
            val nightMode = modeOf(name)
            return nightMode ?: def
        }

        /**
         * Set the app night mode.
         */
        @JvmStatic
        fun setAppNightMode(@Nullable name: String?) {
            val modeToSet = if (name.isNullOrEmpty()) {
                SYSTEM
            } else {
                val nightMode = NightMode.modeOf(name)
                if (nightMode == null) {
                    Logger.logError(LOG_TAG, "Invalid APP_NIGHT_MODE \"" + name + "\"")
                    return
                }
                nightMode
            }
            appNightMode = modeToSet
            Logger.logVerbose(LOG_TAG, "Set APP_NIGHT_MODE to \"" + modeToSet.getName() + "\"")
        }

        /**
         * Get the app night mode.
         */
        @NonNull
        @JvmStatic
        fun getAppNightMode(): NightMode {
            var mode = appNightMode
            if (mode == null) {
                mode = SYSTEM
                appNightMode = mode
            }
            return mode
        }
    }
}
