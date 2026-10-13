package com.termux.shared.termux.interact

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.text.Selection
import android.util.TypedValue
import android.view.KeyEvent
import android.view.ViewGroup.LayoutParams
import android.widget.EditText
import android.widget.LinearLayout

class TextInputDialogUtils {

    fun interface TextSetListener {

        fun onTextSet(text: String?)
    }

    companion object {
        @JvmStatic
        fun textInput(activity: Activity?, titleText: Int, initialText: String?, positiveButtonText: Int, onPositive: TextSetListener, neutralButtonText: Int, onNeutral: TextSetListener?, negativeButtonText: Int, onNegative: TextSetListener?, onDismiss: DialogInterface.OnDismissListener?) {
            val input = EditText(activity)
            input.setSingleLine()
            if (initialText != null) {
                input.setText(initialText)
                Selection.setSelection(input.text, initialText.length)
            }
            val dialogHolder = arrayOfNulls<AlertDialog>(1)
            // Null activity already threw in EditText() above like the Java
            // version, so this never throws for a reached call.
            val nonNullActivity = activity!!
            input.setImeActionLabel(nonNullActivity.resources.getString(positiveButtonText), KeyEvent.KEYCODE_ENTER)
            input.setOnEditorActionListener { _, _, _ ->
                onPositive.onTextSet(input.text.toString())
                // Set before any editor action can fire (dialog is shown first).
                dialogHolder[0]!!.dismiss()
                true
            }
            val dipInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, nonNullActivity.resources.displayMetrics)
            // https://www.google.com/design/spec/components/dialogs.html#dialogs-specs
            val paddingTopAndSides = Math.round(16 * dipInPixels)
            val paddingBottom = Math.round(24 * dipInPixels)
            val layout = LinearLayout(activity)
            layout.orientation = LinearLayout.VERTICAL
            layout.layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            layout.setPadding(paddingTopAndSides, paddingTopAndSides, paddingTopAndSides, paddingBottom)
            layout.addView(input)
            val builder = AlertDialog.Builder(activity).setTitle(titleText).setView(layout).setPositiveButton(positiveButtonText) { _, _ -> onPositive.onTextSet(input.text.toString()) }
            if (onNeutral != null) {
                builder.setNeutralButton(neutralButtonText) { _, _ -> onNeutral.onTextSet(input.text.toString()) }
            }
            if (onNegative == null) {
                builder.setNegativeButton(android.R.string.cancel, null)
            } else {
                builder.setNegativeButton(negativeButtonText) { _, _ -> onNegative.onTextSet(input.text.toString()) }
            }
            if (onDismiss != null)
                builder.setOnDismissListener(onDismiss)
            dialogHolder[0] = builder.create()
            dialogHolder[0]!!.setCanceledOnTouchOutside(false)
            dialogHolder[0]!!.show()
        }
    }
}
