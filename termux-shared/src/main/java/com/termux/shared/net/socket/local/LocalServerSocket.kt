package com.termux.shared.net.socket.local

import androidx.annotation.NonNull
import com.termux.shared.errors.Error
import com.termux.shared.file.FileUtils
import com.termux.shared.jni.models.JniResult
import com.termux.shared.logger.Logger
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * The server socket for {@link LocalSocketManager}.
 */
open class LocalServerSocket : Closeable {

    @JvmField
    protected val mLocalSocketManager: LocalSocketManager

    /**
     * The {@link LocalSocketRunConfig} containing run config for the {@link LocalServerSocket}.
     */
    @JvmField
    protected val mLocalSocketRunConfig: LocalSocketRunConfig

    /**
     * The {@link ILocalSocketManager} client for the {@link LocalSocketManager}.
     */
    @JvmField
    protected val mLocalSocketManagerClient: ILocalSocketManager

    /**
     * The {@link ClientSocketListener} {@link Thread} for the {@link LocalServerSocket}.
     * Recreated on every {@link #start()} since threads cannot be restarted.
     */
    @JvmField
    protected var mClientSocketListener: Thread

    /**
     * Create an new instance of {@link LocalServerSocket}.
     *
     * Internal like the original package-private constructor: only the
     * manager (same module) creates server sockets. In particular this must
     * not be protected, since Kotlin protected constructors are not callable
     * from non-subclasses the way Java package-private ones are.
     *
     * @param localSocketManager The {@link #mLocalSocketManager} value.
     */
    internal constructor(@NonNull localSocketManager: LocalSocketManager) {
        mLocalSocketManager = localSocketManager
        mLocalSocketRunConfig = localSocketManager.getLocalSocketRunConfig()
        mLocalSocketManagerClient = mLocalSocketRunConfig.getLocalSocketManagerClient()
        mClientSocketListener = Thread(ClientSocketListener())
    }

    /**
     * Start server by creating server socket.
     */
    @Synchronized
    fun start(): Error? {
        Logger.logDebug(LOG_TAG, "start")
        var path: String? = mLocalSocketRunConfig.getPath()
        if (path.isNullOrEmpty()) {
            return LocalSocketErrno.ERRNO_SERVER_SOCKET_PATH_NULL_OR_EMPTY.getError(mLocalSocketRunConfig.getTitle())
        }
        if (!mLocalSocketRunConfig.isAbstractNamespaceSocket()) {
            path = FileUtils.getCanonicalPath(path, null)
        }
        // getCanonicalPath never returns null, so !! preserves the Java NPE
        // instead of silently substituting a path.
        val socketPath = path!!
        // On Linux, sun_path is 108 bytes (UNIX_PATH_MAX) in size, so do an early check here to
        // prevent useless parent directory creation since createServerSocket() call will fail since
        // there is a native check as well.
        if (socketPath.toByteArray(StandardCharsets.UTF_8).size > 108) {
            return LocalSocketErrno.ERRNO_SERVER_SOCKET_PATH_TOO_LONG.getError(mLocalSocketRunConfig.getTitle(), socketPath)
        }
        val backlog = mLocalSocketRunConfig.getBacklog()
        if (backlog <= 0) {
            return LocalSocketErrno.ERRNO_SERVER_SOCKET_BACKLOG_INVALID.getError(mLocalSocketRunConfig.getTitle(), backlog)
        }
        // If server socket is not in abstract namespace
        if (!mLocalSocketRunConfig.isAbstractNamespaceSocket()) {
            if (!socketPath.startsWith("/"))
                return LocalSocketErrno.ERRNO_SERVER_SOCKET_PATH_NOT_ABSOLUTE.getError(mLocalSocketRunConfig.getTitle(), socketPath)
            // Create the server socket file parent directory and set SERVER_SOCKET_PARENT_DIRECTORY_PERMISSIONS if missing
            val socketParentPath = File(socketPath).parent
            val error = FileUtils.validateDirectoryFileExistenceAndPermissions(mLocalSocketRunConfig.getTitle() + " server socket file parent", socketParentPath, null, true, SERVER_SOCKET_PARENT_DIRECTORY_PERMISSIONS, true, true, false, false)
            if (error != null)
                return error
            // Delete the server socket file to stop any existing servers and for bind() to succeed
            val deleteError = deleteServerSocketFile()
            if (deleteError != null)
                return deleteError
        }
        // Create the server socket
        var result = LocalSocketManager.createServerSocket(mLocalSocketRunConfig.getLogTitle() + " (server)", socketPath.toByteArray(StandardCharsets.UTF_8), backlog)
        if (result == null || result.retval != 0) {
            return LocalSocketErrno.ERRNO_CREATE_SERVER_SOCKET_FAILED.getError(mLocalSocketRunConfig.getTitle(), JniResult.getErrorString(result))
        }
        val fd = result.intData
        if (fd < 0) {
            return LocalSocketErrno.ERRNO_SERVER_SOCKET_FD_INVALID.getError(fd, mLocalSocketRunConfig.getTitle())
        }
        // Update fd to signify that server socket has been created successfully
        mLocalSocketRunConfig.setFD(fd)
        // Recreate the listener thread: a Thread cannot be restarted, so
        // reusing the field across start() calls after stop() failed with
        // IllegalThreadStateException while still reporting success.
        mClientSocketListener = Thread(ClientSocketListener())
        mClientSocketListener.uncaughtExceptionHandler = mLocalSocketManager.getLocalSocketManagerClientThreadUEH()
        try {
            // Start listening to server clients
            mClientSocketListener.start()
        } catch (e: Exception) {
            Logger.logStackTraceWithMessage(LOG_TAG, "mClientSocketListener start failed", e)
        }
        return null
    }

