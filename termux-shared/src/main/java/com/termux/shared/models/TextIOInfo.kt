package com.termux.shared.models

import android.graphics.Color
import android.graphics.Typeface
import androidx.annotation.Keep
import androidx.annotation.NonNull
import com.termux.shared.data.DataUtils
import java.io.Serializable

/**
 * An object that stored info for {@link com.termux.shared.activities.TextIOActivity}.
 * Max text limit is 95KB to prevent TransactionTooLargeException as per
 * {@link DataUtils#TRANSACTION_SIZE_LIMIT_IN_BYTES}. Larger size can be supported for in-app
 * transactions by storing {@link TextIOInfo} as a serialized object in a file like
 * {@link com.termux.shared.activities.ReportActivity} does.
 */
class TextIOInfo : Serializable {

    companion object {
        /**
         * Explicitly define `serialVersionUID` to prevent exceptions on deserialization.
         *
         * Like when calling `Bundle.getSerializable()` on Android.
         * `android.os.BadParcelableException: Parcelable encountered IOException reading a Serializable object` (name = <class_name>)
         * `java.io.InvalidClassException: <class_name>; local class incompatible`
         *
         * The `@Keep` annotation is necessary to prevent the field from being removed by proguard when
         * app is compiled, even if its kept during library compilation.
         *
         * **See Also:**
         * - https://docs.oracle.com/javase/8/docs/platform/serialization/spec/version.html#a6678
         * - https://docs.oracle.com/javase/8/docs/platform/serialization/spec/class.html#a4100
         */
        @Keep
        private const val serialVersionUID = 1L

        const val GENERAL_DATA_SIZE_LIMIT_IN_BYTES = 1000
        const val LABEL_SIZE_LIMIT_IN_BYTES = 4000
        const val TEXT_SIZE_LIMIT_IN_BYTES = 100000 - GENERAL_DATA_SIZE_LIMIT_IN_BYTES - LABEL_SIZE_LIMIT_IN_BYTES // < 100KB
    }

    /** The action for which {@link TextIOActivity} will be started. */
    val action: String

    /** The internal app component that is will start the {@link TextIOActivity}. */
    val sender: String

    /** The activity title. */
    var title: String? = null

    /** If back button should be shown in {@link android.app.ActionBar}. Backed explicitly since
     * the getter does not follow bean naming. */
    private var _showBackButtonInActionBar: Boolean = false

    /** If label is enabled. */
    var isLabelEnabled: Boolean = false

    /**
     * The label of text input set in {@link android.widget.TextView} that can be updated by user.
     * Max allowed length is {@link #LABEL_SIZE_LIMIT_IN_BYTES}.
     */
    var label: String? = null
        set(value) {
            field = DataUtils.getTruncatedCommandOutput(value, LABEL_SIZE_LIMIT_IN_BYTES, true, false, false)
        }

    /** The text size of label. Defaults to 14sp. */
    var labelSize: Int = 14
        set(value) {
            if (value > 0)
                field = value
        }

    /** The text color of label. Defaults to {@link Color#BLACK}. */
    var labelColor: Int = Color.BLACK

    /** The {@link Typeface} family  of label. Defaults to "sans-serif". */
    var labelTypeFaceFamily: String? = "sans-serif"

    /** The {@link Typeface} style  of label. Defaults to {@link Typeface#BOLD}. */
    var labelTypeFaceStyle: Int = Typeface.BOLD

    /**
     * The text of text input set in {@link android.widget.EditText} that can be updated by user.
     * Max allowed length is {@link #TEXT_SIZE_LIMIT_IN_BYTES}.
     */
    var text: String? = null
        set(value) {
            field = DataUtils.getTruncatedCommandOutput(value, TEXT_SIZE_LIMIT_IN_BYTES, true, false, false)
        }

    /** The text size for text. Defaults to 12sp. */
    var textSize: Int = 12
        set(value) {
            if (value > 0)
                field = value
        }

    /** The text size for text. Defaults to {@link #TEXT_SIZE_LIMIT_IN_BYTES}. */
    var textLengthLimit: Int = TEXT_SIZE_LIMIT_IN_BYTES
        set(value) {
            if (value < TEXT_SIZE_LIMIT_IN_BYTES)
                field = value
        }

    /** The text color of text. Defaults to {@link Color#BLACK}. */
    var textColor: Int = Color.BLACK

    /** The {@link Typeface} family for text. Defaults to "sans-serif". */
    var textTypeFaceFamily: String? = "sans-serif"

    /** The {@link Typeface} style for text. Defaults to {@link Typeface#NORMAL}. */
    var textTypeFaceStyle: Int = Typeface.NORMAL

    /** If horizontal scrolling should be enabled for text. Backed explicitly since the
     * getter/setter do not follow bean naming. */
    private var _textHorizontallyScrolling: Boolean = false

    /** If character usage should be enabled for text. Backed explicitly since the
     * getter/setter do not follow bean naming. */
    private var _showTextCharacterUsage: Boolean = false

    /** If editing text should be disabled so that text acts like its in a {@link android.widget.TextView}. */
    var isEditingTextDisabled: Boolean = false

    constructor(@NonNull action: String, @NonNull sender: String) {
        this.action = action
        this.sender = sender
    }

    fun shouldShowBackButtonInActionBar(): Boolean {
        return _showBackButtonInActionBar
    }

    fun setShowBackButtonInActionBar(showBackButtonInActionBar: Boolean) {
        _showBackButtonInActionBar = showBackButtonInActionBar
    }

    fun isHorizontallyScrollable(): Boolean {
        return _textHorizontallyScrolling
    }

    fun setTextHorizontallyScrolling(textHorizontallyScrolling: Boolean) {
        _textHorizontallyScrolling = textHorizontallyScrolling
    }

    fun shouldShowTextCharacterUsage(): Boolean {
        return _showTextCharacterUsage
    }

    fun setShowTextCharacterUsage(showTextCharacterUsage: Boolean) {
        _showTextCharacterUsage = showTextCharacterUsage
    }
}
