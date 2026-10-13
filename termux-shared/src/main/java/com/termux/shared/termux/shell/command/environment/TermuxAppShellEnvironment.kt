package com.termux.shared.termux.shell.command.environment

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.termux.shared.android.PackageUtils
import com.termux.shared.android.SELinuxUtils
import com.termux.shared.data.DataUtils
import com.termux.shared.shell.command.environment.ShellEnvironmentUtils
import com.termux.shared.termux.TermuxBootstrap
import com.termux.shared.termux.TermuxConstants
import com.termux.shared.termux.TermuxUtils
import com.termux.shared.termux.shell.am.TermuxAmSocketServer
import java.util.HashMap

/**
 * Environment for {@link TermuxConstants#TERMUX_PACKAGE_NAME} app.
 */
class TermuxAppShellEnvironment {

    companion object {
        /**
         * Termux app environment variables.
         */
        @JvmField
        var termuxAppEnvironment: HashMap<String, String>? = null

        /** Environment variable root scope. */
        const val TERMUX_ENV__S_ROOT = "TERMUX_" // Default: "TERMUX_"

        /** Environment variable scope for Termux. */
        // Base is const, so this stays const as well.
        const val TERMUX_ENV__S_TERMUX = TERMUX_ENV__S_ROOT + "_" // Default: "TERMUX__"

        /** Environment variable for the Termux app version. */
        // TermuxConstants is still Java, so its fields are not compile-time
        // constants for const val; @JvmField keeps the identical descriptor.
        @JvmField
        val ENV_TERMUX_VERSION: String = TermuxConstants.TERMUX_ENV_PREFIX_ROOT + "_VERSION"

        /**
         * Environment variable prefix for the Termux app.
         */
        @JvmField
        val TERMUX_APP_ENV_PREFIX: String = TermuxConstants.TERMUX_ENV_PREFIX_ROOT + "_APP__"

        /** Environment variable for the Termux app version name. */
        @JvmField
        val ENV_TERMUX_APP__APP_VERSION_NAME: String = TERMUX_APP_ENV_PREFIX + "APP_VERSION_NAME"

        /** Environment variable for the Termux app version code. */
        @JvmField
        val ENV_TERMUX_APP__APP_VERSION_CODE: String = TERMUX_APP_ENV_PREFIX + "APP_VERSION_CODE"

        /** Environment variable for the Termux app package name. */
        @JvmField
        val ENV_TERMUX_APP__PACKAGE_NAME: String = TERMUX_APP_ENV_PREFIX + "PACKAGE_NAME"

        /**
         * Environment variable for the Termux app process id.
         */
        @JvmField
        val ENV_TERMUX_APP__PID: String = TERMUX_APP_ENV_PREFIX + "PID"

        /** Environment variable for the Termux app uid. */
        @JvmField
        val ENV_TERMUX__UID: String = TERMUX_ENV__S_TERMUX + "UID"

        /** Environment variable for the Termux app targetSdkVersion. */
        @JvmField
        val ENV_TERMUX_APP__TARGET_SDK: String = TERMUX_APP_ENV_PREFIX + "TARGET_SDK"

        /**
         * Environment variable for the Termux app is debuggable apk build.
         */
        @JvmField
        val ENV_TERMUX_APP__IS_DEBUGGABLE_BUILD: String = TERMUX_APP_ENV_PREFIX + "IS_DEBUGGABLE_BUILD"

        /**
         * Environment variable for the Termux app {@link TermuxConstants} APK_RELEASE_*.
         */
        @JvmField
        val ENV_TERMUX_APP__APK_RELEASE: String = TERMUX_APP_ENV_PREFIX + "APK_RELEASE"

        /** Environment variable for the Termux app install path. */
        @JvmField
        val ENV_TERMUX_APP__APK_FILE: String = TERMUX_APP_ENV_PREFIX + "APK_FILE"

        /** Environment variable for the Termux app is installed on external/portable storage. */
        @JvmField
        val ENV_TERMUX_APP__IS_INSTALLED_ON_EXTERNAL_STORAGE: String = TERMUX_APP_ENV_PREFIX + "IS_INSTALLED_ON_EXTERNAL_STORAGE"

        /** Environment variable for the Termux app process selinux context. */
        @JvmField
        val ENV_TERMUX__SE_PROCESS_CONTEXT: String = TERMUX_ENV__S_TERMUX + "SE_PROCESS_CONTEXT"

        /** Environment variable for the Termux app data files selinux context. */
        @JvmField
        val ENV_TERMUX__SE_FILE_CONTEXT: String = TERMUX_ENV__S_TERMUX + "SE_FILE_CONTEXT"

        /** Environment variable for the Termux app seInfo tag found in selinux policy used to set app process and app data files selinux context. */
        @JvmField
        val ENV_TERMUX__SE_INFO: String = TERMUX_ENV__S_TERMUX + "SE_INFO"

        /** Environment variable for the Termux app user id. */
        @JvmField
        val ENV_TERMUX__USER_ID: String = TERMUX_ENV__S_TERMUX + "USER_ID"

        /** Environment variable for the Termux app profile owner. */
        @JvmField
        val ENV_TERMUX__PROFILE_OWNER: String = TERMUX_ENV__S_TERMUX + "PROFILE_OWNER"

        /**
         * Environment variable for the Termux app {@link TermuxBootstrap#TERMUX_APP_PACKAGE_MANAGER}.
         */
        @JvmField
        val ENV_TERMUX_APP__PACKAGE_MANAGER: String = TERMUX_APP_ENV_PREFIX + "PACKAGE_MANAGER"

        /**
         * Environment variable for the Termux app {@link TermuxBootstrap#TERMUX_APP_PACKAGE_VARIANT}.
         */
        @JvmField
        val ENV_TERMUX_APP__PACKAGE_VARIANT: String = TERMUX_APP_ENV_PREFIX + "PACKAGE_VARIANT"

        /** Environment variable for the Termux app files directory. */
        @JvmField
        val ENV_TERMUX_APP__DATA_DIR: String = TERMUX_APP_ENV_PREFIX + "DATA_DIR"

        /**
         * Environment variable for the Termux app {@link TermuxAmSocketServer#getTermuxAppAMSocketServerEnabled(Context)}.
         */
        @JvmField
        val ENV_TERMUX_APP__AM_SOCKET_SERVER_ENABLED: String = TERMUX_APP_ENV_PREFIX + "AM_SOCKET_SERVER_ENABLED"

        /**
         * Get shell environment for Termux app.
         */
        @Nullable
        @JvmStatic
        fun getEnvironment(@NonNull currentPackageContext: Context): HashMap<String, String>? {
            setTermuxAppEnvironment(currentPackageContext)
            return termuxAppEnvironment
        }

        /**
         * Set Termux app environment variables in {@link #termuxAppEnvironment}.
         */
        @JvmStatic
        @Synchronized
        fun setTermuxAppEnvironment(@NonNull currentPackageContext: Context) {
            val isTermuxApp = TermuxConstants.TERMUX_PACKAGE_NAME == currentPackageContext.packageName
            // If current package context is of termux app and its environment is already set, then no need to set again since it won't change
            // Other apps should always set environment again since termux app may be installed/updated/deleted in background
            if (termuxAppEnvironment != null && isTermuxApp)
                return
            termuxAppEnvironment = null
            val packageName = TermuxConstants.TERMUX_PACKAGE_NAME
            val packageInfo: PackageInfo? = PackageUtils.getPackageInfoForPackage(currentPackageContext, packageName)
            if (packageInfo == null)
                return
            val applicationInfo: ApplicationInfo? = PackageUtils.getApplicationInfoForPackage(currentPackageContext, packageName)
            if (applicationInfo == null || !applicationInfo.enabled)
                return
            val environment = HashMap<String, String>()
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_VERSION, PackageUtils.getVersionNameForPackage(packageInfo))
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__APP_VERSION_NAME, PackageUtils.getVersionNameForPackage(packageInfo))
            // String template renders null as "null", exactly like the Java String.valueOf.
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__APP_VERSION_CODE, "${PackageUtils.getVersionCodeForPackage(packageInfo)}")

            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__PACKAGE_NAME, packageName)
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__PID, TermuxUtils.getTermuxAppPID(currentPackageContext))
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX__UID, PackageUtils.getUidForPackage(applicationInfo).toString())
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__TARGET_SDK, PackageUtils.getTargetSDKForPackage(applicationInfo).toString())
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__IS_DEBUGGABLE_BUILD, PackageUtils.isAppForPackageADebuggableBuild(applicationInfo))
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__APK_FILE, PackageUtils.getBaseAPKPathForPackage(applicationInfo))
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__IS_INSTALLED_ON_EXTERNAL_STORAGE, PackageUtils.isAppInstalledOnExternalStorage(applicationInfo))
            putTermuxAPKSignature(currentPackageContext, environment)
            val termuxPackageContext: Context? = TermuxUtils.getTermuxPackageContext(currentPackageContext)
            if (termuxPackageContext != null) {
                /*
                // An app that does not have the same sharedUserId as termux app will not be able to get
                // get termux context's classloader to get BuildConfig.TERMUX_PACKAGE_VARIANT via reflection.
                // Check TermuxBootstrap.setTermuxPackageManagerAndVariantFromTermuxApp()
                if (TermuxBootstrap.TERMUX_APP_PACKAGE_MANAGER != null)
                    environment.put(ENV_TERMUX_APP__PACKAGE_MANAGER, TermuxBootstrap.TERMUX_APP_PACKAGE_MANAGER.getName());
                if (TermuxBootstrap.TERMUX_APP_PACKAGE_VARIANT != null)
                    environment.put(ENV_TERMUX_APP__PACKAGE_VARIANT, TermuxBootstrap.TERMUX_APP_PACKAGE_VARIANT.getName());
                 */

                /*
                // Will not be set for plugins
                ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__AM_SOCKET_SERVER_ENABLED,
                    TermuxAmSocketServer.getTermuxAppAMSocketServerEnabled(currentPackageContext));
                 */

                ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__DATA_DIR, applicationInfo.dataDir)

                ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX__SE_PROCESS_CONTEXT, SELinuxUtils.getContext())
                ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX__SE_FILE_CONTEXT, SELinuxUtils.getFileContext(applicationInfo.dataDir))

                val seInfoUser = PackageUtils.getApplicationInfoSeInfoUserForPackage(applicationInfo)
                ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX__SE_INFO, PackageUtils.getApplicationInfoSeInfoForPackage(applicationInfo) +
                    if (DataUtils.isNullOrEmpty(seInfoUser)) "" else seInfoUser)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX__USER_ID, "${PackageUtils.getUserIdForPackage(currentPackageContext)}")
                ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX__PROFILE_OWNER, PackageUtils.getProfileOwnerPackageNameForUser(currentPackageContext))
            }
            termuxAppEnvironment = environment
        }

        /**
         * Put {@link #ENV_TERMUX_APP__APK_RELEASE} in {@code environment}.
         */
        @JvmStatic
        fun putTermuxAPKSignature(@NonNull currentPackageContext: Context, @NonNull environment: HashMap<String, String>) {
            val signingCertificateSHA256Digest = PackageUtils.getSigningCertificateSHA256DigestForPackage(currentPackageContext, TermuxConstants.TERMUX_PACKAGE_NAME)
            if (signingCertificateSHA256Digest != null) {
                // uppercase() keeps the default-locale behaviour of toUpperCase().
                ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__APK_RELEASE, TermuxUtils.getAPKRelease(signingCertificateSHA256Digest).replace("[^a-zA-Z]".toRegex(), "_").uppercase())
            }
        }

        /**
         * Update {@link #ENV_TERMUX_APP__AM_SOCKET_SERVER_ENABLED} value in {@code environment}.
         */
        @JvmStatic
        @Synchronized
        fun updateTermuxAppAMSocketServerEnabled(@NonNull currentPackageContext: Context) {
            val environment = termuxAppEnvironment ?: return
            environment.remove(ENV_TERMUX_APP__AM_SOCKET_SERVER_ENABLED)
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__AM_SOCKET_SERVER_ENABLED, TermuxAmSocketServer.getTermuxAppAMSocketServerEnabled(currentPackageContext))
        }
    }
}