    /**
     * Stop server.
     */
    @Synchronized
    fun stop(): Error? {
        Logger.logDebug(LOG_TAG, "stop")
        try {
            // Stop the LocalClientSocket listener.
            mClientSocketListener.interrupt()
        } catch (ignored: Exception) {
        }
        val error = closeServerSocket(false)
        if (error != null)
            return error
        return deleteServerSocketFile()
    }

    /**
     * Close server socket.
     */
    @Synchronized
    fun closeServerSocket(logErrorMessage: Boolean): Error? {
        Logger.logDebug(LOG_TAG, "closeServerSocket")
        try {
            close()
        } catch (e: IOException) {
            val error = LocalSocketErrno.ERRNO_CLOSE_SERVER_SOCKET_FAILED_WITH_EXCEPTION.getError(e, mLocalSocketRunConfig.getTitle(), e.message)
            if (logErrorMessage)
                Logger.logErrorExtended(LOG_TAG, error.getErrorLogString())
            return error
        }
        return null
    }

    /**
     * Implementation for {@link Closeable#close()} to close server socket.
     */
    @Synchronized
    override fun close() {
        Logger.logDebug(LOG_TAG, "close")
        val fd = mLocalSocketRunConfig.getFD()
        if (fd >= 0) {
            val result = LocalSocketManager.closeSocket(mLocalSocketRunConfig.getLogTitle() + " (server)", fd)
            if (result == null || result.retval != 0) {
                throw IOException(JniResult.getErrorString(result))
            }
            // Update fd to signify that server socket has been closed
            mLocalSocketRunConfig.setFD(-1)
        }
    }

    /**
     * Delete server socket file if not an abstract namespace socket. This will cause any existing
     * running server to stop.
     */
    private fun deleteServerSocketFile(): Error? {
        return if (!mLocalSocketRunConfig.isAbstractNamespaceSocket())
            FileUtils.deleteSocketFile(mLocalSocketRunConfig.getTitle() + " server socket file", mLocalSocketRunConfig.getPath(), true)
        else
            null
    }

