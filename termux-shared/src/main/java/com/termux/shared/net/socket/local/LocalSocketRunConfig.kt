package com.termux.shared.net.socket.local

import androidx.annotation.NonNull
import com.termux.shared.file.FileUtils
import com.termux.shared.logger.Logger
import com.termux.shared.markdown.MarkdownUtils
import java.io.Serializable
import java.nio.charset.StandardCharsets

/**
 * Run config for {@link LocalSocketManager}.
 */
open class LocalSocketRunConfig : Serializable {

    /**
     * The {@link LocalSocketManager} title.
     */
    @JvmField
    protected val mTitle: String

    /**
     * The {@link LocalServerSocket} path.
     *
     * For a filesystem socket, this must be an absolute path to the socket file. Creation of a new
     * socket will fail if the server starter app process does not have write and search (execute)
     * permission on the directory in which the socket is created. The client process must have write
     * permission on the socket to connect to it. Other app will not be able to connect to socket
     * if its created in private app data directory.
     *
     * For an abstract namespace socket, the first byte must be a null `\0` character. Note that on
     * Android 9+, if server app is using `targetSdkVersion` `28`, then other apps will not be able
     * to connect to it due to selinux restrictions.
     * > Per-app SELinux domains
     * > Apps that target Android 9 or higher cannot share data with other apps using world-accessible
     * Unix permissions. This change improves the integrity of the Android Application Sandbox,
     * particularly the requirement that an app's private data is accessible only by that app.
     * https://developer.android.com/about/versions/pie/android-9.0-changes-28
     * https://github.com/android/ndk/issues/1469
     * https://stackoverflow.com/questions/63806516/avc-denied-connectto-when-using-uds-on-android-10
     *
     * Max allowed length is 108 bytes as per sun_path size (UNIX_PATH_MAX) on Linux.
     */
    @JvmField
    protected val mPath: String?

    /**
     * If abstract namespace {@link LocalServerSocket} instead of filesystem.
     */
    @JvmField
    protected val mAbstractNamespaceSocket: Boolean

    /**
     * The {@link ILocalSocketManager} client for the {@link LocalSocketManager}.
     */
    @JvmField
    protected val mLocalSocketManagerClient: ILocalSocketManager

    /**
     * The {@link LocalServerSocket} file descriptor.
     * Value will be `>= 0` if socket has been created successfully and `-1` if not created or closed.
     */
    @JvmField
    protected var mFD: Int = -1

    /**
     * The {@link LocalClientSocket} receiving (SO_RCVTIMEO) timeout in milliseconds.
     *
     * https://manpages.debian.org/testing/manpages/socket.7.en.html
     * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/services/core/java/com/android/server/am/NativeCrashListener.java;l=55
     * Defaults to {@link #DEFAULT_RECEIVE_TIMEOUT}.
     */
    @JvmField
    protected var mReceiveTimeout: Int? = null

    /**
     * The {@link LocalClientSocket} sending (SO_SNDTIMEO) timeout in milliseconds.
     *
     * https://manpages.debian.org/testing/manpages/socket.7.en.html
     * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/services/core/java/com/android/server/am/NativeCrashListener.java;l=55
     * Defaults to {@link #DEFAULT_SEND_TIMEOUT}.
     */
    @JvmField
    protected var mSendTimeout: Int? = null

    /**
     * The {@link LocalClientSocket} deadline in milliseconds. When the deadline has elapsed after
     * creation time of client socket, all reads and writes will error out. Set to 0, for no
     * deadline.
     * Defaults to {@link #DEFAULT_DEADLINE}.
     */
    @JvmField
    protected var mDeadline: Long? = null

    /**
     * The {@link LocalServerSocket} backlog for the maximum length to which the queue of pending connections
     * for the socket may grow. This value may be ignored or may not have one-to-one mapping
     * in kernel implementation. Value must be greater than 0.
     *
     * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/core/java/android/net/LocalSocketManager.java;l=31
     * Defaults to {@link #DEFAULT_BACKLOG}.
     */
    @JvmField
    protected var mBacklog: Int? = null

