package com.termux.terminal

import android.graphics.Color

/**
 * Current terminal colors (if different from default).
 */
class TerminalColors {

    companion object {
        /**
         * Static data - a bit ugly but ok for now.
         */
        @JvmField
        val COLOR_SCHEME = TerminalColorScheme()

        /**
         * Parse color according to http://manpages.ubuntu.com/manpages/intrepid/man3/XQueryColor.3.html
         * <p/>
         * Highest bit is set if successful, so return value is 0xFF${R}${G}${B}. Return 0 if failed.
         */
        @JvmStatic
        fun parse(c: String): Int {
            try {
                val skipInitial: Int
                val skipBetween: Int
                if (c[0] == '#') {
                    // #RGB, #RRGGBB, #RRRGGGBBB or #RRRRGGGGBBBB. Most significant bits.
                    skipInitial = 1
                    skipBetween = 0
                } else if (c.startsWith("rgb:")) {
                    // rgb:<red>/<green>/<blue> where <red>, <green>, <blue> := h | hh | hhh | hhhh. Scaled.
                    skipInitial = 4
                    skipBetween = 1
                } else {
                    return 0
                }
                val charsForColors = c.length - skipInitial - 2 * skipBetween
                // Unequal lengths.
                if (charsForColors % 3 != 0)
                    return 0
                val componentLength = charsForColors / 3
                val mult = 255 / (Math.pow(2.0, (componentLength * 4).toDouble()) - 1)
                var currentPosition = skipInitial
                val rString = c.substring(currentPosition, currentPosition + componentLength)
                currentPosition += componentLength + skipBetween
                val gString = c.substring(currentPosition, currentPosition + componentLength)
                currentPosition += componentLength + skipBetween
                val bString = c.substring(currentPosition, currentPosition + componentLength)
                val r = (Integer.parseInt(rString, 16) * mult).toInt()
                val g = (Integer.parseInt(gString, 16) * mult).toInt()
                val b = (Integer.parseInt(bString, 16) * mult).toInt()
                return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            } catch (e: NumberFormatException) {
                return 0
            } catch (e: IndexOutOfBoundsException) {
                return 0
            }
        }

        /**
         * Get the perceived brightness of the color based on its RGB components.
         *
         * https://www.nbdtech.com/Blog/archive/2008/04/27/Calculating-the-Perceived-Brightness-of-a-Color.aspx
         * http://alienryderflex.com/hsp.html
         *
         * @param color The color code int.
         * @return Returns value between 0-255.
         */
        @JvmStatic
        fun getPerceivedBrightnessOfColor(color: Int): Int {
            return Math.floor(Math.sqrt(Math.pow(Color.red(color).toDouble(), 2.0) * 0.241 + Math.pow(Color.green(color).toDouble(), 2.0) * 0.691 + Math.pow(Color.blue(color).toDouble(), 2.0) * 0.068)).toInt()
        }
    }

    /**
     * The current terminal colors, which are normally set from the color theme, but may be set dynamically with the OSC
     * 4 control sequence.
     */
    @JvmField
    val mCurrentColors: IntArray = IntArray(TextStyle.NUM_INDEXED_COLORS)

    /**
     * Create a new instance with default colors from the theme.
     */
    init {
        reset()
    }

    /**
     * Reset a particular indexed color with the default color from the color theme.
     */
    fun reset(index: Int) {
        mCurrentColors[index] = COLOR_SCHEME.mDefaultColors[index]
    }

    /**
     * Reset all indexed colors with the default color from the color theme.
     */
    fun reset() {
        System.arraycopy(COLOR_SCHEME.mDefaultColors, 0, mCurrentColors, 0, TextStyle.NUM_INDEXED_COLORS)
    }

    /**
     * Try parse a color from a text parameter and into a specified index.
     */
    fun tryParseColor(intoIndex: Int, textParameter: String) {
        val c = parse(textParameter)
        if (c != 0)
            mCurrentColors[intoIndex] = c
    }
}
