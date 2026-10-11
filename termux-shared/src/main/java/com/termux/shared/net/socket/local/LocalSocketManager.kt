package com.termux.shared.net.socket.local

import android.content.Context
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.termux.shared.errors.Error
import com.termux.shared.jni.models.JniResult
import com.termux.shared.logger.Logger

/**
 * Manager for an AF_UNIX/SOCK_STREAM local server.
 *
 * Usage:
 * 1. Implement the {@link ILocalSocketManager} that will receive call backs from the server including
 *    when client connects via {@link ILocalSocketManager#onClientAccepted(LocalSocketManager, LocalClientSocket)}.
 *    Optionally extend the {@link LocalSocketManagerClientBase} class that provides base implementation.
 * 2. Create a {@link LocalSocketRunConfig} instance with the run config of the server.
 * 3. Create a {@link LocalSocketManager} instance and call {@link #start()}.
 * 4. Stop server if needed with a call to {@link #stop()}.
 */
open class LocalSocketManager {

    /**
     * The {@link Context} that may needed for various operations.
     */
    @JvmField
    protected val mContext: Context

    /**
     * The {@link LocalSocketRunConfig} containing run config for the {@link LocalSocketManager}.
     */
    @JvmField
    protected val mLocalSocketRunConfig: LocalSocketRunConfig

    /**
     * The {@link LocalServerSocket} for the {@link LocalSocketManager}.
     */
    @JvmField
    protected val mServerSocket: LocalServerSocket

    /**
     * The {@link ILocalSocketManager} client for the {@link LocalSocketManager}.
     */
    @JvmField
    protected val mLocalSocketManagerClient: ILocalSocketManager

    /**
     * The {@link Thread.UncaughtExceptionHandler} used for client thread started by {@link LocalSocketManager}.
     */
    @JvmField
    protected val mLocalSocketManagerClientThreadUEH: Thread.UncaughtExceptionHandler

    /**
     * Whether the {@link LocalServerSocket} managed by {@link LocalSocketManager} in running or not.
     * Volatile since it is written under synchronized start/stop but read
     * by unsynchronized isRunning() from other threads.
     */
    @Volatile
    @JvmField
    protected var mIsRunning: Boolean = false

    /**
     * Create an new instance of {@link LocalSocketManager}.
     *
     * @param context The {@link #mContext} value.
     * @param localSocketRunConfig The {@link #mLocalSocketRunConfig} value.
     */
    constructor(@NonNull context: Context, @NonNull localSocketRunConfig: LocalSocketRunConfig) {
        mContext = context.applicationContext
        mLocalSocketRunConfig = localSocketRunConfig
        mServerSocket = LocalServerSocket(this)
        mLocalSocketManagerClient = mLocalSocketRunConfig.getLocalSocketManagerClient()
        mLocalSocketManagerClientThreadUEH = getLocalSocketManagerClientThreadUEHOrDefault()
    }

    /**
     * Create the {@link LocalServerSocket} and start listening for new {@link LocalClientSocket}.
     */
    @Synchronized
    fun start(): Error? {
        Logger.logDebugExtended(LOG_TAG, "start\n" + mLocalSocketRunConfig)
        if (!localSocketLibraryLoaded) {
            try {
                Logger.logDebug(LOG_TAG, "Loading \"" + LOCAL_SOCKET_LIBRARY + "\" library")
                System.loadLibrary(LOCAL_SOCKET_LIBRARY)
                localSocketLibraryLoaded = true
            } catch (t: Throwable) {
                val error = LocalSocketErrno.ERRNO_START_LOCAL_SOCKET_LIB_LOAD_FAILED_WITH_EXCEPTION.getError(t, LOCAL_SOCKET_LIBRARY, t.message)
                Logger.logErrorExtended(LOG_TAG, error.getErrorLogString())
                return error
            }
        }
        val error = mServerSocket.start()
        // Only mark running on success: the Java version set this before the
        // call, so a failed start left the manager wrongly marked running.
        if (error == null)
            mIsRunning = true
        return error
    }

