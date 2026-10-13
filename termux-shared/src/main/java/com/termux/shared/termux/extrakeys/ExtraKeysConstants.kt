package com.termux.shared.termux.extrakeys

import android.view.KeyEvent

class ExtraKeysConstants {

    companion object {
        /**
         * Defines the repetitive keys that can be passed to {@link ExtraKeysView#setRepetitiveKeys(List)}.
         */
        @JvmField
        var PRIMARY_REPETITIVE_KEYS: List<String> = listOf("UP", "DOWN", "LEFT", "RIGHT", "BKSP", "DEL", "PGUP", "PGDN")

        /**
         * Defines the {@link KeyEvent} for common keys.
         *
         * Note: the Java double-brace anonymous HashMap subclass is replaced
         * with an equivalent plain HashMap (no synthetic subclass).
         */
        @JvmField
        var PRIMARY_KEY_CODES_FOR_STRINGS: Map<String, Int> = hashMapOf(
            "SPACE" to KeyEvent.KEYCODE_SPACE,
            "ESC" to KeyEvent.KEYCODE_ESCAPE,
            "TAB" to KeyEvent.KEYCODE_TAB,
            "HOME" to KeyEvent.KEYCODE_MOVE_HOME,
            "END" to KeyEvent.KEYCODE_MOVE_END,
            "PGUP" to KeyEvent.KEYCODE_PAGE_UP,
            "PGDN" to KeyEvent.KEYCODE_PAGE_DOWN,
            "INS" to KeyEvent.KEYCODE_INSERT,
            "DEL" to KeyEvent.KEYCODE_FORWARD_DEL,
            "BKSP" to KeyEvent.KEYCODE_DEL,
            "UP" to KeyEvent.KEYCODE_DPAD_UP,
            "LEFT" to KeyEvent.KEYCODE_DPAD_LEFT,
            "RIGHT" to KeyEvent.KEYCODE_DPAD_RIGHT,
            "DOWN" to KeyEvent.KEYCODE_DPAD_DOWN,
            "ENTER" to KeyEvent.KEYCODE_ENTER,
            "F1" to KeyEvent.KEYCODE_F1,
            "F2" to KeyEvent.KEYCODE_F2,
            "F3" to KeyEvent.KEYCODE_F3,
            "F4" to KeyEvent.KEYCODE_F4,
            "F5" to KeyEvent.KEYCODE_F5,
            "F6" to KeyEvent.KEYCODE_F6,
            "F7" to KeyEvent.KEYCODE_F7,
            "F8" to KeyEvent.KEYCODE_F8,
            "F9" to KeyEvent.KEYCODE_F9,
            "F10" to KeyEvent.KEYCODE_F10,
            "F11" to KeyEvent.KEYCODE_F11,
            "F12" to KeyEvent.KEYCODE_F12
        )

        /**
         * Aliases for the keys
         */
        @JvmField
        val CONTROL_CHARS_ALIASES: ExtraKeyDisplayMap = ExtraKeyDisplayMap().apply {
            put("ESCAPE", "ESC")
            put("CONTROL", "CTRL")
            put("SHFT", "SHIFT")
            // Technically different keys, but most applications won't see the difference
            put("RETURN", "ENTER")
            put("FUNCTION", "FN")
            // no alias for ALT
            // Directions are sometimes written as first and last letter for brevety
            put("LT", "LEFT")
            put("RT", "RIGHT")
            put("DN", "DOWN")
            // put("UP", "UP"); well, "UP" is already two letters
            put("PAGEUP", "PGUP")
            put("PAGE_UP", "PGUP")
            put("PAGE UP", "PGUP")
            put("PAGE-UP", "PGUP")
            // no alias for HOME
            // no alias for END
            put("PAGEDOWN", "PGDN")
            put("PAGE_DOWN", "PGDN")
            put("PAGE-DOWN", "PGDN")
            put("DELETE", "DEL")
            put("BACKSPACE", "BKSP")
            // easier for writing in termux.properties
            put("BACKSLASH", "\\")
            put("QUOTE", "\"")
            put("APOSTROPHE", "'")
        }
    }

