package com.termux.shared.image

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.ImageDecoder
import android.graphics.Point
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.termux.shared.errors.Error
import com.termux.shared.file.FileUtils
import com.termux.shared.file.FileUtilsErrno
import com.termux.shared.logger.Logger
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ImageUtils private constructor() {

    companion object {
        /**
         * Request code that can be used to distinguish Activity result.
         */
        const val REQUEST_CODE_IMAGE = 100

        /**
         * Compression quality used to compress image. The value is interpreted differently depending on the {@link CompressFormat CompressFormat}.
         */
        const val COMPRESS_QUALITY = 80

        /**
         * Tolerance for diffrence in original image required optimized image.
         */
        const val OPTIMALITY_TOLERANCE = 50

        const val IMAGE_TYPE = "image"

        const val ANY_IMAGE_TYPE = IMAGE_TYPE + "/*"

        private const val LOG_TAG = "ImageUtils"

        /**
         * Get an {@link Bitmap} image from the {@link Uri}.
         *
         * @param context The context for the operations.
         * @param uri     The uri from where image content will be loaded.
         * @return Bitmap containing the image, or return {@code null} if failed to
         * load bitmap content.
         */
        @JvmStatic
        @Suppress("DEPRECATION") // MediaStore getBitmap must stay: the pre-P fallback path
        fun getBitmap(context: Context?, uri: Uri?): Bitmap? {
            var bitmap: Bitmap? = null
            try {
                // Null context or uri threw NPE here in Java as well.
                val nonNullContext = context!!
                bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(nonNullContext.contentResolver, uri!!))
                } else {
                    MediaStore.Images.Media.getBitmap(nonNullContext.contentResolver, uri)
                }
            } catch (e: IOException) {
                // Only IOException is caught; anything else (including the
                // NPE above) propagates exactly like the Java version.
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to load bitmap from $uri", e)
            }
            return bitmap
        }

        /**
         * Generate image bitmap from the path.
         *
         * @param path The path for image file
         * @return Bitmap generated from image path, if fails to
         * to generate returns {@code null}
         */
        @JvmStatic
        fun getBitmap(path: String?): Bitmap? {
            return BitmapFactory.decodeFile(path)
        }

        /**
         * Creates an centered and resized {@link Bitmap} according to given size.
         *
         * @param bitmap Original bitmap source to resize.
         * @param point  Target size containing width and height to resize.
         * @return Returns the resized bitmap for given size.
         */
        @JvmStatic
        fun resizeBitmap(bitmap: Bitmap?, point: Point?): Bitmap {
            // Null point threw NPE here in Java as well.
            val size = point!!
            return ThumbnailUtils.extractThumbnail(bitmap, size.x, size.y)
        }

        /**
         * Wrapper for {@link #compressAndSaveBitmap(Bitmap, int, String)} with `{@link #COMPRESS_QUALITY} `quality`.
         */
        @JvmStatic
        fun compressAndSaveBitmap(bitmap: Bitmap?, path: String?): Error? {
            return compressAndSaveBitmap(bitmap, COMPRESS_QUALITY, path)
        }

        /**
         * Wrapper for {@link #compressAndSaveBitmap(Bitmap, CompressFormat, int, String)} with `{@link Bitmap.CompressFormat#JPEG}` `format`.
         */
        @JvmStatic
        fun compressAndSaveBitmap(bitmap: Bitmap?, quality: Int, path: String?): Error? {
            return compressAndSaveBitmap(bitmap, Bitmap.CompressFormat.JPEG, quality, path)
        }

        /**
         * Compress the given bitmap image file for given format and quality.
         *
         * @param bitmap  Original source bitmap.
         * @param foramt  The format for image compression.
         * @param quality Hint to the compressor, 0-100. The value is interpreted differently
         *               depending on the {@link Bitmap.CompressFormat}.
         * @param path    The path to store compressed bitmap.
         * @return Returns the {@code error} if compression and save operation was not successful,
         * otherwise {@code null}.
         */
        @JvmStatic
        fun compressAndSaveBitmap(bitmap: Bitmap?, foramt: CompressFormat?, quality: Int, path: String?): Error? {
            FileUtils.deleteRegularFile(null, path, true)
            var error = FileUtils.createRegularFile(path)
            if (error != null)
                return error
            try {
                FileOutputStream(path).use { out ->
                    // Null bitmap or format threw NPE here, caught below like Java.
                    bitmap!!.compress(foramt!!, quality, out)
                }
            } catch (e: Exception) {
                FileUtils.deleteRegularFile(null, path, true)
                // Fixed: the 3-placeholder message was passed a single arg,
                // so formatting always threw and callers got the fallback.
                error = FileUtilsErrno.ERRNO_CREATING_FILE_FAILED_WITH_EXCEPTION.getError(e, "file", path, e.message)
            }
            return error
        }

        /**
         * Wrapper for {@link #getDrawable(String)} with `file.getAbsolutePath()` `path` of file.
         */
        @JvmStatic
        fun getDrawable(file: File?): Drawable? {
            // Null file threw NPE here in Java as well.
            val path = file!!.absolutePath
            return getDrawable(path)
        }

        /**
         * Create {@link BitmapDrawable} from specified file path.
         *
         * @param path The path file to load image bitmap drawable.
         * @return Drawable created from image file path.
         */
        @JvmStatic
        fun getDrawable(path: String?): Drawable? {
            return BitmapDrawable.createFromPath(path)
        }

        /**
         * Add an overlay color/tint on image with {@link BlendMode#DARKEN}.
         *
         * @param drawable The source image bitmap drawable.
         * @param color    Overlay color for image.
         */
        @JvmStatic
        fun addOverlay(drawable: Drawable?, color: Int) {
            // Null drawable threw NPE here in Java as well.
            val nonNullDrawable = drawable!!
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                nonNullDrawable.colorFilter = BlendModeColorFilter(color, BlendMode.DARKEN)
            } else {
                nonNullDrawable.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.DARKEN)
            }
        }

        /**
         * Wrapper for {@link #isImageOptimized(String, Point, int)} with `{@link #OPTIMALITY_TOLERANCE}` `tolerance`.
         */
        @JvmStatic
        fun isImageOptimized(path: String?, point: Point?): Boolean {
            return isImageOptimized(path, point, OPTIMALITY_TOLERANCE)
        }

        /**
         * Wrapper for {@link #isImageOptimized(String, int, int, int)} with `width` and `height` obtained from {@link Point}.
         */
        @JvmStatic
        fun isImageOptimized(path: String?, point: Point?, tolerance: Int): Boolean {
            // Null point threw NPE here in Java as well.
            val size = point!!
            return isImageOptimized(path, size.x, size.y, tolerance)
        }

        /**
         * Check whether the image file present at file location is optimized corresponding
         * to given width and height. It can tolorent error upto given value.
         *
         * @param path      The file path of image.
         * @param width     The required width of image file.
         * @param height    The required width of image file.
         * @param tolerance The tolerance value upto which diffrence is ignored.
         * @return Returns whether the given image is optimized or not.
         */
        @JvmStatic
        fun isImageOptimized(path: String?, width: Int, height: Int, tolerance: Int): Boolean {
            if (!FileUtils.regularFileExists(path, false)) {
                Logger.logInfo(LOG_TAG, "Image file $path does not exist.")
                return false
            }
            val opt = BitmapFactory.Options()
            opt.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, opt)
            val imgWidth = opt.outWidth
            val imgHeight = opt.outHeight
            return Math.abs(imgWidth - width) <= tolerance && Math.abs(imgHeight - height) <= tolerance
        }

        @JvmStatic
        fun isImage(path: String?): Boolean {
            if (!FileUtils.regularFileExists(path, false)) {
                Logger.logInfo(LOG_TAG, "Image file $path does not exist.")
                return false
            }
            val opt = BitmapFactory.Options()
            opt.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, opt)
            return opt.outWidth != -1 && opt.outHeight != -1
        }

        /**
         * Resize bitmap for {@link Configuration#ORIENTATION_PORTRAIT Portrait} and {@link
         * Configuration#ORIENTATION_LANDSCAPE Landscape} display view.
         * Also Compress the image bitmap before saving it to given path.
         *
         * @param bitmap The original bitmap image to resize and store.
         * @param point  Display resolution containing width and height.
         * @param path1  The path for storing image with width point.x and height point.y
         * @param path2  The path for storing image with width point.y and height point.x
         * @return Returns the {@code error} if save operation was not successful,
         * otherwise {@code null}.
         */
        @JvmStatic
        fun saveForDisplayResolution(bitmap: Bitmap?, point: Point?, path1: String?, path2: String?): Error? {
            // Null point threw NPE here in Java as well.
            val size = point!!
            var error: Error?
            val bitmap1 = resizeBitmap(bitmap, size)
            error = compressAndSaveBitmap(bitmap1, path1)
            if (error != null) {
                return error
            }
            val bitmap2 = resizeBitmap(bitmap, Point(size.y, size.x))
            error = compressAndSaveBitmap(bitmap2, path2)
            return error
        }

        /**
         * Check for the given {@link Drawable} whether it is instance of {@link
         * BitmapDrawable} or not.
         *
         * @param drawable The drawable to check.
         * @return Retruns whether drawable is bitmap drawable.
         */
        @JvmStatic
        fun isBitmapDrawable(drawable: Drawable?): Boolean {
            return drawable is BitmapDrawable
        }
    }
}
