package com.termux.shared.termux.extrakeys

import androidx.annotation.NonNull

/**
 * The {@link Class} that implements special buttons for {@link ExtraKeysView}.
 *
 * @param key The unique key name for the special button. The key is registered in {@link #map}
 *            with which the {@link SpecialButton} can be retrieved via a call to
 *            {@link #valueOf(String)}.
 */
class SpecialButton(private val key: String) {

    companion object {
        private val map = HashMap<String, SpecialButton>()

        @JvmField
        val CTRL = SpecialButton("CTRL")

        @JvmField
        val ALT = SpecialButton("ALT")

        @JvmField
        val SHIFT = SpecialButton("SHIFT")

        @JvmField
        val FN = SpecialButton("FN")

        /**
         * Get the {@link SpecialButton} for {@code key}.
         *
         * @param key The unique key name for the special button.
         */
        @JvmStatic
        fun valueOf(key: String?): SpecialButton? {
            return map[key]
        }
    }

    init {
        // Public constructor self-registers like the Java version, so a
        // second instance with the same key overwrites the first in map.
        map[key] = this
    }

    /**
     * Get key for this {@link SpecialButton}.
     */
    fun getKey(): String {
        return key
    }

    @NonNull
    override fun toString(): String {
        return key
    }
}
