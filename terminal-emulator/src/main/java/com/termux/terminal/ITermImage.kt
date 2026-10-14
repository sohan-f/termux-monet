package com.termux.terminal

import android.util.Base64

/**
 * An iTerm image received via `OSC 1337`.
 *
 * - https://iterm2.com/documentation-images.html
 */
open class ITermImage(client: TerminalSessionClient?, isMultiPart: Boolean) {

    /**
     * The {@link Enum} that defines {@link ITermImage} state.
     */
    enum class ImageState(private val stateName: String, private val stateValue: Int) {

        INIT("init", 0),
        ARGUMENTS_READ("arguments_read", 1),
        IMAGE_READING("image_reading", 2),
        IMAGE_READ("image_read", 3),
        IMAGE_DECODED("image_decoded", 4),
        FAILED("Failed", 5);

        fun getName(): String {
            return stateName
        }

        fun getValue(): Int {
            return stateValue
        }
    }

    companion object {
        const val LOG_TAG = "ITermImage"
    }

    @JvmField
    protected val mClient: TerminalSessionClient? = client

    @JvmField
    protected val mIsMultipart: Boolean = isMultiPart

    @JvmField
    protected var mWidth = -1
    @JvmField
    protected var mHeight = -1

    @JvmField
    protected var mInline = false

    @JvmField
    protected var mPreserveAspectRatio = true

    @JvmField
    protected val mEncodedImage = StringBuilder(/* Initial capacity. */ 4096)
    @JvmField
    protected var mDecodedImage: ByteArray? = null

    /** The current state of the {@link ImageState}. */
    @JvmField
    protected var mCurrentState: ImageState = ImageState.INIT
    /** The previous state of the {@link ImageState}. */
    @JvmField
    protected var mPreviousState: ImageState = ImageState.INIT

    fun getClient(): TerminalSessionClient? {
        return mClient
    }

    fun isMultipart(): Boolean {
        return mIsMultipart
    }

    fun getWidth(): Int {
        return mWidth
    }

    fun getHeight(): Int {
        return mHeight
    }

    fun isInline(): Boolean {
        return mInline
    }

    fun shouldPreserveAspectRatio(): Boolean {
        return mPreserveAspectRatio
    }

    fun getEncodedImage(): String {
        return mEncodedImage.toString()
    }

    fun getDecodedImage(): ByteArray? {
        return mDecodedImage
    }

    @Synchronized
    fun getCurrentState(): ImageState {
        return mCurrentState
    }

    @Synchronized
    fun getPreviousState(): ImageState {
        return mPreviousState
    }

    @Synchronized
    protected fun setState(newState: ImageState): Boolean {
        // The state transition cannot go back or change if already at `ImageState.IMAGE_DECODED`
        if (newState.getValue() < mCurrentState.getValue() || mCurrentState == ImageState.IMAGE_DECODED) {
            Logger.logError(mClient, LOG_TAG,
                "Invalid image state transition from \"" + mCurrentState.getName() + "\" to " + "\"" + newState.getName() + "\"")
            return false
        }

        // The `ImageState.FAILED` can be set again, like to add more errors, but we don't update
        // `mPreviousState` with the `mCurrentState` value if its at `ImageState.FAILED` to
        // preserve the last valid state.
        if (mCurrentState != ImageState.FAILED)
            mPreviousState = mCurrentState

        mCurrentState = newState
        return true
    }

    @Synchronized
    protected fun setStateFailed(error: String?): Boolean {
        if (error != null) {
            Logger.logError(mClient, LOG_TAG, error)
        }
        return setState(ImageState.FAILED)
    }

    @Synchronized
    protected fun ensureState(expectedState: ImageState): Boolean {
        return ensureState(expectedState, null)
    }

    @Synchronized
    protected fun ensureState(expectedState: ImageState, functionName: String?): Boolean {
        if (mCurrentState != expectedState) {
            Logger.logError(mClient, LOG_TAG,
                "The current image state is \"" + mCurrentState.getName() + "\" but expected \"" + expectedState.getName() + "\"" +
                    (if (functionName != null) " while calling '" + functionName else "'") +
                    " for " + (if (!mIsMultipart) "singlepart" else "multipart") + " image")
            return false
        }
        return true
    }

