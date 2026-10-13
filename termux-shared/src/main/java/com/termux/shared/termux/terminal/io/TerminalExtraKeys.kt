package com.termux.shared.termux.terminal.io

import android.os.Build
import android.view.KeyEvent
import android.view.View
import com.google.android.material.button.MaterialButton
import com.termux.shared.termux.extrakeys.ExtraKeyButton
import com.termux.shared.termux.extrakeys.ExtraKeysView
import com.termux.shared.termux.extrakeys.ExtraKeysConstants
import com.termux.shared.termux.extrakeys.SpecialButton
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView

open class TerminalExtraKeys(private val mTerminalView: TerminalView) : ExtraKeysView.IExtraKeysView {

    override fun onExtraKeyButtonClick(view: View?, buttonInfo: ExtraKeyButton?, button: MaterialButton?) {
        // Null buttonInfo threw NPE here in Java as well.
        // Explicit call: isMacro() maps to the private macro property.
        if (buttonInfo!!.isMacro()) {
            // Kotlin split is literal (Java split took a regex), but " " as
            // a regex is exactly a literal space; any trailing "" iterations
            // are no-ops below, so outcomes cannot differ.
            val keys = buttonInfo.getKey().split(" ")
            var ctrlDown = false
            var altDown = false
            var shiftDown = false
            var fnDown = false
            for (key in keys) {
                if (SpecialButton.CTRL.getKey() == key) {
                    ctrlDown = true
                } else if (SpecialButton.ALT.getKey() == key) {
                    altDown = true
                } else if (SpecialButton.SHIFT.getKey() == key) {
                    shiftDown = true
                } else if (SpecialButton.FN.getKey() == key) {
                    fnDown = true
                } else {
                    onTerminalExtraKeyButtonClick(view, key, ctrlDown, altDown, shiftDown, fnDown)
                    ctrlDown = false
                    altDown = false
                    shiftDown = false
                    fnDown = false
                }
            }
        } else {
            onTerminalExtraKeyButtonClick(view, buttonInfo.getKey(), false, false, false, false)
        }
    }

    protected open fun onTerminalExtraKeyButtonClick(view: View?, key: String?, ctrlDown: Boolean, altDown: Boolean, shiftDown: Boolean, fnDown: Boolean) {
        if (ExtraKeysConstants.PRIMARY_KEY_CODES_FOR_STRINGS.containsKey(key)) {
            val keyCode = ExtraKeysConstants.PRIMARY_KEY_CODES_FOR_STRINGS[key]
            if (keyCode == null)
                return
            var metaState = 0
            if (ctrlDown)
                metaState = metaState or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
            if (altDown)
                metaState = metaState or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
            if (shiftDown)
                metaState = metaState or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
            if (fnDown)
                metaState = metaState or KeyEvent.META_FUNCTION_ON
            val keyEvent = KeyEvent(0, 0, KeyEvent.ACTION_UP, keyCode, 0, metaState)
            mTerminalView.onKeyDown(keyCode, keyEvent)
        } else {
            // not a control char
            // Null key threw NPE here in Java as well.
            val nonNullKey = key!!
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                nonNullKey.codePoints().forEach { codePoint ->
                    mTerminalView.inputCodePoint(TerminalView.KEY_EVENT_SOURCE_VIRTUAL_KEYBOARD, codePoint, ctrlDown, altDown)
                }
            } else {
                val session: TerminalSession? = mTerminalView.currentSession
                if (session != null && nonNullKey.isNotEmpty())
                    session.write(nonNullKey)
            }
        }
    }

    override fun performExtraKeyButtonHapticFeedback(view: View?, buttonInfo: ExtraKeyButton?, button: MaterialButton?): Boolean {
        return false
    }
}