    /**
     * Stop the {@link LocalServerSocket} and stop listening for new {@link LocalClientSocket}.
     */
    @Synchronized
    fun stop(): Error? {
        if (mIsRunning) {
            Logger.logDebugExtended(LOG_TAG, "stop\n" + mLocalSocketRunConfig)
            mIsRunning = false
            return mServerSocket.stop()
        }
        return null
    }

    /**
     * Wrapper for {@link #onError(LocalClientSocket, Error)} for {@code null} {@link LocalClientSocket}.
     */
    fun onError(@NonNull error: Error) {
        onError(null, error)
    }

    /**
     * Wrapper to call {@link ILocalSocketManager#onError(LocalSocketManager, LocalClientSocket, Error)} in a new thread.
     */
    fun onError(@Nullable clientSocket: LocalClientSocket?, @NonNull error: Error) {
        startLocalSocketManagerClientThread { mLocalSocketManagerClient.onError(this, clientSocket, error) }
    }

    /**
     * Wrapper to call {@link ILocalSocketManager#onDisallowedClientConnected(LocalSocketManager, LocalClientSocket, Error)} in a new thread.
     */
    fun onDisallowedClientConnected(@NonNull clientSocket: LocalClientSocket, @NonNull error: Error) {
        startLocalSocketManagerClientThread { mLocalSocketManagerClient.onDisallowedClientConnected(this, clientSocket, error) }
    }

    /**
     * Wrapper to call {@link ILocalSocketManager#onClientAccepted(LocalSocketManager, LocalClientSocket)} in a new thread.
     */
    fun onClientAccepted(@NonNull clientSocket: LocalClientSocket) {
        startLocalSocketManagerClientThread { mLocalSocketManagerClient.onClientAccepted(this, clientSocket) }
    }

    /**
     * All client accept logic must be run on separate threads so that incoming client acceptance is not blocked.
     */
    fun startLocalSocketManagerClientThread(@NonNull runnable: Runnable) {
        val thread = Thread(runnable)
        thread.uncaughtExceptionHandler = getLocalSocketManagerClientThreadUEH()
        try {
            thread.start()
        } catch (e: Exception) {
            Logger.logStackTraceWithMessage(LOG_TAG, "LocalSocketManagerClientThread start failed", e)
        }
    }

    /**
     * Get {@link #mContext}.
     */
    fun getContext(): Context {
        return mContext
    }

    /**
     * Get {@link #mLocalSocketRunConfig}.
     */
    fun getLocalSocketRunConfig(): LocalSocketRunConfig {
        return mLocalSocketRunConfig
    }

    /**
     * Get {@link #mLocalSocketManagerClient}.
     */
    fun getLocalSocketManagerClient(): ILocalSocketManager {
        return mLocalSocketManagerClient
    }

    /**
     * Get {@link #mServerSocket}.
     */
    fun getServerSocket(): LocalServerSocket {
        return mServerSocket
    }

    /**
     * Get {@link #mLocalSocketManagerClientThreadUEH}.
     */
    fun getLocalSocketManagerClientThreadUEH(): Thread.UncaughtExceptionHandler {
        return mLocalSocketManagerClientThreadUEH
    }

    /**
     * Get {@link Thread.UncaughtExceptionHandler} returned by call to
     * {@link ILocalSocketManager#getLocalSocketManagerClientThreadUEH(LocalSocketManager)}
     * or the default handler that just logs the exception.
     */
    protected open fun getLocalSocketManagerClientThreadUEHOrDefault(): Thread.UncaughtExceptionHandler {
        val uncaughtExceptionHandler = mLocalSocketManagerClient.getLocalSocketManagerClientThreadUEH(this)
        return uncaughtExceptionHandler ?: Thread.UncaughtExceptionHandler { t, e -> Logger.logStackTraceWithMessage(LOG_TAG, "Uncaught exception for " + t + " in " + mLocalSocketRunConfig.getTitle() + " server", e) }
    }