    @Synchronized
    fun isArgumentsRead(): Boolean {
        return mCurrentState == ImageState.ARGUMENTS_READ
    }

    @Synchronized
    fun isImageReading(): Boolean {
        return mCurrentState == ImageState.IMAGE_READING
    }

    @Synchronized
    fun isImageRead(): Boolean {
        return mCurrentState == ImageState.IMAGE_READ
    }

    @Synchronized
    fun isImageDecoded(): Boolean {
        return mCurrentState == ImageState.IMAGE_DECODED
    }

    @Synchronized
    fun readArguments(terminalEmulator: TerminalEmulator, oscArgs: StringBuilder, index: Int): Int {
        var index = index
        if (!ensureState(ImageState.INIT, "ImageState.readArguments()")) {
            return -1
        }

        var lastParam = false
        while (index < oscArgs.length) {
            val ch = oscArgs[index]
            // End of optional arguments.
            if (ch == ':' && !mIsMultipart) {
                break
            } else if (ch == ' ') {
                index++
                continue
            }

            val keyEndIndex = oscArgs.indexOf("=", index)
            if (keyEndIndex == -1) {
                setStateFailed("The key for an argument not found after index " + index + " in osc argument string: " + oscArgs)
                return -1
            }
            val argKey = oscArgs.substring(index, keyEndIndex)

            var valueEndIndex = oscArgs.indexOf(";", keyEndIndex)
            if (valueEndIndex == -1) {
                if (!mIsMultipart) {
                    // The last key value for `File=` command arguments may end with a colon `:` instead of a semi colon `;`.
                    valueEndIndex = oscArgs.indexOf(":", keyEndIndex)
                    if (valueEndIndex == -1) {
                        setStateFailed("The value for an argument not found after index " + index + " in osc argument string: " + oscArgs)
                        return -1
                    } else {
                        index = valueEndIndex
                        lastParam = true
                    }
                } else {
                    // The last key value for `MultipartFile=` command arguments may end without a semi colon `;`.
                    valueEndIndex = oscArgs.length
                    index = valueEndIndex
                }
            } else {
                index = valueEndIndex + 1
            }

            if (valueEndIndex <= keyEndIndex) {
                setStateFailed("The argument key end index " + keyEndIndex +
                    " is <= value end index " + valueEndIndex + " in osc argument string: " + oscArgs)
                return -1
            }

            val argValue = oscArgs.substring(keyEndIndex + 1, valueEndIndex)

            if (argKey.equals("inline", ignoreCase = true)) {
                mInline = argValue == "1"
            } else if (argKey.equals("preserveAspectRatio", ignoreCase = true)) {
                mPreserveAspectRatio = argValue != "0"
            } else if (argKey.equals("width", ignoreCase = true)) {
                var factor: Double = terminalEmulator.getCellWidthPixels().toDouble()
                var intValueEndIndex = argValue.length
                if (argValue.endsWith("px")) {
                    factor = 1.0
                    intValueEndIndex -= 2
                } else if (argValue.endsWith("%")) {
                    factor = 0.01 * terminalEmulator.getCellWidthPixels() * terminalEmulator.getColumns()
                    intValueEndIndex -= 1
                }
                try {
                    mWidth = (factor * Integer.parseInt(argValue.substring(0, intValueEndIndex))).toInt()
                } catch (e: Exception) {
                }
            } else if (argKey.equals("height", ignoreCase = true)) {
                var factor: Double = terminalEmulator.getCellHeightPixels().toDouble()
                var intValueEndIndex = argValue.length
                if (argValue.endsWith("px")) {
                    factor = 1.0
                    intValueEndIndex -= 2
                } else if (argValue.endsWith("%")) {
                    factor = 0.01 * terminalEmulator.getCellHeightPixels() * terminalEmulator.getRows()
                    intValueEndIndex -= 1
                }
                try {
                    mHeight = (factor * Integer.parseInt(argValue.substring(0, intValueEndIndex))).toInt()
                } catch (e: Exception) {
                }
            } else {
                // `name` and `size` keys are not supported.
            }

            if (lastParam) {
                break
            }
        }

        setState(ImageState.ARGUMENTS_READ)

        return index
    }

