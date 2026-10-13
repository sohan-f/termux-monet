package com.termux.shared.termux.settings.properties

import android.content.Context
import androidx.annotation.NonNull
import com.termux.shared.termux.TermuxConstants

class TermuxAppSharedProperties private constructor(@NonNull context: Context) : TermuxSharedProperties(context, TermuxConstants.TERMUX_APP_NAME, TermuxConstants.TERMUX_PROPERTIES_FILE_PATHS_LIST, TermuxPropertyConstants.TERMUX_APP_PROPERTIES_LIST, SharedPropertiesParserClient()) {

    companion object {
        private var properties: TermuxAppSharedProperties? = null

        /**
         * Initialize the {@link #properties} and load properties from disk.
         *
         * @param context The {@link Context} for operations.
         * @return Returns the {@link TermuxAppSharedProperties}.
         */
        @JvmStatic
        fun init(@NonNull context: Context): TermuxAppSharedProperties {
            if (properties == null)
                properties = TermuxAppSharedProperties(context)
            return properties!!
        }

        /**
         * Get the {@link #properties}.
         *
         * @return Returns the {@link TermuxAppSharedProperties}.
         */
        @JvmStatic
        fun getProperties(): TermuxAppSharedProperties? {
            return properties
        }
    }
}
