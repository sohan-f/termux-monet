package com.termux.shared.termux.shell.am

import android.content.Context
import androidx.annotation.Keep
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.termux.shared.errors.Error
import com.termux.shared.logger.Logger
import com.termux.shared.net.socket.local.LocalClientSocket
import com.termux.shared.net.socket.local.LocalServerSocket
import com.termux.shared.net.socket.local.LocalSocketManager
import com.termux.shared.net.socket.local.LocalSocketRunConfig
import com.termux.shared.shell.am.AmSocketServer
import com.termux.shared.shell.am.AmSocketServerRunConfig
import com.termux.shared.termux.TermuxConstants
import com.termux.shared.termux.crash.TermuxCrashUtils
import com.termux.shared.termux.plugins.TermuxPluginUtils
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties
import com.termux.shared.termux.settings.properties.TermuxPropertyConstants
import com.termux.shared.termux.shell.command.environment.TermuxAppShellEnvironment

/**
 * A wrapper for {@link AmSocketServer} for termux-app usage.
 *
 * The static {@link #termuxAmSocketServer} variable stores the {@link LocalSocketManager} for the
 * {@link AmSocketServer}.
 *
 * The {@link TermuxAmSocketServerClient} extends the {@link AmSocketServer.AmSocketServerClient}
 * class to also show plugin error notifications for errors and disallowed client connections in
 * addition to logging the messages to logcat, which are only logged by {@link LocalSocketManagerClientBase}
 * if log level is debug or higher for privacy issues.
 *
 * It uses a filesystem socket server with the socket file at
 * {@link TermuxConstants.TERMUX_APP#TERMUX_AM_SOCKET_FILE_PATH}. It would normally only allow
 * processes belonging to the termux user and root user to connect to it. If commands are sent by the
 * root user, then the am commands executed will be run as the termux user and its permissions,
 * capabilities and selinux context instead of root.
 *
 * The `$PREFIX/bin/termux-am` client connects to the server via `$PREFIX/bin/termux-am-socket` to
 * run the am commands. It provides similar functionality to "$PREFIX/bin/am"
 * (and "/system/bin/am"), but should be faster since it does not require starting a dalvik vm for
 * every command as done by "am" via termux/TermuxAm.
 *
 * The server is started by termux-app Application class but is not started if
 * {@link TermuxPropertyConstants#KEY_RUN_TERMUX_AM_SOCKET_SERVER} is `false` which can be done by
 * adding the prop with value "false" to the "~/.termux/termux.properties" file. Changes
 * require termux-app to be force stopped and restarted.
 *
 * The current state of the server can be checked with the
 * {@link TermuxAppShellEnvironment#ENV_TERMUX_APP__AM_SOCKET_SERVER_ENABLED} env variable, which is exported
 * for all shell sessions and tasks.
 *
 * https://github.com/termux/termux-am-socket
 * https://github.com/termux/TermuxAm
 */
class TermuxAmSocketServer {

    /**
     * Enhanced implementation for {@link AmSocketServer.AmSocketServerClient} for {@link TermuxAmSocketServer}.
     */
    open class TermuxAmSocketServerClient : AmSocketServer.AmSocketServerClient() {

        companion object {
            const val LOG_TAG = "TermuxAmSocketServerClient"
        }

        @Nullable
        override fun getLocalSocketManagerClientThreadUEH(@NonNull localSocketManager: LocalSocketManager): Thread.UncaughtExceptionHandler? {
            // Use termux crash handler for socket listener thread just like used for main app process thread.
            return TermuxCrashUtils.getCrashHandler(localSocketManager.getContext())
        }

        override fun onError(@NonNull localSocketManager: LocalSocketManager, @Nullable clientSocket: LocalClientSocket?, @NonNull error: Error) {
            // Don't show notification if server is not running since errors may be triggered
            // when server is stopped and server and client sockets are closed.
            if (localSocketManager.isRunning()) {
                showErrorNotification(localSocketManager.getContext(), error, localSocketManager.getLocalSocketRunConfig(), clientSocket)
            }
            // But log the exception
            super.onError(localSocketManager, clientSocket, error)
        }

        override fun onDisallowedClientConnected(@NonNull localSocketManager: LocalSocketManager, @NonNull clientSocket: LocalClientSocket, @NonNull error: Error) {
            // Always show notification and log error regardless of if server is running or not
            showErrorNotification(localSocketManager.getContext(), error, localSocketManager.getLocalSocketRunConfig(), clientSocket)
            super.onDisallowedClientConnected(localSocketManager, clientSocket, error)
        }

        protected override fun getLogTag(): String {
            return LOG_TAG
        }
    }

