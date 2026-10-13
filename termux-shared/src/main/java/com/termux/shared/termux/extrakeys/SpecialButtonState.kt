package com.termux.shared.termux.extrakeys

import com.google.android.material.button.MaterialButton

/**
 * The {@link Class} that maintains a state of a {@link SpecialButton}
 *
 * Note: the fields were package-private in Java and are accessed directly by
 * {@link ExtraKeysView} in the same package. Kotlin has no package-private,
 * so they are widened to public fields to keep that Java code compiling.
 */
class SpecialButtonState(extraKeysView: ExtraKeysView?) {

    /**
     * If special button has been created for the {@link ExtraKeysView}.
     */
    @JvmField
    var isCreated = false

    /**
     * If special button is active.
     */
    @JvmField
    var isActive = false

    /**
     * If special button is locked due to long hold on it and should not be deactivated if its
     * state is read.
     */
    @JvmField
    var isLocked = false

    @JvmField
    var buttons = ArrayList<MaterialButton>()

    @JvmField
    var mExtraKeysView: ExtraKeysView? = extraKeysView

    /**
     * Set {@link #isCreated}.
     */
    fun setIsCreated(value: Boolean) {
        isCreated = value
    }

    /**
     * Set {@link #isActive}.
     */
    fun setIsActive(value: Boolean) {
        isActive = value
        // Null view threw here in Java as well; !! keeps the NPE.
        val view = mExtraKeysView!!
        for (button in buttons) {
            button.setTextColor(if (value) view.buttonActiveTextColor else view.buttonTextColor)
        }
    }

    /**
     * Set {@link #isLocked}.
     */
    fun setIsLocked(value: Boolean) {
        isLocked = value
    }
}
