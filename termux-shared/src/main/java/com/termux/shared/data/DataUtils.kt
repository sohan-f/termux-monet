package com.termux.shared.data

import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.google.common.base.Strings
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.Serializable

class DataUtils {

    companion object {
        /**
         * Max safe limit of data size to prevent TransactionTooLargeException when transferring data
         * inside or to other apps via transactions.
         */
        // 100KB
        const val TRANSACTION_SIZE_LIMIT_IN_BYTES = 100 * 1024

        private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()

        @JvmStatic
        fun getTruncatedCommandOutput(text: String?, maxLength: Int, fromEnd: Boolean, onNewline: Boolean, addPrefix: Boolean): String? {
            if (text == null)
                return null
            val prefix = "(truncated) "
            var maxLen = maxLength
            if (addPrefix)
                maxLen -= prefix.length
            if (maxLen < 0 || text.length < maxLen)
                return text
            var result = text
            if (fromEnd) {
                result = text.substring(0, maxLen)
            } else {
                var cutOffIndex = text.length - maxLen
                if (onNewline) {
                    val nextNewlineIndex = text.indexOf('\n', cutOffIndex)
                    if (nextNewlineIndex != -1 && nextNewlineIndex != text.length - 1) {
                        cutOffIndex = nextNewlineIndex + 1
                    }
                }
                result = text.substring(cutOffIndex)
            }
            if (addPrefix)
                result = prefix + result
            return result
        }

        /**
         * Replace a sub string in each item of a {@link String[]}.
         *
         * @param array The {@link String[]} to replace in.
         * @param find The sub string to replace.
         * @param replace The sub string to replace with.
         */
        @JvmStatic
        fun replaceSubStringsInStringArrayItems(array: Array<String>?, find: String?, replace: String?) {
            if (array == null || array.isEmpty())
                return
            for (i in array.indices) {
                // Nulls throw NPE here exactly like the Java version.
                array[i] = array[i].replace(find!!, replace!!)
            }
        }

        /**
         * Get the {@code float} from a {@link String}.
         *
         * @param value The {@link String} value.
         * @param def The default value if failed to read a valid value.
         * @return Returns the {@code float} value after parsing the {@link String} value, otherwise
         * returns default if failed to read a valid value, like in case of an exception.
         */
        @JvmStatic
        fun getFloatFromString(value: String?, def: Float): Float {
            if (value == null)
                return def
            return try {
                value.toFloat()
            } catch (e: Exception) {
                def
            }
        }

        /**
         * Get the {@code int} from a {@link String}.
         *
         * @param value The {@link String} value.
         * @param def The default value if failed to read a valid value.
         * @return Returns the {@code int} value after parsing the {@link String} value stored in
         * {@link Bundle}, otherwise returns default if failed to read a valid value,
         * like in case of an exception.
         */
        @JvmStatic
        fun getIntFromString(value: String?, def: Int): Int {
            if (value == null)
                return def
            return try {
                value.toInt()
            } catch (e: Exception) {
                def
            }
        }

        /**
         * Get the {@link String} from an {@link Integer}.
         *
         * @param value The {@link Integer} value.
         * @param def The default {@link String} value.
         * @return Returns {@code value} if it is not {@code null}, otherwise returns {@code def}.
         */
        @JvmStatic
        fun getStringFromInteger(value: Int?, def: String?): String? {
            return if (value == null) def else value.toString()
        }

        /**
         * Get the {@code hex string} from a {@link byte[]}.
         *
         * @param bytes The {@link byte[]} value.
         * @return Returns the {@code hex string} value.
         */
        @JvmStatic
        fun bytesToHex(bytes: ByteArray): String {
            val hexChars = CharArray(bytes.size * 2)
            for (j in bytes.indices) {
                val v = bytes[j].toInt() and 0xFF
                hexChars[j * 2] = HEX_ARRAY[v ushr 4]
                hexChars[j * 2 + 1] = HEX_ARRAY[v and 0x0F]
            }
            return String(hexChars)
        }

        /**
         * Get an {@code int} from {@link Bundle} that is stored as a {@link String}.
         *
         * @param bundle The {@link Bundle} to get the value from.
         * @param key The key for the value.
         * @param def The default value if failed to read a valid value.
         * @return Returns the {@code int} value after parsing the {@link String} value stored in
         * {@link Bundle}, otherwise returns default if failed to read a valid value,
         * like in case of an exception.
         */
        @JvmStatic
        fun getIntStoredAsStringFromBundle(bundle: Bundle?, key: String?, def: Int): Int {
            if (bundle == null)
                return def
            return getIntFromString(bundle.getString(key, def.toString()), def)
        }

        /**
         * If value is not in the range [min, max], set it to either min or max.
         */
        @JvmStatic
        fun clamp(value: Int, min: Int, max: Int): Int {
            // Kept as Math calls: coerceIn differs on NaN/edge semantics.
            return Math.min(Math.max(value, min), max)
        }

        /**
         * If value is not in the range [min, max], set it to default.
         */
        @JvmStatic
        fun rangedOrDefault(value: Float, def: Float, min: Float, max: Float): Float {
            return if (value < min || value > max)
                def
            else
                value
        }

        /**
         * Add a space indent to a {@link String}. Each indent is 4 space characters long.
         *
         * @param string The {@link String} to add indent to.
         * @param count The indent count.
         * @return Returns the indented {@link String}.
         */
        @JvmStatic
        fun getSpaceIndentedString(string: String?, count: Int): String? {
            return if (string.isNullOrEmpty())
                string
            else
                getIndentedString(string, "    ", count)
        }

        /**
         * Add a tab indent to a {@link String}. Each indent is 1 tab character long.
         *
         * @param string The {@link String} to add indent to.
         * @param count The indent count.
         * @return Returns the indented {@link String}.
         */
        @JvmStatic
        fun getTabIndentedString(string: String?, count: Int): String? {
            return if (string.isNullOrEmpty())
                string
            else
                getIndentedString(string, "\t", count)
        }

        /**
         * Add an indent to a {@link String}.
         *
         * @param string The {@link String} to add indent to.
         * @param indent The indent characters.
         * @param count The indent count.
         * @return Returns the indented {@link String}.
         */
        @JvmStatic
        fun getIndentedString(string: String?, @NonNull indent: String, count: Int): String? {
            return if (string.isNullOrEmpty())
                string
            else
                string.replace(Regex("(?m)^"), Strings.repeat(indent, Math.max(count, 1)))
        }

        /**
         * Get the object itself if it is not {@code null}, otherwise default.
         *
         * @param obj The {@link Object} to check.
         * @param def The default {@link Object}.
         * @return Returns {@code obj} if it is not {@code null}, otherwise returns {@code def}.
         */
        @JvmStatic
        fun <T> getDefaultIfNull(@Nullable obj: T, @Nullable def: T): T {
            return obj ?: def
        }

        /**
         * Get the {@link String} itself if it is not {@code null} or empty, otherwise default.
         *
         * @param value The {@link String} to check.
         * @param def The default {@link String}.
         * @return Returns {@code value} if it is not {@code null} or empty, otherwise returns {@code def}.
         */
        @JvmStatic
        fun getDefaultIfUnset(@Nullable value: String?, def: String?): String? {
            return if (value.isNullOrEmpty()) def else value
        }

        /**
         * Check if a string is null or empty.
         */
        @JvmStatic
        fun isNullOrEmpty(string: String?): Boolean {
            return string.isNullOrEmpty()
        }

        /**
         * Get size of a serializable object.
         */
        @JvmStatic
        fun getSerializedSize(obj: Serializable?): Long {
            if (obj == null)
                return 0
            return try {
                val byteOutputStream = ByteArrayOutputStream()
                val objectOutputStream = ObjectOutputStream(byteOutputStream)
                objectOutputStream.writeObject(obj)
                objectOutputStream.flush()
                objectOutputStream.close()
                byteOutputStream.toByteArray().size.toLong()
            } catch (e: Exception) {
                -1
            }
        }

        /**
         * Wrapper for {@link #getIntColorFromString(String, int, boolean)} with `setAlpha` `false`.
         */
        @JvmStatic
        fun getIntColorFromString(value: String?, def: Int): Int {
            if (value == null)
                return def
            return try {
                Color.parseColor(value)
            } catch (e: Exception) {
                def
            }
        }

        /**
         * Get an {@code int} color from {@link String} with alpha value change. If {@code setAlpha}
         * is {@code true} and given value is missing alpha then set it using def alpha.
         *
         * @param value    The {@link String} value.
         * @param def      The default value if failed to read a valid value.
         * @param setAlpha The {@code boolean} value that decides whether to set alpha or not.
         * @return Returns the {@code int} color value after parsing the {@link String}
         * value, otherwise returns default value.
         */
        @JvmStatic
        fun getIntColorFromString(value: String?, def: Int, setAlpha: Boolean): Int {
            if (value == null)
                return def
            return try {
                var color = Color.parseColor(value)
                if (setAlpha && value.length == 7) {
                    // Use alpha value of `def` color and rgb value of given `value`.
                    // 0xff000000 overflows Int, so convert from Long like the
                    // Java wraparound.
                    color = (def and 0xff000000.toInt()) or (color and 0x00ffffff)
                }
                color
            } catch (e: Exception) {
                def
            }
        }

        /**
         * Exchanges the value of x and y in {@link Point}.
         *
         * @param point The original source point to swap.
         * @return Returns new swaped point.
         */
        @JvmStatic
        fun swap(point: Point): Point {
            return Point(point.y, point.x)
        }
    }
}
