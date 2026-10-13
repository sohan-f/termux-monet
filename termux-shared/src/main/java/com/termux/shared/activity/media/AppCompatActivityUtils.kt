package com.termux.shared.activity.media

import androidx.annotation.IdRes
import androidx.annotation.NonNull
import androidx.annotation.StyleRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import com.termux.shared.logger.Logger
import com.termux.shared.theme.NightMode

class AppCompatActivityUtils {

    companion object {
        private const val LOG_TAG = "AppCompatActivityUtils"

        /**
         * Set activity night mode.
         *
         * @param activity The host {@link AppCompatActivity}.
         * @param name The {@link String} representing the name for a {@link NightMode}.
         * @param local If set to {@code true}, then a call to {@link AppCompatDelegate#setLocalNightMode(int)}
         *              will be made, otherwise to {@link AppCompatDelegate#setDefaultNightMode(int)}.
         */
        @JvmStatic
        fun setNightMode(activity: AppCompatActivity?, name: String?, local: Boolean) {
            if (name == null)
                return
            val nightMode = NightMode.modeOf(name)
            if (nightMode != null) {
                if (local) {
                    if (activity != null) {
                        activity.delegate.setLocalNightMode(nightMode.getMode())
                    }
                } else {
                    AppCompatDelegate.setDefaultNightMode(nightMode.getMode())
                }
            }
        }

        /**
         * Set activity toolbar.
         *
         * @param activity The host {@link AppCompatActivity}.
         * @param id The toolbar resource id.
         */
        @JvmStatic
        fun setToolbar(@NonNull activity: AppCompatActivity, @IdRes id: Int) {
            val toolbar: Toolbar? = activity.findViewById(id)
            if (toolbar != null)
                // Explicit setter: getSupportActionBar()/setSupportActionBar()
                // have different types, so they are not one Kotlin property.
                activity.setSupportActionBar(toolbar)
        }

        /**
         * Set activity toolbar title.
         *
         * @param activity The host {@link AppCompatActivity}.
         * @param id The toolbar resource id.
         * @param title The toolbar title {@link String}.
         * @param titleAppearance The toolbar title TextAppearance resource id.
         */
        @JvmStatic
        fun setToolbarTitle(@NonNull activity: AppCompatActivity, @IdRes id: Int, title: String?, @StyleRes titleAppearance: Int) {
            val toolbar: Toolbar? = activity.findViewById(id)
            if (toolbar != null) {
                //toolbar.setTitle(title); // Does not work
                val actionBar: ActionBar? = activity.supportActionBar
                if (actionBar != null)
                    actionBar.title = title
                try {
                    if (titleAppearance != 0)
                        toolbar.setTitleTextAppearance(activity, titleAppearance)
                } catch (e: Exception) {
                    Logger.logStackTraceWithMessage(LOG_TAG, "Failed to set toolbar title appearance to style resource id $titleAppearance", e)
                }
            }
        }

        /**
         * Set activity toolbar subtitle.
         *
         * @param activity The host {@link AppCompatActivity}.
         * @param id The toolbar resource id.
         * @param subtitle The toolbar subtitle {@link String}.
         * @param subtitleAppearance The toolbar subtitle TextAppearance resource id.
         */
        @JvmStatic
        fun setToolbarSubtitle(@NonNull activity: AppCompatActivity, @IdRes id: Int, subtitle: String?, @StyleRes subtitleAppearance: Int) {
            val toolbar: Toolbar? = activity.findViewById(id)
            if (toolbar != null) {
                toolbar.subtitle = subtitle
                try {
                    if (subtitleAppearance != 0)
                        toolbar.setSubtitleTextAppearance(activity, subtitleAppearance)
                } catch (e: Exception) {
                    Logger.logStackTraceWithMessage(LOG_TAG, "Failed to set toolbar subtitle appearance to style resource id $subtitleAppearance", e)
                }
            }
        }

        /**
         * Set whether back button should be shown in activity toolbar.
         *
         * @param activity The host {@link AppCompatActivity}.
         * @param showBackButtonInActionBar Set to {@code true} to enable and {@code false} to disable.
         */
        @JvmStatic
        fun setShowBackButtonInActionBar(@NonNull activity: AppCompatActivity, showBackButtonInActionBar: Boolean) {
            val actionBar: ActionBar? = activity.supportActionBar
            if (actionBar != null) {
                if (showBackButtonInActionBar) {
                    actionBar.setDisplayHomeAsUpEnabled(true)
                    actionBar.setDisplayShowHomeEnabled(true)
                } else {
                    actionBar.setDisplayHomeAsUpEnabled(false)
                    actionBar.setDisplayShowHomeEnabled(false)
                }
            }
        }
    }
}
