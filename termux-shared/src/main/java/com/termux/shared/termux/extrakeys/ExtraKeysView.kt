package com.termux.shared.termux.extrakeys

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.GridLayout
import android.widget.PopupWindow
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.google.android.material.button.MaterialButton
import com.termux.shared.R
import com.termux.shared.theme.ThemeUtils
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * A {@link View} showing extra keys (such as Escape, Ctrl, Alt) not normally available on an Android soft
 * keyboards.
 *
 * To use it, add following to a layout file and import it in your activity layout file or inflate
 * it with a {@link androidx.viewpager.widget.ViewPager}.:
 * {@code
 * <?xml version="1.0" encoding="utf-8"?>
 * <com.termux.shared.termux.extrakeys.ExtraKeysView xmlns:android="http://schemas.android.com/apk/res/android"
 *     android:id="@+id/extra_keys"
 *     style="?android:attr/buttonBarStyle"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     android:layout_alignParentBottom="true"
 *     android:orientation="horizontal" />
 * }
 *
 * Then in your activity, get its reference by a call to {@link android.app.Activity#findViewById(int)}
 * or {@link LayoutInflater#inflate(int, ViewGroup)} if using {@link androidx.viewpager.widget.ViewPager}.
 * Then call {@link #setExtraKeysViewClient(IExtraKeysView)} and pass it the implementation of
 * {@link IExtraKeysView} so that you can receive callbacks. You can also override other values set
 * in {@link ExtraKeysView#ExtraKeysView(Context, AttributeSet)} by calling the respective functions.
 * If you extend {@link ExtraKeysView}, you can also set them in the constructor, but do call super().
 *
 * After this you will have to make a call to {@link ExtraKeysView#reload(ExtraKeysInfo, float) and pass
 * it the {@link ExtraKeysInfo} to load and display the extra keys. Read its class javadocs for more
 * info on how to create it.
 *
 * Termux app defines the view in res/layout/view_terminal_toolbar_extra_keys and
 * inflates it in TerminalToolbarViewPager.instantiateItem() and sets the {@link ExtraKeysView} client
 * and calls {@link ExtraKeysView#reload(ExtraKeysInfo).
 * The {@link ExtraKeysInfo} is created by TermuxAppSharedProperties.setExtraKeys().
 * Then its got and the view height is adjusted in TermuxActivity.setTerminalToolbarHeight().
 * The client used is TermuxTerminalExtraKeys, which extends
 * {@link TerminalExtraKeys } to handle Termux app specific logic and
 * leave the rest to the super class.
 */
class ExtraKeysView(context: Context, attrs: AttributeSet?) : GridLayout(context, attrs) {

    /**
     * The client for the {@link ExtraKeysView}.
     */
    interface IExtraKeysView {

        /**
         * This is called by {@link ExtraKeysView} when a button is clicked. This is also called
         * for {@link #mRepetitiveKeys} and {@link ExtraKeyButton} that have a popup set.
         * However, this is not called for {@link #mSpecialButtons}, whose state can instead be read
         * via a call to {@link #readSpecialButton(SpecialButton, boolean)}.
         *
         * @param view The view that was clicked.
         * @param buttonInfo The {@link ExtraKeyButton} for the button that was clicked.
         *                   The button may be a {@link ExtraKeyButton#KEY_MACRO} set which can be
         *                   checked with a call to {@link ExtraKeyButton#isMacro()}.
         * @param button The {@link MaterialButton} that was clicked.
         */
        fun onExtraKeyButtonClick(view: View?, buttonInfo: ExtraKeyButton?, button: MaterialButton?)

        /**
         * This is called by {@link ExtraKeysView} when a button is clicked so that the client
         * can perform any hepatic feedback. This is only called in the {@link MaterialButton.OnClickListener}
         * and not for every repeat. Its also called for {@link #mSpecialButtons}.
         *
         * @param view The view that was clicked.
         * @param buttonInfo The {@link ExtraKeyButton} for the button that was clicked.
         * @param button The {@link MaterialButton} that was clicked.
         * @return Return {@code true} if the client handled the feedback, otherwise {@code false}
         * so that {@link ExtraKeysView#performExtraKeyButtonHapticFeedback(View, ExtraKeyButton, MaterialButton)}
         * can handle it depending on system settings.
         */
        fun performExtraKeyButtonHapticFeedback(view: View?, buttonInfo: ExtraKeyButton?, button: MaterialButton?): Boolean
    }

    companion object {
        /**
         * Defines the default value for {@link #mButtonTextColor} defined by current theme.
         */
        @JvmField
        val ATTR_BUTTON_TEXT_COLOR: Int = R.attr.extraKeysButtonTextColor

        /**
         * Defines the default value for {@link #mButtonActiveTextColor} defined by current theme.
         */
        @JvmField
        val ATTR_BUTTON_ACTIVE_TEXT_COLOR: Int = R.attr.extraKeysButtonActiveTextColor

        /**
         * Defines the default value for {@link #mButtonBackgroundColor} defined by current theme.
         */
        @JvmField
        val ATTR_BUTTON_BACKGROUND_COLOR: Int = R.attr.extraKeysButtonBackgroundColor

        /**
         * Defines the default value for {@link #mButtonActiveBackgroundColor} defined by current theme.
         */
        @JvmField
        val ATTR_BUTTON_ACTIVE_BACKGROUND_COLOR: Int = R.attr.extraKeysButtonActiveBackgroundColor

        /**
         * Defines the default fallback value for {@link #mButtonTextColor} if {@link #ATTR_BUTTON_TEXT_COLOR} is undefined.
         */
        @JvmField
        val DEFAULT_BUTTON_TEXT_COLOR: Int = 0xFFFFFFFF.toInt()

        /**
         * Defines the default fallback value for {@link #mButtonActiveTextColor} if {@link #ATTR_BUTTON_ACTIVE_TEXT_COLOR} is undefined.
         */
        @JvmField
        val DEFAULT_BUTTON_ACTIVE_TEXT_COLOR: Int = 0xFF80DEEA.toInt()

        /**
         * Defines the default fallback value for {@link #mButtonBackgroundColor} if {@link #ATTR_BUTTON_BACKGROUND_COLOR} is undefined.
         */
        const val DEFAULT_BUTTON_BACKGROUND_COLOR = 0x00000000

        /**
         * Defines the default fallback value for {@link #mButtonActiveBackgroundColor} if {@link #ATTR_BUTTON_ACTIVE_BACKGROUND_COLOR} is undefined.
         */
        @JvmField
        val DEFAULT_BUTTON_ACTIVE_BACKGROUND_COLOR: Int = 0xFF7F7F7F.toInt()

        /**
         * Defines the minimum allowed duration in milliseconds for {@link #mLongPressTimeout}.
         */
        const val MIN_LONG_PRESS_DURATION = 200

        /**
         * Defines the maximum allowed duration in milliseconds for {@link #mLongPressTimeout}.
         */
        const val MAX_LONG_PRESS_DURATION = 3000

        /**
         * Defines the fallback duration in milliseconds for {@link #mLongPressTimeout}.
         */
        const val FALLBACK_LONG_PRESS_DURATION = 400

        /**
         * Defines the minimum allowed duration in milliseconds for {@link #mLongPressRepeatDelay}.
         */
        const val MIN_LONG_PRESS__REPEAT_DELAY = 5

        /**
         * Defines the maximum allowed duration in milliseconds for {@link #mLongPressRepeatDelay}.
         */
        const val MAX_LONG_PRESS__REPEAT_DELAY = 2000

        /**
         * Defines the default duration in milliseconds for {@link #mLongPressRepeatDelay}.
         */
        const val DEFAULT_LONG_PRESS_REPEAT_DELAY = 80

        /**
         * General util function to compute the longest column length in a matrix.
         */
        @JvmStatic
        fun maximumLength(matrix: Array<out Array<*>>): Int {
            var m = 0
            for (row in matrix) m = Math.max(m, row.size)
            return m
        }
    }

    /**
     * The implementation of the {@link IExtraKeysView} that acts as a client for the {@link ExtraKeysView}.
     */
    @JvmField
    protected var mExtraKeysViewClient: IExtraKeysView? = null

    /**
     * The map for the {@link SpecialButton} and their {@link SpecialButtonState}. Defaults to
     * the one returned by {@link #getDefaultSpecialButtons(ExtraKeysView)}.
     */
    @JvmField
    protected var mSpecialButtons: Map<SpecialButton, SpecialButtonState>? = null

    /**
     * The keys for the {@link SpecialButton} added to {@link #mSpecialButtons}. This is automatically
     * set when the call to {@link #setSpecialButtons(Map)} is made.
     */
    @JvmField
    protected var mSpecialButtonsKeys: Set<String>? = null

    /**
     * The list of keys for which auto repeat of key should be triggered if its extra keys button
     * is long pressed. This is done by calling {@link IExtraKeysView#onExtraKeyButtonClick(View, ExtraKeyButton, MaterialButton)}
     * every {@link #mLongPressRepeatDelay} seconds after {@link #mLongPressTimeout} has passed.
     * The default keys are defined by {@link ExtraKeysConstants#PRIMARY_REPETITIVE_KEYS}.
     */
    @JvmField
    protected var mRepetitiveKeys: List<String>? = null

    /**
     * The text color for the extra keys button. Defaults to {@link #DEFAULT_BUTTON_TEXT_COLOR}.
     */
    @JvmField
    protected var mButtonTextColor: Int = 0

    /**
     * The text color for the extra keys button when its active.
     * Defaults to {@link #DEFAULT_BUTTON_ACTIVE_TEXT_COLOR}.
     */
    @JvmField
    protected var mButtonActiveTextColor: Int = 0

    /**
     * The background color for the extra keys button. Defaults to {@link #DEFAULT_BUTTON_BACKGROUND_COLOR}.
     */
    @JvmField
    protected var mButtonBackgroundColor: Int = 0

    /**
     * The background color for the extra keys button when its active. Defaults to
     * {@link #DEFAULT_BUTTON_ACTIVE_BACKGROUND_COLOR}.
     */
    @JvmField
    protected var mButtonActiveBackgroundColor: Int = 0

    /**
     * Defines whether text for the extra keys button should be all capitalized automatically.
     */
    @JvmField
    protected var mButtonTextAllCaps = true

    /**
     * Defines the duration in milliseconds before a press turns into a long press. The default
     * duration used is the one returned by a call to {@link ViewConfiguration#getLongPressTimeout()}
     * which will return the system defined duration which can be changed in accessibility settings.
     * The duration must be in between {@link #MIN_LONG_PRESS_DURATION} and {@link #MAX_LONG_PRESS_DURATION},
     * otherwise {@link #FALLBACK_LONG_PRESS_DURATION} is used.
     */
    @JvmField
    protected var mLongPressTimeout: Int = 0

    /**
     * Defines the duration in milliseconds for the delay between trigger of each repeat of
     * {@link #mRepetitiveKeys}. The default value is defined by {@link #DEFAULT_LONG_PRESS_REPEAT_DELAY}.
     * The duration must be in between {@link #MIN_LONG_PRESS__REPEAT_DELAY} and
     * {@link #MAX_LONG_PRESS__REPEAT_DELAY}, otherwise {@link #DEFAULT_LONG_PRESS_REPEAT_DELAY} is used.
     */
    @JvmField
    protected var mLongPressRepeatDelay: Int = 0

    /**
     * The popup window shown if {@link ExtraKeyButton#getPopup()} returns a {@code non-null} value
     * and a swipe up action is done on an extra key.
     */
    @JvmField
    protected var mPopupWindow: PopupWindow? = null

    @JvmField
    protected var mScheduledExecutor: ScheduledExecutorService? = null

    @JvmField
    protected var mHandler: Handler? = null

    @JvmField
    protected var mSpecialButtonsLongHoldRunnable: SpecialButtonsLongHoldRunnable? = null

    @JvmField
    protected var mLongPressCount: Int = 0

    @JvmField
    protected var mAccessibilityEnabled = false

    init {
        setRepetitiveKeys(ExtraKeysConstants.PRIMARY_REPETITIVE_KEYS)
        setSpecialButtons(getDefaultSpecialButtons(this))
        setButtonColors(ThemeUtils.getSystemAttrColor(context, ATTR_BUTTON_TEXT_COLOR, DEFAULT_BUTTON_TEXT_COLOR), ThemeUtils.getSystemAttrColor(context, ATTR_BUTTON_ACTIVE_TEXT_COLOR, DEFAULT_BUTTON_ACTIVE_TEXT_COLOR), ThemeUtils.getSystemAttrColor(context, ATTR_BUTTON_BACKGROUND_COLOR, DEFAULT_BUTTON_BACKGROUND_COLOR), ThemeUtils.getSystemAttrColor(context, ATTR_BUTTON_ACTIVE_BACKGROUND_COLOR, DEFAULT_BUTTON_ACTIVE_BACKGROUND_COLOR))
        setLongPressTimeout(ViewConfiguration.getLongPressTimeout())
        setLongPressRepeatDelay(DEFAULT_LONG_PRESS_REPEAT_DELAY)

        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager?
        // Null service threw NPE below in Java as well.
        mAccessibilityEnabled = am!!.isEnabled
    }

    /**
     * Get {@link #mExtraKeysViewClient}.
     */
    fun getExtraKeysViewClient(): IExtraKeysView? {
        return mExtraKeysViewClient
    }

    /**
     * Set {@link #mExtraKeysViewClient}.
     */
    fun setExtraKeysViewClient(extraKeysViewClient: IExtraKeysView?) {
        mExtraKeysViewClient = extraKeysViewClient
    }

    /**
     * Get {@link #mRepetitiveKeys}.
     */
    fun getRepetitiveKeys(): List<String>? {
        val keys = mRepetitiveKeys ?: return null
        return ArrayList(keys)
    }

    /**
     * Set {@link #mRepetitiveKeys}. Must not be {@code null}.
     */
    fun setRepetitiveKeys(@NonNull repetitiveKeys: List<String>) {
        mRepetitiveKeys = repetitiveKeys
    }

    /**
     * Get {@link #mSpecialButtons}.
     */
    fun getSpecialButtons(): Map<SpecialButton, SpecialButtonState>? {
        val buttons = mSpecialButtons ?: return null
        return HashMap(buttons)
    }

    /**
     * Get {@link #mSpecialButtonsKeys}.
     */
    fun getSpecialButtonsKeys(): Set<String>? {
        val keys = mSpecialButtonsKeys ?: return null
        return HashSet(keys)
    }

    /**
     * Set {@link #mSpecialButtonsKeys}. Must not be {@code null}.
     */
    fun setSpecialButtons(@NonNull specialButtons: Map<SpecialButton, SpecialButtonState>) {
        mSpecialButtons = specialButtons
        mSpecialButtonsKeys = specialButtons.keys.mapTo(HashSet()) { it.getKey() }
    }

    /**
     * Set the {@link ExtraKeysView} button colors.
     *
     * @param buttonTextColor The value for {@link #mButtonTextColor}.
     * @param buttonActiveTextColor The value for {@link #mButtonActiveTextColor}.
     * @param buttonBackgroundColor The value for {@link #mButtonBackgroundColor}.
     * @param buttonActiveBackgroundColor The value for {@link #mButtonActiveBackgroundColor}.
     */
    fun setButtonColors(buttonTextColor: Int, buttonActiveTextColor: Int, buttonBackgroundColor: Int, buttonActiveBackgroundColor: Int) {
        mButtonTextColor = buttonTextColor
        mButtonActiveTextColor = buttonActiveTextColor
        mButtonBackgroundColor = buttonBackgroundColor
        mButtonActiveBackgroundColor = buttonActiveBackgroundColor
    }

    /**
     * Get {@link #mButtonTextColor}.
     */
    fun getButtonTextColor(): Int {
        return mButtonTextColor
    }

    /**
     * Set {@link #mButtonTextColor}.
     */
    fun setButtonTextColor(buttonTextColor: Int) {
        mButtonTextColor = buttonTextColor
    }

    /**
     * Get {@link #mButtonActiveTextColor}.
     */
    fun getButtonActiveTextColor(): Int {
        return mButtonActiveTextColor
    }

    /**
     * Set {@link #mButtonActiveTextColor}.
     */
    fun setButtonActiveTextColor(buttonActiveTextColor: Int) {
        mButtonActiveTextColor = buttonActiveTextColor
    }

    /**
     * Get {@link #mButtonBackgroundColor}.
     */
    fun getButtonBackgroundColor(): Int {
        return mButtonBackgroundColor
    }

    /**
     * Set {@link #mButtonBackgroundColor}.
     */
    fun setButtonBackgroundColor(buttonBackgroundColor: Int) {
        mButtonBackgroundColor = buttonBackgroundColor
    }

    /**
     * Get {@link #mButtonActiveBackgroundColor}.
     */
    fun getButtonActiveBackgroundColor(): Int {
        return mButtonActiveBackgroundColor
    }

    /**
     * Set {@link #mButtonActiveBackgroundColor}.
     */
    fun setButtonActiveBackgroundColor(buttonActiveBackgroundColor: Int) {
        mButtonActiveBackgroundColor = buttonActiveBackgroundColor
    }

    /**
     * Set {@link #mButtonTextAllCaps}.
     */
    fun setButtonTextAllCaps(buttonTextAllCaps: Boolean) {
        mButtonTextAllCaps = buttonTextAllCaps
    }

    /**
     * Get {@link #mLongPressTimeout}.
     */
    fun getLongPressTimeout(): Int {
        return mLongPressTimeout
    }

    /**
     * Set {@link #mLongPressTimeout}.
     */
    fun setLongPressTimeout(longPressDuration: Int) {
        if (longPressDuration >= MIN_LONG_PRESS_DURATION && longPressDuration <= MAX_LONG_PRESS_DURATION) {
            mLongPressTimeout = longPressDuration
        } else {
            mLongPressTimeout = FALLBACK_LONG_PRESS_DURATION
        }
    }

    /**
     * Get {@link #mLongPressRepeatDelay}.
     */
    fun getLongPressRepeatDelay(): Int {
        return mLongPressRepeatDelay
    }

    /**
     * Set {@link #mLongPressRepeatDelay}.
     */
    fun setLongPressRepeatDelay(longPressRepeatDelay: Int) {
        // Fixed: the condition read the field instead of the parameter, so
        // the first call always fell back to the default (the field starts
        // at 0, below the minimum) and the argument was silently ignored.
        if (longPressRepeatDelay >= MIN_LONG_PRESS__REPEAT_DELAY && longPressRepeatDelay <= MAX_LONG_PRESS__REPEAT_DELAY) {
            mLongPressRepeatDelay = longPressRepeatDelay
        } else {
            mLongPressRepeatDelay = DEFAULT_LONG_PRESS_REPEAT_DELAY
        }
    }

    /**
     * Get the default map that can be used for {@link #mSpecialButtons}.
     */
    @NonNull
    fun getDefaultSpecialButtons(extraKeysView: ExtraKeysView?): Map<SpecialButton, SpecialButtonState> {
        return hashMapOf(
            SpecialButton.CTRL to SpecialButtonState(extraKeysView),
            SpecialButton.ALT to SpecialButtonState(extraKeysView),
            SpecialButton.SHIFT to SpecialButtonState(extraKeysView),
            SpecialButton.FN to SpecialButtonState(extraKeysView)
        )
    }

    /**
     * Reload this instance of {@link ExtraKeysView} with the info passed in {@code extraKeysInfo}.
     *
     * @param extraKeysInfo The {@link ExtraKeysInfo} that defines the necessary info for the extra keys.
     * @param heightPx The height in pixels of the parent surrounding the {@link ExtraKeysView}. It must
     *                 be a single child.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun reload(extraKeysInfo: ExtraKeysInfo?, heightPx: Float) {
        if (extraKeysInfo == null)
            return
        val specialButtons = mSpecialButtons!!
        for (state in specialButtons.values) state.buttons = ArrayList()
        removeAllViews()
        val buttons = extraKeysInfo.getMatrix()
        setRowCount(buttons.size)
        setColumnCount(maximumLength(buttons))
        for (row in buttons.indices) {
            for (col in buttons[row].indices) {
                val buttonInfo = buttons[row][col]
                val button: MaterialButton = if (isSpecialButton(buttonInfo)) {
                    createSpecialButton(buttonInfo.getKey(), true) ?: return
                } else {
                    MaterialButton(getContext(), null, android.R.attr.buttonBarButtonStyle)
                }
                button.setAccessibilityDelegate(object : View.AccessibilityDelegate() {
                    override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                        super.onInitializeAccessibilityNodeInfo(host, info)
                        // these should funtion like soft keyboard keys;
                        // with this flag set, they honor the TalkBack setting
                        // of hold/release to activate, 2x tap to activate or hybrid;
                        // assuming your Android and TalkBack are recent enough
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            info.setTextEntryKey(true)
                        }
                    }
                })

                button.text = buttonInfo.getDisplay()
                button.setTextColor(mButtonTextColor)
                button.isAllCaps = mButtonTextAllCaps
                button.setPadding(0, 0, 0, 0)
                button.setOnClickListener { view ->
                    performExtraKeyButtonHapticFeedback(view, buttonInfo, button)
                    onAnyExtraKeyButtonClick(view, buttonInfo, button)
                }
                button.setOnTouchListener { view, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            view.setBackgroundColor(mButtonActiveBackgroundColor)
                            // Start long press scheduled executors which will be stopped in next MotionEvent
                            startScheduledExecutors(view, buttonInfo, button)
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val popup = buttonInfo.getPopup()
                            if (popup != null) {
                                // Show popup on swipe up
                                if (mPopupWindow == null && event.y < 0) {
                                    stopScheduledExecutors()
                                    view.setBackgroundColor(mButtonBackgroundColor)
                                    showPopup(view, popup)
                                }
                                if (mPopupWindow != null && event.y > 0) {
                                    view.setBackgroundColor(mButtonActiveBackgroundColor)
                                    dismissPopup()
                                }
                            }
                            true
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            view.setBackgroundColor(mButtonBackgroundColor)
                            stopScheduledExecutors()
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            view.setBackgroundColor(mButtonBackgroundColor)
                            stopScheduledExecutors()
                            // If ACTION_UP up was not from a repetitive key or was with a key with a popup button
                            if (mLongPressCount == 0 || mPopupWindow != null) {
                                // Trigger popup button click if swipe up complete
                                if (mPopupWindow != null) {
                                    dismissPopup()
                                    val popup = buttonInfo.getPopup()
                                    if (popup != null) {
                                        onAnyExtraKeyButtonClick(view, popup, button)
                                    }
                                } else {
                                    view.performClick()
                                }
                            }
                            true
                        }
                        else -> true
                    }
                }
                val param = GridLayout.LayoutParams()
                param.width = 0
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
                    param.height = (heightPx + 0.5).toInt()
                } else {
                    param.height = 0
                }
                param.setMargins(0, 0, 0, 0)
                param.columnSpec = GridLayout.spec(col, GridLayout.FILL, 1f)
                param.rowSpec = GridLayout.spec(row, GridLayout.FILL, 1f)
                button.layoutParams = param
                addView(button)
            }
        }
    }

    fun onExtraKeyButtonClick(view: View?, buttonInfo: ExtraKeyButton?, button: MaterialButton?) {
        mExtraKeysViewClient?.onExtraKeyButtonClick(view, buttonInfo, button)
    }

    @Suppress("DEPRECATION") // HAPTIC_FEEDBACK_ENABLED must stay: still the documented setting key
    fun performExtraKeyButtonHapticFeedback(view: View?, buttonInfo: ExtraKeyButton?, button: MaterialButton?) {
        val client = mExtraKeysViewClient
        if (client != null) {
            // If client handled the feedback, then just return
            if (client.performExtraKeyButtonHapticFeedback(view, buttonInfo, button))
                return
        }
        if (Settings.System.getInt(getContext().getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED, 0) != 0) {
            if (Build.VERSION.SDK_INT >= 28) {
                // Null button threw NPE here in Java as well.
                button!!.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            } else {
                // Perform haptic feedback only if no total silence mode enabled.
                if (Settings.Global.getInt(getContext().getContentResolver(), "zen_mode", 0) != 2) {
                    button!!.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
            }
        }
    }

    fun onAnyExtraKeyButtonClick(view: View?, @NonNull buttonInfo: ExtraKeyButton, button: MaterialButton?) {
        if (isSpecialButton(buttonInfo)) {
            if (mLongPressCount > 0)
                return
            val state = mSpecialButtons!![SpecialButton.valueOf(buttonInfo.getKey())] ?: return
            // Toggle active state and disable lock state if new state is not active
            state.setIsActive(!state.isActive)
            if (!state.isActive)
                state.setIsLocked(false)

            announceSpecialKeyStateChangeForAccessibility(buttonInfo.getKey(), state)
        } else {
            onExtraKeyButtonClick(view, buttonInfo, button)
        }
    }

    @Suppress("DEPRECATION") // announceForAccessibility must stay: the compat way to announce
    private fun announceSpecialKeyStateChangeForAccessibility(buttonName: CharSequence?, state: SpecialButtonState) {
        if (mAccessibilityEnabled) {
            val stateText: CharSequence = if (!state.isActive) {
                getResources().getText(R.string.a11y_special_key_off)
            } else if (state.isLocked) {
                getResources().getText(R.string.a11y_special_key_latched_on)
            } else {
                getResources().getText(R.string.a11y_special_key_on)

            }
            val announcementText = buttonName.toString() + " " + stateText
            announceForAccessibility(announcementText)
        }
    }

    fun startScheduledExecutors(view: View?, buttonInfo: ExtraKeyButton?, button: MaterialButton?) {
        stopScheduledExecutors()
        mLongPressCount = 0
        // Null buttonInfo threw NPE here in Java as well.
        val nonNullButtonInfo = buttonInfo!!
        if (mRepetitiveKeys!!.contains(nonNullButtonInfo.getKey())) {
            // Auto repeat key if long pressed until ACTION_UP stops it by calling stopScheduledExecutors.
            // Currently, only one (last) repeat key can run at a time. Old ones are stopped.
            val executor = Executors.newSingleThreadScheduledExecutor()
            mScheduledExecutor = executor
            executor.scheduleWithFixedDelay({
                mLongPressCount++
                onExtraKeyButtonClick(view, nonNullButtonInfo, button)
            }, mLongPressTimeout.toLong(), mLongPressRepeatDelay.toLong(), TimeUnit.MILLISECONDS)
        } else if (isSpecialButton(nonNullButtonInfo)) {
            // Lock the key if long pressed by running mSpecialButtonsLongHoldRunnable after
            // waiting for mLongPressTimeout milliseconds. If user does not long press, then the
            // ACTION_UP triggered will cancel the runnable by calling stopScheduledExecutors before
            // it has a chance to run.
            val state = mSpecialButtons!![SpecialButton.valueOf(nonNullButtonInfo.getKey())] ?: return
            var handler = mHandler
            if (handler == null) {
                handler = Handler(Looper.getMainLooper())
                mHandler = handler
            }
            val runnable = SpecialButtonsLongHoldRunnable(state, nonNullButtonInfo)
            mSpecialButtonsLongHoldRunnable = runnable
            handler.postDelayed(runnable, mLongPressTimeout.toLong())
        }
    }

    fun stopScheduledExecutors() {
        mScheduledExecutor?.shutdownNow()
        mScheduledExecutor = null
        val runnable = mSpecialButtonsLongHoldRunnable
        val handler = mHandler
        if (runnable != null && handler != null) {
            handler.removeCallbacks(runnable)
            mSpecialButtonsLongHoldRunnable = null
        }
    }

    inner class SpecialButtonsLongHoldRunnable(state: SpecialButtonState, buttonInfo: ExtraKeyButton) : Runnable {

        @JvmField
        val mState: SpecialButtonState = state

        @JvmField
        val mButtonInfo: ExtraKeyButton = buttonInfo

        override fun run() {
            // Toggle active and lock state
            mState.setIsLocked(!mState.isActive)
            mState.setIsActive(!mState.isActive)
            mLongPressCount++

            announceSpecialKeyStateChangeForAccessibility(mButtonInfo.getKey(), mState)
        }
    }

    private fun showPopup(view: View?, extraButton: ExtraKeyButton?) {
        // Both threw NPE here in Java as well.
        val nonNullView = view!!
        val nonNullButton = extraButton!!
        val width = nonNullView.measuredWidth
        val height = nonNullView.measuredHeight
        val button: MaterialButton = if (isSpecialButton(nonNullButton)) {
            createSpecialButton(nonNullButton.getKey(), false) ?: return
        } else {
            MaterialButton(getContext(), null, android.R.attr.buttonBarButtonStyle).also {
                it.setTextColor(mButtonTextColor)
            }
        }
        button.text = nonNullButton.getDisplay()
        button.isAllCaps = mButtonTextAllCaps
        button.setPadding(0, 0, 0, 0)
        button.minHeight = 0
        button.minWidth = 0
        button.minimumWidth = 0
        button.minimumHeight = 0
        button.width = width
        button.height = height
        button.setBackgroundColor(mButtonActiveBackgroundColor)
        mPopupWindow = PopupWindow(this)
        val popupWindow = mPopupWindow!!
        popupWindow.width = LayoutParams.WRAP_CONTENT
        popupWindow.height = LayoutParams.WRAP_CONTENT
        popupWindow.contentView = button
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = false
        popupWindow.showAsDropDown(nonNullView, 0, -2 * height)
    }

    fun dismissPopup() {
        // Only invoked with a shown popup in Java as well.
        val popupWindow = mPopupWindow!!
        popupWindow.contentView = null
        popupWindow.dismiss()
        mPopupWindow = null
    }

    /**
     * Check whether a {@link ExtraKeyButton} is a {@link SpecialButton}.
     */
    fun isSpecialButton(button: ExtraKeyButton?): Boolean {
        // Null button threw NPE here in Java as well.
        return mSpecialButtonsKeys!!.contains(button!!.getKey())
    }

    /**
     * Read whether {@link SpecialButton} registered in {@link #mSpecialButtons} is active or not.
     *
     * @param specialButton The {@link SpecialButton} to read.
     * @param autoSetInActive Set to {@code true} if {@link SpecialButtonState#isActive} should be
     *                        set {@code false} if button is not locked.
     * @return Returns {@code null} if button does not exist in {@link #mSpecialButtons}. If button
     *         exists, then returns {@code true} if the button is created in {@link ExtraKeysView}
     *         and is active, otherwise {@code false}.
     */
    @Nullable
    fun readSpecialButton(specialButton: SpecialButton?, autoSetInActive: Boolean): Boolean? {
        // A null button misses the map like the Java version (get(null)).
        val button = specialButton ?: return null
        val state = mSpecialButtons!![button] ?: return null
        if (!state.isCreated || !state.isActive)
            return false
        // Disable active state only if not locked
        if (autoSetInActive && !state.isLocked) {
            state.setIsActive(false)
            announceSpecialKeyStateChangeForAccessibility(button.getKey(), state)
        }

        return true
    }

    fun createSpecialButton(buttonKey: String?, needUpdate: Boolean): MaterialButton? {
        val key = SpecialButton.valueOf(buttonKey) ?: return null
        val state = mSpecialButtons!![key] ?: return null
        state.setIsCreated(true)
        val button = MaterialButton(getContext(), null, android.R.attr.buttonBarButtonStyle)
        button.setTextColor(if (state.isActive) mButtonActiveTextColor else mButtonTextColor)
        if (needUpdate) {
            state.buttons.add(button)
        }
        return button
    }
}