    /**
     * Create an new instance of {@link LocalSocketRunConfig}.
     *
     * @param title The {@link #mTitle} value.
     * @param path The {@link #mPath} value.
     * @param localSocketManagerClient The {@link #mLocalSocketManagerClient} value.
     */
    constructor(@NonNull title: String, @NonNull path: String, @NonNull localSocketManagerClient: ILocalSocketManager) {
        mTitle = title
        mLocalSocketManagerClient = localSocketManagerClient
        mAbstractNamespaceSocket = path.toByteArray(StandardCharsets.UTF_8)[0].toInt() == 0
        mPath = if (mAbstractNamespaceSocket)
            path
        else
            FileUtils.getCanonicalPath(path, null)
    }

    /**
     * Get {@link #mTitle}.
     */
    fun getTitle(): String {
        return mTitle
    }

    /**
     * Get log title that should be used for {@link LocalSocketManager}.
     */
    fun getLogTitle(): String {
        return Logger.getDefaultLogTag() + "." + mTitle
    }

    /**
     * Get {@link #mPath}.
     */
    fun getPath(): String? {
        return mPath
    }

    /**
     * Get {@link #mAbstractNamespaceSocket}.
     */
    fun isAbstractNamespaceSocket(): Boolean {
        return mAbstractNamespaceSocket
    }

    /**
     * Get {@link #mLocalSocketManagerClient}.
     */
    fun getLocalSocketManagerClient(): ILocalSocketManager {
        return mLocalSocketManagerClient
    }

    /**
     * Get {@link #mFD}.
     */
    fun getFD(): Int? {
        return mFD
    }

    /**
     * Set {@link #mFD}. Value must be greater than 0 or -1.
     */
    fun setFD(fd: Int) {
        mFD = if (fd >= 0)
            fd
        else
            -1
    }

    /**
     * Get {@link #mReceiveTimeout} if set, otherwise {@link #DEFAULT_RECEIVE_TIMEOUT}.
     */
    fun getReceiveTimeout(): Int? {
        return mReceiveTimeout ?: DEFAULT_RECEIVE_TIMEOUT
    }

    /**
     * Set {@link #mReceiveTimeout}.
     */
    fun setReceiveTimeout(receiveTimeout: Int?) {
        mReceiveTimeout = receiveTimeout
    }

    /**
     * Get {@link #mSendTimeout} if set, otherwise {@link #DEFAULT_SEND_TIMEOUT}.
     */
    fun getSendTimeout(): Int? {
        return mSendTimeout ?: DEFAULT_SEND_TIMEOUT
    }

    /**
     * Set {@link #mSendTimeout}.
     */
    fun setSendTimeout(sendTimeout: Int?) {
        mSendTimeout = sendTimeout
    }

    /**
     * Get {@link #mDeadline} if set, otherwise {@link #DEFAULT_DEADLINE}.
     */
    fun getDeadline(): Long? {
        return mDeadline ?: DEFAULT_DEADLINE.toLong()
    }

    /**
     * Set {@link #mDeadline}.
     */
    fun setDeadline(deadline: Long?) {
        mDeadline = deadline
    }

    /**
     * Get {@link #mBacklog} if set, otherwise {@link #DEFAULT_BACKLOG}.
     */
    fun getBacklog(): Int? {
        return mBacklog ?: DEFAULT_BACKLOG
    }

    /**
     * Set {@link #mBacklog}. Value must be greater than 0.
     */
    fun setBacklog(backlog: Int?) {
        // The Java version unboxed here and threw NPE for null; !! preserves that.
        if (backlog!! > 0)
            mBacklog = backlog
    }

