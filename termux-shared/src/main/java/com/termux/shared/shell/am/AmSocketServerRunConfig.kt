package com.termux.shared.shell.am

import androidx.annotation.NonNull
import com.termux.shared.logger.Logger
import com.termux.shared.markdown.MarkdownUtils
import com.termux.shared.net.socket.local.ILocalSocketManager
import com.termux.shared.net.socket.local.LocalSocketRunConfig

/**
 * Run config for {@link AmSocketServer}.
 */
class AmSocketServerRunConfig : LocalSocketRunConfig {

    /**
     * Check if `SYSTEM_ALERT_WINDOW` permission has been granted if running on Android `>= 10`
     * if starting activities. Will also check when starting services in case starting foreground
     * service is not allowed.
     *
     * https://developer.android.com/guide/components/activities/background-starts
     */
    private var checkDisplayOverAppsPermission: Boolean? = null

    /**
     * Create an new instance of {@link AmSocketServerRunConfig}.
     *
     * @param title The title value.
     * @param path The path value.
     * @param localSocketManagerClient The local socket manager client value.
     */
    constructor(@NonNull title: String, @NonNull path: String, @NonNull localSocketManagerClient: ILocalSocketManager) : super(title, path, localSocketManagerClient)

    /**
     * Get {@link #checkDisplayOverAppsPermission} if set, otherwise {@link #DEFAULT_CHECK_DISPLAY_OVER_APPS_PERMISSION}.
     */
    fun shouldCheckDisplayOverAppsPermission(): Boolean {
        return checkDisplayOverAppsPermission ?: DEFAULT_CHECK_DISPLAY_OVER_APPS_PERMISSION
    }

    /**
     * Set {@link #checkDisplayOverAppsPermission}.
     */
    fun setCheckDisplayOverAppsPermission(checkDisplayOverAppsPermission: Boolean?) {
        this.checkDisplayOverAppsPermission = checkDisplayOverAppsPermission
    }

    /**
     * Get a log {@link String} for the {@link AmSocketServerRunConfig}.
     */
    @NonNull
    override fun getLogString(): String {
        val logString = StringBuilder()
        logString.append(super.getLogString()).append("\n\n\n")
        logString.append("Am Command:")
        logString.append("\n").append(Logger.getSingleLineLogStringEntry("CheckDisplayOverAppsPermission", shouldCheckDisplayOverAppsPermission(), "-"))
        return logString.toString()
    }

    /**
     * Get a markdown {@link String} for the {@link AmSocketServerRunConfig}.
     */
    @NonNull
    override fun getMarkdownString(): String {
        val markdownString = StringBuilder()
        markdownString.append(super.getMarkdownString()).append("\n\n\n")
        markdownString.append("## ").append("Am Command")
        markdownString.append("\n").append(MarkdownUtils.getSingleLineMarkdownStringEntry("CheckDisplayOverAppsPermission", shouldCheckDisplayOverAppsPermission(), "-"))
        return markdownString.toString()
    }

    @NonNull
    override fun toString(): String {
        return getLogString()
    }

    companion object {
        const val DEFAULT_CHECK_DISPLAY_OVER_APPS_PERMISSION = true

        /**
         * Get a log {@link String} for {@link AmSocketServerRunConfig}.
         *
         * @param config The {@link AmSocketServerRunConfig} to get info of.
         * @return Returns the log {@link String}.
         */
        @NonNull
        @JvmStatic
        fun getRunConfigLogString(config: AmSocketServerRunConfig?): String {
            if (config == null)
                return "null"
            return config.getLogString()
        }

        /**
         * Get a markdown {@link String} for {@link AmSocketServerRunConfig}.
         *
         * @param config The {@link AmSocketServerRunConfig} to get info of.
         * @return Returns the markdown {@link String}.
         */
        @JvmStatic
        fun getRunConfigMarkdownString(config: AmSocketServerRunConfig?): String {
            if (config == null)
                return "null"
            return config.getMarkdownString()
        }
    }
}
