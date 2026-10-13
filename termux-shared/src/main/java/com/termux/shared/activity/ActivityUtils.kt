package com.termux.shared.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import com.termux.shared.errors.Error
import com.termux.shared.errors.FunctionErrno

class ActivityUtils {

    companion object {
        private const val LOG_TAG = "ActivityUtils"

        /**
         * Wrapper for {@link #startActivity(Context, Intent, boolean, boolean)}.
         */
        @JvmStatic
        fun startActivity(@NonNull context: Context, @NonNull intent: Intent): Error? {
            return startActivity(context, intent, true, true)
        }

        /**
         * Start an {@link Activity}.
         *
         * @param context The context for operations.
         * @param intent The {@link Intent} to send to start the activity.
         * @param logErrorMessage If an error message should be logged if failed to start activity.
         * @param showErrorMessage If an error message toast should be shown if failed to start activity
         *                         in addition to logging a message. The {@code context} must not be
         *                         {@code null}.
         * @return Returns the {@code error} if starting activity was not successful, otherwise {@code null}.
         */
        @JvmStatic
        fun startActivity(context: Context?, @NonNull intent: Intent, logErrorMessage: Boolean, showErrorMessage: Boolean): Error? {
            var error: Error?
            val activityName = intent.component?.className ?: "Unknown"
            if (context == null) {
                error = ActivityErrno.ERRNO_STARTING_ACTIVITY_WITH_NULL_CONTEXT.getError(activityName)
                if (logErrorMessage)
                    error.logErrorAndShowToast(null, LOG_TAG)
                return error
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                error = ActivityErrno.ERRNO_START_ACTIVITY_FAILED_WITH_EXCEPTION.getError(e, activityName, e.message)
                if (logErrorMessage)
                    error.logErrorAndShowToast(if (showErrorMessage) context else null, LOG_TAG)
                return error
            }
            return null
        }

        /**
         * Wrapper for {@link #startActivityForResult(Context, int, Intent, boolean, boolean, ActivityResultLauncher)}.
         */
        @JvmStatic
        fun startActivityForResult(context: Context?, requestCode: Int, @NonNull intent: Intent): Error? {
            return startActivityForResult(context, requestCode, intent, true, true, null)
        }

        /**
         * Wrapper for {@link #startActivityForResult(Context, int, Intent, boolean, boolean, ActivityResultLauncher)}.
         */
        @JvmStatic
        fun startActivityForResult(context: Context?, requestCode: Int, @NonNull intent: Intent, logErrorMessage: Boolean, showErrorMessage: Boolean): Error? {
            return startActivityForResult(context, requestCode, intent, logErrorMessage, showErrorMessage, null)
        }

        /**
         * Start an {@link Activity} for result.
         *
         * @param context The context for operations. It must be an instance of {@link Activity} or
         *               {@link AppCompatActivity}. It is ignored if {@code activityResultLauncher}
         *                is not {@code null}.
         * @param requestCode The request code to use while sending intent. This must be >= 0, otherwise
         *                    exception will be raised. This is ignored if {@code activityResultLauncher}
         *                    is {@code null}.
         * @param intent The {@link Intent} to send to start the activity.
         * @param logErrorMessage If an error message should be logged if failed to start activity.
         * @param showErrorMessage If an error message toast should be shown if failed to start activity
         *                         in addition to logging a message. The {@code context} must not be
         *                         {@code null}.
         * @param activityResultLauncher The {@link ActivityResultLauncher<Intent>} to use for start the
         *                               activity. If this is {@code null}, then
         *                               {@link Activity#startActivityForResult(Intent, int)} will be
         *                               used instead.
         *                               Note that later is deprecated.
         * @return Returns the {@code error} if starting activity was not successful, otherwise {@code null}.
         */
        @JvmStatic
        @Suppress("DEPRECATION") // startActivityForResult must stay: the null-launcher fallback path for plain Activity contexts
        fun startActivityForResult(context: Context?, requestCode: Int, @NonNull intent: Intent, logErrorMessage: Boolean, showErrorMessage: Boolean, @Nullable activityResultLauncher: ActivityResultLauncher<Intent>?): Error? {
            var error: Error?
            val activityName = intent.component?.className ?: "Unknown"
            try {
                if (activityResultLauncher != null) {
                    activityResultLauncher.launch(intent)
                } else {
                    if (context == null) {
                        error = ActivityErrno.ERRNO_STARTING_ACTIVITY_WITH_NULL_CONTEXT.getError(activityName)
                        if (logErrorMessage)
                            error.logErrorAndShowToast(null, LOG_TAG)
                        return error
                    }
                    if (context is AppCompatActivity)
                        context.startActivityForResult(intent, requestCode)
                    else if (context is Activity)
                        context.startActivityForResult(intent, requestCode)
                    else {
                        error = FunctionErrno.ERRNO_PARAMETER_NOT_INSTANCE_OF.getError("context", "startActivityForResult", "Activity or AppCompatActivity")
                        if (logErrorMessage)
                            error.logErrorAndShowToast(if (showErrorMessage) context else null, LOG_TAG)
                        return error
                    }
                }
            } catch (e: Exception) {
                error = ActivityErrno.ERRNO_START_ACTIVITY_FOR_RESULT_FAILED_WITH_EXCEPTION.getError(e, activityName, e.message)
                if (logErrorMessage)
                    error.logErrorAndShowToast(if (showErrorMessage) context else null, LOG_TAG)
                return error
            }
            return null
        }

        /**
         * Wrapper for {@link #startActivityForResult(Context, ActivityResultLauncher, Object, boolean, boolean)}.
         */
        @JvmStatic
        fun <T> startActivityForResult(context: Context?, @NonNull activityResultLauncher: ActivityResultLauncher<T>, input: T): Error? {
            return startActivityForResult(context, activityResultLauncher, input, true, true)
        }

        /**
         * Generic method to start an {@link Activity} for result.
         * @param context The context for operations. It must be an instance of {@link Activity} or
         *                {@link AppCompatActivity}. It is ignored if {@code activityResultLauncher}
         *                is not {@code null}.
         * @param activityResultLauncher A launcher for start the process of executing an {@link ActivityResultContract}.
         * @param input The data required to {@link ActivityResultLauncher#launch(Object) launch} Activity.
         * @param logErrorMessage If an error message should be logged if failed to start activity.
         * @param showErrorMessage If an error message toast should be shown if failed to start activity
         *                         in addition to logging a message. The {@code context} must not be
         *                         {@code null}.
         * @param <T> Type of the input required to {@link ActivityResultLauncher#launch(Object) launch}.
         * @return Returns the {@code error} if starting activity was not successful, otherwise {@code null}.
         */
        @JvmStatic
        fun <T> startActivityForResult(context: Context?, @NonNull activityResultLauncher: ActivityResultLauncher<T>, input: T, logErrorMessage: Boolean, showErrorMessage: Boolean): Error? {
            var error: Error?
            var activityName = "Unknown"
            if (input is Intent && input.component != null) {
                activityName = input.component!!.className
            }
            try {
                activityResultLauncher.launch(input)
            } catch (e: Exception) {
                error = ActivityErrno.ERRNO_START_ACTIVITY_FOR_RESULT_FAILED_WITH_EXCEPTION.getError(e, activityName, e.message)
                if (logErrorMessage)
                    error.logErrorAndShowToast(if (showErrorMessage) context else null, LOG_TAG)
                return error
            }
            return null
        }
    }
}