    /**
     * Get {@link #mIsRunning}.
     */
    fun isRunning(): Boolean {
        return mIsRunning
    }

    companion object {
        const val LOG_TAG = "LocalSocketManager"

        /**
         * The native JNI local socket library. Never reassigned; const like
         * the original effective behaviour.
         */
        protected const val LOCAL_SOCKET_LIBRARY = "local-socket"

        /**
         * Whether {@link #LOCAL_SOCKET_LIBRARY} has been loaded or not.
         */
        @JvmField
        protected var localSocketLibraryLoaded: Boolean = false

    /*
     Note: Exceptions thrown from JNI must be caught with Throwable class instead of Exception,
     otherwise exception will be sent to UncaughtExceptionHandler of the thread.
    */
    /**
     * Creates an AF_UNIX/SOCK_STREAM local server socket at {@code path}, with the specified backlog.
     *
     * @param serverTitle The server title used for logging and errors.
     * @param path The path at which to create the socket.
     *             For a filesystem socket, this must be an absolute path to the socket file.
     *             For an abstract namespace socket, the first byte must be a null `\0` character.
     *             Max allowed length is 108 bytes as per sun_path size (UNIX_PATH_MAX) on Linux.
     * @param backlog The maximum length to which the queue of pending connections for the socket
     *                may grow. This value may be ignored or may not have one-to-one mapping
     *                in kernel implementation. Value must be greater than 0.
     * @return Returns the {@link JniResult}. If server creation was successful, then
     * {@link JniResult#retval} will be 0 and {@link JniResult#intData} will contain the server socket
     * fd.
     */
    @JvmStatic
    fun createServerSocket(@NonNull serverTitle: String, @NonNull path: ByteArray, backlog: Int): JniResult? {
        return try {
            createServerSocketNative(serverTitle, path, backlog)
        } catch (t: Throwable) {
            val message = "Exception in createServerSocketNative()"
            Logger.logStackTraceWithMessage(LOG_TAG, message, t)
            JniResult(message, t)
        }
    }

    /**
     * Closes the socket with fd.
     *
     * @param serverTitle The server title used for logging and errors.
     * @param fd The socket fd.
     * @return Returns the {@link JniResult}. If closing socket was successful, then
     * {@link JniResult#retval} will be 0.
     */
    @JvmStatic
    fun closeSocket(@NonNull serverTitle: String, fd: Int): JniResult? {
        return try {
            closeSocketNative(serverTitle, fd)
        } catch (t: Throwable) {
            val message = "Exception in closeSocketNative()"
            Logger.logStackTraceWithMessage(LOG_TAG, message, t)
            JniResult(message, t)
        }
    }

    /**
     * Accepts a connection on the supplied server socket fd.
     *
     * @param serverTitle The server title used for logging and errors.
     * @param fd The server socket fd.
     * @return Returns the {@link JniResult}. If accepting socket was successful, then
     * {@link JniResult#retval} will be 0 and {@link JniResult#intData} will contain the client socket
     * fd.
     */
    @JvmStatic
    fun accept(@NonNull serverTitle: String, fd: Int): JniResult? {
        return try {
            acceptNative(serverTitle, fd)
        } catch (t: Throwable) {
            val message = "Exception in acceptNative()"
            Logger.logStackTraceWithMessage(LOG_TAG, message, t)
            JniResult(message, t)
        }
    }

    /**
     * Attempts to read up to data buffer length bytes from file descriptor fd into the data buffer.
     * On success, the number of bytes read is returned (zero indicates end of file).
     * It is not an error if bytes read is smaller than the number of bytes requested; this may happen
     * for example because fewer bytes are actually available right now (maybe because we were close
     * to end-of-file, or because we are reading from a pipe), or because read() was interrupted by
     * a signal. On error, the {@link JniResult#errno} and {@link JniResult#errmsg} will be set.
     *
     * If while reading the deadline elapses but all the data has not been read, the call will fail.
     *
     * @param serverTitle The server title used for logging and errors.
     * @param fd The socket fd.
     * @param data The data buffer to read bytes into.
     * @param deadline The deadline milliseconds since epoch.
     * @return Returns the {@link JniResult}. If reading was successful, then {@link JniResult#retval}
     * will be 0 and {@link JniResult#intData} will contain the bytes read.
     */
    @JvmStatic
    fun read(@NonNull serverTitle: String, fd: Int, @NonNull data: ByteArray, deadline: Long): JniResult? {
        return try {
            readNative(serverTitle, fd, data, deadline)
        } catch (t: Throwable) {
            val message = "Exception in readNative()"
            Logger.logStackTraceWithMessage(LOG_TAG, message, t)
            JniResult(message, t)
        }
    }

    /**
     * Attempts to send data buffer to the file descriptor. On error, the {@link JniResult#errno} and
     * {@link JniResult#errmsg} will be set.
     *
     * If while sending the deadline elapses but all the data has not been sent, the call will fail.
     *
     * @param serverTitle The server title used for logging and errors.
     * @param fd The socket fd.
     * @param data The data buffer containing bytes to send.
     * @param deadline The deadline milliseconds since epoch.
     * @return Returns the {@link JniResult}. If sending was successful, then
     * {@link JniResult#retval} will be 0.
     */
    @JvmStatic
    fun send(@NonNull serverTitle: String, fd: Int, @NonNull data: ByteArray, deadline: Long): JniResult? {
        return try {
            sendNative(serverTitle, fd, data, deadline)
        } catch (t: Throwable) {
            val message = "Exception in sendNative()"
            Logger.logStackTraceWithMessage(LOG_TAG, message, t)
            JniResult(message, t)
        }
    }

    /**
     * Gets the number of bytes available to read on the socket.
     *
     * @param serverTitle The server title used for logging and errors.
     * @param fd The socket fd.
     * @return Returns the {@link JniResult}. If checking availability was successful, then
     * {@link JniResult#retval} will be 0 and {@link JniResult#intData} will contain the bytes available.
     */
    @JvmStatic
    fun available(@NonNull serverTitle: String, fd: Int): JniResult? {
        return try {
            availableNative(serverTitle, fd)
        } catch (t: Throwable) {
            val message = "Exception in availableNative()"
            Logger.logStackTraceWithMessage(LOG_TAG, message, t)
            JniResult(message, t)
        }
    }

    /**
     * Set receiving (SO_RCVTIMEO) timeout in milliseconds for socket.
     *
     * @param serverTitle The server title used for logging and errors.
     * @param fd The socket fd.
     * @param timeout The timeout value in milliseconds.
     * @return Returns the {@link JniResult}. If setting timeout was successful, then
     * {@link JniResult#retval} will be 0.
     */
    @JvmStatic
    fun setSocketReadTimeout(@NonNull serverTitle: String, fd: Int, timeout: Int): JniResult? {
        return try {
            setSocketReadTimeoutNative(serverTitle, fd, timeout)
        } catch (t: Throwable) {
            val message = "Exception in setSocketReadTimeoutNative()"
            Logger.logStackTraceWithMessage(LOG_TAG, message, t)
            JniResult(message, t)
        }
    }

    /**
     * Set sending (SO_SNDTIMEO) timeout in milliseconds for fd.
     *
     * @param serverTitle The server title used for logging and errors.
     * @param fd The socket fd.
     * @param timeout The timeout value in milliseconds.
     * @return Returns the {@link JniResult}. If setting timeout was successful, then
     * {@link JniResult#retval} will be 0.
     */
    @JvmStatic
    fun setSocketSendTimeout(@NonNull serverTitle: String, fd: Int, timeout: Int): JniResult? {
        return try {
            setSocketSendTimeoutNative(serverTitle, fd, timeout)
        } catch (t: Throwable) {
            val message = "Exception in setSocketSendTimeoutNative()"
            Logger.logStackTraceWithMessage(LOG_TAG, message, t)
            JniResult(message, t)
        }
    }

    /**
     * Get the {@link PeerCred} for the socket.
     *
     * @param serverTitle The server title used for logging and errors.
     * @param fd The socket fd.
     * @param peerCred The {@link PeerCred} object that should be filled.
     * @return Returns the {@link JniResult}. If setting timeout was successful, then
     * {@link JniResult#retval} will be 0.
     */
    @JvmStatic
    fun getPeerCred(@NonNull serverTitle: String, fd: Int, peerCred: PeerCred?): JniResult? {
        return try {
            getPeerCredNative(serverTitle, fd, peerCred)
        } catch (t: Throwable) {
            val message = "Exception in getPeerCredNative()"
            Logger.logStackTraceWithMessage(LOG_TAG, message, t)
            JniResult(message, t)
        }
    }


        /**
         * Get an error log {@link String} for the {@link LocalSocketManager}.
         */
        @JvmStatic
        fun getErrorLogString(@NonNull error: Error, @NonNull localSocketRunConfig: LocalSocketRunConfig, @Nullable clientSocket: LocalClientSocket?): String {
            val logString = StringBuilder()
            logString.append(localSocketRunConfig.getTitle()).append(" Socket Server Error:\n")
            logString.append(error.getErrorLogString())
            logString.append("\n\n\n")
            logString.append(localSocketRunConfig.getLogString())
            if (clientSocket != null) {
                logString.append("\n\n\n")
                logString.append(clientSocket.getLogString())
            }
            return logString.toString()
        }

        /**
         * Get an error markdown {@link String} for the {@link LocalSocketManager}.
         */
        @JvmStatic
        fun getErrorMarkdownString(@NonNull error: Error, @NonNull localSocketRunConfig: LocalSocketRunConfig, @Nullable clientSocket: LocalClientSocket?): String {
            val markdownString = StringBuilder()
            markdownString.append(error.getErrorMarkdownString())
            markdownString.append("\n##\n\n\n")
            markdownString.append(localSocketRunConfig.getMarkdownString())
            if (clientSocket != null) {
                markdownString.append("\n\n\n")
                markdownString.append(clientSocket.getMarkdownString())
            }
            return markdownString.toString()
        }

        // Note: @JvmStatic on private external funs generates the private
        // static bridges that JNI static binding looks up by class name.
        @JvmStatic
        private external fun createServerSocketNative(@NonNull serverTitle: String, @NonNull path: ByteArray, backlog: Int): JniResult?

        @JvmStatic
        private external fun closeSocketNative(@NonNull serverTitle: String, fd: Int): JniResult?

        @JvmStatic
        private external fun acceptNative(@NonNull serverTitle: String, fd: Int): JniResult?

        @JvmStatic
        private external fun readNative(@NonNull serverTitle: String, fd: Int, @NonNull data: ByteArray, deadline: Long): JniResult?

        @JvmStatic
        private external fun sendNative(@NonNull serverTitle: String, fd: Int, @NonNull data: ByteArray, deadline: Long): JniResult?

        @JvmStatic
        private external fun availableNative(@NonNull serverTitle: String, fd: Int): JniResult?

        // The Java version missed @Nullable here while all siblings have it;
        // the callers already null-check, so this only fixes the annotation.
        @JvmStatic
        private external fun setSocketReadTimeoutNative(@NonNull serverTitle: String, fd: Int, timeout: Int): JniResult?

        @JvmStatic
        private external fun setSocketSendTimeoutNative(@NonNull serverTitle: String, fd: Int, timeout: Int): JniResult?

        @JvmStatic
        private external fun getPeerCredNative(@NonNull serverTitle: String, fd: Int, peerCred: PeerCred?): JniResult?
    }
}
