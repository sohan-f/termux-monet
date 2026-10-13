package com.termux.shared.android

import android.content.Context
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.termux.shared.logger.Logger

class SettingsProviderUtils {

    companion object {
        private const val LOG_TAG = "SettingsProviderUtils"

        /**
         * Get settings key value from {@link SettingNamespace} namespace and of {@link SettingType} type.
         *
         * @param context The {@link Context} for operations.
         * @param namespace The {@link SettingNamespace} in which to get key value from.
         * @param type The {@link SettingType} for the key.
         * @param key The {@link String} name for key.
         * @param def The {@link Object} default value for key.
         * @return Returns the key value, or {@code def} if an exception is raised.
         */
        @Nullable
        @JvmStatic
        fun getSettingsValue(@NonNull context: Context, @NonNull namespace: SettingNamespace, @NonNull type: SettingType, key: String?, def: Any?): Any? {
            return try {
                when (namespace) {
                    SettingNamespace.GLOBAL -> when (type) {
                        SettingType.FLOAT -> android.provider.Settings.Global.getFloat(context.contentResolver, key)
                        SettingType.INT -> android.provider.Settings.Global.getInt(context.contentResolver, key)
                        SettingType.LONG -> android.provider.Settings.Global.getLong(context.contentResolver, key)
                        SettingType.STRING -> android.provider.Settings.Global.getString(context.contentResolver, key)
                        SettingType.URI -> android.provider.Settings.Global.getUriFor(key)
                    }
                    SettingNamespace.SECURE -> when (type) {
                        SettingType.FLOAT -> android.provider.Settings.Secure.getFloat(context.contentResolver, key)
                        SettingType.INT -> android.provider.Settings.Secure.getInt(context.contentResolver, key)
                        SettingType.LONG -> android.provider.Settings.Secure.getLong(context.contentResolver, key)
                        SettingType.STRING -> android.provider.Settings.Secure.getString(context.contentResolver, key)
                        SettingType.URI -> android.provider.Settings.Secure.getUriFor(key)
                    }
                    SettingNamespace.SYSTEM -> when (type) {
                        SettingType.FLOAT -> android.provider.Settings.System.getFloat(context.contentResolver, key)
                        SettingType.INT -> android.provider.Settings.System.getInt(context.contentResolver, key)
                        SettingType.LONG -> android.provider.Settings.System.getLong(context.contentResolver, key)
                        SettingType.STRING -> android.provider.Settings.System.getString(context.contentResolver, key)
                        SettingType.URI -> android.provider.Settings.System.getUriFor(key)
                    }
                }
            } catch (e: android.provider.Settings.SettingNotFoundException) {
                // Ignore
                def
            } catch (e: Exception) {
                Logger.logError(LOG_TAG, "Failed to get \"" + key + "\" key value from settings \"" + namespace.name + "\" namespace of type \"" + type.name + "\"")
                def
            }
        }
    }

    /**
     * The namespaces for {@link Settings} provider.
     */
    enum class SettingNamespace {

        /**
         * The {@link android.provider.Settings.Global} namespace
         */
        GLOBAL,
        /**
         * The {@link android.provider.Settings.Secure} namespace
         */
        SECURE,
        /**
         * The {@link android.provider.Settings.System} namespace
         */
        SYSTEM
    }

    /**
     * The type of values for {@link Settings} provider.
     */
    enum class SettingType {

        FLOAT, INT, LONG, STRING, URI
    }
}
