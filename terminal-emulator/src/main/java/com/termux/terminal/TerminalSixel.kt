package com.termux.terminal

import android.graphics.Bitmap

/**
 * A terminal sixel received via `DCS q s..s ST` or `DCS P1; P2; P3; q s..s ST`.
 *
 * **See Also:**
 * - `TerminalEmulator.ESC_DCS__SIXEL`
 * - https://vt100.net/docs/vt3xx-gp/chapter14.html
 * - https://en.wikipedia.org/wiki/Sixel
 * - https://www.digiater.nl/openvms/decus/vax90b1/krypton-nasa/all-about-sixels.text
 */
open class TerminalSixel private constructor(client: TerminalSessionClient?, bitmap: Bitmap?) {

    companion object {
        const val LOG_TAG = "TerminalSixel"

        @JvmField
        val SIXEL__INITIAL_COLOR_MAP = intArrayOf(
            0xFF000000.toInt(), 0xFF3333CC.toInt(), 0xFFCC2323.toInt(), 0xFF33CC33.toInt(), 0xFFCC33CC.toInt(), 0xFF33CCCC.toInt(), 0xFFCCCC33.toInt(), 0xFF777777.toInt(),
            0xFF444444.toInt(), 0xFF565699.toInt(), 0xFF994444.toInt(), 0xFF569956.toInt(), 0xFF995699.toInt(), 0xFF569999.toInt(), 0xFF999956.toInt(), 0xFFCCCCCC.toInt()
        )

        /**
         * A sixel is a group of six pixels in a vertical column.
         */
        const val SIXEL__LINE_LEN = 6

        /**
         * The amount of extra dimension added when resizing a sixel when receiving more image data.
         */
        const val SIXEL__BITMAP_RESIZE_EXTRA_DIMENSION = 100

        /**
         * The max value for the sixel Graphics Repeat Introducer.
         *
         * The value `8192` can support 8K images, see also {@link TerminalBitmap#MAX_BITMAP_SIZE}.
         *
         * Each repeat creates a new sixel line of `1x6` pixels, where `6` is the {@link #SIXEL__LINE_LEN}.
         *
         *  - https://vt100.net/docs/vt3xx-gp/chapter14.html#S14.3.1
         */
        const val SIXEL__MAX_REPEAT = 8192

        @JvmStatic
        fun build(client: TerminalSessionClient?, bitmapWidth: Int, bitmapHeight: Int): TerminalSixel? {
            try {
                val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(0)

                return TerminalSixel(client, bitmap)
            } catch (t: Throwable) {
                if (t is OutOfMemoryError) System.gc()
                Logger.logError(client, LOG_TAG, "Create sixel bitmap for" +
                    " width " + bitmapWidth + " with height " + bitmapHeight + " failed: " + t.message)
                return null
            }
        }
    }

    @JvmField
    protected val mClient: TerminalSessionClient? = client

    @JvmField
    protected var mBitmap: Bitmap? = bitmap

    @JvmField
    protected var mWidth = 0
    @JvmField
    protected var mHeight = 0

    @JvmField
    protected var mCurX = 0
    @JvmField
    protected var mCurY = 0

    @JvmField
    protected val mColorMap: IntArray = IntArray(256)
    @JvmField
    protected var mColor: Int

    init {
        System.arraycopy(SIXEL__INITIAL_COLOR_MAP, 0, mColorMap, 0, 16)
        mColor = mColorMap[0]
    }

    fun getClient(): TerminalSessionClient? {
        return mClient
    }

    fun getBitmap(): Bitmap? {
        return mBitmap
    }

    fun getWidth(): Int {
        return mWidth
    }

    fun getHeight(): Int {
        return mHeight
    }

    fun getCurX(): Int {
        return mCurX
    }

    fun getCurY(): Int {
        return mCurY
    }

    fun getColorMap(): IntArray {
        return mColorMap
    }

    fun getColor(): Int {
        return mColor
    }

