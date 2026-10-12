package com.termux.shared.android

import android.content.Context
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.termux.shared.logger.Logger
import com.termux.shared.reflection.ReflectionUtils

class UserUtils {

    companion object {
        const val LOG_TAG = "UserUtils"

        /**
         * Get the user name for user id with a call to {@link #getNameForUidFromPackageManager(Context, int)}
         * and if that fails, then a call to {@link #getNameForUidFromLibcore(int)}.
         *
         * @param context The {@link Context} for operations.
         * @param uid The user id.
         * @return Returns the user name if found, otherwise {@code null}.
         */
        @Nullable
        @JvmStatic
        fun getNameForUid(@NonNull context: Context, uid: Int): String? {
            var name = getNameForUidFromPackageManager(context, uid)
            if (name == null)
                name = getNameForUidFromLibcore(uid)
            return name
        }

        /**
         * Get the user name for user id with a call to {@link android.content.pm.PackageManager#getNameForUid(int)}.
         *
         * This will not return user names for non app user id like for root user 0, use {@link #getNameForUidFromLibcore(int)}
         * to get those.
         *
         * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/core/java/android/content/pm/PackageManager.java;l=5556
         * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/core/java/android/app/ApplicationPackageManager.java;l=1028
         * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/services/core/java/com/android/server/pm/PackageManagerService.java;l=10293
         *
         * @param context The {@link Context} for operations.
         * @param uid The user id.
         * @return Returns the user name if found, otherwise {@code null}.
         */
        @Nullable
        @JvmStatic
        fun getNameForUidFromPackageManager(@NonNull context: Context, uid: Int): String? {
            if (uid < 0)
                return null
            return try {
                var name = context.packageManager.getNameForUid(uid)
                if (name != null && name.endsWith(":$uid"))
                    // Remove ":<uid>" suffix. The regex ":uid$" matches
                    // literally here (digits need no escaping), so a plain
                    // suffix removal is identical.
                    name = name.removeSuffix(":$uid")
                name
            } catch (e: Exception) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to get name for uid \"$uid\" from package manager", e)
                null
            }
        }

        /**
         * Get the user name for user id with a call to `Libcore.os.getpwuid()`.
         *
         * This will return user names for non app user id like for root user 0 as well, but this call
         * is expensive due to usage of reflection, and requires hidden API bypass, check
         * {@link ReflectionUtils#bypassHiddenAPIReflectionRestrictions()} for details.
         *
         * `BlockGuardOs` implements the `Os` interface and its instance is stored in `Libcore` class static `os` field.
         * The `getpwuid` method is implemented by `ForwardingOs`, which is the super class of `BlockGuardOs`.
         * The `getpwuid` method returns `StructPasswd` object whose `pw_name` contains the user name for id.
         *
         * https://stackoverflow.com/a/28057167/14686958
         * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:libcore/luni/src/main/java/libcore/io/Libcore.java;l=39
         * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:libcore/luni/src/main/java/libcore/io/Os.java;l=279
         * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:libcore/luni/src/main/java/libcore/io/BlockGuardOs.java
         * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:libcore/luni/src/main/java/libcore/io/ForwardingOs.java;l=340
         * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:libcore/luni/src/main/java/android/system/StructPasswd.java
         * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:bionic/libc/bionic/grp_pwd.cpp;l=553
         * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:system/core/libcutils/include/private/android_filesystem_config.h;l=43
         *
         * @param uid The user id.
         * @return Returns the user name if found, otherwise {@code null}.
         */
        @Nullable
        @JvmStatic
        fun getNameForUidFromLibcore(uid: Int): String? {
            if (uid < 0)
                return null
            ReflectionUtils.bypassHiddenAPIReflectionRestrictions()
            try {
                val libcoreClassName = "libcore.io.Libcore"
                val libcoreClass = Class.forName(libcoreClassName)
                // libcore.io.BlockGuardOs
                val os: Any? = try {
                    ReflectionUtils.invokeField(libcoreClass, "os", null).value
                } catch (e: Exception) {
                    // ClassCastException may be thrown
                    Logger.logStackTraceWithMessage(LOG_TAG, "Failed to get \"os\" field value for $libcoreClassName class", e)
                    return null
                }
                if (os == null) {
                    Logger.logError(LOG_TAG, "Failed to get BlockGuardOs class obj from Libcore")
                    return null
                }
                // libcore.io.ForwardingOs
                val forwardingOsClass = os.javaClass.superclass
                if (forwardingOsClass == null) {
                    Logger.logError(LOG_TAG, "Failed to find super class ForwardingOs from object of class " + os.javaClass.name)
                    return null
                }
                // android.system.StructPasswd
                val structPasswd: Any? = try {
                    val getpwuidMethod = ReflectionUtils.getDeclaredMethod(forwardingOsClass, "getpwuid", java.lang.Integer.TYPE)
                        ?: return null
                    ReflectionUtils.invokeMethod(getpwuidMethod, os, uid).value
                } catch (e: Exception) {
                    Logger.logStackTraceWithMessage(LOG_TAG, "Failed to invoke getpwuid() method of " + forwardingOsClass.name + " class", e)
                    return null
                }
                if (structPasswd == null) {
                    Logger.logError(LOG_TAG, "Failed to get StructPasswd obj from call to ForwardingOs.getpwuid()")
                    return null
                }
                return try {
                    ReflectionUtils.invokeField(structPasswd.javaClass, "pw_name", structPasswd).value as String?
                } catch (e: Exception) {
                    // ClassCastException may be thrown
                    Logger.logStackTraceWithMessage(LOG_TAG, "Failed to get \"pw_name\" field value for " + structPasswd.javaClass.name + " class", e)
                    null
                }
            } catch (e: Exception) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to get name for uid \"$uid\" from Libcore", e)
                return null
            }
        }
    }
}
