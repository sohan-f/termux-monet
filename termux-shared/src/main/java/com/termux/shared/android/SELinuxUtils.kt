package com.termux.shared.android

import android.annotation.SuppressLint
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.termux.shared.logger.Logger
import com.termux.shared.reflection.ReflectionUtils
import java.lang.reflect.Method

class SELinuxUtils {

    companion object {
        const val ANDROID_OS_SELINUX_CLASS = "android.os.SELinux"

        private const val LOG_TAG = "SELinuxUtils"

        /**
         * Gets the security context of the current process.
         *
         * @return Returns a {@link String} representing the security context of the current process.
         * This will be {@code null} if an exception is raised.
         */
        @Nullable
        @JvmStatic
        @SuppressLint("PrivateApi")
        fun getContext(): String? {
            ReflectionUtils.bypassHiddenAPIReflectionRestrictions()
            val methodName = "getContext"
            return try {
                val clazz = Class.forName(ANDROID_OS_SELINUX_CLASS)
                val method: Method? = ReflectionUtils.getDeclaredMethod(clazz, methodName)
                if (method == null) {
                    Logger.logError(LOG_TAG, "Failed to get " + methodName + "() method of " + ANDROID_OS_SELINUX_CLASS + " class")
                    return null
                }
                ReflectionUtils.invokeMethod(method, null).value as String?
            } catch (e: Exception) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to call " + methodName + "() method of " + ANDROID_OS_SELINUX_CLASS + " class", e)
                null
            }
        }

        /**
         * Get the security context of a given process id.
         *
         * @param pid The pid of process.
         * @return Returns a {@link String} representing the security context of the given pid.
         * This will be {@code null} if an exception is raised.
         */
        @Nullable
        @JvmStatic
        @SuppressLint("PrivateApi")
        fun getPidContext(pid: Int): String? {
            ReflectionUtils.bypassHiddenAPIReflectionRestrictions()
            val methodName = "getPidContext"
            return try {
                val clazz = Class.forName(ANDROID_OS_SELINUX_CLASS)
                val method: Method? = ReflectionUtils.getDeclaredMethod(clazz, methodName, java.lang.Integer.TYPE)
                if (method == null) {
                    Logger.logError(LOG_TAG, "Failed to get " + methodName + "() method of " + ANDROID_OS_SELINUX_CLASS + " class")
                    return null
                }
                ReflectionUtils.invokeMethod(method, null, pid).value as String?
            } catch (e: Exception) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to call " + methodName + "() method of " + ANDROID_OS_SELINUX_CLASS + " class", e)
                null
            }
        }

        /**
         * Get the security context of a file object.
         *
         * @param path The pathname of the file object.
         * @return Returns a {@link String} representing the security context of the file.
         * This will be {@code null} if an exception is raised.
         */
        @Nullable
        @JvmStatic
        @SuppressLint("PrivateApi")
        fun getFileContext(@NonNull path: String): String? {
            ReflectionUtils.bypassHiddenAPIReflectionRestrictions()
            val methodName = "getFileContext"
            return try {
                val clazz = Class.forName(ANDROID_OS_SELINUX_CLASS)
                val method: Method? = ReflectionUtils.getDeclaredMethod(clazz, methodName, String::class.java)
                if (method == null) {
                    Logger.logError(LOG_TAG, "Failed to get " + methodName + "() method of " + ANDROID_OS_SELINUX_CLASS + " class")
                    return null
                }
                ReflectionUtils.invokeMethod(method, null, path).value as String?
            } catch (e: Exception) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to call " + methodName + "() method of " + ANDROID_OS_SELINUX_CLASS + " class", e)
                null
            }
        }
    }
}
