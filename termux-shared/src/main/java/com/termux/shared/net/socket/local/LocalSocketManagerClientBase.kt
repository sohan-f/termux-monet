package com.termux.shared.net.socket.local

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.termux.shared.errors.Error
import com.termux.shared.logger.Logger

/**
 * Base helper implementation for {@link ILocalSocketManager}.
 */
abstract class LocalSocketManagerClientBase : ILocalSocketManager {

    @Nullable
    override fun getLocalSocketManagerClientThreadUEH(@NonNull localSocketManager: LocalSocketManager): Thread.UncaughtExceptionHandler? {
        return null
    }

    override fun onError(@NonNull localSocketManager: LocalSocketManager, @Nullable clientSocket: LocalClientSocket?, @NonNull error: Error) {
        // Only log if log level is debug or higher since PeerCred.cmdline may contain private info
        Logger.logErrorPrivate(getLogTag(), "onError")
        Logger.logErrorPrivateExtended(getLogTag(), LocalSocketManager.getErrorLogString(error, localSocketManager.getLocalSocketRunConfig(), clientSocket))
    }

    override fun onDisallowedClientConnected(@NonNull localSocketManager: LocalSocketManager, @NonNull clientSocket: LocalClientSocket, @NonNull error: Error) {
        Logger.logWarn(getLogTag(), "onDisallowedClientConnected")
        Logger.logWarnExtended(getLogTag(), LocalSocketManager.getErrorLogString(error, localSocketManager.getLocalSocketRunConfig(), clientSocket))
    }

    override fun onClientAccepted(@NonNull localSocketManager: LocalSocketManager, @NonNull clientSocket: LocalClientSocket) {
        // Just close socket and let child class handle any required communication
        clientSocket.closeClientSocket(true)
    }

    protected abstract fun getLogTag(): String
}
