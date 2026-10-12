package com.termux.shared.android

import android.app.ActivityManager
import android.content.Context
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.termux.shared.logger.Logger

class ProcessUtils {

    companion object {
        const val LOG_TAG = "ProcessUtils"

        /**
         * Get the app process name for a pid with a call to {@link ActivityManager#getRunningAppProcesses()}.
         *
         * This will not return child process names. Android did not keep track of them before android 12
         * phantom process addition, but there is no API via IActivityManager to get them.
         *
         * To get process name for pids of own app's child processes, check `get_process_name_from_cmdline()`
         * in `local-socket.cpp`.
         *
         * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/core/java/android/app/ActivityManager.java;l=3362
         * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/services/core/java/android/server/am/ActivityManagerService.java;l=8434
         * https://cs.android.com/android/_/android/platform/frameworks/base/+/refs/tags/android-12.0.0_r32:services/core/java/android/server/am/PhantomProcessList.java
         * https://cs.android.com/android/_/android/platform/frameworks/base/+/refs/tags/android-12.0.0_r32:services/core/java/android/server/am/PhantomProcessRecord.java
         *
         * @param context The {@link Context} for operations.
         * @param pid The pid of the process.
         * @return Returns the app process name if found, otherwise {@code null}.
         */
        @Nullable
        @JvmStatic
        fun getAppProcessNameForPid(@NonNull context: Context, pid: Int): String? {
            if (pid < 0)
                return null
            // A Java-style cast of a missing service returns null, but Kotlin
            // `as` would throw, so use a safe cast to preserve the early return.
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (activityManager == null)
                return null
            return try {
                val runningApps = activityManager.runningAppProcesses
                if (runningApps == null) {
                    null
                } else {
                    var processName: String? = null
                    for (procInfo in runningApps) {
                        if (procInfo.pid == pid) {
                            processName = procInfo.processName
                            break
                        }
                    }
                    processName
                }
            } catch (e: Exception) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to get app process name for pid $pid", e)
                null
            }
        }
    }
}
