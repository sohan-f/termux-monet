package com.termux.shared.termux.theme

import android.content.Context
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.termux.shared.termux.settings.properties.TermuxPropertyConstants
import com.termux.shared.termux.settings.properties.TermuxSharedProperties
import com.termux.shared.theme.NightMode

class TermuxThemeUtils {

    companion object {
        /**
         * Get the {@link TermuxPropertyConstants#KEY_NIGHT_MODE} value from the properties file on disk
         * and set it to app wide night mode value.
         */
        @JvmStatic
        fun setAppNightMode(@NonNull context: Context) {
            NightMode.setAppNightMode(TermuxSharedProperties.getNightMode(context))
        }

        /**
         * Set name as app wide night mode value.
         */
        @JvmStatic
        fun setAppNightMode(@Nullable name: String?) {
            NightMode.setAppNightMode(name)
        }
    }
}