    fun readData(codePoint: Int, repeat: Int): Boolean {
        var repeat = repeat
        var bitmap = mBitmap ?: return false

        if (repeat > SIXEL__MAX_REPEAT) {
            Logger.logError(mClient, LOG_TAG, "The sixel repeat value " + repeat + " is greater than max repeat value " + SIXEL__MAX_REPEAT)
            return false
        }

        // Graphics Carriage Return `$`.
        // > The $ (2/4) character indicates the end of the sixel line. The active position returns
        // > to the left page border of the same sixel line. You can use this character to overprint lines.
        // - https://vt100.net/docs/vt3xx-gp/chapter14.html#S14.3.4
        if (codePoint == '$'.code) {
            mCurX = 0
            return true
        }

        // Graphics New Line `-`.
        // > The - (2/13) character indicates the end of a sixel line. The active position moves to
        // > the left margin of the next sixel line.
        // - https://vt100.net/docs/vt3xx-gp/chapter14.html#S14.3.5
        if (codePoint == '-'.code) {
            mCurX = 0
            mCurY += SIXEL__LINE_LEN
            return true
        }

        if (bitmap.width < mCurX + repeat) {
            val newBitmapWidth = mCurX + repeat + SIXEL__BITMAP_RESIZE_EXTRA_DIMENSION

            if (newBitmapWidth < 0) {
                Logger.logError(mClient, LOG_TAG, "The new sixel bitmap width overflowed: " +
                    mCurX + " (cursor x) + " + repeat + " (repeat) + " + SIXEL__BITMAP_RESIZE_EXTRA_DIMENSION)
                return false
            }

            bitmap = TerminalBitmap.resizeBitmap(LOG_TAG, "sixel", mClient, bitmap, newBitmapWidth, bitmap.height) ?: return false
            mBitmap = bitmap
        }

        if (bitmap.height < mCurY + SIXEL__LINE_LEN) {
            // Very unlikely to resize both at the same time.
            val newBitmapHeight = mCurY + SIXEL__BITMAP_RESIZE_EXTRA_DIMENSION

            if (newBitmapHeight < 0) {
                Logger.logError(mClient, LOG_TAG, "The new sixel bitmap height overflowed: " +
                    mCurY + " (cursor y) + " + SIXEL__BITMAP_RESIZE_EXTRA_DIMENSION)
                return false
            }

            bitmap = TerminalBitmap.resizeBitmap(LOG_TAG, "sixel", mClient, bitmap, bitmap.width, newBitmapHeight) ?: return false
            mBitmap = bitmap
        }

        if (mCurX + repeat > bitmap.width) {
            repeat = bitmap.width - mCurX
        }

        if (mCurY + SIXEL__LINE_LEN > bitmap.height) {
            Logger.logError(mClient, LOG_TAG, "The sixel curson y position " +
                mCurY + SIXEL__LINE_LEN + " is greater than bitmap height " + bitmap.height)
            return false
        }

        // Sixel data characters are in the range of `?` (0x3F) to `~` (0x7E).
        // > Each sixel data character represents six vertical pixels of data. Each sixel data character
        // > represents a binary value equal to the character code value minus hex 3F.
        // - https://vt100.net/docs/vt3xx-gp/chapter14.html#S14.2.1
        if (repeat > 0 && codePoint >= '?'.code && codePoint <= '~'.code) {
            val b = codePoint - '?'.code
            if (mCurY + SIXEL__LINE_LEN > mHeight) {
                mHeight = mCurY + SIXEL__LINE_LEN
            }

            while (repeat > 0) {
                repeat--
                for (i in 0 until SIXEL__LINE_LEN) {
                    if ((b and (1 shl i)) != 0) {
                        bitmap.setPixel(mCurX, mCurY + i, mColor)
                    }
                }

                mCurX += 1
                if (mCurX > mWidth) {
                    mWidth = mCurX
                }
            }
        }

        return true
    }

    fun resize(sixelWidth: Int, sixelHeight: Int): Boolean {
        val bitmap = mBitmap ?: return false

        if (sixelWidth < 1 || sixelHeight < 1)
            return false

        val bitmapWidth = bitmap.width
        val newBitmapWidth = Math.max(sixelWidth, bitmapWidth)

        val bitmapHeight = bitmap.height
        val newBitmapHeight = Math.max(sixelHeight, bitmapHeight)

        if (bitmapWidth < newBitmapWidth || bitmapHeight < newBitmapHeight) {
            mBitmap = TerminalBitmap.resizeBitmap(LOG_TAG, "sixel", mClient, bitmap, newBitmapWidth, newBitmapHeight)
            return mBitmap != null
        }

        return true
    }

    fun setColor(color: Int) {
        if (color >= 0 && color < mColorMap.size) {
            mColor = mColorMap[color]
        }
    }

    fun setRGBColor(color: Int, r: Int, g: Int, b: Int) {
        if (color >= 0 && color < mColorMap.size) {
            val red = Math.min(255, r * 255 / 100)
            val green = Math.min(255, g * 255 / 100)
            val blue = Math.min(255, b * 255 / 100)
            mColor = 0xff000000.toInt() + (red shl 16) + (green shl 8) + blue
            mColorMap[color] = mColor
        }
    }
}
