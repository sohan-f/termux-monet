package com.termux.shared.data

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.NonNull
import java.util.Arrays

class IntentUtils {

    companion object {
        private const val LOG_TAG = "IntentUtils"

        /**
         * Get a {@link String} extra from an {@link Intent} if its not {@code null} or empty.
         *
         * @param intent The {@link Intent} to get the extra from.
         * @param key The {@link String} key name.
         * @param def The default value if extra is not set.
         * @param throwExceptionIfNotSet If set to {@code true}, then an exception will be thrown if extra
         *                               is not set.
         * @return Returns the {@link String} extra if set, otherwise {@code null}.
         */
        @JvmStatic
        @Throws(Exception::class)
        fun getStringExtraIfSet(@NonNull intent: Intent, key: String?, def: String?, throwExceptionIfNotSet: Boolean): String? {
            val value = getStringExtraIfSet(intent, key, def)
            if (value == null && throwExceptionIfNotSet)
                throw Exception("The \"" + key + "\" key string value is null or empty")
            return value
        }

        /**
         * Get a {@link String} extra from an {@link Intent} if its not {@code null} or empty.
         *
         * @param intent The {@link Intent} to get the extra from.
         * @param key The {@link String} key name.
         * @param def The default value if extra is not set.
         * @return Returns the {@link String} extra if set, otherwise {@code null}.
         */
        @JvmStatic
        fun getStringExtraIfSet(@NonNull intent: Intent, key: String?, def: String?): String? {
            val value = intent.getStringExtra(key)
            if (value.isNullOrEmpty()) {
                if (!def.isNullOrEmpty())
                    return def
                else
                    return null
            }
            return value
        }

        /**
         * Get an {@link Integer} from an {@link Intent} stored as a {@link String} extra if its not
         * {@code null} or empty.
         *
         * @param intent The {@link Intent} to get the extra from.
         * @param key The {@link String} key name.
         * @param def The default value if extra is not set.
         * @return Returns the {@link Integer} extra if set, otherwise {@code null}.
         */
        @JvmStatic
        fun getIntegerExtraIfSet(@NonNull intent: Intent, key: String?, def: Int?): Int? {
            return try {
                val value = intent.getStringExtra(key)
                if (value.isNullOrEmpty()) {
                    def
                } else
                    value.toInt()
            } catch (e: Exception) {
                def
            }
        }

        /**
         * Get a {@link String[]} extra from an {@link Intent} if its not {@code null} or empty.
         *
         * @param intent The {@link Intent} to get the extra from.
         * @param key The {@link String} key name.
         * @param def The default value if extra is not set.
         * @param throwExceptionIfNotSet If set to {@code true}, then an exception will be thrown if extra
         *                               is not set.
         * @return Returns the {@link String[]} extra if set, otherwise {@code null}.
         */
        @JvmStatic
        @Throws(Exception::class)
        fun getStringArrayExtraIfSet(@NonNull intent: Intent, key: String?, def: Array<String?>?, throwExceptionIfNotSet: Boolean): Array<String?>? {
            val value = getStringArrayExtraIfSet(intent, key, def)
            if (value == null && throwExceptionIfNotSet)
                throw Exception("The \"" + key + "\" key string array is null or empty")
            return value
        }

        /**
         * Get a {@link String[]} extra from an {@link Intent} if its not {@code null} or empty.
         *
         * @param intent The {@link Intent} to get the extra from.
         * @param key The {@link String} key name.
         * @param def The default value if extra is not set.
         * @return Returns the {@link String[]} extra if set, otherwise {@code null}.
         */
        @JvmStatic
        fun getStringArrayExtraIfSet(intent: Intent, key: String?, def: Array<String?>?): Array<String?>? {
            val value: Array<String?>? = intent.getStringArrayExtra(key)
            if (value == null || value.isEmpty()) {
                if (def != null && def.isNotEmpty())
                    return def
                else
                    return null
            }
            return value
        }

        @JvmStatic
        fun getIntentString(intent: Intent?): String? {
            if (intent == null)
                return null
            return intent.toString() + "\n" + getBundleString(intent.extras)
        }

        @JvmStatic
        @Suppress("DEPRECATION") // Bundle.get must stay: typed getters would change semantics
        fun getBundleString(bundle: Bundle?): String {
            if (bundle == null || bundle.size() == 0)
                return "Bundle[]"
            val bundleString = StringBuilder("Bundle[\n")
            var first = true
            for (key in bundle.keySet()) {
                if (!first)
                    bundleString.append("\n")
                bundleString.append(key).append(": `")
                val value = bundle.get(key)
                if (value is IntArray) {
                    bundleString.append(Arrays.toString(value))
                } else if (value is ByteArray) {
                    bundleString.append(Arrays.toString(value))
                } else if (value is BooleanArray) {
                    bundleString.append(Arrays.toString(value))
                } else if (value is ShortArray) {
                    bundleString.append(Arrays.toString(value))
                } else if (value is LongArray) {
                    bundleString.append(Arrays.toString(value))
                } else if (value is FloatArray) {
                    bundleString.append(Arrays.toString(value))
                } else if (value is DoubleArray) {
                    bundleString.append(Arrays.toString(value))
                } else if (isStringCharSequenceOrParcelableArray(value)) {
                    // The three Java instanceof branches (String[], CharSequence[],
                    // Parcelable[]) all call the same Arrays.toString(Object[]),
                    // so they collapse into one check with identical output.
                    // Plain `is Array<String>` is an erased-type error in Kotlin.
                    bundleString.append(Arrays.toString(value as Array<*>))
                } else if (value is Bundle) {
                    bundleString.append(getBundleString(value))
                } else {
                    bundleString.append(value)
                }
                bundleString.append("`")
                first = false
            }
            bundleString.append("\n]")
            return bundleString.toString()
        }

        /**
         * Exact equivalent of the Java `instanceof String[] || instanceof
         * CharSequence[] || instanceof Parcelable[]` chain, including
         * covariance (e.g. a custom Parcelable[] matches).
         */
        private fun isStringCharSequenceOrParcelableArray(value: Any?): Boolean {
            return Array<String>::class.java.isInstance(value) ||
                Array<CharSequence>::class.java.isInstance(value) ||
                Array<Parcelable>::class.java.isInstance(value)
        }
    }
}
