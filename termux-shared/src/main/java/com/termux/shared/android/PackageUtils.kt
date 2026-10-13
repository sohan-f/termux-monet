package com.termux.shared.android

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import com.termux.shared.R
import com.termux.shared.data.DataUtils
import com.termux.shared.interact.MessageDialogUtils
import com.termux.shared.logger.Logger
import com.termux.shared.reflection.ReflectionUtils
import java.security.MessageDigest

class PackageUtils {

    companion object {
        private const val LOG_TAG = "PackageUtils"

        /**
         * Get the {@link Context} for the package name with {@link Context#CONTEXT_RESTRICTED} flags.
         *
         * @param context The {@link Context} to use to get the {@link Context} of the {@code packageName}.
         * @param packageName The package name whose {@link Context} to get.
         * @return Returns the {@link Context}. This will {@code null} if an exception is raised.
         */
        @Nullable
        @JvmStatic
        fun getContextForPackage(@NonNull context: Context, packageName: String?): Context? {
            return getContextForPackage(context, packageName, Context.CONTEXT_RESTRICTED)
        }

        /**
         * Get the {@link Context} for the package name.
         *
         * @param context The {@link Context} to use to get the {@link Context} of the {@code packageName}.
         * @param packageName The package name whose {@link Context} to get.
         * @param flags The flags for {@link Context} type.
         * @return Returns the {@link Context}. This will {@code null} if an exception is raised.
         */
        @Nullable
        @JvmStatic
        fun getContextForPackage(@NonNull context: Context, packageName: String?, flags: Int): Context? {
            return try {
                context.createPackageContext(packageName, flags)
            } catch (e: Exception) {
                Logger.logVerbose(LOG_TAG, "Failed to get \"" + packageName + "\" package context with flags " + flags + ": " + e.message)
                null
            }
        }

        /**
         * Get the {@link Context} for a package name.
         *
         * @param context The {@link Context} to use to get the {@link Context} of the {@code packageName}.
         * @param packageName The package name whose {@link Context} to get.
         * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
         *                       be shown which when dismissed will exit the app.
         * @param helpUrl The help user to add to {@link R.string#error_get_package_context_failed_help_url_message}.
         * @return Returns the {@link Context}. This will {@code null} if an exception is raised.
         */
        @Nullable
        @JvmStatic
        fun getContextForPackageOrExitApp(@NonNull context: Context, packageName: String?, exitAppOnError: Boolean, @Nullable helpUrl: String?): Context? {
            val packageContext = getContextForPackage(context, packageName)
            if (packageContext == null && exitAppOnError) {
                var errorMessage = context.getString(R.string.error_get_package_context_failed_message, packageName)
                if (!DataUtils.isNullOrEmpty(helpUrl))
                    errorMessage += "\n" + context.getString(R.string.error_get_package_context_failed_help_url_message, helpUrl)
                Logger.logError(LOG_TAG, errorMessage)
                MessageDialogUtils.exitAppWithErrorMessage(context, context.getString(R.string.error_get_package_context_failed_title), errorMessage)
            }
            return packageContext
        }

        /**
         * Get the {@link PackageInfo} for the package associated with the {@code context}.
         *
         * @param context The {@link Context} for the package.
         * @return Returns the {@link PackageInfo}. This will be {@code null} if an exception is raised.
         */
        @JvmStatic
        fun getPackageInfoForPackage(@NonNull context: Context): PackageInfo? {
            return getPackageInfoForPackage(context, context.packageName)
        }

        /**
         * Get the {@link PackageInfo} for the package associated with the {@code context}.
         *
         * @param context The {@link Context} for the package.
         * @param flags The flags to pass to {@link PackageManager#getPackageInfo(String, int)}.
         * @return Returns the {@link PackageInfo}. This will be {@code null} if an exception is raised.
         */
        @Nullable
        @JvmStatic
        fun getPackageInfoForPackage(@NonNull context: Context, flags: Int): PackageInfo? {
            return getPackageInfoForPackage(context, context.packageName, flags)
        }

        /**
         * Get the {@link PackageInfo} for the package associated with the {@code packageName}.
         *
         * @param context The {@link Context} for operations.
         * @param packageName The package name of the package.
         * @return Returns the {@link PackageInfo}. This will be {@code null} if an exception is raised.
         */
        @JvmStatic
        fun getPackageInfoForPackage(@NonNull context: Context, packageName: String?): PackageInfo? {
            return getPackageInfoForPackage(context, packageName, 0)
        }

        /**
         * Get the {@link PackageInfo} for the package associated with the {@code packageName}.
         *
         * Also check {@link #isAppInstalled(Context, String, String) if targetting targeting sdk
         * `30` (android `11`) since {@link PackageManager.NameNotFoundException} may be thrown.
         *
         * @param context The {@link Context} for operations.
         * @param packageName The package name of the package.
         * @param flags The flags to pass to {@link PackageManager#getPackageInfo(String, int)}.
         * @return Returns the {@link PackageInfo}. This will be {@code null} if an exception is raised.
         */
        @Nullable
        @JvmStatic
        fun getPackageInfoForPackage(@NonNull context: Context, packageName: String?, flags: Int): PackageInfo? {
            return try {
                // Null names throw inside PackageManager like the Java version
                // instead of failing to compile against the non-null stub.
                context.packageManager.getPackageInfo(packageName!!, flags)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Get the {@link ApplicationInfo} for the {@code packageName}.
         *
         * @param context The {@link Context} for operations.
         * @param packageName The package name of the package.
         * @return Returns the {@link ApplicationInfo}. This will be {@code null} if an exception is raised.
         */
        @Nullable
        @JvmStatic
        fun getApplicationInfoForPackage(@NonNull context: Context, packageName: String?): ApplicationInfo? {
            return getApplicationInfoForPackage(context, packageName, 0)
        }

        /**
         * Get the {@link ApplicationInfo} for the {@code packageName}.
         *
         * Also check {@link #isAppInstalled(Context, String, String) if targetting targeting sdk
         * `30` (android `11`) since {@link PackageManager.NameNotFoundException} may be thrown.
         *
         * @param context The {@link Context} for operations.
         * @param packageName The package name of the package.
         * @param flags The flags to pass to {@link PackageManager#getApplicationInfo(String, int)}.
         * @return Returns the {@link ApplicationInfo}. This will be {@code null} if an exception is raised.
         */
        @Nullable
        @JvmStatic
        fun getApplicationInfoForPackage(@NonNull context: Context, packageName: String?, flags: Int): ApplicationInfo? {
            return try {
                // See getPackageInfoForPackage for the !! rationale.
                context.packageManager.getApplicationInfo(packageName!!, flags)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Get the {@code privateFlags} {@link Field} of the {@link ApplicationInfo} class.
         *
         * @param applicationInfo The {@link ApplicationInfo} for the package.
         * @return Returns the private flags or {@code null} if an exception was raised.
         */
        @Nullable
        @JvmStatic
        fun getApplicationInfoPrivateFlagsForPackage(@NonNull applicationInfo: ApplicationInfo): Int? {
            ReflectionUtils.bypassHiddenAPIReflectionRestrictions()
            return try {
                ReflectionUtils.invokeField(ApplicationInfo::class.java, "privateFlags", applicationInfo).value as Int?
            } catch (e: Exception) {
                // ClassCastException may be thrown
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to get privateFlags field value for ApplicationInfo class", e)
                null
            }
        }

        /**
         * Get the {@code seInfo} {@link Field} of the {@link ApplicationInfo} class.
         *
         * String retrieved from the seinfo tag found in selinux policy. This value can be set through
         * the mac_permissions.xml policy construct. This value is used for setting an SELinux security
         * context on the process as well as its data directory.
         *
         * https://cs.android.com/android/platform/superproject/+/android-7.1.0_r1:frameworks/base/core/java/android/content/pm/ApplicationInfo.java;l=609
         * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/core/java/android/content/pm/ApplicationInfo.java;l=981
         * https://cs.android.com/android/platform/superproject/+/android-7.0.0_r1:frameworks/base/services/core/java/android/content/pm/SELinuxMMAC.java;l=282
         * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/services/core/java/android/content/pm/SELinuxMMAC.java;l=375
         * https://cs.android.com/android/_/android/platform/frameworks/base/+/be0b8896d1bc385d4c8fb54c21929745935dcbea
         *
         * @param applicationInfo The {@link ApplicationInfo} for the package.
         * @return Returns the selinux info or {@code null} if an exception was raised.
         */
        @Nullable
        @JvmStatic
        fun getApplicationInfoSeInfoForPackage(@NonNull applicationInfo: ApplicationInfo): String? {
            ReflectionUtils.bypassHiddenAPIReflectionRestrictions()
            return try {
                ReflectionUtils.invokeField(ApplicationInfo::class.java, if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) "seinfo" else "seInfo", applicationInfo).value as String?
            } catch (e: Exception) {
                // ClassCastException may be thrown
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to get seInfo field value for ApplicationInfo class", e)
                null
            }
        }

        /**
         * Get the {@code seInfoUser} {@link Field} of the {@link ApplicationInfo} class.
         *
         * Also check {@link #getApplicationInfoSeInfoForPackage(ApplicationInfo)}.
         *
         * @param applicationInfo The {@link ApplicationInfo} for the package.
         * @return Returns the selinux info user or {@code null} if an exception was raised.
         */
        @Nullable
        @JvmStatic
        fun getApplicationInfoSeInfoUserForPackage(@NonNull applicationInfo: ApplicationInfo): String? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                return null
            ReflectionUtils.bypassHiddenAPIReflectionRestrictions()
            return try {
                ReflectionUtils.invokeField(ApplicationInfo::class.java, "seInfoUser", applicationInfo).value as String?
            } catch (e: Exception) {
                // ClassCastException may be thrown
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to get seInfoUser field value for ApplicationInfo class", e)
                null
            }
        }

        /**
         * Get the {@code privateFlags} {@link Field} of the {@link ApplicationInfo} class.
         *
         * @param fieldName The name of the field to get.
         * @return Returns the field value or {@code null} if an exception was raised.
         */
        @Nullable
        @JvmStatic
        fun getApplicationInfoStaticIntFieldValue(@NonNull fieldName: String): Int? {
            ReflectionUtils.bypassHiddenAPIReflectionRestrictions()
            return try {
                ReflectionUtils.invokeField(ApplicationInfo::class.java, fieldName, null).value as Int?
            } catch (e: Exception) {
                // ClassCastException may be thrown
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to get \"" + fieldName + "\" field value for ApplicationInfo class", e)
                null
            }
        }

        /**
         * Check if the app associated with the {@code applicationInfo} has a specific flag set.
         *
         * @param flagToCheckName The name of the field for the flag to check.
         * @param applicationInfo The {@link ApplicationInfo} for the package.
         * @return Returns {@code true} if app has flag is set, otherwise {@code false}. This will be
         * {@code null} if an exception is raised.
         */
        @Nullable
        @JvmStatic
        fun isApplicationInfoPrivateFlagSetForPackage(@NonNull flagToCheckName: String, @NonNull applicationInfo: ApplicationInfo): Boolean? {
            val privateFlags = getApplicationInfoPrivateFlagsForPackage(applicationInfo)
            if (privateFlags == null)
                return null
            val flagToCheck = getApplicationInfoStaticIntFieldValue(flagToCheckName)
            if (flagToCheck == null)
                return null
            return 0 != (privateFlags and flagToCheck)
        }

        /**
         * Get the app name for the package associated with the {@code context}.
         *
         * @param context The {@link Context} for the package.
         * @return Returns the {@code android:name} attribute.
         */
        @JvmStatic
        fun getAppNameForPackage(@NonNull context: Context): String {
            return getAppNameForPackage(context, context.applicationInfo)
        }

        /**
         * Get the app name for the package associated with the {@code applicationInfo}.
         *
         * @param context The {@link Context} for operations.
         * @param applicationInfo The {@link ApplicationInfo} for the package.
         * @return Returns the {@code android:name} attribute.
         */
        @JvmStatic
        fun getAppNameForPackage(@NonNull context: Context, @NonNull applicationInfo: ApplicationInfo): String {
            return applicationInfo.loadLabel(context.packageManager).toString()
        }

        /**
         * Get the package name for the package associated with the {@code context}.
         *
         * @param context The {@link Context} for the package.
         * @return Returns the package name.
         */
        @JvmStatic
        fun getPackageNameForPackage(@NonNull context: Context): String {
            return getPackageNameForPackage(context.applicationInfo)
        }

        /**
         * Get the package name for the package associated with the {@code applicationInfo}.
         *
         * @param applicationInfo The {@link ApplicationInfo} for the package.
         * @return Returns the package name.
         */
        @JvmStatic
        fun getPackageNameForPackage(@NonNull applicationInfo: ApplicationInfo): String {
            return applicationInfo.packageName
        }

        /**
         * Get the uid for the package associated with the {@code context}.
         *
         * @param context The {@link Context} for the package.
         * @return Returns the uid.
         */
        @JvmStatic
        fun getUidForPackage(@NonNull context: Context): Int {
            return getUidForPackage(context.applicationInfo)
        }

        /**
         * Get the uid for the package associated with the {@code applicationInfo}.
         *
         * @param applicationInfo The {@link ApplicationInfo} for the package.
         * @return Returns the uid.
         */
        @JvmStatic
        fun getUidForPackage(@NonNull applicationInfo: ApplicationInfo): Int {
            return applicationInfo.uid
        }

        /**
         * Get the {@code targetSdkVersion} for the package associated with the {@code context}.
         *
         * @param context The {@link Context} for the package.
         * @return Returns the {@code targetSdkVersion}.
         */
        @JvmStatic
        fun getTargetSDKForPackage(@NonNull context: Context): Int {
            return getTargetSDKForPackage(context.applicationInfo)
        }

        /**
         * Get the {@code targetSdkVersion} for the package associated with the {@code applicationInfo}.
         *
         * @param applicationInfo The {@link ApplicationInfo} for the package.
         * @return Returns the {@code targetSdkVersion}.
         */
        @JvmStatic
        fun getTargetSDKForPackage(@NonNull applicationInfo: ApplicationInfo): Int {
            return applicationInfo.targetSdkVersion
        }

        /**
         * Get the base apk path for the package associated with the {@code context}.
         *
         * @param context The {@link Context} for the package.
         * @return Returns the base apk path.
         */
        @JvmStatic
        fun getBaseAPKPathForPackage(@NonNull context: Context): String {
            return getBaseAPKPathForPackage(context.applicationInfo)
        }

        /**
         * Get the base apk path for the package associated with the {@code applicationInfo}.
         *
         * @param applicationInfo The {@link ApplicationInfo} for the package.
         * @return Returns the base apk path.
         */
        @JvmStatic
        fun getBaseAPKPathForPackage(@NonNull applicationInfo: ApplicationInfo): String {
            return applicationInfo.publicSourceDir
        }

        /**
         * Check if the app associated with the {@code context} has {@link ApplicationInfo#FLAG_DEBUGGABLE}
         * set.
         *
         * @param context The {@link Context} for the package.
         * @return Returns {@code true} if app is debuggable, otherwise {@code false}.
         */
        @JvmStatic
        fun isAppForPackageADebuggableBuild(@NonNull context: Context): Boolean {
            return isAppForPackageADebuggableBuild(context.applicationInfo)
        }

        /**
         * Check if the app associated with the {@code applicationInfo} has {@link ApplicationInfo#FLAG_DEBUGGABLE}
         * set.
         *
         * @param applicationInfo The {@link ApplicationInfo} for the package.
         * @return Returns {@code true} if app is debuggable, otherwise {@code false}.
         */
        @JvmStatic
        fun isAppForPackageADebuggableBuild(@NonNull applicationInfo: ApplicationInfo): Boolean {
            return 0 != (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)
        }

        /**
         * Check if the app associated with the {@code context} has {@link ApplicationInfo#FLAG_EXTERNAL_STORAGE}
         * set.
         *
         * @param context The {@link Context} for the package.
         * @return Returns {@code true} if app is installed on external storage, otherwise {@code false}.
         */
        @JvmStatic
        fun isAppInstalledOnExternalStorage(@NonNull context: Context): Boolean {
            return isAppInstalledOnExternalStorage(context.applicationInfo)
        }

        /**
         * Check if the app associated with the {@code applicationInfo} has {@link ApplicationInfo#FLAG_EXTERNAL_STORAGE}
         * set.
         *
         * @param applicationInfo The {@link ApplicationInfo} for the package.
         * @return Returns {@code true} if app is installed on external storage, otherwise {@code false}.
         */
        @JvmStatic
        fun isAppInstalledOnExternalStorage(@NonNull applicationInfo: ApplicationInfo): Boolean {
            return 0 != (applicationInfo.flags and ApplicationInfo.FLAG_EXTERNAL_STORAGE)
        }

        /**
         * Check if the app associated with the {@code context} has
         * ApplicationInfo.PRIVATE_FLAG_REQUEST_LEGACY_EXTERNAL_STORAGE (requestLegacyExternalStorage)
         * set to {@code true} in app manifest.
         *
         * @param context The {@link Context} for the package.
         * @return Returns {@code true} if app has requested legacy external storage, otherwise
         * {@code false}. This will be {@code null} if an exception is raised.
         */
        @Nullable
        @JvmStatic
        fun hasRequestedLegacyExternalStorage(@NonNull context: Context): Boolean? {
            return hasRequestedLegacyExternalStorage(context.applicationInfo)
        }

        /**
         * Check if the app associated with the {@code applicationInfo} has
         * ApplicationInfo.PRIVATE_FLAG_REQUEST_LEGACY_EXTERNAL_STORAGE (requestLegacyExternalStorage)
         * set to {@code true} in app manifest.
         *
         * @param applicationInfo The {@link ApplicationInfo} for the package.
         * @return Returns {@code true} if app has requested legacy external storage, otherwise
         * {@code false}. This will be {@code null} if an exception is raised.
         */
        @Nullable
        @JvmStatic
        fun hasRequestedLegacyExternalStorage(@NonNull applicationInfo: ApplicationInfo): Boolean? {
            return isApplicationInfoPrivateFlagSetForPackage("PRIVATE_FLAG_REQUEST_LEGACY_EXTERNAL_STORAGE", applicationInfo)
        }

        /**
         * Get the {@code versionCode} for the package associated with the {@code context}.
         *
         * @param context The {@link Context} for the package.
         * @return Returns the {@code versionCode}. This will be {@code null} if an exception is raised.
         */
        @Nullable
        @JvmStatic
        fun getVersionCodeForPackage(@NonNull context: Context): Int? {
            return getVersionCodeForPackage(context, context.packageName)
        }

        /**
         * Get the {@code versionCode} for the {@code packageName}.
         *
         * @param context The {@link Context} for operations.
         * @param packageName The package name of the package.
         * @return Returns the {@code versionCode}. This will be {@code null} if an exception is raised.
         */
        @Nullable
        @JvmStatic
        fun getVersionCodeForPackage(@NonNull context: Context, packageName: String?): Int? {
            return getVersionCodeForPackage(getPackageInfoForPackage(context, packageName))
        }

        /**
         * Get the {@code versionCode} for the {@code packageName}.
         *
         * @param packageInfo The {@link PackageInfo} for the package.
         * @return Returns the {@code versionCode}. This will be {@code null} if an exception is raised.
         */
        @Nullable
        @JvmStatic
        @Suppress("DEPRECATION") // versionCode is deprecated; kept for the pre-28 fallback branch
        fun getVersionCodeForPackage(@Nullable packageInfo: PackageInfo?): Int? {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return if (packageInfo != null) packageInfo.longVersionCode.toInt() else null
            } else {
                // backward compatibility for Android API < 28
                @Suppress("DEPRECATION")
                return if (packageInfo != null) packageInfo.versionCode else null
            }
        }

        /**
         * Get the {@code versionName} for the package associated with the {@code context}.
         *
         * @param context The {@link Context} for the package.
         * @return Returns the {@code versionName}. This will be {@code null} if an exception is raised.
         */
        @Nullable
        @JvmStatic
        fun getVersionNameForPackage(@NonNull context: Context): String? {
            return getVersionNameForPackage(context, context.packageName)
        }

        /**
         * Get the {@code versionName} for the {@code packageName}.
         *
         * @param context The {@link Context} for operations.
         * @param packageName The package name of the package.
         * @return Returns the {@code versionName}. This will be {@code null} if an exception is raised.
         */
        @Nullable
        @JvmStatic
        fun getVersionNameForPackage(@NonNull context: Context, packageName: String?): String? {
            return getVersionNameForPackage(getPackageInfoForPackage(context, packageName))
        }

        /**
         * Get the {@code versionName} for the {@code packageName}.
         *
         * @param packageInfo The {@link PackageInfo} for the package.
         * @return Returns the {@code versionName}. This will be {@code null} if an {@code packageInfo}
         * is {@code null}.
         */
        @Nullable
        @JvmStatic
        fun getVersionNameForPackage(@Nullable packageInfo: PackageInfo?): String? {
            return if (packageInfo != null) packageInfo.versionName else null
        }

        /**
         * Get the {@code SHA-256 digest} of signing certificate for the package associated with the {@code context}.
         *
         * @param context The {@link Context} for the package.
         * @return Returns the {@code SHA-256 digest}. This will be {@code null} if an exception is raised.
         */
        @Nullable
        @JvmStatic
        fun getSigningCertificateSHA256DigestForPackage(@NonNull context: Context): String? {
            return getSigningCertificateSHA256DigestForPackage(context, context.packageName)
        }

        /**
         * Get the {@code SHA-256 digest} of signing certificate for the {@code packageName}.
         *
         * @param context The {@link Context} for operations.
         * @param packageName The package name of the package.
         * @return Returns the {@code SHA-256 digest}. This will be {@code null} if an exception is raised.
         */
        @Nullable
        @JvmStatic
        @Suppress("DEPRECATION") // GET_SIGNATURES is deprecated; migration tracked by the Todo below
        fun getSigningCertificateSHA256DigestForPackage(@NonNull context: Context, packageName: String?): String? {
            return try {
                /*
                 * Todo: We may need AndroidManifest queries entries if package is installed but with a different signature on android 11
                 * https://developer.android.com/training/package-visibility
                 * Need a device that allows (manual) installation of apk with mismatched signature of
                 * sharedUserId apps to test. Currently, if its done, PackageManager just doesn't load
                 * the package and removes its apk automatically if its installed as a user app instead of system app
                 * W/PackageManager: Failed to parse /path/to/com.termux.tasker.apk: Signature mismatch for shared user: SharedUserSetting{xxxxxxx com.termux/10xxx}
                 */
                val packageInfo = getPackageInfoForPackage(context, packageName, PackageManager.GET_SIGNATURES)
                if (packageInfo == null)
                    null
                else {
                    // Null or empty arrays throw here like the Java version;
                    // the catch below converts them to a null return.
                    val signatures = packageInfo.signatures
                    if (signatures == null || signatures.isEmpty())
                        null
                    else
                        DataUtils.bytesToHex(MessageDigest.getInstance("SHA-256").digest(signatures[0].toByteArray()))
                }
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Get the serial number for the user for the package associated with the {@code context}.
         *
         * @param context The {@link Context} for the package.
         * @return Returns the serial number. This will be {@code null} if failed to get it.
         */
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Nullable
        @JvmStatic
        fun getUserIdForPackage(@NonNull context: Context): Long? {
            // A Java-style cast of a missing service returns null, but Kotlin
            // `as` would throw, so use a safe cast to preserve the early return.
            val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
            if (userManager == null)
                return null
            return userManager.getSerialNumberForUser(UserHandle.getUserHandleForUid(getUidForPackage(context)))
        }

        /**
         * Check if the current user is the primary user. This is done by checking if the the serial
         * number for the current user equals 0.
         *
         * @param context The {@link Context} for operations.
         * @return Returns {@code true} if the current user is the primary user, otherwise [@code false}.
         */
        @RequiresApi(api = Build.VERSION_CODES.N)
        @JvmStatic
        fun isCurrentUserThePrimaryUser(@NonNull context: Context): Boolean {
            val userId = getUserIdForPackage(context)
            // Boxed Long must compare against a Long literal: == 0 would use
            // equals() and always be false, unlike Java's unboxing compare.
            return userId != null && userId == 0L
        }

        /**
         * Get the profile owner package name for the current user.
         *
         * @param context The {@link Context} for operations.
         * @return Returns the profile owner package name. This will be {@code null} if failed to get it
         * or no profile owner for the current user.
         */
        @Nullable
        @JvmStatic
        fun getProfileOwnerPackageNameForUser(@NonNull context: Context): String? {
            // A Java-style cast of a missing service returns null, but Kotlin
            // `as` would throw, so use a safe cast to preserve the early return.
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            if (devicePolicyManager == null)
                return null
            val activeAdmins = devicePolicyManager.activeAdmins
            if (activeAdmins != null) {
                for (admin in activeAdmins) {
                    val packageName = admin.packageName
                    if (devicePolicyManager.isProfileOwnerApp(packageName))
                        return packageName
                }
            }
            return null
        }

        /**
         * Get the process id of the main app process of a package. This will work for sharedUserId. Note
         * that some apps have multiple processes for the app like with `android:process=":background"`
         * attribute in AndroidManifest.xml.
         *
         * @param context The {@link Context} for operations.
         * @param packageName The package name of the process.
         * @return Returns the process if found and running, otherwise {@code null}.
         */
        @Nullable
        @JvmStatic
        fun getPackagePID(context: Context, packageName: String?): String? {
            // A Java-style cast of a missing service returns null, but Kotlin
            // `as` would throw, so use a safe cast to preserve the early return.
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (activityManager != null) {
                val processInfos = activityManager.runningAppProcesses
                if (processInfos != null) {
                    for (processInfo in processInfos) {
                        if (processInfo.processName.equals(packageName))
                            return processInfo.pid.toString()
                    }
                }
            }
            return null
        }

        /**
         * Check if app is installed and enabled. This can be used by external apps that don't
         * share `sharedUserId` with the an app.
         *
         * If your third-party app is targeting sdk `30` (android `11`), then it needs to add package
         * name to the `queries` element or request `QUERY_ALL_PACKAGES` permission in its
         * `AndroidManifest.xml`. Otherwise it will get `PackageSetting{...... package_name/......} BLOCKED`
         * errors in `logcat` and  {@link PackageManager.NameNotFoundException} may be thrown.
         * `RUN_COMMAND` intent won't work either.
         * Check [package-visibility](https://developer.android.com/training/basics/intents/package-visibility#package-name),
         * `QUERY_ALL_PACKAGES` [googleplay policy](https://support.google.com/googleplay/android-developer/answer/10158779
         * and this [article](https://medium.com/androiddevelopers/working-with-package-visibility-dc252829de2d) for more info.
         *
         * {@code
         * <manifest
         *     <queries>
         *         <package android:name="com.termux" />
         *    </queries>
         *
         *    <application
         *        ....
         *    </application>
         * </manifest>
         * }
         *
         * @param context The context for operations.
         * @param appName The name of the app.
         * @param packageName The package name of the package.
         * @return Returns {@code errmsg} if {@code packageName} is not installed or disabled, otherwise {@code null}.
         */
        @JvmStatic
        fun isAppInstalled(@NonNull context: Context, appName: String?, packageName: String?): String? {
            val applicationInfo = getApplicationInfoForPackage(context, packageName)
            val isAppEnabled = applicationInfo != null && applicationInfo.enabled
            // If app is not installed or is disabled
            if (!isAppEnabled)
                return context.getString(R.string.error_app_not_installed_or_disabled_warning, appName, packageName)
            return null
        }

        /**
         * Wrapper for {@link #setComponentState(Context, String, String, boolean, String, boolean, boolean)} with
         * {@code alwaysShowToast} {@code true}.
         */
        @JvmStatic
        fun setComponentState(@NonNull context: Context, @NonNull packageName: String, @NonNull className: String, newState: Boolean, toastString: String?, showErrorMessage: Boolean): String? {
            return setComponentState(context, packageName, className, newState, toastString, showErrorMessage, true)
        }

        /**
         * Enable or disable a {@link ComponentName} with a call to
         * {@link PackageManager#setComponentEnabledSetting(ComponentName, int, int)}.
         *
         * @param context The {@link Context} for operations.
         * @param packageName The package name of the component.
         * @param className The {@link Class} name of the component.
         * @param newState If component should be enabled or disabled.
         * @param toastString If this is not {@code null} or empty, then a toast before setting state.
         * @param showErrorMessage If an error message toast should be shown.
         * @param alwaysShowToast If toast should always be shown even if current state matches new state.
         * @return Returns the errmsg if failed to set state, otherwise {@code null}.
         */
        @Nullable
        @JvmStatic
        fun setComponentState(@NonNull context: Context, @NonNull packageName: String, @NonNull className: String, newState: Boolean, toastString: String?, alwaysShowToast: Boolean, showErrorMessage: Boolean): String? {
            var toast = toastString
            return try {
                val packageManager = context.packageManager
                if (packageManager != null) {
                    if (toast != null && alwaysShowToast) {
                        Logger.showToast(context, toast, true)
                        toast = null
                    }
                    val currentlyDisabled = isComponentDisabled(context, packageName, className, false)
                    if (currentlyDisabled == null)
                        throw UnsupportedOperationException("Failed to find if component currently disabled")
                    val setState: Boolean? = if (newState && currentlyDisabled) true else if (!newState && !currentlyDisabled) false else null
                    if (setState == null)
                        return null
                    if (toast != null)
                        Logger.showToast(context, toast, true)
                    val componentName = ComponentName(packageName, className)
                    packageManager.setComponentEnabledSetting(componentName, if (setState) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                }
                null
            } catch (e: Exception) {
                val errmsg = context.getString(if (newState) R.string.error_enable_component_failed else R.string.error_disable_component_failed, packageName, className) + ": " + e.message
                if (showErrorMessage)
                    Logger.showToast(context, errmsg, true)
                errmsg
            }
        }

        /**
         * Check if state of a {@link ComponentName} is {@link PackageManager#COMPONENT_ENABLED_STATE_DISABLED}
         * with a call to {@link PackageManager#getComponentEnabledSetting(ComponentName)}.
         *
         * @param context The {@link Context} for operations.
         * @param packageName The package name of the component.
         * @param className The {@link Class} name of the component.
         * @param logErrorMessage If an error message should be logged.
         * @return Returns {@code true} if disabled, {@code false} if not and {@code null} if failed to
         * get the state.
         */
        @JvmStatic
        fun isComponentDisabled(@NonNull context: Context, @NonNull packageName: String, @NonNull className: String, logErrorMessage: Boolean): Boolean? {
            return try {
                val packageManager = context.packageManager
                if (packageManager != null) {
                    val componentName = ComponentName(packageName, className)
                    // Will throw IllegalArgumentException: Unknown component: ComponentInfo{} if app
                    // for context is not installed or component does not exist.
                    packageManager.getComponentEnabledSetting(componentName) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                } else
                    null
            } catch (e: Exception) {
                if (logErrorMessage)
                    Logger.logStackTraceWithMessage(LOG_TAG, context.getString(R.string.error_get_component_state_failed, packageName, className), e)
                null
            }
        }

        /**
         * Check if an {@link android.app.Activity} {@link ComponentName} can be called by calling
         * {@link PackageManager#queryIntentActivities(Intent, int)}.
         *
         * @param context The {@link Context} for operations.
         * @param packageName The package name of the component.
         * @param className The {@link Class} name of the component.
         * @param flags The flags to filter results.
         * @return Returns {@code true} if it exists, otherwise {@code false}.
         */
        @JvmStatic
        fun doesActivityComponentExist(@NonNull context: Context, @NonNull packageName: String, @NonNull className: String, flags: Int): Boolean {
            return try {
                val packageManager = context.packageManager
                if (packageManager != null) {
                    val intent = Intent()
                    intent.setClassName(packageName, className)
                    packageManager.queryIntentActivities(intent, flags).isNotEmpty()
                } else
                    false
            } catch (e: Exception) {
                // ignore
                false
            }
        }
    }
}