    @Synchronized
    fun readImage(oscArgs: StringBuilder, index: Int): Boolean {
        if (!mIsMultipart) {
            if (!ensureState(ImageState.ARGUMENTS_READ, "ImageState.readImage()")) {
                return false
            }

            if (index < oscArgs.length) {
                val colonIndex = oscArgs.indexOf(":", index)
                if (colonIndex >= 0 && colonIndex + 1 < oscArgs.length) {
                    setState(ImageState.IMAGE_READING)
                    val imageStartIndex = colonIndex + 1

                    try {
                        // Appending can cause an increase in capacity and cause an OOM.
                        mEncodedImage.append(oscArgs.substring(imageStartIndex))
                    } catch (t: Throwable) {
                        if (t is OutOfMemoryError) System.gc()
                        setStateFailed("Collecting singlepart image" + " in osc argument string failed: " + t.message)
                        return false
                    }

                    setState(ImageState.IMAGE_READ)
                    return true
                }
            }

            setStateFailed("Failed to read singlepart image from index " + index + " in osc argument string: " + oscArgs)
            return false
        } else {
            if (mCurrentState != ImageState.IMAGE_READING &&
                !ensureState(ImageState.ARGUMENTS_READ, "ImageState.readImage()")) {
                return false
            }

            // An empty `FilePart=` command could be received as well, so change state before `if` below.
            setState(ImageState.IMAGE_READING)

            if (index < oscArgs.length) {
                try {
                    // Appending can cause an increase in capacity and cause an OOM.
                    mEncodedImage.append(oscArgs.substring(index))
                } catch (t: Throwable) {
                    if (t is OutOfMemoryError) System.gc()
                    setStateFailed("Collecting multipart image" + " in osc argument string failed: " + t.message)
                    return false
                }
                return true
            }

            setStateFailed("Failed to read multipart image" + " in osc argument string: " + oscArgs)
            return false
        }
    }

    @Synchronized
    fun setMultiPartImageRead(): Boolean {
        if (!mIsMultipart) {
            Logger.logError(mClient, LOG_TAG, "Attempting to call 'ImageState.setMultiPartImageRead()' for a singlepart image")
            return false
        }

        // A `FileEnd` command may have been received without a `FilePart=` command preceding it.
        if (!ensureState(ImageState.IMAGE_READING, "ImageState.setMultiPartImageRead()")) {
            return false
        }

        setState(ImageState.IMAGE_READ)
        return true
    }

    @Synchronized
    fun decodeImage(): Boolean {
        if (!ensureState(ImageState.IMAGE_READ, "ImageState.decodeImage()")) {
            return false
        }

        var encodedImageString: String? = null
        try {
            if (mEncodedImage.length < 1) {
                setStateFailed("Cannot decoded an empty image")
                return false
            }

            while (mEncodedImage.length % 4 != 0) {
                mEncodedImage.append('=')
            }

            encodedImageString = mEncodedImage.toString()

            // Clear original encoded image from memory as it is no longer needed.
            mEncodedImage.setLength(0)
            mEncodedImage.trimToSize()

            mDecodedImage = Base64.decode(encodedImageString, Base64.DEFAULT)
            val decodedImage = mDecodedImage
            if (decodedImage == null || decodedImage.size < 2) {
                setStateFailed("The decoded image is not valid: " + decodedImage?.contentToString() + "\nimage: " + encodedImageString)
                return false
            }

            setState(ImageState.IMAGE_DECODED)
            return true
        } catch (t: Throwable) {
            if (t is OutOfMemoryError) {
                Logger.logError(mClient, LOG_TAG, "Failed to decode image: " + t.message)
                System.gc()
            } else {
                Logger.logStackTraceWithMessage(mClient, LOG_TAG, "Failed to decode image: " + encodedImageString, t)
            }
            setStateFailed(null)
            return false
        }
    }
}
