package com.termux.shared.interact

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import com.termux.shared.R

class MessageDialogUtils {

    companion object {
        /**
         * Show a message in a dialog
         *
         * @param context The {@link Context} to use to start the dialog. An {@link Activity} {@link Context}
         *                must be passed, otherwise exceptions will be thrown.
         * @param titleText The title text of the dialog.
         * @param messageText The message text of the dialog.
         * @param onDismiss The {@link DialogInterface.OnDismissListener} to run when dialog is dismissed.
         */
        @JvmStatic
        fun showMessage(context: Context, titleText: String?, messageText: String?, onDismiss: DialogInterface.OnDismissListener?) {
            showMessage(context, titleText, messageText, null, null, null, null, onDismiss)
        }

        /**
         * Show a message in a dialog
         *
         * @param context The {@link Context} to use to start the dialog. An {@link Activity} {@link Context}
         *                must be passed, otherwise exceptions will be thrown.
         * @param titleText The title text of the dialog.
         * @param messageText The message text of the dialog.
         * @param positiveText The positive button text of the dialog.
         * @param onPositiveButton The {@link DialogInterface.OnClickListener} to run when positive button
         *                         is pressed.
         * @param negativeText The negative button text of the dialog. If this is {@code null}, then
         *                         negative button will not be shown.
         * @param onNegativeButton The {@link DialogInterface.OnClickListener} to run when negative button
         *                         is pressed.
         * @param onDismiss The {@link DialogInterface.OnDismissListener} to run when dialog is dismissed.
         */
        @JvmStatic
        fun showMessage(context: Context, titleText: String?, messageText: String?,
                        positiveText: String?,
                        onPositiveButton: DialogInterface.OnClickListener?,
                        negativeText: String?,
                        onNegativeButton: DialogInterface.OnClickListener?,
                        onDismiss: DialogInterface.OnDismissListener?) {

            val builder = AlertDialog.Builder(context, androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog)

            // A Java-style cast of a missing service returns null, but Kotlin
            // `as` would throw, so use a safe cast like the dialog code below.
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as? LayoutInflater
            val view = inflater?.inflate(R.layout.dialog_show_message, null)
            if (view != null) {
                builder.setView(view)
                val titleView: TextView? = view.findViewById(R.id.dialog_title)
                if (titleView != null)
                    titleView.text = titleText
                val messageView: TextView? = view.findViewById(R.id.dialog_message)
                if (messageView != null)
                    messageView.text = messageText
            }
            val positive = positiveText ?: context.getString(android.R.string.ok)
            builder.setPositiveButton(positive, onPositiveButton)
            if (negativeText != null)
                builder.setNegativeButton(negativeText, onNegativeButton)
            if (onDismiss != null)
                builder.setOnDismissListener(onDismiss)
            val dialog = builder.create()
            dialog.setOnShowListener {
                // The Java version logged an error with just "dialog" here on
                // every show; that stray log call is dropped, the button
                // styling below is kept.
                var button: Button? = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                if (button != null)
                    button.setTextColor(Color.BLACK)
                button = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                if (button != null)
                    button.setTextColor(Color.BLACK)
            }
            dialog.show()
        }

        @JvmStatic
        fun exitAppWithErrorMessage(context: Context, titleText: String?, messageText: String?) {
            showMessage(context, titleText, messageText) { System.exit(0) }
        }
    }
}
