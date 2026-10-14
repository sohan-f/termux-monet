package com.termux.terminal

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RecordingCanvas
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import java.util.Properties

/**
 * A terminal bitmap for images.
 */
open class TerminalBitmap private constructor(client: TerminalSessionClient?, bitmapNum: Int, bitmap: Bitmap?,
                                              cellWidth: Int, cellHeight: Int,
                                              scrollLines: Int, cursorDelta: IntArray?) {

    companion object {
        const val LOG_TAG = "TerminalBitmap"

        /**
         * The max size of a Terminal {@link Bitmap} for its pixels. The limit is defined as per how
         * `RecordingCanvas.MAX_BITMAP_SIZE` value is defined, check below for details. The value should
         * normally be between `100-200MB` depending on device and Android version.
         *
         * Each pixel is stored on 4 bytes for a {@link Bitmap.Config#ARGB_8888} bitmap color config.
         * The bitmap will have following memory usage for its respective resolution (`width x height x 4`).
         * - 1280x720 (HD): 3,686,400 bytes/3.6MB.
         * - 1920x1080 (FHD): 8,294,400 bytes/8MB.
         * - 2560x1440 (QHD): 14,745,600 bytes/14.7MB.
         * - 3840x2160 (4K UHD): 33,177,600 bytes/33MB.
         * - 7680x4320 (8K UHD): 132,710,400 bytes/132MB.
         * .
         * - https://en.wikipedia.org/wiki/Display_resolution_standards#High-definition
         *
         * The terminal uses {@link Canvas#drawBitmap(Bitmap, Rect, RectF, Paint)} to draw the bitmap
         * when `TerminalRenderer.render()` is called.
         *
         * The {@link Canvas} class defines `Canvas.MAXIMUM_BITMAP_SIZE` for the maximum dimension
         * for a bitmap which is returned by {@link Canvas#getMaximumBitmapWidth()} and
         * {@link Canvas#getMaximumBitmapHeight()}. It is hardcoded with the value `32766` as defined by
         * Skia (2D graphics library), which technically has the limit `32767` as it requires supporting
         * math on 16-bit buffers.
         * - https://cs.android.com/android/_/android/platform/frameworks/base/+/f61970fc79e9c5cf340fa942597628242361864a
         * - https://cs.android.com/android/platform/superproject/+/android-16.0.0_r1:frameworks/base/graphics/java/android/graphics/Canvas.java;l=76-78
         * - https://cs.android.com/android/platform/superproject/+/android-16.0.0_r1:external/skia/src/shaders/SkImageShader.cpp;l=254-267
         *
         * The {@link RecordingCanvas} class defines `RecordingCanvas.MAX_BITMAP_SIZE` for the
         * maximum size (not dimension) for a bitmap, which is checked by
         * `RecordingCanvas.throwIfCannotDraw()` when `BaseRecordingCanvas.drawBitmap()` is called.
         * The `RecordingCanvas` is a specialized implementation of the `Canvas` class that is designed
         * to record draw commands for deferred rendering instead of executing draw commands instantly.
         * By recording draw commands, they can be cached so that complex views can be efficiently
         * re-drawn without recalculating them again for every frame. The caching part is similar to
         * how a terminal behaves, where it stores all the bitmaps for rendering depending on scroll
         * position. So both `RecordingCanvas` and a terminal require similar limits on bitmap
         * sizes considering memory consumption limits of apps, and multiple bitmaps being loaded
         * instead of a single one like for wallpapers, hence why `TerminalBitmap.MAX_BITMAP_SIZE` is
         * synced with `RecordingCanvas`.
         * The `RecordingCanvas.MAX_BITMAP_SIZE` is set from `ro.hwui.max_texture_allocation_size`
         * system property if set for Android `>= 12`, otherwise `150MB` (`100MB` for Android `10-14`).
         * The values `>= 150MB` are enough to support `7680x4320` (8K UHD) bitmaps.
         * Some devices like larger xiaomi devices have `ro.hwui.max_texture_allocation_size` set to `209715200` (`200MB`).
         * - https://cs.android.com/android/_/android/platform/frameworks/base/+/e4d011201cb2b2eef401db8fddc5c9c6
         * - https://cs.android.com/android/_/android/platform/frameworks/base/+/0e717a9d06ded980908649393bd73e46ffafcd54
         * - https://cs.android.com/android/_/android/platform/frameworks/base/+/97396260ed06cc9d1834d4d8e4e649a3ef09f1f3
         * - https://cs.android.com/android/platform/superproject/+/android-16.0.0_r1:frameworks/base/graphics/java/android/graphics/RecordingCanvas.java;l=42-50
         *
         * The Android wallpaper manager service also checks if dimensions of cropped wallpaper exceeds
         * max texture size that the GPU can support, otherwise it will cause System UI to keep crashing
         * because it can not initialize EGL with an appropriate surface. The `GLHelper.getMaxTextureSize()`
         * returns the max texture size, which is defined by `sys.max_texture_size` system property if set,
         * otherwise by value for `GL_MAX_TEXTURE_SIZE`. The `sys.max_texture_size` defines the maximum
         * width or height of a texture, not total size. Its value can be low like `2048` or high like
         * `16384` for 16K support.
         * - https://cs.android.com/android/_/android/platform/frameworks/base/+/32c6a7c691b0d91085c1ed13fe6f1c473c94b4c8
         * - https://cs.android.com/android/_/android/platform/frameworks/base/+/android-16.0.0_r1:frameworks/base/services/core/java/com/android/server/wallpaper/WallpaperCropper.java;l=461
         * - https://cs.android.com/android/platform/superproject/+/android-16.0.0_r1:frameworks/base/services/core/java/com/android/server/wallpaper/GLHelper.java;l=145
         * - https://developer.android.com/reference/android/opengl/GLES10#GL_MAX_TEXTURE_SIZE
         *
         * The {@link WallpaperManager#getDesiredMinimumWidth()} and {@link WallpaperManager#getDesiredMinimumHeight()}
         * can also be called to get minimum suggested width and height of the wallpaper that an app
         * should use when setting the wallpaper. This normally is equal to the width and height of the
         * current device display, but the width can be higher than display width if the homescreen is
         * scrollable horizontally with multiple pages, in which case the width returned is equal to
         * entire workspace width. The launcher apps can provide Android their desired width and height
         * dimensions depending on the homescreen pages config by calling
         * {@link WallpaperManager#suggestDesiredDimensions(int, int)}, which also ensures that values
         * passed are scaled down to `sys.max_texture_size` system property if its set.
         * - https://cs.android.com/android/_/android/platform/frameworks/base/+/289c273ec49462c7bfdbf6238e9016936da7307c
         * - https://cs.android.com/android/platform/superproject/+/android-16.0.0_r1:frameworks/base/core/java/android/app/WallpaperManager.java;l=2737-2794
         * - https://cs.android.com/android/platform/superproject/+/android-16.0.0_r1:frameworks/base/services/core/java/com/android/server/wallpaper/WallpaperManagerService.java;l=2330-2366
         * - https://cs.android.com/android/platform/superproject/+/android-16.0.0_r1:frameworks/base/core/java/android/view/Display.java;l=1052-1063
         *
         * If an app specifies `largeHeap=true` in its `AndroidManifest.xml`, then it can be allocated
         * larger heap memory to load larger bitmaps maps instead of resulting in an OOM. The Termux app
         * does not have it enabled, and hence is more likely to have OOMs when loading larger bitmaps.
         * - https://developer.android.com/guide/topics/manifest/application-element#largeHeap
         * - https://developer.android.com/topic/performance/memory
         */
        @JvmField
        val MAX_BITMAP_SIZE = initMaxBitmapSize()

        @JvmStatic
        private fun initMaxBitmapSize(): Int {
            // Synced with `RecordingCanvas.MAX_BITMAP_SIZE`.
            // - https://cs.android.com/android/platform/superproject/+/android-16.0.0_r1:frameworks/base/graphics/java/android/graphics/RecordingCanvas.java;l=42-50
            val defaultSize =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)
                    150 * 1024 * 1024 // 150 MB
                else
                    100 * 1024 * 1024 // 100 MB

            val systemProperties: Properties = AndroidUtils.getSystemProperties(LOG_TAG)
            val maxTextureSizeString = systemProperties.getProperty("ro.hwui.max_texture_allocation_size")

            if (maxTextureSizeString == null) return defaultSize

            try {
                val maxTextureSize = Integer.parseInt(maxTextureSizeString)
                return if (maxTextureSize > 0) maxTextureSize else defaultSize
            } catch (e: Exception) {
                return defaultSize
            }
        }

        /** Build a {@link TerminalBitmap} from a {@link TerminalSixel}. */
        @JvmStatic
        fun build(terminalBuffer: TerminalBuffer, bitmapNum: Int, terminalSixel: TerminalSixel,
                  x: Int, y: Int, cellWidth: Int, cellHeight: Int): TerminalBitmap? {
            try {
                var bitmap = terminalSixel.getBitmap()
                bitmap = resizeBitmapConstrained(LOG_TAG, "sixel", terminalBuffer.getClient(), bitmap,
                    terminalSixel.getWidth(), terminalSixel.getHeight(), cellWidth, cellHeight,
                    terminalBuffer.mColumns - x)
                if (bitmap == null) {
                    Logger.logError(terminalBuffer.getClient(), LOG_TAG,
                        "Create terminal bitmap " + bitmapNum + " from terminal sixel failed")
                    return null
                }

                return buildOrThrow(terminalBuffer, bitmapNum, bitmap,
                    x, y, cellWidth, cellHeight)
            } catch (t: Throwable) {
                if (t is OutOfMemoryError) System.gc()
                Logger.logError(terminalBuffer.getClient(), LOG_TAG,
                    "Create terminal bitmap " + bitmapNum + " from terminal sixel failed: " + t.message)
                return null
            }
        }

        /** Build a {@link TerminalBitmap} from an image `byte[]`. */
        @JvmStatic
        fun build(terminalBuffer: TerminalBuffer, bitmapNum: Int, image: ByteArray?,
                  x: Int, y: Int, cellWidth: Int, cellHeight: Int,
                  width: Int, height: Int, shouldPreserveAspectRatio: Boolean): TerminalBitmap? {
            try {
                var newBitmap: Bitmap?
                var imageHeight: Int
                var imageWidth: Int
                var newWidth = width
                var newHeight = height

                if (image == null) {
                    Logger.logError(terminalBuffer.getClient(), LOG_TAG,
                        "Create terminal bitmap " + bitmapNum + " from image byte array failed:" +
                            " Image data not set")
                    return null
                }

                if (height > 0 || width > 0) {
                    // Get image dimensions without creating a bitmap.
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    try {
                        BitmapFactory.decodeByteArray(image, 0, image.size, options)
                    } catch (t: Throwable) {
                        if (t is OutOfMemoryError) System.gc()
                        Logger.logWarn(terminalBuffer.getClient(), LOG_TAG,
                            "Decode bitmap failed while creating" +
                                " terminal bitmap " + bitmapNum + " from image byte array: " + t.message)
                    }

                    imageHeight = options.outHeight
                    imageWidth = options.outWidth
                    if (shouldPreserveAspectRatio) {
                        var wFactor = 9999.0
                        var hFactor = 9999.0
                        if (width > 0) {
                            wFactor = width.toDouble() / imageWidth
                        }
                        if (height > 0) {
                            hFactor = height.toDouble() / imageHeight
                        }
                        val factor = Math.min(wFactor, hFactor)
                        newWidth = (factor * imageWidth).toInt()
                        newHeight = (factor * imageHeight).toInt()
                    } else {
                        if (height <= 0) {
                            newHeight = imageHeight
                        }
                        if (width <= 0) {
                            newWidth = imageWidth
                        }
                    }

                    var scaleFactor = 1
                    while (imageHeight >= 2 * newHeight * scaleFactor && imageWidth >= 2 * newWidth * scaleFactor) {
                        scaleFactor = scaleFactor * 2
                    }

                    // Create bitmap from image.
                    try {
                        newBitmap = if (scaleFactor > 1) {
                            // Subsample the original image to get a smaller image to save memory.
                            val scaleOptions = BitmapFactory.Options()
                            scaleOptions.inSampleSize = scaleFactor
                            BitmapFactory.decodeByteArray(image, 0, image.size, scaleOptions)
                        } else {
                            BitmapFactory.decodeByteArray(image, 0, image.size)
                        }
                    } catch (t: Throwable) {
                        if (t is OutOfMemoryError) System.gc()
                        Logger.logError(terminalBuffer.getClient(), LOG_TAG,
                            "Create terminal bitmap " + bitmapNum + " from image byte array failed:" +
                                " Decode scaled bitmap for scale factor " + scaleFactor + " failed: " + t.message)
                        return null
                    }
                    if (newBitmap == null) {
                        Logger.logError(terminalBuffer.getClient(), LOG_TAG,
                            "Create terminal bitmap " + bitmapNum + " from image byte array failed:" +
                                " Decoded scaled bitmap not set for scale factor " + scaleFactor)
                        return null
                    }

                    // Crop the bitmap if it exceeds terminal bounds.
                    val maxWidth = (terminalBuffer.mColumns - x) * cellWidth
                    if (newWidth > maxWidth) {
                        val cropWidth = newBitmap.width * maxWidth / newWidth
                        try {
                            newBitmap = Bitmap.createBitmap(newBitmap, 0, 0, cropWidth, newBitmap.height)
                            newWidth = maxWidth
                        } catch (t: Throwable) {
                            if (t is OutOfMemoryError) {
                                // This is just a memory optimization. If it fails,
                                // continue (and probably fail later).
                                System.gc()
                            }
                        }
                    }

                    // Create final scaled bitmap.
                    try {
                        newBitmap = Bitmap.createScaledBitmap(newBitmap, newWidth, newHeight, true)
                    } catch (t: Throwable) {
                        if (t is OutOfMemoryError) System.gc()
                        Logger.logError(terminalBuffer.getClient(), LOG_TAG,
                            "Create terminal bitmap " + bitmapNum + " from image byte array failed:" +
                                " Create scaled bitmap failed: " + t.message)
                        return null
                    }
                } else {
                    // Create bitmap from image.
                    try {
                        newBitmap = BitmapFactory.decodeByteArray(image, 0, image.size)
                    } catch (t: Throwable) {
                        if (t is OutOfMemoryError) System.gc()
                        Logger.logError(terminalBuffer.getClient(), LOG_TAG,
                            "Create terminal bitmap " + bitmapNum + " from image byte array failed:" +
                                " Create full bitmap failed: " + t.message)
                        return null
                    }
                }

                if (newBitmap == null) {
                    Logger.logError(terminalBuffer.getClient(), LOG_TAG,
                        "Create terminal bitmap " + bitmapNum + " from image byte array failed: New bitmap not set")
                    return null
                }

                newBitmap = resizeBitmapConstrained(LOG_TAG, "image byte array", terminalBuffer.getClient(), newBitmap,
                    newBitmap.width, newBitmap.height, cellWidth, cellHeight,
                    terminalBuffer.mColumns - x)
                val terminalBitmap = build(terminalBuffer, bitmapNum, newBitmap, x, y, cellWidth, cellHeight)
                if (terminalBitmap == null) {
                    return terminalBitmap
                }

                terminalBitmap.setCursorDelta(intArrayOf(
                    terminalBitmap.getScrollLines(),
                    (terminalBitmap.getBitmap()!!.width + cellWidth - 1) / cellWidth))

                return terminalBitmap
            } catch (t: Throwable) {
                if (t is OutOfMemoryError) System.gc()
                Logger.logError(terminalBuffer.getClient(), LOG_TAG,
                    "Create terminal bitmap " + bitmapNum + " from image byte array failed: " + t.message)
                return null
            }
        }

        /** Build a {@link TerminalBitmap} from a {@link Bitmap}. */
        @JvmStatic
        fun build(terminalBuffer: TerminalBuffer, bitmapNum: Int, bitmap: Bitmap?,
                  x: Int, y: Int, cellWidth: Int, cellHeight: Int): TerminalBitmap? {
            try {
                return buildOrThrow(terminalBuffer, bitmapNum, bitmap, x, y, cellWidth, cellHeight)
            } catch (t: Throwable) {
                if (t is OutOfMemoryError) System.gc()
                Logger.logError(terminalBuffer.getClient(), LOG_TAG,
                    "Create terminal bitmap " + bitmapNum + " from bitmap failed: " + t.message)
                return null
            }
        }

        /** Build a {@link TerminalBitmap} from a {@link Bitmap}. */
        @JvmStatic
        @Throws(Throwable::class)
        fun buildOrThrow(terminalBuffer: TerminalBuffer, bitmapNum: Int, bitmap: Bitmap?,
                         x: Int, y: Int, cellWidth: Int, cellHeight: Int): TerminalBitmap {
            if (bitmap == null) {
                throw IllegalArgumentException("Cannot create terminal bitmap from an unset bitmap")
            }

            var bitmap = bitmap
            val bitmapWidth = bitmap.width
            val bitmapHeight = bitmap.height
            val width = Math.min(terminalBuffer.mColumns - x, (bitmapWidth + cellWidth - 1) / cellWidth)
            val height = (bitmapHeight + cellHeight - 1) / cellHeight
            var s = 0

            for (i in 0 until height) {
                if (y + i - s == terminalBuffer.mScreenRows) {
                    terminalBuffer.scrollDownOneLine(0, terminalBuffer.mScreenRows, TextStyle.NORMAL)
                    s++
                }
                for (j in 0 until width) {
                    terminalBuffer.setChar(x + j, y + i - s, '+'.code, TextStyle.encodeTerminalBitmap(bitmapNum, j, i))
                }
            }

            if (width * cellWidth < bitmapWidth) {
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, width * cellWidth, bitmapHeight)
            }

            val scrollLines = height - s

            return TerminalBitmap(terminalBuffer.getClient(), bitmapNum, bitmap,
                cellWidth, cellHeight, scrollLines, null)
        }

        @JvmStatic
        fun resizeBitmap(logTag: String?, label: String?, client: TerminalSessionClient?, bitmap: Bitmap?,
                         bitmapWidth: Int, bitmapHeight: Int): Bitmap? {
            var newBitmap: Bitmap?
            try {
                // Null bitmap threw NPE below in Java as well (caught and logged).
                val nonNullBitmap = bitmap!!
                val newBitmapSize = bitmapWidth * bitmapHeight * 4
                if (newBitmapSize < 0 || newBitmapSize > MAX_BITMAP_SIZE) {
                    Logger.logError(client, logTag, "The new " + label + " bitmap after resize with" +
                        " width " + bitmapWidth + " and height " + bitmapHeight +
                        " has size " + newBitmapSize + " greater than max bitmap size " + MAX_BITMAP_SIZE)
                    return null
                }

                val pixels = IntArray(nonNullBitmap.allocationByteCount)
                nonNullBitmap.getPixels(pixels, 0, nonNullBitmap.width, 0, 0, nonNullBitmap.width, nonNullBitmap.height)

                newBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)

                val newWidth = Math.min(nonNullBitmap.width, bitmapWidth)
                val newHeight = Math.min(nonNullBitmap.height, bitmapHeight)
                newBitmap.setPixels(pixels, 0, nonNullBitmap.width, 0, 0, newWidth, newHeight)
                return newBitmap
            } catch (t: Throwable) {
                if (t is OutOfMemoryError) System.gc()
                Logger.logError(client, logTag, "Resize " + label + " bitmap to" +
                    " width " + bitmapWidth + " and height " + bitmapHeight + " failed: " + t.message)
                return null
            }
        }

        @JvmStatic
        fun resizeBitmapConstrained(logTag: String?, label: String?, client: TerminalSessionClient?, bitmap: Bitmap?,
                                    bitmapWidth: Int, bitmapHeight: Int,
                                    cellWidth: Int, cellHeight: Int, columns: Int): Bitmap? {
            // Width and height must be multiples of the cell width and height.
            // Bitmap should not extend beyond screen width.
            val originalBitmap = bitmap
            var constrainedBitmap = bitmap
            if (bitmapWidth > cellWidth * columns || (bitmapWidth % cellWidth) != 0 || (bitmapHeight % cellHeight) != 0) {
                val newBitmapWidth = Math.min(cellWidth * columns, ((bitmapWidth - 1) / cellWidth) * cellWidth + cellWidth)
                val newBitmapHeight = ((bitmapHeight - 1) / cellHeight) * cellHeight + cellHeight
                constrainedBitmap = resizeBitmap(logTag, label, client, originalBitmap, newBitmapWidth, newBitmapHeight)
                // Only a minor display glitch if resize failed.
                return constrainedBitmap ?: originalBitmap
            } else {
                return originalBitmap
            }
        }
    }

    @JvmField
    protected val mClient: TerminalSessionClient? = client

    @JvmField
    protected var mBitmapNum: Int = bitmapNum
    @JvmField
    protected var mBitmap: Bitmap? = bitmap

    @JvmField
    protected var mCellWidth: Int = cellWidth
    @JvmField
    protected var mCellHeight: Int = cellHeight

    @JvmField
    protected var mScrollLines: Int = scrollLines

    @JvmField
    protected var mCursorDelta: IntArray? = cursorDelta

    fun getClient(): TerminalSessionClient? {
        return mClient
    }

    fun getBitmapNum(): Int {
        return mBitmapNum
    }

    fun getBitmap(): Bitmap? {
        return mBitmap
    }

    fun getCellWidth(): Int {
        return mCellWidth
    }

    fun getCellHeight(): Int {
        return mCellHeight
    }

    fun getScrollLines(): Int {
        return mScrollLines
    }

    fun getCursorDelta(): IntArray? {
        return mCursorDelta
    }

    fun setCursorDelta(cursorDelta: IntArray?) {
        mCursorDelta = cursorDelta
    }
}
