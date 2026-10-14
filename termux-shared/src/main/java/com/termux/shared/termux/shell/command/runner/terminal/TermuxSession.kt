package com.termux.shared.termux.shell.command.runner.terminal

import android.content.Context
import androidx.annotation.NonNull
import com.google.common.base.Joiner
import com.termux.shared.R
import com.termux.shared.logger.Logger
import com.termux.shared.shell.ShellUtils
import com.termux.shared.shell.command.ExecutionCommand
import com.termux.shared.shell.command.ExecutionCommand.ExecutionState
import com.termux.shared.shell.command.environment.IShellEnvironment
import com.termux.shared.shell.command.environment.ShellEnvironmentUtils
import com.termux.shared.shell.command.environment.UnixShellEnvironment
import com.termux.shared.errors.Errno
import com.termux.terminal.TerminalSessionClient
import java.io.File
import java.util.Collections
import java.util.HashMap

/**
 * A class that maintains info for foreground Termux sessions.
 * It also provides a way to link each terminal session with the {@link ExecutionCommand}
 * that started it.
 *
 * NOTE: the terminal class is always referenced by fully qualified name
 * below. An unqualified `TerminalSession` inside this class body would resolve
 * to this class itself and not to `com.termux.terminal.TerminalSession`.
 */
class TermuxSession private constructor(
    private val mTerminalSession: com.termux.terminal.TerminalSession,
    private val mExecutionCommand: ExecutionCommand,
    private val mTermuxSessionClient: TermuxSessionClient?,
    private val mSetStdoutOnExit: Boolean
) {

    companion object {
        private const val LOG_TAG = "TermuxSession"

        /**
         * Start execution of an {@link ExecutionCommand} with {@link Runtime#exec(String[], String[], File)}.
         *
         * The {@link ExecutionCommand#executable}, must be set, {@link ExecutionCommand#commandLabel},
         * {@link ExecutionCommand#arguments} and {@link ExecutionCommand#workingDirectory} may optionally
         * be set.
         *
         * If {@link ExecutionCommand#executable} is {@code null}, then a default shell is automatically
         * chosen.
         *
         * @param currentPackageContext The {@link Context} for operations. This must be the context for
         *                              the current package and not the context of a `sharedUserId` package,
         *                              since environment setup may be dependent on current package.
         * @param executionCommand The {@link ExecutionCommand} containing the information for execution command.
         * @param terminalSessionClient The terminal session client interface implementation.
         * @param termuxSessionClient The {@link TermuxSessionClient} interface implementation.
         * @param shellEnvironmentClient The {@link IShellEnvironment} interface implementation.
         * @param additionalEnvironment The additional shell environment variables to export. Existing
         *                              variables will be overridden.
         * @param setStdoutOnExit If set to {@code true}, then the {@link ResultData#stdout}
         *                        available in the {@link TermuxSessionClient#onTermuxSessionExited(TermuxSession)}
         *                        callback will be set to the terminal session transcript. The session
         *                        transcript will contain both stdout and stderr combined, basically
         *                        anything sent to the the pseudo terminal /dev/pts, including PS1 prefixes.
         *                        Set this to {@code true} only if the session transcript is required,
         *                        since this requires extra processing to get it.
         * @return Returns the {@link TermuxSession}. This will be {@code null} if failed to start the execution command.
         */
        @JvmStatic
        fun execute(@NonNull currentPackageContext: Context, @NonNull executionCommand: ExecutionCommand, @NonNull terminalSessionClient: TerminalSessionClient, termuxSessionClient: TermuxSessionClient?, @NonNull shellEnvironmentClient: IShellEnvironment, additionalEnvironment: HashMap<String, String>?, setStdoutOnExit: Boolean): TermuxSession? {
            if (executionCommand.executable.isNullOrEmpty())
                executionCommand.executable = null
            if (executionCommand.workingDirectory.isNullOrEmpty())
                executionCommand.workingDirectory = shellEnvironmentClient.getDefaultWorkingDirectoryPath()
            if (executionCommand.workingDirectory.isNullOrEmpty())
                executionCommand.workingDirectory = "/"
            var defaultBinPath = shellEnvironmentClient.getDefaultBinPath()
            if (defaultBinPath.isNullOrEmpty())
                defaultBinPath = "/system/bin"
            var isLoginShell = false
            if (executionCommand.executable == null) {
                if (!executionCommand.isFailsafe) {
                    for (shellBinary in UnixShellEnvironment.LOGIN_SHELL_BINARIES) {
                        val shellFile = File(defaultBinPath, shellBinary)
                        if (shellFile.canExecute()) {
                            executionCommand.executable = shellFile.absolutePath
                            break
                        }
                    }
                }
                if (executionCommand.executable == null) {
                    // Fall back to system shell as last resort:
                    // Do not start a login shell since ~/.profile may cause startup failure if its invalid.
                    // /system/bin/sh is provided by mksh (not toybox) and does load .mkshrc but for android its set
                    // to /system/etc/mkshrc even though its default is ~/.mkshrc.
                    // So /system/etc/mkshrc must still be valid for failsafe session to start properly.
                    // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r3:external/mksh/src/main.c;l=663
                    // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r3:external/mksh/src/main.c;l=41
                    // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r3:external/mksh/Android.bp;l=114
                    executionCommand.executable = "/system/bin/sh"
                } else {
                    isLoginShell = true
                }
            }
            // Non-null on all paths above: originally set, found by the shell
            // search, or the /system/bin/sh fallback. !! preserves the Java NPE
            // instead of silently substituting a path.
            val executable = executionCommand.executable!!
            // Setup command args. setupShellCommandArguments always returns a
            // non-empty array with the executable first, so [0] is safe.
            val commandArgs = shellEnvironmentClient.setupShellCommandArguments(executable, executionCommand.arguments)
            executionCommand.executable = commandArgs[0]
            val processName = (if (isLoginShell) "-" else "") + ShellUtils.getExecutableBasename(executable)
            val arguments: Array<String?> = arrayOfNulls(commandArgs.size)
            arguments[0] = processName
            if (commandArgs.size > 1)
                commandArgs.copyInto(arguments, 1, 1)
            executionCommand.arguments = arguments
            if (executionCommand.commandLabel == null)
                executionCommand.commandLabel = processName
            // Setup command environment
            val environment = shellEnvironmentClient.setupShellCommandEnvironment(currentPackageContext, executionCommand)
            if (additionalEnvironment != null)
                environment.putAll(additionalEnvironment)
            val environmentList = ShellEnvironmentUtils.convertEnvironmentToEnviron(environment)
            Collections.sort(environmentList)
            val environmentArray = environmentList.toTypedArray()
            if (!executionCommand.setState(ExecutionState.EXECUTING)) {
                executionCommand.setStateFailed(Errno.ERRNO_FAILED.getCode(), currentPackageContext.getString(R.string.error_failed_to_execute_termux_session_command, executionCommand.getCommandIdAndLabelLogString()))
                processTermuxSessionResult(null, executionCommand)
                return null
            }
            Logger.logDebugExtended(LOG_TAG, executionCommand.toString())
            Logger.logVerboseExtended(LOG_TAG, "\"" + executionCommand.getCommandIdAndLabelLogString() + "\" TermuxSession Environment:\n" + Joiner.on("\n").join(environmentArray))
            Logger.logDebug(LOG_TAG, "Running \"" + executionCommand.getCommandIdAndLabelLogString() + "\" TermuxSession")
            val terminalSession = com.termux.terminal.TerminalSession(executable, executionCommand.workingDirectory, executionCommand.arguments, environmentArray, executionCommand.terminalTranscriptRows, terminalSessionClient)
            if (executionCommand.shellName != null) {
                terminalSession.mSessionName = executionCommand.shellName
            }
            return TermuxSession(terminalSession, executionCommand, termuxSessionClient, setStdoutOnExit)
        }

        /**
         * Process the results of {@link TermuxSession} or {@link ExecutionCommand}.
         *
         * Only one of {@code termuxSession} and {@code executionCommand} must be set.
         *
         * If the {@code termuxSession} and its {@link #mTermuxSessionClient} are not {@code null},
         * then the {@link TermuxSession.TermuxSessionClient#onTermuxSessionExited(TermuxSession)}
         * callback will be called.
         *
         * @param termuxSession The {@link TermuxSession}, which should be set if
         *                  {@link #execute(Context, ExecutionCommand, TerminalSessionClient, TermuxSessionClient, IShellEnvironment, HashMap, boolean)}
         *                   successfully started the process.
         * @param executionCommand The {@link ExecutionCommand}, which should be set if
         *                          {@link #execute(Context, ExecutionCommand, TerminalSessionClient, TermuxSessionClient, IShellEnvironment, HashMap, boolean)}
         *                          failed to start the process.
         */
        private fun processTermuxSessionResult(termuxSession: TermuxSession?, executionCommand: ExecutionCommand?) {
            var command = executionCommand
            if (termuxSession != null)
                command = termuxSession.mExecutionCommand
            if (command == null)
                return
            if (command.shouldNotProcessResults()) {
                Logger.logDebug(LOG_TAG, "Ignoring duplicate call to process \"" + command.getCommandIdAndLabelLogString() + "\" TermuxSession result")
                return
            }
            Logger.logDebug(LOG_TAG, "Processing \"" + command.getCommandIdAndLabelLogString() + "\" TermuxSession result")
            if (termuxSession != null && termuxSession.mTermuxSessionClient != null) {
                termuxSession.mTermuxSessionClient.onTermuxSessionExited(termuxSession)
            } else {
                // If a callback is not set and execution command didn't fail, then we set success state now
                // Otherwise, the callback host can set it himself when its done with the termuxSession
                if (!command.isStateFailed())
                    command.setState(ExecutionState.SUCCESS)
            }
        }
    }

    /**
     * Signal that this {@link TermuxSession} has finished.  This should be called when
     * the terminal session finished callback is received by the caller.
     *
     * If the processes has finished, then sets {@link ResultData#stdout}, {@link ResultData#stderr}
     * and {@link ResultData#exitCode} for the {@link #mExecutionCommand} of the {@code termuxTask}
     * and then calls {@link #processTermuxSessionResult(TermuxSession, ExecutionCommand)} to process the result}.
     */
    fun finish() {
        // If process is still running, then ignore the call
        if (mTerminalSession.isRunning())
            return
        val exitCode = mTerminalSession.getExitStatus()
        if (exitCode == 0)
            Logger.logDebug(LOG_TAG, "The \"" + mExecutionCommand.getCommandIdAndLabelLogString() + "\" TermuxSession exited normally")
        else
            Logger.logDebug(LOG_TAG, "The \"" + mExecutionCommand.getCommandIdAndLabelLogString() + "\" TermuxSession exited with code: " + exitCode)
        // If the execution command has already failed, like SIGKILL was sent, then don't continue
        if (mExecutionCommand.isStateFailed()) {
            Logger.logDebug(LOG_TAG, "Ignoring setting \"" + mExecutionCommand.getCommandIdAndLabelLogString() + "\" TermuxSession state to ExecutionState.EXECUTED and processing results since it has already failed")
            return
        }
        mExecutionCommand.resultData.exitCode = exitCode
        if (this.mSetStdoutOnExit)
            mExecutionCommand.resultData.stdout.append(ShellUtils.getTerminalSessionTranscriptText(mTerminalSession, true, false))
        if (!mExecutionCommand.setState(ExecutionState.EXECUTED))
            return
        processTermuxSessionResult(this, null)
    }

    /**
     * Kill this {@link TermuxSession} by sending a {@link OsConstants#SIGKILL} to its terminal session
     * if its still executing.
     *
     * @param context The {@link Context} for operations.
     * @param processResult If set to {@code true}, then the {@link #processTermuxSessionResult(TermuxSession, ExecutionCommand)}
     *                      will be called to process the failure.
     */
    fun killIfExecuting(@NonNull context: Context, processResult: Boolean) {
        // If execution command has already finished executing, then no need to process results or send SIGKILL
        if (mExecutionCommand.hasExecuted()) {
            Logger.logDebug(LOG_TAG, "Ignoring sending SIGKILL to \"" + mExecutionCommand.getCommandIdAndLabelLogString() + "\" TermuxSession since it has already finished executing")
            return
        }
        Logger.logDebug(LOG_TAG, "Send SIGKILL to \"" + mExecutionCommand.getCommandIdAndLabelLogString() + "\" TermuxSession")
        if (mExecutionCommand.setStateFailed(Errno.ERRNO_FAILED.getCode(), context.getString(R.string.error_sending_sigkill_to_process))) {
            if (processResult) {
                // SIGKILL
                mExecutionCommand.resultData.exitCode = 137
                // Get whatever output has been set till now in case its needed
                if (this.mSetStdoutOnExit)
                    mExecutionCommand.resultData.stdout.append(ShellUtils.getTerminalSessionTranscriptText(mTerminalSession, true, false))
                processTermuxSessionResult(this, null)
            }
        }
        // Send SIGKILL to process
        mTerminalSession.finishIfRunning()
    }

    fun getTerminalSession(): com.termux.terminal.TerminalSession {
        return mTerminalSession
    }

    fun getExecutionCommand(): ExecutionCommand {
        return mExecutionCommand
    }

    fun interface TermuxSessionClient {

        /**
         * Callback function for when {@link TermuxSession} exits.
         *
         * @param termuxSession The {@link TermuxSession} that exited.
         */
        fun onTermuxSessionExited(termuxSession: TermuxSession)
    }
}
