package com.termux.shared.shell.command.runner.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import androidx.annotation.NonNull
import com.google.common.base.Joiner
import com.termux.shared.R
import com.termux.shared.logger.Logger
import com.termux.shared.shell.ShellUtils
import com.termux.shared.shell.StreamGobbler
import com.termux.shared.shell.command.ExecutionCommand
import com.termux.shared.shell.command.ExecutionCommand.ExecutionState
import com.termux.shared.shell.command.environment.IShellEnvironment
import com.termux.shared.shell.command.environment.ShellEnvironmentUtils
import com.termux.shared.errors.Errno
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.HashMap
import kotlin.concurrent.thread

/**
 * A class that maintains info for background app shells run with {@link Runtime#exec(String[], String[], File)}.
 * It also provides a way to link each {@link Process} with the {@link ExecutionCommand}
 * that started it. The shell is run in the app user context.
 */
class AppShell private constructor(
    private val mProcess: Process,
    private val mExecutionCommand: ExecutionCommand,
    private val mAppShellClient: AppShellClient?
) {

    companion object {
        private const val LOG_TAG = "AppShell"

        /**
         * Start execution of an {@link ExecutionCommand} with {@link Runtime#exec(String[], String[], File)}.
         *
         * The {@link ExecutionCommand#executable}, must be set.
         * The  {@link ExecutionCommand#commandLabel}, {@link ExecutionCommand#arguments} and
         * {@link ExecutionCommand#workingDirectory} may optionally be set.
         *
         * @param currentPackageContext The {@link Context} for operations. This must be the context for
         *                              the current package and not the context of a `sharedUserId` package,
         *                              since environment setup may be dependent on current package.
         * @param executionCommand The {@link ExecutionCommand} containing the information for execution command.
         * @param appShellClient The {@link AppShellClient} interface implementation.
         *                           The {@link AppShellClient#onAppShellExited(AppShell)} will
         *                           be called regardless of {@code isSynchronous} value but not if
         *                           {@code null} is returned by this method. This can
         *                           optionally be {@code null}.
         * @param shellEnvironmentClient The {@link IShellEnvironment} interface implementation.
         * @param additionalEnvironment The additional shell environment variables to export. Existing
         *                              variables will be overridden.
         * @param isSynchronous If set to {@code true}, then the command will be executed in the
         *                      caller thread and results returned synchronously in the {@link ExecutionCommand}
         *                      sub object of the {@link AppShell} returned.
         *                      If set to {@code false}, then a new thread is started run the commands
         *                      asynchronously in the background and control is returned to the caller thread.
         * @return Returns the {@link AppShell}. This will be {@code null} if failed to start the execution command.
         */
        @JvmStatic
        fun execute(@NonNull currentPackageContext: Context, @NonNull executionCommand: ExecutionCommand, appShellClient: AppShellClient?, @NonNull shellEnvironmentClient: IShellEnvironment, additionalEnvironment: HashMap<String, String>?, isSynchronous: Boolean): AppShell? {
            val executable = executionCommand.executable
            if (executable.isNullOrEmpty()) {
                executionCommand.setStateFailed(Errno.ERRNO_FAILED.getCode(), currentPackageContext.getString(R.string.error_executable_unset, executionCommand.getCommandIdAndLabelLogString()))
                processAppShellResult(null, executionCommand)
                return null
            }
            if (executionCommand.workingDirectory.isNullOrEmpty())
                executionCommand.workingDirectory = shellEnvironmentClient.getDefaultWorkingDirectoryPath()
            if (executionCommand.workingDirectory.isNullOrEmpty())
                executionCommand.workingDirectory = "/"
            // Transform executable path to shell/session name, e.g. "/bin/do-something.sh" => "do-something.sh".
            val executableBasename = ShellUtils.getExecutableBasename(executable)
            if (executionCommand.shellName == null)
                executionCommand.shellName = executableBasename
            if (executionCommand.commandLabel == null)
                executionCommand.commandLabel = executableBasename
            // Setup command args
            val commandArray = shellEnvironmentClient.setupShellCommandArguments(executable, executionCommand.arguments)
            // Setup command environment
            val environment = shellEnvironmentClient.setupShellCommandEnvironment(currentPackageContext, executionCommand)
            if (additionalEnvironment != null)
                environment.putAll(additionalEnvironment)
            val environmentList = ShellEnvironmentUtils.convertEnvironmentToEnviron(environment)
            Collections.sort(environmentList)
            val environmentArray = environmentList.toTypedArray()
            if (!executionCommand.setState(ExecutionState.EXECUTING)) {
                executionCommand.setStateFailed(Errno.ERRNO_FAILED.getCode(), currentPackageContext.getString(R.string.error_failed_to_execute_app_shell_command, executionCommand.getCommandIdAndLabelLogString()))
                processAppShellResult(null, executionCommand)
                return null
            }
            // No need to log stdin if logging is disabled, like for app internal scripts
            Logger.logDebugExtended(LOG_TAG, ExecutionCommand.getExecutionInputLogString(executionCommand, true, Logger.shouldEnableLoggingForCustomLogLevel(executionCommand.backgroundCustomLogLevel)))
            Logger.logVerboseExtended(LOG_TAG, "\"" + executionCommand.getCommandIdAndLabelLogString() + "\" AppShell Environment:\n" + Joiner.on("\n").join(environmentArray))
            // Exec the process
            val process: Process
            try {
                // Non-null after the defaults above; !! preserves the Java NPE
                // instead of silently substituting a path.
                process = Runtime.getRuntime().exec(commandArray, environmentArray, File(executionCommand.workingDirectory!!))
            } catch (e: IOException) {
                executionCommand.setStateFailed(Errno.ERRNO_FAILED.getCode(), currentPackageContext.getString(R.string.error_failed_to_execute_app_shell_command, executionCommand.getCommandIdAndLabelLogString()), e)
                processAppShellResult(null, executionCommand)
                return null
            }
            val appShell = AppShell(process, executionCommand, appShellClient)
            if (isSynchronous) {
                try {
                    appShell.executeInner(currentPackageContext)
                } catch (e: IllegalThreadStateException) {
                    // TODO: Should either of these be handled or returned?
                } catch (e: InterruptedException) {
                    // TODO: Should either of these be handled or returned?
                }
            } else {
                thread {
                    try {
                        appShell.executeInner(currentPackageContext)
                    } catch (e: IllegalThreadStateException) {
                        // TODO: Should either of these be handled or returned?
                    } catch (e: InterruptedException) {
                        // TODO: Should either of these be handled or returned?
                    }
                }
            }
            return appShell
        }

        /**
         * Process the results of {@link AppShell} or {@link ExecutionCommand}.
         *
         * Only one of {@code appShell} and {@code executionCommand} must be set.
         *
         * If the {@code appShell} and its client are not {@code null},
         * then the {@link AppShellClient#onAppShellExited(AppShell)} callback will be called.
         *
         * @param appShell The {@link AppShell}, which should be set if
         *                  {@link #execute(Context, ExecutionCommand, AppShellClient, IShellEnvironment, HashMap, boolean)}
         *                   successfully started the process.
         * @param executionCommand The {@link ExecutionCommand}, which should be set if
         *                          {@link #execute(Context, ExecutionCommand, AppShellClient, IShellEnvironment, HashMap, boolean)}
         *                          failed to start the process.
         */
        private fun processAppShellResult(appShell: AppShell?, executionCommand: ExecutionCommand?) {
            var command = executionCommand
            if (appShell != null)
                command = appShell.mExecutionCommand
            if (command == null)
                return
            if (command.shouldNotProcessResults()) {
                Logger.logDebug(LOG_TAG, "Ignoring duplicate call to process \"" + command.getCommandIdAndLabelLogString() + "\" AppShell result")
                return
            }
            Logger.logDebug(LOG_TAG, "Processing \"" + command.getCommandIdAndLabelLogString() + "\" AppShell result")
            if (appShell != null && appShell.mAppShellClient != null) {
                appShell.mAppShellClient.onAppShellExited(appShell)
            } else {
                // If a callback is not set and execution command didn't fail, then we set success state now
                // Otherwise, the callback host can set it himself when its done with the appShell
                if (!command.isStateFailed())
                    command.setState(ExecutionState.SUCCESS)
            }
        }
    }

    /**
     * Sets up stdout and stderr readers for the {@link #mProcess} and waits for the process to end.
     *
     * If the processes finishes, then sets {@link ResultData#stdout}, {@link ResultData#stderr}
     * and {@link ResultData#exitCode} for the {@link #mExecutionCommand} of the {@code appShell}
     * and then calls {@link #processAppShellResult(AppShell, ExecutionCommand) to process the result}.
     *
     * @param context The {@link Context} for operations.
     */
    @Throws(IllegalThreadStateException::class, InterruptedException::class)
    private fun executeInner(@NonNull context: Context) {
        mExecutionCommand.mPid = ShellUtils.getPid(mProcess)
        Logger.logDebug(LOG_TAG, "Running \"" + mExecutionCommand.getCommandIdAndLabelLogString() + "\" AppShell with pid " + mExecutionCommand.mPid)
        mExecutionCommand.resultData.exitCode = null
        // setup stdin, and stdout and stderr gobblers
        val stdinStream = DataOutputStream(mProcess.outputStream)
        val stdoutGobbler = StreamGobbler(mExecutionCommand.mPid.toString() + "-stdout", mProcess.inputStream, mExecutionCommand.resultData.stdout, mExecutionCommand.backgroundCustomLogLevel)
        val stderrGobbler = StreamGobbler(mExecutionCommand.mPid.toString() + "-stderr", mProcess.errorStream, mExecutionCommand.resultData.stderr, mExecutionCommand.backgroundCustomLogLevel)
        // start gobbling
        stdoutGobbler.start()
        stderrGobbler.start()
        val stdin = mExecutionCommand.stdin
        if (!stdin.isNullOrEmpty()) {
            try {
                stdinStream.write((stdin + "\n").toByteArray(StandardCharsets.UTF_8))
                stdinStream.flush()
                stdinStream.close()
                //STDIN.write("exit\n".getBytes(StandardCharsets.UTF_8));
                //STDIN.flush();
            } catch (e: IOException) {
                val message = e.message
                if (message != null && (message.contains("EPIPE") || message.contains("Stream closed"))) {
                    // Method most horrid to catch broken pipe, in which case we
                    // do nothing. The command is not a shell, the shell closed
                    // STDIN, the script already contained the exit command, etc.
                    // these cases we want the output instead of returning null.
                } else {
                    // other issues we don't know how to handle, leads to
                    // returning null
                    mExecutionCommand.setStateFailed(Errno.ERRNO_FAILED.getCode(), context.getString(R.string.error_exception_received_while_executing_app_shell_command, mExecutionCommand.getCommandIdAndLabelLogString(), message), e)
                    mExecutionCommand.resultData.exitCode = 1
                    processAppShellResult(this, null)
                    kill()
                    return
                }
            }
        }
        // wait for our process to finish, while we gobble away in the background
        val exitCode = mProcess.waitFor()
        // make sure our threads are done gobbling
        // and the process is destroyed - while the latter shouldn't be
        // needed in theory, and may even produce warnings, in "normal" Java
        // they are required for guaranteed cleanup of resources, so lets be
        // safe and do this on Android as well
        try {
            stdinStream.close()
        } catch (e: IOException) {
            // might be closed already
        }
        stdoutGobbler.join()
        stderrGobbler.join()
        mProcess.destroy()
        // Process result
        if (exitCode == 0)
            Logger.logDebug(LOG_TAG, "The \"" + mExecutionCommand.getCommandIdAndLabelLogString() + "\" AppShell with pid " + mExecutionCommand.mPid + " exited normally")
        else
            Logger.logDebug(LOG_TAG, "The \"" + mExecutionCommand.getCommandIdAndLabelLogString() + "\" AppShell with pid " + mExecutionCommand.mPid + " exited with code: " + exitCode)
        // If the execution command has already failed, like SIGKILL was sent, then don't continue
        if (mExecutionCommand.isStateFailed()) {
            Logger.logDebug(LOG_TAG, "Ignoring setting \"" + mExecutionCommand.getCommandIdAndLabelLogString() + "\" AppShell state to ExecutionState.EXECUTED and processing results since it has already failed")
            return
        }
        mExecutionCommand.resultData.exitCode = exitCode
        if (!mExecutionCommand.setState(ExecutionState.EXECUTED))
            return
        processAppShellResult(this, null)
    }

    /**
     * Terminate this {@link AppShell} by sending a {@link OsConstants#SIGTERM} to its {@link #mProcess}
     * if it is still executing. After {@code sigkillDelayOnStop} milliseconds {@link OsConstants#SIGKILL} is
     * signalled.
     *
     * @param context The {@link Context} for operations.
     * @param sigkillDelayOnStop The delay after which a SIGKILL is send.
     * @param processResult If set to {@code true}, then the {@link #processAppShellResult(AppShell, ExecutionCommand)}
     *                      will be called to process the failure.
     */
    fun terminateIfExecuting(@NonNull context: Context, sigkillDelayOnStop: Long, processResult: Boolean) {
        if (sigkillDelayOnStop == 0L) {
            killIfExecuting(context, processResult)
            return
        }

        // If execution command has already finished executing, then no need to process results or sending any signals
        if (mExecutionCommand.hasExecuted()) {
            Logger.logDebug(LOG_TAG, "Ignoring sending SIGTERM or SIGKILL to \"" + mExecutionCommand.getCommandIdAndLabelLogString() + "\" AppShell since it has already finished executing")
            return
        }

        Logger.logDebug(LOG_TAG, "Send SIGTERM to \"" + mExecutionCommand.getCommandIdAndLabelLogString() + "\" AppShell")

        if (mExecutionCommand.isExecuting()) {
            term()
        }

        // Explicit main looper: the no-arg Handler() binds whatever looper the
        // calling thread has and crashes on threads without one. All current
        // callers run on the main thread, so this is identical for them.
        Handler(Looper.getMainLooper()).postDelayed({ killIfExecuting(context, processResult) }, sigkillDelayOnStop)
    }

    /**
     * Kill this {@link AppShell} by sending a {@link OsConstants#SIGKILL} to its {@link #mProcess}
     * if its still executing.
     *
     * @param context The {@link Context} for operations.
     * @param processResult If set to {@code true}, then the {@link #processAppShellResult(AppShell, ExecutionCommand)}
     *                      will be called to process the failure.
     */
    fun killIfExecuting(@NonNull context: Context, processResult: Boolean) {
        // If execution command has already finished executing, then no need to process results or send SIGKILL
        if (mExecutionCommand.hasExecuted()) {
            Logger.logDebug(LOG_TAG, "Ignoring sending SIGKILL to \"" + mExecutionCommand.getCommandIdAndLabelLogString() + "\" AppShell since it has already finished executing")
            return
        }
        Logger.logDebug(LOG_TAG, "Send SIGKILL to \"" + mExecutionCommand.getCommandIdAndLabelLogString() + "\" AppShell")
        if (mExecutionCommand.setStateFailed(Errno.ERRNO_FAILED.getCode(), context.getString(R.string.error_sending_sigkill_to_process))) {
            if (processResult) {
                // SIGKILL
                mExecutionCommand.resultData.exitCode = 137
                processAppShellResult(this, null)
            }
        }
        if (mExecutionCommand.isExecuting()) {
            kill()
        }
    }

    /**
     * Terminate this {@link AppShell} by sending a {@link OsConstants#SIGTERM} to its {@link #mProcess}.
     */
    fun term() {
        val pid = ShellUtils.getPid(mProcess)
        try {
            // Send SIGTERM to process
            Os.kill(pid, OsConstants.SIGTERM)
        } catch (e: ErrnoException) {
            Logger.logWarn(LOG_TAG, "Failed to send SIGTERM to \"" + mExecutionCommand.getCommandIdAndLabelLogString() + "\" AppShell with pid " + pid + ": " + e.message)
        }
    }

    /**
     * Kill this {@link AppShell} by sending a {@link OsConstants#SIGKILL} to its {@link #mProcess}.
     */
    fun kill() {
        val pid = ShellUtils.getPid(mProcess)
        try {
            // Send SIGKILL to process
            Os.kill(pid, OsConstants.SIGKILL)
        } catch (e: ErrnoException) {
            Logger.logWarn(LOG_TAG, "Failed to send SIGKILL to \"" + mExecutionCommand.getCommandIdAndLabelLogString() + "\" AppShell with pid " + pid + ": " + e.message)
        }
    }

    fun getProcess(): Process {
        return mProcess
    }

    fun getExecutionCommand(): ExecutionCommand {
        return mExecutionCommand
    }

    fun interface AppShellClient {

        /**
         * Callback function for when {@link AppShell} exits.
         *
         * @param appShell The {@link AppShell} that exited.
         */
        fun onAppShellExited(appShell: AppShell)
    }
}
