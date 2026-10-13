package com.termux.shared.termux.shell.command.environment

import android.content.Context
import android.content.pm.PackageInfo
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.termux.shared.android.PackageUtils
import com.termux.shared.shell.command.environment.ShellEnvironmentUtils
import com.termux.shared.termux.TermuxConstants
import com.termux.shared.termux.TermuxUtils
import java.util.HashMap

/**
 * Environment for {@link TermuxConstants#TERMUX_API_PACKAGE_NAME} app.
 */
class TermuxAPIShellEnvironment {

    companion object {
        /**
         * Environment variable prefix for the Termux:API app.
         */
        @JvmField
        val TERMUX_API_APP_ENV_PREFIX: String = TermuxConstants.TERMUX_ENV_PREFIX_ROOT + "_API_APP__"

        /**
         * Environment variable for the Termux:API app version.
         */
        @JvmField
        val ENV_TERMUX_API_APP__VERSION_NAME: String = TERMUX_API_APP_ENV_PREFIX + "VERSION_NAME"

        /**
         * Get shell environment for Termux:API app.
         */
        @Nullable
        @JvmStatic
        fun getEnvironment(@NonNull currentPackageContext: Context): HashMap<String, String>? {
            // isTermuxAPIAppInstalled returns an errmsg if not installed,
            // null if installed, so non-null means "not installed".
            if (TermuxUtils.isTermuxAPIAppInstalled(currentPackageContext) != null)
                return null
            val packageName = TermuxConstants.TERMUX_API_PACKAGE_NAME
            val packageInfo: PackageInfo? = PackageUtils.getPackageInfoForPackage(currentPackageContext, packageName)
            if (packageInfo == null)
                return null
            val environment = HashMap<String, String>()
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_API_APP__VERSION_NAME, PackageUtils.getVersionNameForPackage(packageInfo))
            return environment
        }
    }
}
