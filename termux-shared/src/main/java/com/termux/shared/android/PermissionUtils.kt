package com.termux.shared.android

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.common.base.Joiner
import com.termux.shared.R
import com.termux.shared.activity.ActivityUtils
import com.termux.shared.errors.Error
import com.termux.shared.errors.FunctionErrno
import com.termux.shared.file.FileUtils
import com.termux.shared.logger.Logger
import java.util.ArrayList
import java.util.Arrays

class PermissionUtils {

    companion object {
        const val REQUEST_GRANT_STORAGE_PERMISSION = 1000

        const val REQUEST_DISABLE_BATTERY_OPTIMIZATIONS = 2000

        const val REQUEST_GRANT_DISPLAY_OVER_OTHER_APPS_PERMISSION = 2001

        private const val LOG_TAG = "PermissionUtils"

        /**
         * Check if app has been granted the required permission.
         *
         * @param context The context for operations.
         * @param permission The {@link String} name for permission to check.
         * @return Returns {@code true} if permission is granted, otherwise {@code false}.
         */
        @JvmStatic
        fun checkPermission(@NonNull context: Context, @NonNull permission: String): Boolean {
            return checkPermissions(context, arrayOf(permission))
        }

        /**
         * Check if app has been granted the required permissions.
         *
         * @param context The context for operations.
         * @param permissions The {@link String[]} names for permissions to check.
         * @return Returns {@code true} if permissions are granted, otherwise {@code false}.
         */
        @JvmStatic
        fun checkPermissions(@NonNull context: Context, @NonNull permissions: Array<String>): Boolean {
            // checkSelfPermission may return true for permissions not even requested
            val permissionsNotRequested = getPermissionsNotRequested(context, permissions)
            if (permissionsNotRequested.isNotEmpty()) {
                Logger.logError(LOG_TAG, context.getString(R.string.error_attempted_to_check_for_permissions_not_requested, Joiner.on(", ").join(permissionsNotRequested)))
                return false
            }
            var result: Int
            for (permission in permissions) {
                result = ContextCompat.checkSelfPermission(context, permission)
                if (result != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
            return true
        }

        /**
         * Request user to grant required permissions to the app.
         *
         * @param context The context for operations. It must be an instance of {@link Activity} or
         * {@link AppCompatActivity}.
         * @param permission The {@link String} name for permission to request.
         * @param requestCode The request code to use while asking for permission. It must be `>=0` or
         *                    will fail silently and will log an exception.
         * @return Returns {@code true} if requesting the permission was successful, otherwise {@code false}.
         */
        @RequiresApi(api = Build.VERSION_CODES.M)
        @JvmStatic
        fun requestPermission(@NonNull context: Context, @NonNull permission: String, requestCode: Int): Boolean {
            return requestPermissions(context, arrayOf(permission), requestCode)
        }

        /**
         * Request user to grant required permissions to the app.
         *
         * On sdk 30 (android 11), Activity.onRequestPermissionsResult() will pass
         * {@link PackageManager#PERMISSION_DENIED} (-1) without asking the user for the permission
         * if user previously denied the permission prompt. On sdk 29 (android 10),
         * Activity.onRequestPermissionsResult() will pass {@link PackageManager#PERMISSION_DENIED} (-1)
         * without asking the user for the permission if user previously selected "Deny & don't ask again"
         * option in prompt. The user will have to manually enable permission in app info in Android
         * settings. If user grants and then denies in settings, then next time prompt will shown.
         *
         * @param context The context for operations. It must be an instance of {@link Activity} or
         * {@link AppCompatActivity}.
         * @param permissions The {@link String[]} names for permissions to request.
         * @param requestCode The request code to use while asking for permissions. It must be `>=0` or
         *                    will fail silently and will log an exception.
         * @return Returns {@code true} if requesting the permissions was successful, otherwise {@code false}.
         */
        @RequiresApi(api = Build.VERSION_CODES.M)
        @JvmStatic
        fun requestPermissions(@NonNull context: Context, @NonNull permissions: Array<String>, requestCode: Int): Boolean {
            val permissionsNotRequested = getPermissionsNotRequested(context, permissions)
            if (permissionsNotRequested.isNotEmpty()) {
                Logger.logErrorAndShowToast(context, LOG_TAG, context.getString(R.string.error_attempted_to_ask_for_permissions_not_requested, Joiner.on(", ").join(permissionsNotRequested)))
                return false
            }
            for (permission in permissions) {
                val result = ContextCompat.checkSelfPermission(context, permission)
                // If at least one permission not granted
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Logger.logInfo(LOG_TAG, "Requesting Permissions: " + Arrays.toString(permissions))
                    try {
                        if (context is AppCompatActivity)
                            context.requestPermissions(permissions, requestCode)
                        else if (context is Activity)
                            context.requestPermissions(permissions, requestCode)
                        else {
                            Error.logErrorAndShowToast(context, LOG_TAG, FunctionErrno.ERRNO_PARAMETER_NOT_INSTANCE_OF.getError("context", "requestPermissions", "Activity or AppCompatActivity"))
                            return false
                        }
                    } catch (e: Exception) {
                        val errmsg = context.getString(R.string.error_failed_to_request_permissions, requestCode, Arrays.toString(permissions))
                        Logger.logStackTraceWithMessage(LOG_TAG, errmsg, e)
                        Logger.showToast(context, errmsg + "\n" + e.message, true)
                        return false
                    }
                    break
                }
            }
            return true
        }

        /**
         * Check if app has requested the required permission in the manifest.
         *
         * @param context The context for operations.
         * @param permission The {@link String} name for permission to check.
         * @return Returns {@code true} if permission has been requested, otherwise {@code false}.
         */
        @JvmStatic
        fun isPermissionRequested(@NonNull context: Context, @NonNull permission: String): Boolean {
            return getPermissionsNotRequested(context, arrayOf(permission)).isEmpty()
        }

        /**
         * Check if app has requested the required permissions or not in the manifest.
         *
         * @param context The context for operations.
         * @param permissions The {@link String[]} names for permissions to check.
         * @return Returns {@link List<String>} of permissions that have not been requested. It will have
         * size 0 if all permissions have been requested.
         */
        @NonNull
        @JvmStatic
        fun getPermissionsNotRequested(@NonNull context: Context, @NonNull permissions: Array<String>): MutableList<String> {
            val permissionsNotRequested = ArrayList<String>()
            permissionsNotRequested.addAll(permissions.asList())
            val packageInfo: PackageInfo? = PackageUtils.getPackageInfoForPackage(context, PackageManager.GET_PERMISSIONS)
            if (packageInfo == null) {
                return permissionsNotRequested
            }
            // If no permissions are requested, then nothing to check
            val requestedPermissions = packageInfo.requestedPermissions
            if (requestedPermissions == null || requestedPermissions.isEmpty())
                return permissionsNotRequested
            val requestedPermissionsList = requestedPermissions.asList()
            for (permission in permissions) {
                if (requestedPermissionsList.contains(permission)) {
                    permissionsNotRequested.remove(permission)
                }
            }
            return permissionsNotRequested
        }

        /**
         * If path is under primary external storage directory and storage permission is missing,
         * then legacy or manage external storage permission will be requested from the user via a call
         * to {@link #checkAndRequestLegacyOrManageExternalStoragePermission(Context, int, boolean, boolean)}.
         *
         * @param context The context for operations.
         * @param filePath The path to check.
         * @param requestCode The request code to use while asking for permission.
         * @param prioritizeManageExternalStoragePermission If {@link Manifest.permission#MANAGE_EXTERNAL_STORAGE}
         *                                                  permission should be requested if on
         *                                                  Android `>= 11` instead of getting legacy
         *                                                  storage permission.
         * @param showErrorMessage If an error message toast should be shown if permission is not granted.
         * @return Returns {@code true} if permission is granted, otherwise {@code false}.
         */
        @SuppressLint("SdCardPath")
        @JvmStatic
        fun checkAndRequestLegacyOrManageExternalStoragePermissionIfPathOnPrimaryExternalStorage(
            @NonNull context: Context, filePath: String?, requestCode: Int,
            prioritizeManageExternalStoragePermission: Boolean, showErrorMessage: Boolean): Boolean {
            // If path is under primary external storage directory, then check for missing permissions.
            // listOf with an explicit nullable arg: Arrays.asList infers a
            // flexible String! argument that never conforms to List<String?>
            // as a type argument, breaking compilation. Behaviour is identical.
            if (!FileUtils.isPathInDirPaths(filePath, listOf<String?>(Environment.getExternalStorageDirectory().absolutePath, "/sdcard"), true))
                return true

            return checkAndRequestLegacyOrManageExternalStoragePermission(context, requestCode, prioritizeManageExternalStoragePermission, showErrorMessage)
        }

        /**
         * Check if legacy or manage external storage permissions has been granted.
         *
         * - If `prioritizeManageExternalStoragePermission` is `true and running on Android `>= 11`, then
         *   it will be checked if app has been granted the
         *   {@link Manifest.permission#MANAGE_EXTERNAL_STORAGE}.
         * - If `prioritizeManageExternalStoragePermission` is `false` and running on Android `>= 11`, then
         *   if {@link #isLegacyExternalStoragePossible(Context)} returns `true`, them it will be
         *   checked if app has has been granted {@link Manifest.permission#READ_EXTERNAL_STORAGE} and
         *   {@link Manifest.permission#WRITE_EXTERNAL_STORAGE} permissions, otherwise it will be checked
         *   if app has been granted the {@link Manifest.permission#MANAGE_EXTERNAL_STORAGE} permission.
         * - If running on Android `< 11`, then it will only be checked if app has been granted
         * {@link Manifest.permission#READ_EXTERNAL_STORAGE} and
         * {@link Manifest.permission#WRITE_EXTERNAL_STORAGE} permissions.
         *
         *  If storage permission is missing, it will be requested from the user if {@code context} is an
         *  instance of {@link Activity} or {@link AppCompatActivity} and {@code requestCode}
         *  is `>=0` and the function will automatically return. The caller should register for
         *  Activity.onActivityResult() and Activity.onRequestPermissionsResult() and call this function
         *  again but set {@code requestCode} to `-1` to check if permission was granted or not.
         *
         *  Caller must add following to AndroidManifest.xml of the app, otherwise errors will be thrown.
         *  {@code
         *  <manifest
         *      <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
         *      <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
         *      <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" tools:ignore="ScopedStorage" />
         *
         *    <application
         *        android:requestLegacyExternalStorage="true"
         *        ....
         *    </application>
         * </manifest>
         *}
         * @param context The context for operations.
         * @param requestCode The request code to use while asking for permission.
         * @param prioritizeManageExternalStoragePermission If {@link Manifest.permission#MANAGE_EXTERNAL_STORAGE}
         *                                                  permission should be requested if on
         *                                                  Android `>= 11` instead of getting legacy
         *                                                  storage permission.
         * @param showErrorMessage If an error message toast should be shown if permission is not granted.
         * @return Returns {@code true} if permission is granted, otherwise {@code false}.
         */
        @JvmStatic
        fun checkAndRequestLegacyOrManageExternalStoragePermission(@NonNull context: Context,
                                                                  requestCode: Int,
                                                                  prioritizeManageExternalStoragePermission: Boolean,
                                                                  showErrorMessage: Boolean): Boolean {
            Logger.logVerbose(LOG_TAG, "Checking storage permission")

            var errmsg: String
            var requestLegacyStoragePermission: Boolean? = null

            if (prioritizeManageExternalStoragePermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                requestLegacyStoragePermission = false

            if (requestLegacyStoragePermission == null)
                requestLegacyStoragePermission = isLegacyExternalStoragePossible(context)

            val hasRequestedLegacyCheck = checkIfHasRequestedLegacyExternalStorage(context)

            Logger.logVerbose(LOG_TAG, "prioritizeManageExternalStoragePermission=" + prioritizeManageExternalStoragePermission +
                    ", requestLegacyStoragePermission=" + requestLegacyStoragePermission +
                    ", checkIfHasRequestedLegacyExternalStorage=" + hasRequestedLegacyCheck)

            if (requestLegacyStoragePermission && hasRequestedLegacyCheck) {
                // Check if requestLegacyExternalStorage is set to true in app manifest
                if (!hasRequestedLegacyExternalStorage(context, showErrorMessage))
                    return false
            }
            if (checkStoragePermission(context, requestLegacyStoragePermission)) {
                return true
            }
            errmsg = context.getString(R.string.msg_storage_permission_not_granted)
            Logger.logError(LOG_TAG, errmsg)
            if (showErrorMessage)
                Logger.showToast(context, errmsg, false)
            if (requestCode < 0 || Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                return false
            if (requestLegacyStoragePermission || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                requestLegacyStorageExternalPermission(context, requestCode)
            } else {
                requestManageStorageExternalPermission(context, requestCode)
            }
            return false
        }

        /**
         * Check if app has been granted storage permission.
         *
         * @param context The context for operations.
         * @param checkLegacyStoragePermission If set to {@code true}, then it will be checked if app
         *                                     has been granted {@link Manifest.permission#READ_EXTERNAL_STORAGE}
         *                                     and {@link Manifest.permission#WRITE_EXTERNAL_STORAGE}
         *                                     permissions, otherwise it will be checked if app has been
         *                                     granted the {@link Manifest.permission#MANAGE_EXTERNAL_STORAGE}
         *                                     permission.
         * @return Returns {@code true} if permission is granted, otherwise {@code false}.
         */
        @JvmStatic
        fun checkStoragePermission(@NonNull context: Context, checkLegacyStoragePermission: Boolean): Boolean {
            return if (checkLegacyStoragePermission || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                checkPermissions(context, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            } else {
                Environment.isExternalStorageManager()
            }
        }

        /**
         * Request user to grant {@link Manifest.permission#READ_EXTERNAL_STORAGE} and
         * {@link Manifest.permission#WRITE_EXTERNAL_STORAGE} permissions to the app.
         *
         * @param context The context for operations. It must be an instance of {@link Activity} or
         * {@link AppCompatActivity}.
         * @param requestCode The request code to use while asking for permission. It must be `>=0` or
         *                    will fail silently and will log an exception.
         * @return Returns {@code true} if requesting the permission was successful, otherwise {@code false}.
         */
        @RequiresApi(api = Build.VERSION_CODES.M)
        @JvmStatic
        fun requestLegacyStorageExternalPermission(@NonNull context: Context, requestCode: Int): Boolean {
            Logger.logInfo(LOG_TAG, "Requesting legacy external storage permission")
            return requestPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE, requestCode)
        }

        /**
         * Wrapper for {@link #requestManageStorageExternalPermission(Context, int)}.
         */
        @RequiresApi(api = Build.VERSION_CODES.R)
        @JvmStatic
        fun requestManageStorageExternalPermission(@NonNull context: Context): Error? {
            return requestManageStorageExternalPermission(context, -1)
        }

        /**
         * Request user to grant {@link Manifest.permission#MANAGE_EXTERNAL_STORAGE} permission to the app.
         *
         * @param context The {@link Context} for operations, like an {@link Activity} or {@link Service} context.
         *                It must be an instance of {@link Activity} or {@link AppCompatActivity} if
         *                result is required via the Activity#onActivityResult() callback and
         *                {@code requestCode} is `>=0`.
         * @param requestCode The request code to use while asking for permission. It must be `>=0` if
         *                    result it required.
         * @return Returns the {@code error} if requesting the permission was not successful, otherwise {@code null}.
         */
        @RequiresApi(api = Build.VERSION_CODES.R)
        @JvmStatic
        fun requestManageStorageExternalPermission(@NonNull context: Context, requestCode: Int): Error? {
            Logger.logInfo(LOG_TAG, "Requesting manage external storage permission")
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.addCategory("android.intent.category.DEFAULT")
            intent.setData(Uri.parse("package:" + context.packageName))
            // Flag must not be passed for activity contexts, otherwise onActivityResult() will not be called with permission grant result.
            // Flag must be passed for non-activity contexts like services, otherwise "Calling startActivity() from outside of an Activity context requires the FLAG_ACTIVITY_NEW_TASK flag" exception will be raised.
            if (context !is Activity)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val error: Error?
            if (requestCode >= 0)
                error = ActivityUtils.startActivityForResult(context, requestCode, intent, true, false)
            else
                error = ActivityUtils.startActivity(context, intent, true, false)
            // Use fallback if matching Activity did not exist for ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION.
            if (error != null) {
                val fallbackIntent = Intent()
                fallbackIntent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                if (requestCode >= 0)
                    return ActivityUtils.startActivityForResult(context, requestCode, fallbackIntent)
                else
                    return ActivityUtils.startActivity(context, fallbackIntent)
            }
            return null
        }

        /**
         * If app is targeting targetSdkVersion 30 (android 11) and running on sdk 30 (android 11) or
         * higher, then {@link android.R.attr#requestLegacyExternalStorage} attribute is ignored.
         * https://developer.android.com/training/data-storage/use-cases#opt-out-scoped-storage
         */
        @JvmStatic
        fun isLegacyExternalStoragePossible(@NonNull context: Context): Boolean {
            return !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && PackageUtils.getTargetSDKForPackage(context) >= Build.VERSION_CODES.R)
        }

        /**
         * Return whether it should be checked if app has set
         * {@link android.R.attr#requestLegacyExternalStorage} attribute to {@code true}, if storage
         * permissions are to be requested based on if {@link #isLegacyExternalStoragePossible(Context)}
         * return {@code true}.
         *
         * If app is targeting targetSdkVersion 30 (android 11), then legacy storage can only be
         * requested if running on sdk 29 (android 10).
         * If app is targeting targetSdkVersion 29 (android 10), then legacy storage can only be
         * requested if running on sdk 29 (android 10) and higher.
         */
        @JvmStatic
        fun checkIfHasRequestedLegacyExternalStorage(@NonNull context: Context): Boolean {
            val targetSdkVersion = PackageUtils.getTargetSDKForPackage(context)
            return if (targetSdkVersion >= Build.VERSION_CODES.R) {
                Build.VERSION.SDK_INT == Build.VERSION_CODES.Q
            } else if (targetSdkVersion == Build.VERSION_CODES.Q) {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            } else {
                false
            }
        }

        /**
         * Call to {@link Environment#isExternalStorageLegacy()} will not return the actual value defined
         * in app manifest for {@link android.R.attr#requestLegacyExternalStorage} attribute,
         * since an app may inherit its legacy state based on when it was first installed, target sdk and
         * other factors. To provide consistent experience for all users regardless of current legacy
         * state on a specific device, we directly use the value defined in app` manifest.
         */
        @JvmStatic
        fun hasRequestedLegacyExternalStorage(@NonNull context: Context, showErrorMessage: Boolean): Boolean {
            var errmsg: String
            val hasRequestedLegacyExternalStorage = PackageUtils.hasRequestedLegacyExternalStorage(context)
            if (hasRequestedLegacyExternalStorage != null && !hasRequestedLegacyExternalStorage) {
                errmsg = context.getString(R.string.error_has_not_requested_legacy_external_storage, context.packageName, PackageUtils.getTargetSDKForPackage(context), Build.VERSION.SDK_INT)
                Logger.logError(LOG_TAG, errmsg)
                if (showErrorMessage)
                    Logger.showToast(context, errmsg, true)
                return false
            }
            return true
        }

        /**
         * Check if {@link Manifest.permission#SYSTEM_ALERT_WINDOW} permission has been granted.
         *
         * @param context The context for operations.
         * @return Returns {@code true} if permission is granted, otherwise {@code false}.
         */
        @JvmStatic
        fun checkDisplayOverOtherAppsPermission(@NonNull context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                Settings.canDrawOverlays(context)
            else
                true
        }

        /**
         * Wrapper for {@link #requestDisplayOverOtherAppsPermission(Context, int)}.
         */
        @JvmStatic
        fun requestDisplayOverOtherAppsPermission(@NonNull context: Context): Error? {
            return requestDisplayOverOtherAppsPermission(context, -1)
        }

        /**
         * Request user to grant {@link Manifest.permission#SYSTEM_ALERT_WINDOW} permission to the app.
         *
         * @param context The {@link Context} for operations, like an {@link Activity} or {@link Service} context.
         *                It must be an instance of {@link Activity} or {@link AppCompatActivity} if
         *                result is required via the Activity#onActivityResult() callback and
         *                {@code requestCode} is `>=0`.
         * @param requestCode The request code to use while asking for permission. It must be `>=0` if
         *                    result it required.
         * @return Returns the {@code error} if requesting the permission was not successful, otherwise {@code null}.
         */
        @JvmStatic
        fun requestDisplayOverOtherAppsPermission(@NonNull context: Context, requestCode: Int): Error? {
            Logger.logInfo(LOG_TAG, "Requesting display over apps permission")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                return null
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.setData(Uri.parse("package:" + context.packageName))
            // Flag must not be passed for activity contexts, otherwise onActivityResult() will not be called with permission grant result.
            // Flag must be passed for non-activity contexts like services, otherwise "Calling startActivity() from outside of an Activity context requires the FLAG_ACTIVITY_NEW_TASK flag" exception will be raised.
            if (context !is Activity)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return if (requestCode >= 0)
                ActivityUtils.startActivityForResult(context, requestCode, intent)
            else
                ActivityUtils.startActivity(context, intent)
        }

        /**
         * Check if running on sdk 29 (android 10) or higher and {@link Manifest.permission#SYSTEM_ALERT_WINDOW}
         * permission has been granted or not.
         *
         * @param context The {@link Context} for operations.
         * @param logResults If it should be logged that permission has been granted or not.
         * @return Returns {@code true} if permission is granted, otherwise {@code false}.
         */
        @JvmStatic
        fun validateDisplayOverOtherAppsPermissionForPostAndroid10(@NonNull context: Context, logResults: Boolean): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                return true
            return if (!checkDisplayOverOtherAppsPermission(context)) {
                if (logResults)
                    Logger.logWarn(LOG_TAG, context.packageName + " does not have Display over other apps (SYSTEM_ALERT_WINDOW) permission")
                false
            } else {
                if (logResults)
                    Logger.logDebug(LOG_TAG, context.packageName + " already has Display over other apps (SYSTEM_ALERT_WINDOW) permission")
                true
            }
        }

        /**
         * Check if {@link Manifest.permission#REQUEST_IGNORE_BATTERY_OPTIMIZATIONS} permission has been
         * granted.
         *
         * @param context The context for operations.
         * @return Returns {@code true} if permission is granted, otherwise {@code false}.
         */
        @JvmStatic
        fun checkIfBatteryOptimizationsDisabled(@NonNull context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // A Java-style cast of a missing service returns null, but Kotlin
                // `as` would throw, so use a safe cast and preserve the Java NPE
                // explicitly. The service is never missing in practice.
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
                powerManager!!.isIgnoringBatteryOptimizations(context.packageName)
            } else
                true
        }

        /**
         * Wrapper for {@link #requestDisableBatteryOptimizations(Context, int)}.
         */
        @JvmStatic
        fun requestDisableBatteryOptimizations(@NonNull context: Context): Error? {
            return requestDisableBatteryOptimizations(context, -1)
        }

        /**
         * Request user to grant {@link Manifest.permission#REQUEST_IGNORE_BATTERY_OPTIMIZATIONS}
         * permission to the app.
         *
         * @param context The {@link Context} for operations, like an {@link Activity} or {@link Service} context.
         * @param result it required.
         * @return Returns the {@code error} if requesting the permission was not successful, otherwise {@code null}.
         */
        @SuppressLint("BatteryLife")
        @JvmStatic
        fun requestDisableBatteryOptimizations(@NonNull context: Context, requestCode: Int): Error? {
            Logger.logInfo(LOG_TAG, "Requesting to disable battery optimizations")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                return null
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.setData(Uri.parse("package:" + context.packageName))
            // Flag must not be passed for activity contexts, otherwise onActivityResult() will not be called with permission grant result.
            // Flag must be passed for non-activity contexts like services, otherwise "Calling startActivity() from outside of an Activity context requires the FLAG_ACTIVITY_NEW_TASK flag" exception will be raised.
            if (context !is Activity)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return if (requestCode >= 0)
                ActivityUtils.startActivityForResult(context, requestCode, intent)
            else
                ActivityUtils.startActivity(context, intent)
        }
    }
}