    /**
     * Get a log {@link String} for the {@link LocalSocketRunConfig}.
     */
    @NonNull
    open fun getLogString(): String {
        val logString = StringBuilder()
        logString.append(mTitle).append(" Socket Server Run Config:")
        logString.append("\n").append(Logger.getSingleLineLogStringEntry("Path", mPath, "-"))
        logString.append("\n").append(Logger.getSingleLineLogStringEntry("AbstractNamespaceSocket", mAbstractNamespaceSocket, "-"))
        logString.append("\n").append(Logger.getSingleLineLogStringEntry("LocalSocketManagerClient", mLocalSocketManagerClient.javaClass.name, "-"))
        logString.append("\n").append(Logger.getSingleLineLogStringEntry("FD", mFD, "-"))
        logString.append("\n").append(Logger.getSingleLineLogStringEntry("ReceiveTimeout", getReceiveTimeout(), "-"))
        logString.append("\n").append(Logger.getSingleLineLogStringEntry("SendTimeout", getSendTimeout(), "-"))
        logString.append("\n").append(Logger.getSingleLineLogStringEntry("Deadline", getDeadline(), "-"))
        logString.append("\n").append(Logger.getSingleLineLogStringEntry("Backlog", getBacklog(), "-"))
        return logString.toString()
    }

    /**
     * Get a markdown {@link String} for the {@link LocalSocketRunConfig}.
     */
    @NonNull
    open fun getMarkdownString(): String {
        val markdownString = StringBuilder()
        markdownString.append("## ").append(mTitle).append(" Socket Server Run Config")
        markdownString.append("\n").append(MarkdownUtils.getSingleLineMarkdownStringEntry("Path", mPath, "-"))
        markdownString.append("\n").append(MarkdownUtils.getSingleLineMarkdownStringEntry("AbstractNamespaceSocket", mAbstractNamespaceSocket, "-"))
        markdownString.append("\n").append(MarkdownUtils.getSingleLineMarkdownStringEntry("LocalSocketManagerClient", mLocalSocketManagerClient.javaClass.name, "-"))
        markdownString.append("\n").append(MarkdownUtils.getSingleLineMarkdownStringEntry("FD", mFD, "-"))
        markdownString.append("\n").append(MarkdownUtils.getSingleLineMarkdownStringEntry("ReceiveTimeout", getReceiveTimeout(), "-"))
        markdownString.append("\n").append(MarkdownUtils.getSingleLineMarkdownStringEntry("SendTimeout", getSendTimeout(), "-"))
        markdownString.append("\n").append(MarkdownUtils.getSingleLineMarkdownStringEntry("Deadline", getDeadline(), "-"))
        markdownString.append("\n").append(MarkdownUtils.getSingleLineMarkdownStringEntry("Backlog", getBacklog(), "-"))
        return markdownString.toString()
    }

    @NonNull
    open override fun toString(): String {
        return getLogString()
    }

    companion object {
        const val DEFAULT_RECEIVE_TIMEOUT = 10000
        const val DEFAULT_SEND_TIMEOUT = 10000
        const val DEFAULT_DEADLINE = 0
        const val DEFAULT_BACKLOG = 50

        /**
         * Get a log {@link String} for {@link LocalSocketRunConfig}.
         *
         * @param config The {@link LocalSocketRunConfig} to get info of.
         * @return Returns the log {@link String}.
         */
        @NonNull
        @JvmStatic
        fun getRunConfigLogString(config: LocalSocketRunConfig?): String {
            if (config == null)
                return "null"
            return config.getLogString()
        }

        /**
         * Get a markdown {@link String} for {@link LocalSocketRunConfig}.
         *
         * @param config The {@link LocalSocketRunConfig} to get info of.
         * @return Returns the markdown {@link String}.
         */
        @JvmStatic
        fun getRunConfigMarkdownString(config: LocalSocketRunConfig?): String {
            if (config == null)
                return "null"
            return config.getMarkdownString()
        }
    }
}
