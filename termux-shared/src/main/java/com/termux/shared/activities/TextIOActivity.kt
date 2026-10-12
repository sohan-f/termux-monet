package com.termux.shared.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.termux.shared.R
import com.termux.shared.interact.ShareUtils
import com.termux.shared.logger.Logger
import com.termux.shared.models.TextIOInfo
import com.termux.shared.view.KeyboardUtils
import java.util.Locale

/**
 * An activity to edit or view text based on config passed as {@link TextIOInfo}.
 *
 * Add Following to `AndroidManifest.xml` to use in an app:
 *
 * {@code ` <activity android:name="com.termux.shared.activities.TextIOActivity" android:theme="@style/Theme.AppCompat.TermuxTextIOActivity" />` }
 */
class TextIOActivity : AppCompatActivity() {

    private lateinit var mTextIOLabel: TextView

    private lateinit var mTextIOLabelSeparator: View

    private lateinit var mTextIOText: EditText

    private lateinit var mTextIOHorizontalScrollView: HorizontalScrollView

    private lateinit var mTextIOTextLinearLayout: LinearLayout

    private lateinit var mTextIOTextCharacterUsage: TextView

    private var mTextIOInfo: TextIOInfo? = null

    private var mBundle: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.logVerbose(LOG_TAG, "onCreate")
        setContentView(R.layout.activity_text_io)
        mTextIOLabel = findViewById(R.id.text_io_label)
        mTextIOLabelSeparator = findViewById(R.id.text_io_label_separator)
        mTextIOText = findViewById(R.id.text_io_text)
        mTextIOHorizontalScrollView = findViewById(R.id.text_io_horizontal_scroll_view)
        mTextIOTextLinearLayout = findViewById(R.id.text_io_text_linear_layout)
        mTextIOTextCharacterUsage = findViewById(R.id.text_io_text_character_usage)
        val toolbar: Toolbar? = findViewById(R.id.toolbar)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
        }
        mBundle = null
        val intent = getIntent()
        // Prefer the saved state: it holds the current (possibly edited) text,
        // while the original intent only has the initial text. The Java version
        // read the intent first, silently dropping edits on configuration change.
        mBundle = savedInstanceState ?: intent?.extras
        updateUI()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Logger.logVerbose(LOG_TAG, "onNewIntent")
        // Views must be re-created since different configs for isEditingTextDisabled() and
        // isHorizontallyScrollable() will not work or at least reliably
        finish()
        startActivity(intent)
    }

    @SuppressLint("ClickableViewAccessibility")
    @Suppress("DEPRECATION") // Bundle.getSerializable(String) must stay: the typed overload is API 33+
    private fun updateUI() {
        if (mBundle == null) {
            finish()
            return
        }
        // Safe cast: a wrong-typed extra finishes the activity instead of
        // throwing ClassCastException.
        mTextIOInfo = mBundle!!.getSerializable(EXTRA_TEXT_IO_INFO_OBJECT) as? TextIOInfo
        if (mTextIOInfo == null) {
            finish()
            return
        }
        val info = mTextIOInfo!!
        val actionBar: ActionBar? = supportActionBar
        if (actionBar != null) {
            if (info.title != null)
                actionBar.setTitle(info.title)
            else
                actionBar.setTitle("Text Input")
            if (info.shouldShowBackButtonInActionBar()) {
                actionBar.setDisplayHomeAsUpEnabled(true)
                actionBar.setDisplayShowHomeEnabled(true)
            }
        }
        mTextIOLabel.visibility = View.GONE
        mTextIOLabelSeparator.visibility = View.GONE
        if (info.isLabelEnabled) {
            mTextIOLabel.visibility = View.VISIBLE
            mTextIOLabelSeparator.visibility = View.VISIBLE
            mTextIOLabel.text = info.label
            mTextIOLabel.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(TextIOInfo.LABEL_SIZE_LIMIT_IN_BYTES))
            mTextIOLabel.setTextSize(info.labelSize.toFloat())
            mTextIOLabel.setTextColor(info.labelColor)
            mTextIOLabel.setTypeface(Typeface.create(info.labelTypeFaceFamily, info.labelTypeFaceStyle))
        }
        if (info.isHorizontallyScrollable()) {
            mTextIOHorizontalScrollView.isEnabled = true
            mTextIOText.setHorizontallyScrolling(true)
        } else {
            // Remove mTextIOHorizontalScrollView and add mTextIOText in its place
            val parent = mTextIOHorizontalScrollView.parent as ViewGroup?
            if (parent != null && parent.indexOfChild(mTextIOText) < 0) {
                val params = mTextIOHorizontalScrollView.layoutParams
                val index = parent.indexOfChild(mTextIOHorizontalScrollView)
                mTextIOTextLinearLayout.removeAllViews()
                mTextIOHorizontalScrollView.removeAllViews()
                parent.removeView(mTextIOHorizontalScrollView)
                parent.addView(mTextIOText, index, params)
                mTextIOText.setHorizontallyScrolling(false)
            }
        }
        mTextIOText.setText(info.text)
        mTextIOText.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(info.textLengthLimit))
        mTextIOText.setTextSize(info.textSize.toFloat())
        mTextIOText.setTextColor(info.textColor)
        mTextIOText.setTypeface(Typeface.create(info.textTypeFaceFamily, info.textTypeFaceStyle))
        // setTextIsSelectable must be called after changing KeyListener to regain focusability and selectivity
        if (info.isEditingTextDisabled) {
            mTextIOText.isCursorVisible = false
            mTextIOText.keyListener = null
            mTextIOText.setTextIsSelectable(true)
        }
        if (info.shouldShowTextCharacterUsage()) {
            mTextIOTextCharacterUsage.visibility = View.VISIBLE
            updateTextIOTextCharacterUsage(info.text)
            mTextIOText.addTextChangedListener(object : TextWatcher {

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(editable: Editable?) {
                    if (editable != null)
                        updateTextIOTextCharacterUsage(editable.toString())
                }
            })
        } else {
            mTextIOTextCharacterUsage.visibility = View.GONE
            mTextIOText.addTextChangedListener(null)
        }
    }

    private fun updateTextIOInfoText() {
        // mTextIOInfo is null when updateUI bailed out early; saving nothing
        // must not crash state saving or finishing then.
        val info = mTextIOInfo ?: return
        if (::mTextIOText.isInitialized)
            info.text = mTextIOText.text.toString()
    }

    private fun updateTextIOTextCharacterUsage(text: String?) {
        if (!::mTextIOTextCharacterUsage.isInitialized)
            return
        val info = mTextIOInfo ?: return
        val content = text ?: ""
        mTextIOTextCharacterUsage.setText(String.format(Locale.getDefault(), "%1\$d/%2\$d", content.length, info.textLengthLimit))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        updateTextIOInfoText()
        outState.putSerializable(EXTRA_TEXT_IO_INFO_OBJECT, mTextIOInfo)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_text_io, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var text = ""
        if (::mTextIOText.isInitialized)
            text = mTextIOText.text.toString()
        val id = item.itemId
        if (id == android.R.id.home) {
            confirm()
        }
        if (id == R.id.menu_item_cancel) {
            cancel()
        } else if (id == R.id.menu_item_share_text) {
            // Null-safe title: the activity may already be finishing without info.
            ShareUtils.shareText(this, mTextIOInfo?.title, text)
        } else if (id == R.id.menu_item_copy_text) {
            ShareUtils.copyTextToClipboard(this, text, null)
        }
        return false
    }

    @Suppress("OVERRIDE_DEPRECATION") // Old callback kept for minSdk compat
    override fun onBackPressed() {
        confirm()
    }

    /**
     * Confirm current text and send it back to calling {@link Activity}.
     */
    private fun confirm() {
        updateTextIOInfoText()
        KeyboardUtils.hideSoftKeyboard(this, mTextIOText)
        setResult(Activity.RESULT_OK, getResultIntent())
        finish()
    }

    /**
     * Cancel current text and notify calling {@link Activity}.
     */
    private fun cancel() {
        KeyboardUtils.hideSoftKeyboard(this, mTextIOText)
        setResult(Activity.RESULT_CANCELED, getResultIntent())
        finish()
    }

    private fun getResultIntent(): Intent {
        val intent = Intent()
        val bundle = Bundle()
        bundle.putSerializable(EXTRA_TEXT_IO_INFO_OBJECT, mTextIOInfo)
        intent.putExtras(bundle)
        return intent
    }

    companion object {
        // Fixed copy-paste: this was built from ReportActivity's class name.
        // Safe to change since nothing reads or writes this key yet.
        private val CLASS_NAME: String = TextIOActivity::class.java.canonicalName ?: "TextIOActivity"

        @JvmField
        val EXTRA_TEXT_IO_INFO_OBJECT: String = "$CLASS_NAME.EXTRA_TEXT_IO_INFO_OBJECT"

        private const val LOG_TAG = "TextIOActivity"

        /**
         * Get the {@link Intent} that can be used to start the {@link TextIOActivity}.
         *
         * @param context The {@link Context} for operations.
         * @param textIOInfo The {@link TextIOInfo} containing info for the edit text.
         */
        @JvmStatic
        fun newInstance(@NonNull context: Context, @NonNull textIOInfo: TextIOInfo): Intent {
            val intent = Intent(context, TextIOActivity::class.java)
            val bundle = Bundle()
            bundle.putSerializable(EXTRA_TEXT_IO_INFO_OBJECT, textIOInfo)
            intent.putExtras(bundle)
            return intent
        }
    }
}