    /**
     * Listen and accept new {@link LocalClientSocket}.
     */
    fun accept(): LocalClientSocket? {
        Logger.logVerbose(LOG_TAG, "accept")
        while (true) {
            // If server socket closed
            val fd = mLocalSocketRunConfig.getFD()
            if (fd < 0) {
                return null
            }
            var result = LocalSocketManager.accept(mLocalSocketRunConfig.getLogTitle() + " (client)", fd)
            if (result == null || result.retval != 0) {
                mLocalSocketManager.onError(LocalSocketErrno.ERRNO_ACCEPT_CLIENT_SOCKET_FAILED.getError(mLocalSocketRunConfig.getTitle(), JniResult.getErrorString(result)))
                continue
            }
            val clientFD = result.intData
            if (clientFD < 0) {
                mLocalSocketManager.onError(LocalSocketErrno.ERRNO_CLIENT_SOCKET_FD_INVALID.getError(clientFD, mLocalSocketRunConfig.getTitle()))
                continue
            }
            val peerCred = PeerCred()
            result = LocalSocketManager.getPeerCred(mLocalSocketRunConfig.getLogTitle() + " (client)", clientFD, peerCred)
            if (result == null || result.retval != 0) {
                mLocalSocketManager.onError(LocalSocketErrno.ERRNO_GET_CLIENT_SOCKET_PEER_UID_FAILED.getError(mLocalSocketRunConfig.getTitle(), JniResult.getErrorString(result)))
                LocalClientSocket.closeClientSocket(mLocalSocketManager, clientFD)
                continue
            }
            val peerUid = peerCred.uid
            if (peerUid < 0) {
                mLocalSocketManager.onError(LocalSocketErrno.ERRNO_CLIENT_SOCKET_PEER_UID_INVALID.getError(peerUid, mLocalSocketRunConfig.getTitle()))
                LocalClientSocket.closeClientSocket(mLocalSocketManager, clientFD)
                continue
            }
            val clientSocket = LocalClientSocket(mLocalSocketManager, clientFD, peerCred)
            Logger.logVerbose(LOG_TAG, "Client socket accept for \"" + mLocalSocketRunConfig.getTitle() + "\" server\n" + clientSocket.getLogString())
            // Only allow connection if the peer has the same uid as server app's user id or root user id
            if (peerUid != mLocalSocketManager.getContext().applicationInfo.uid && peerUid != 0) {
                mLocalSocketManager.onDisallowedClientConnected(clientSocket, LocalSocketErrno.ERRNO_CLIENT_SOCKET_PEER_UID_DISALLOWED.getError(clientSocket.getPeerCred().getMinimalString(), mLocalSocketRunConfig.getTitle()))
                clientSocket.closeClientSocket(true)
                continue
            }
            return clientSocket
        }
    }

    /**
     * The {@link LocalClientSocket} listener {@link java.lang.Runnable} for {@link LocalServerSocket}.
     */
    protected inner class ClientSocketListener : Runnable {

        override fun run() {
            try {
                Logger.logVerbose(LOG_TAG, "ClientSocketListener start")
                while (!Thread.currentThread().isInterrupted) {
                    var clientSocket: LocalClientSocket? = null
                    try {
                        // Listen for new client socket connections
                        clientSocket = accept()
                        // If server socket is closed, then stop listener thread.
                        if (clientSocket == null)
                            break
                        var error = clientSocket.setReadTimeout()
                        if (error != null) {
                            mLocalSocketManager.onError(clientSocket, error)
                            clientSocket.closeClientSocket(true)
                            continue
                        }
                        error = clientSocket.setWriteTimeout()
                        if (error != null) {
                            mLocalSocketManager.onError(clientSocket, error)
                            clientSocket.closeClientSocket(true)
                            continue
                        }
                        // Start new thread for client logic and pass control to ILocalSocketManager implementation
                        mLocalSocketManager.onClientAccepted(clientSocket)
                    } catch (t: Throwable) {
                        mLocalSocketManager.onError(clientSocket, LocalSocketErrno.ERRNO_CLIENT_SOCKET_LISTENER_FAILED_WITH_EXCEPTION.getError(t, mLocalSocketRunConfig.getTitle(), t.message))
                        if (clientSocket != null)
                            clientSocket.closeClientSocket(true)
                    }
                }
            } catch (ignored: Exception) {
            } finally {
                try {
                    close()
                } catch (ignored: Exception) {
                }
            }
            Logger.logVerbose(LOG_TAG, "ClientSocketListener end")
        }
    }

    companion object {
        const val LOG_TAG = "LocalServerSocket"

        /**
         * The required permissions for server socket file parent directory.
         * Creation of a new socket will fail if the server starter app process does not have
         * write and search (execute) permission on the directory in which the socket is created.
         */
        // Default: "rwx"
        const val SERVER_SOCKET_PARENT_DIRECTORY_PERMISSIONS = "rwx"
    }
}