    /**
     * HashMap that implements Python dict.get(key, default) function.
     * Default java.util .get(key) is then the same as .get(key, null);
     *
     * Note: package-private in Java and widened to public because Kotlin has
     * no package-private and the public ExtraKeyDisplayMap extends it.
     */
    open class CleverMap<K, V> : HashMap<K, V>() {

        // Params are nullable and the return is a trust-typed V, mirroring
        // the Java signature exactly: nulls pass through with no new checks.
        @Suppress("UNCHECKED_CAST") // K/V are generic: passthrough keeps Java null semantics exactly
        fun get(key: K?, defaultValue: V?): V {
            // The as-casts are unchecked (erased) and pass nulls through
            // instead of throwing, exactly like the Java version.
            return if (containsKey(key)) super.get(key as K) as V else defaultValue as V
        }
    }

    open class ExtraKeyDisplayMap : CleverMap<String, String>() {
    }

    /*
     * Multiple maps are available to quickly change
     * the style of the keys.
     */
    class EXTRA_KEY_DISPLAY_MAPS {

        companion object {
            /**
             * Keys are displayed in a natural looking way, like "→" for "RIGHT"
             */
            @JvmField
            val CLASSIC_ARROWS_DISPLAY: ExtraKeyDisplayMap = ExtraKeyDisplayMap().apply {
                // classic arrow keys (for ◀ ▶ ▲ ▼ @see arrowVariationDisplay)
                // U+2190 ← LEFTWARDS ARROW
                put("LEFT", "←")
                // U+2192 → RIGHTWARDS ARROW
                put("RIGHT", "→")
                // U+2191 ↑ UPWARDS ARROW
                put("UP", "↑")
                // U+2193 ↓ DOWNWARDS ARROW
                put("DOWN", "↓")
            }

            @JvmField
            val WELL_KNOWN_CHARACTERS_DISPLAY: ExtraKeyDisplayMap = ExtraKeyDisplayMap().apply {
                // well known characters // https://en.wikipedia.org/wiki/{Enter_key, Tab_key, Delete_key}
                // U+21B2 ↲ DOWNWARDS ARROW WITH TIP LEFTWARDS
                put("ENTER", "↲")
                // U+21B9 ↹ LEFTWARDS ARROW TO BAR OVER RIGHTWARDS ARROW TO BAR
                put("TAB", "↹")
                // U+232B ⌫ ERASE TO THE LEFT sometimes seen and easy to understand
                put("BKSP", "⌫")
                // U+2326 ⌦ ERASE TO THE RIGHT not well known but easy to understand
                put("DEL", "⌦")
                // U+2630 ☰ TRIGRAM FOR HEAVEN not well known but easy to understand
                put("DRAWER", "☰")
                // U+2328 ⌨ KEYBOARD not well known but easy to understand
                put("KEYBOARD", "⌨")
                // U+2398
                put("PASTE", "⎘")
                // U+21F3
                put("SCROLL", "⇳")
            }

            @JvmField
            val LESS_KNOWN_CHARACTERS_DISPLAY: ExtraKeyDisplayMap = ExtraKeyDisplayMap().apply {
                // https://en.wikipedia.org/wiki/{Home_key, End_key, Page_Up_and_Page_Down_keys}
                // home key can mean "goto the beginning of line" or "goto first page" depending on context, hence the diagonal
                // from IEC 9995 // U+21F1 ⇱ NORTH WEST ARROW TO CORNER
                put("HOME", "⇱")
                // from IEC 9995 // ⇲ // U+21F2 ⇲ SOUTH EAST ARROW TO CORNER
                put("END", "⇲")
                // no ISO character exists, U+21D1 ⇑ UPWARDS DOUBLE ARROW will do the trick
                put("PGUP", "⇑")
                // no ISO character exists, U+21D3 ⇓ DOWNWARDS DOUBLE ARROW will do the trick
                put("PGDN", "⇓")
            }

            @JvmField
            val ARROW_TRIANGLE_VARIATION_DISPLAY: ExtraKeyDisplayMap = ExtraKeyDisplayMap().apply {
                // alternative to classic arrow keys
                // U+25C0 ◀ BLACK LEFT-POINTING TRIANGLE
                put("LEFT", "◀")
                // U+25B6 ▶ BLACK RIGHT-POINTING TRIANGLE
                put("RIGHT", "▶")
                // U+25B2 ▲ BLACK UP-POINTING TRIANGLE
                put("UP", "▲")
                // U+25BC ▼ BLACK DOWN-POINTING TRIANGLE
                put("DOWN", "▼")
            }

            @JvmField
            val NOT_KNOWN_ISO_CHARACTERS: ExtraKeyDisplayMap = ExtraKeyDisplayMap().apply {
                // Control chars that are more clear as text // https://en.wikipedia.org/wiki/{Function_key, Alt_key, Control_key, Esc_key}
                // put("FN", "FN"); // no ISO character exists
                // ISO character "U+2388 ⎈ HELM SYMBOL" is unknown to people and never printed on computers, however "U+25C7 ◇ WHITE DIAMOND" is a nice presentation, and "^" for terminal app and mac is often used
                put("CTRL", "⎈")
                // ISO character "U+2387 ⎇ ALTERNATIVE KEY SYMBOL'" is unknown to people and only printed as the Option key "⌥" on Mac computer
                put("ALT", "⎇")
                // ISO character "U+238B ⎋ BROKEN CIRCLE WITH NORTHWEST ARROW" is unknown to people and not often printed on computers
                put("ESC", "⎋")
            }

            @JvmField
            val NICER_LOOKING_DISPLAY: ExtraKeyDisplayMap = ExtraKeyDisplayMap().apply {
                // nicer looking for most cases
                // U+2015 ― HORIZONTAL BAR
                put("-", "―")
            }

            /**
             * Full Iso
             */
            @JvmField
            val FULL_ISO_CHAR_DISPLAY: ExtraKeyDisplayMap = ExtraKeyDisplayMap().apply {
                putAll(CLASSIC_ARROWS_DISPLAY)
                putAll(WELL_KNOWN_CHARACTERS_DISPLAY)
                // NEW
                putAll(LESS_KNOWN_CHARACTERS_DISPLAY)
                putAll(NICER_LOOKING_DISPLAY)
                // NEW
                putAll(NOT_KNOWN_ISO_CHARACTERS)
            }

            /**
             * Only arrows
             */
            @JvmField
            val ARROWS_ONLY_CHAR_DISPLAY: ExtraKeyDisplayMap = ExtraKeyDisplayMap().apply {
                putAll(CLASSIC_ARROWS_DISPLAY)
                // putAll(wellKnownCharactersDisplay); // REMOVED
                // putAll(lessKnownCharactersDisplay); // REMOVED
                putAll(NICER_LOOKING_DISPLAY)
            }

            /**
             * Classic symbols and less known symbols
             */
            @JvmField
            val LOTS_OF_ARROWS_CHAR_DISPLAY: ExtraKeyDisplayMap = ExtraKeyDisplayMap().apply {
                putAll(CLASSIC_ARROWS_DISPLAY)
                putAll(WELL_KNOWN_CHARACTERS_DISPLAY)
                // NEW
                putAll(LESS_KNOWN_CHARACTERS_DISPLAY)
                putAll(NICER_LOOKING_DISPLAY)
            }

            /**
             * Some classic symbols everybody knows
             */
            @JvmField
            val DEFAULT_CHAR_DISPLAY: ExtraKeyDisplayMap = ExtraKeyDisplayMap().apply {
                putAll(CLASSIC_ARROWS_DISPLAY)
                putAll(WELL_KNOWN_CHARACTERS_DISPLAY)
                putAll(NICER_LOOKING_DISPLAY)
                // all other characters are displayed as themselves
            }
        }
    }
}
