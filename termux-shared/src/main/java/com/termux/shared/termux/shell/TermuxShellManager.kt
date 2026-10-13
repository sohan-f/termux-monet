package com.termux.shared.termux.shell

import android.content.Context
import android.content.Intent
import androidx.annotation.NonNull
import com.termux.shared.shell.command.ExecutionCommand
import com.termux.shared.shell.command.runner.app.AppShell
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession

class TermuxShellManager(@NonNull context: Context) {

    @JvmField
    protected val mContext: Context

    init {
        mContext = context.applicationContext
    }

    /**
     * The foreground TermuxSessions which this service manages.
     * Note that this list is observed by an activity, like TermuxActivity.mTermuxSessionListViewController,
     * so any changes must be made on the UI thread and followed by a call to
     * {@link android.widget.ArrayAdapter#notifyDataSetChanged()}.
     */
    @JvmField
    val mTermuxSessions: MutableList<TermuxSession> = ArrayList()

    /**
     * The background TermuxTasks which this service manages.
     */
    @JvmField
    val mTermuxTasks: MutableList<AppShell> = ArrayList()

    /**
     * The pending plugin ExecutionCommands that have yet to be processed by this service.
     */
    @JvmField
    val mPendingPluginExecutionCommands: MutableList<ExecutionCommand> = ArrayList()

    companion object {
        private var shellManager: TermuxShellManager? = null

        private var SHELL_ID = 0

        /**
         * The {@link ExecutionCommand.Runner#APP_SHELL} number after app process was started/restarted.
         */
        @JvmField
        var APP_SHELL_NUMBER_SINCE_APP_START = 0

        /**
         * The {@link ExecutionCommand.Runner#TERMINAL_SESSION} number after app process was started/restarted.
         */
        @JvmField
        var TERMINAL_SESSION_NUMBER_SINCE_APP_START = 0

        /**
         * Initialize the {@link #shellManager}.
         *
         * @param context The {@link Context} for operations.
         * @return Returns the {@link TermuxShellManager}.
         */
        @JvmStatic
        fun init(@NonNull context: Context): TermuxShellManager {
            if (shellManager == null)
                shellManager = TermuxShellManager(context)
            // Non-null: just created above if it was missing.
            return shellManager!!
        }

        /**
         * Get the {@link #shellManager}.
         *
         * @return Returns the {@link TermuxShellManager}.
         */
        @JvmStatic
        fun getShellManager(): TermuxShellManager? {
            return shellManager
        }

        @JvmStatic
        @Synchronized
        fun onActionBootCompleted(@NonNull context: Context, @NonNull intent: Intent) {
            val preferences = TermuxAppSharedPreferences.build(context)
            if (preferences == null)
                return
            // Ensure any shells started after boot have valid ENV_SHELL_CMD__APP_SHELL_NUMBER_SINCE_BOOT and
            // ENV_SHELL_CMD__TERMINAL_SESSION_NUMBER_SINCE_BOOT exported
            preferences.resetAppShellNumberSinceBoot()
            preferences.resetTerminalSessionNumberSinceBoot()
        }

        @JvmStatic
        fun onAppExit(@NonNull context: Context) {
            // Ensure any shells started after boot have valid ENV_SHELL_CMD__APP_SHELL_NUMBER_SINCE_APP_START and
            // ENV_SHELL_CMD__TERMINAL_SESSION_NUMBER_SINCE_APP_START exported
            APP_SHELL_NUMBER_SINCE_APP_START = 0
            TERMINAL_SESSION_NUMBER_SINCE_APP_START = 0
        }

        @JvmStatic
        @Synchronized
        fun getNextShellId(): Int {
            return SHELL_ID++
        }

        @JvmStatic
        @Synchronized
        fun getAndIncrementAppShellNumberSinceAppStart(): Int {
            // Keep value at MAX_VALUE on integer overflow and not 0, since not first shell
            var curValue = APP_SHELL_NUMBER_SINCE_APP_START
            if (curValue < 0)
                curValue = Integer.MAX_VALUE
            APP_SHELL_NUMBER_SINCE_APP_START = curValue + 1
            if (APP_SHELL_NUMBER_SINCE_APP_START < 0)
                APP_SHELL_NUMBER_SINCE_APP_START = Integer.MAX_VALUE
            return curValue
        }

        @JvmStatic
        @Synchronized
        fun getAndIncrementTerminalSessionNumberSinceAppStart(): Int {
            // Keep value at MAX_VALUE on integer overflow and not 0, since not first shell
            var curValue = TERMINAL_SESSION_NUMBER_SINCE_APP_START
            if (curValue < 0)
                curValue = Integer.MAX_VALUE
            TERMINAL_SESSION_NUMBER_SINCE_APP_START = curValue + 1
            if (TERMINAL_SESSION_NUMBER_SINCE_APP_START < 0)
                TERMINAL_SESSION_NUMBER_SINCE_APP_START = Integer.MAX_VALUE
            return curValue
        }
    }
}
