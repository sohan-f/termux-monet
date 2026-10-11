package com.termux.shared.net.socket.local

import android.content.Context
import androidx.annotation.Keep
import androidx.annotation.NonNull
import com.termux.shared.android.ProcessUtils
import com.termux.shared.android.UserUtils
import com.termux.shared.logger.Logger
import com.termux.shared.markdown.MarkdownUtils

/**
 * The {@link PeerCred} of the {@link LocalClientSocket} containing info of client/peer.
 */
@Keep
class PeerCred internal constructor() {

    /**
     * Process Id.
     *
     * Set by native code via JNI field lookup: keep the name, type and
     * visibility exact (@JvmField).
     */
    @JvmField
    var pid: Int = -1

    /**
     * Process Name.
     *
     * Set by native code via JNI field lookup: keep the name, type and
     * visibility exact (@JvmField).
     */
    @JvmField
    var pname: String? = null

    /**
     * User Id.
     *
     * Set by native code via JNI field lookup: keep the name, type and
     * visibility exact (@JvmField).
     */
    @JvmField
    var uid: Int = -1

    /**
     * User name.
     */
    @JvmField
    var uname: String? = null

    /**
     * Group Id.
     *
     * Set by native code via JNI field lookup: keep the name, type and
     * visibility exact (@JvmField).
     */
    @JvmField
    var gid: Int = -1

    /**
     * Group name.
     */
    @JvmField
    var gname: String? = null

    /**
     * Command line that started the process.
     *
     * Set by native code via JNI field lookup: keep the name, type and
     * visibility exact (@JvmField).
     */
    @JvmField
    var cmdline: String? = null

    /**
     * Set data that was not set by JNI.
     */
    fun fillPeerCred(@NonNull context: Context) {
        fillUnameAndGname(context)
        fillPname(context)
    }

    /**
     * Set {@link #uname} and {@link #gname} if not set.
     */
    fun fillUnameAndGname(@NonNull context: Context) {
        uname = UserUtils.getNameForUid(context, uid)
        if (gid != uid)
            gname = UserUtils.getNameForUid(context, gid)
        else
            gname = uname
    }

    /**
     * Set {@link #pname} if not set.
     */
    fun fillPname(@NonNull context: Context) {
        // If jni did not set process name since it wouldn't be able to access /proc/<pid> of other
        // users/apps, then try to see if any app has that pid, but this wouldn't check child
        // processes of the app.
        if (pid > 0 && pname == null)
            pname = ProcessUtils.getAppProcessNameForPid(context, pid)
    }

    /**
     * Get a log {@link String} for the {@link PeerCred}.
     */
    @NonNull
    fun getLogString(): String {
        val logString = StringBuilder()
        logString.append("Peer Cred:")
        logString.append("\n").append(Logger.getSingleLineLogStringEntry("Process", getProcessString(), "-"))
        logString.append("\n").append(Logger.getSingleLineLogStringEntry("User", getUserString(), "-"))
        logString.append("\n").append(Logger.getSingleLineLogStringEntry("Group", getGroupString(), "-"))
        if (cmdline != null)
            logString.append("\n").append(Logger.getMultiLineLogStringEntry("Cmdline", cmdline, "-"))
        return logString.toString()
    }

    /**
     * Get a markdown {@link String} for the {@link PeerCred}.
     */
    @NonNull
    fun getMarkdownString(): String {
        val markdownString = StringBuilder()
        markdownString.append("## ").append("Peer Cred")
        markdownString.append("\n").append(MarkdownUtils.getSingleLineMarkdownStringEntry("Process", getProcessString(), "-"))
        markdownString.append("\n").append(MarkdownUtils.getSingleLineMarkdownStringEntry("User", getUserString(), "-"))
        markdownString.append("\n").append(MarkdownUtils.getSingleLineMarkdownStringEntry("Group", getGroupString(), "-"))
        if (cmdline != null)
            markdownString.append("\n").append(MarkdownUtils.getMultiLineMarkdownStringEntry("Cmdline", cmdline, "-"))
        return markdownString.toString()
    }

    @NonNull
    fun getMinimalString(): String {
        return "process=" + getProcessString() + ", user=" + getUserString() + ", group=" + getGroupString()
    }

    @NonNull
    fun getProcessString(): String {
        return if (!pname.isNullOrEmpty()) "$pid ($pname)" else pid.toString()
    }

    @NonNull
    fun getUserString(): String {
        return if (uname != null) "$uid ($uname)" else uid.toString()
    }

    @NonNull
    fun getGroupString(): String {
        return if (gname != null) "$gid ($gname)" else gid.toString()
    }

    companion object {
        const val LOG_TAG = "PeerCred"

        /**
         * Get a log {@link String} for {@link PeerCred}.
         *
         * @param peerCred The {@link PeerCred} to get info of.
         * @return Returns the log {@link String}.
         */
        @JvmStatic
        fun getPeerCredLogString(peerCred: PeerCred?): String {
            if (peerCred == null)
                return "null"
            return peerCred.getLogString()
        }

        /**
         * Get a markdown {@link String} for {@link PeerCred}.
         *
         * @param peerCred The {@link PeerCred} to get info of.
         * @return Returns the markdown {@link String}.
         */
        @JvmStatic
        fun getPeerCredMarkdownString(peerCred: PeerCred?): String {
            if (peerCred == null)
                return "null"
            return peerCred.getMarkdownString()
        }
    }
}