    companion object {
        const val LOG_TAG = "TermuxAmSocketServer"

        const val TITLE = "TermuxAm"

        /**
         * The static instance for the {@link TermuxAmSocketServer} {@link LocalSocketManager}.
         */
        private var termuxAmSocketServer: LocalSocketManager? = null

        /**
         * Whether {@link TermuxAmSocketServer} is enabled and running or not.
         */
        @Keep
        @JvmField
        protected var TERMUX_APP_AM_SOCKET_SERVER_ENABLED: Boolean? = null

        /**
         * Setup the {@link AmSocketServer} {@link LocalServerSocket} and start listening for
         * new {@link LocalClientSocket} if enabled.
         *
         * @param context The {@link Context} for {@link LocalSocketManager}.
         */
        @JvmStatic
        fun setupTermuxAmSocketServer(@NonNull context: Context) {
            // Start termux-am-socket server if enabled by user
            var enabled = false
            // getProperties() is null before init; Java NPE'd here, so keep it.
            if (TermuxAppSharedProperties.getProperties()!!.shouldRunTermuxAmSocketServer()) {
                Logger.logDebug(LOG_TAG, "Starting " + TITLE + " socket server since its enabled")
                start(context)
                val server = termuxAmSocketServer
                if (server != null && server.isRunning()) {
                    enabled = true
                    Logger.logDebug(LOG_TAG, TITLE + " socket server successfully started")
                }
            } else {
                Logger.logDebug(LOG_TAG, "Not starting " + TITLE + " socket server since its not enabled")
            }
            // Once termux-app has started, the server state must not be changed since the variable is
            // exported in shell sessions and tasks and if state is changed, then env of older shells will
            // retain invalid value. User should force stop the app to update state after changing prop.
            TERMUX_APP_AM_SOCKET_SERVER_ENABLED = enabled
            TermuxAppShellEnvironment.updateTermuxAppAMSocketServerEnabled(context)
        }

        /**
         * Create the {@link AmSocketServer} {@link LocalServerSocket} and start listening for new {@link LocalClientSocket}.
         */
        @JvmStatic
        @Synchronized
        fun start(@NonNull context: Context) {
            stop()
            val amSocketServerRunConfig = AmSocketServerRunConfig(TITLE, TermuxConstants.TERMUX_APP.TERMUX_AM_SOCKET_FILE_PATH, TermuxAmSocketServerClient())
            termuxAmSocketServer = AmSocketServer.start(context, amSocketServerRunConfig)
        }

        /**
         * Stop the {@link AmSocketServer} {@link LocalServerSocket} and stop listening for new {@link LocalClientSocket}.
         */
        @JvmStatic
        @Synchronized
        fun stop() {
            val server = termuxAmSocketServer
            if (server != null) {
                val error = server.stop()
                if (error != null) {
                    server.onError(error)
                }
                termuxAmSocketServer = null
            }
        }

        /**
         * Update the state of the {@link AmSocketServer} {@link LocalServerSocket} depending on current
         * value of {@link TermuxPropertyConstants#KEY_RUN_TERMUX_AM_SOCKET_SERVER}.
         */
        @JvmStatic
        @Synchronized
        fun updateState(@NonNull context: Context) {
            // getProperties() is null before init; Java NPE'd here, so keep it.
            val properties = TermuxAppSharedProperties.getProperties()!!
            if (properties.shouldRunTermuxAmSocketServer()) {
                if (termuxAmSocketServer == null) {
                    Logger.logDebug(LOG_TAG, "updateState: Starting " + TITLE + " socket server")
                    start(context)
                }
            } else {
                if (termuxAmSocketServer != null) {
                    Logger.logDebug(LOG_TAG, "updateState: Disabling " + TITLE + " socket server")
                    stop()
                }
            }
        }

        /**
         * Get {@link #termuxAmSocketServer}.
         */
        @JvmStatic
        @Synchronized
        fun getTermuxAmSocketServer(): LocalSocketManager? {
            return termuxAmSocketServer
        }

        /**
         * Show an error notification on the {@link TermuxConstants#TERMUX_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_ID}
         * {@link TermuxConstants#TERMUX_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_NAME} with a call
         * to {@link TermuxPluginUtils#sendPluginCommandErrorNotification(Context, String, CharSequence, String, String)}.
         *
         * @param context The {@link Context} to send the notification with.
         * @param error The {@link Error} generated.
         * @param localSocketRunConfig The {@link LocalSocketRunConfig} for {@link LocalSocketManager}.
         * @param clientSocket The optional {@link LocalClientSocket} for which the error was generated.
         */
        @JvmStatic
        @Synchronized
        fun showErrorNotification(@NonNull context: Context, @NonNull error: Error, @NonNull localSocketRunConfig: LocalSocketRunConfig, @Nullable clientSocket: LocalClientSocket?) {
            TermuxPluginUtils.sendPluginCommandErrorNotification(context, LOG_TAG, localSocketRunConfig.getTitle() + " Socket Server Error", error.getMinimalErrorString(), LocalSocketManager.getErrorMarkdownString(error, localSocketRunConfig, clientSocket))
        }

        @JvmStatic
        fun getTermuxAppAMSocketServerEnabled(@NonNull currentPackageContext: Context): Boolean? {
            val isTermuxApp = TermuxConstants.TERMUX_PACKAGE_NAME == currentPackageContext.packageName
            return if (isTermuxApp) {
                TERMUX_APP_AM_SOCKET_SERVER_ENABLED
            } else {
                // Currently, unsupported since plugin app processes don't know that value is set in termux
                // app process TermuxAmSocketServer class. A binder API or a way to check if server is actually
                // running needs to be used. Long checks would also not be possible on main application thread
                null
            }
        }
    }
}
