package com.termux.shared.termux

import android.annotation.SuppressLint
import java.io.File

/**
 * Termux app constants. This is a mechanical port of TermuxConstants.java:
 * string/int literals stay const val; anything constructed (File, lists)
 * uses @JvmField with identical descriptors.
 */
class TermuxConstants {

    companion object {
        const val TERMUX_GITHUB_ORGANIZATION_NAME = "termux"
        const val TERMUX_GITHUB_ORGANIZATION_URL = "https://github.com" + "/" + TERMUX_GITHUB_ORGANIZATION_NAME
        const val FDROID_PACKAGES_BASE_URL = "https://f-droid.org/en/packages"
        const val TERMUX_APP_NAME = "Termux"
        const val TERMUX_PACKAGE_NAME = "com.termux"
        const val TERMUX_GITHUB_REPO_NAME = "termux-app"
        const val TERMUX_GITHUB_REPO_URL = TERMUX_GITHUB_ORGANIZATION_URL + "/" + TERMUX_GITHUB_REPO_NAME
        const val TERMUX_GITHUB_ISSUES_REPO_URL = TERMUX_GITHUB_REPO_URL + "/issues"
        const val TERMUX_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + TERMUX_PACKAGE_NAME
        const val TERMUX_API_APP_NAME = "Termux:API"
        const val TERMUX_API_PACKAGE_NAME = TERMUX_PACKAGE_NAME + ".api"
        const val TERMUX_API_GITHUB_REPO_NAME = "termux-api"
        const val TERMUX_API_GITHUB_REPO_URL = TERMUX_GITHUB_ORGANIZATION_URL + "/" + TERMUX_API_GITHUB_REPO_NAME
        const val TERMUX_API_GITHUB_ISSUES_REPO_URL = TERMUX_API_GITHUB_REPO_URL + "/issues"
        const val TERMUX_API_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + TERMUX_API_PACKAGE_NAME
        const val TERMUX_BOOT_APP_NAME = "Termux:Boot"
        const val TERMUX_BOOT_PACKAGE_NAME = TERMUX_PACKAGE_NAME + ".boot"
        const val TERMUX_BOOT_GITHUB_REPO_NAME = "termux-boot"
        const val TERMUX_BOOT_GITHUB_REPO_URL = TERMUX_GITHUB_ORGANIZATION_URL + "/" + TERMUX_BOOT_GITHUB_REPO_NAME
        const val TERMUX_BOOT_GITHUB_ISSUES_REPO_URL = TERMUX_BOOT_GITHUB_REPO_URL + "/issues"
        const val TERMUX_BOOT_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + TERMUX_BOOT_PACKAGE_NAME
        const val TERMUX_FLOAT_APP_NAME = "Termux:Float"
        const val TERMUX_FLOAT_PACKAGE_NAME = TERMUX_PACKAGE_NAME + ".window"
        const val TERMUX_FLOAT_GITHUB_REPO_NAME = "termux-float"
        const val TERMUX_FLOAT_GITHUB_REPO_URL = TERMUX_GITHUB_ORGANIZATION_URL + "/" + TERMUX_FLOAT_GITHUB_REPO_NAME
        const val TERMUX_FLOAT_GITHUB_ISSUES_REPO_URL = TERMUX_FLOAT_GITHUB_REPO_URL + "/issues"
        const val TERMUX_FLOAT_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + TERMUX_FLOAT_PACKAGE_NAME
        const val TERMUX_STYLING_APP_NAME = "Termux:Styling"
        const val TERMUX_STYLING_PACKAGE_NAME = TERMUX_PACKAGE_NAME + ".styling"
        const val TERMUX_STYLING_GITHUB_REPO_NAME = "termux-styling"
        const val TERMUX_STYLING_GITHUB_REPO_URL = TERMUX_GITHUB_ORGANIZATION_URL + "/" + TERMUX_STYLING_GITHUB_REPO_NAME
        const val TERMUX_STYLING_GITHUB_ISSUES_REPO_URL = TERMUX_STYLING_GITHUB_REPO_URL + "/issues"
        const val TERMUX_STYLING_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + TERMUX_STYLING_PACKAGE_NAME
        const val TERMUX_TASKER_APP_NAME = "Termux:Tasker"
        const val TERMUX_GUI_APP_NAME = "Termux:GUI"
        const val TERMUX_TASKER_PACKAGE_NAME = TERMUX_PACKAGE_NAME + ".tasker"
        const val TERMUX_GUI_PACKAGE_NAME = TERMUX_PACKAGE_NAME + ".gui"
        const val TERMUX_TASKER_GITHUB_REPO_NAME = "termux-tasker"
        const val TERMUX_GUI_GITHUB_REPO_NAME = "termux-gui"
        const val TERMUX_TASKER_GITHUB_REPO_URL = TERMUX_GITHUB_ORGANIZATION_URL + "/" + TERMUX_TASKER_GITHUB_REPO_NAME
        const val TERMUX_GUI_GITHUB_REPO_URL = TERMUX_GITHUB_ORGANIZATION_URL + "/" + TERMUX_GUI_GITHUB_REPO_NAME
        const val TERMUX_TASKER_GITHUB_ISSUES_REPO_URL = TERMUX_TASKER_GITHUB_REPO_URL + "/issues"
        const val TERMUX_GUI_GITHUB_ISSUES_REPO_URL = TERMUX_GUI_GITHUB_REPO_URL + "/issues"
        const val TERMUX_TASKER_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + TERMUX_TASKER_PACKAGE_NAME
        const val TERMUX_GUI_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + TERMUX_GUI_PACKAGE_NAME
        const val TERMUX_WIDGET_APP_NAME = "Termux:Widget"
        const val TERMUX_WIDGET_PACKAGE_NAME = TERMUX_PACKAGE_NAME + ".widget"
        const val TERMUX_WIDGET_GITHUB_REPO_NAME = "termux-widget"
        const val TERMUX_WIDGET_GITHUB_REPO_URL = TERMUX_GITHUB_ORGANIZATION_URL + "/" + TERMUX_WIDGET_GITHUB_REPO_NAME
        const val TERMUX_WIDGET_GITHUB_ISSUES_REPO_URL = TERMUX_WIDGET_GITHUB_REPO_URL + "/issues"
        const val TERMUX_WIDGET_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + TERMUX_WIDGET_PACKAGE_NAME
        @JvmField
        val TERMUX_PLUGIN_APP_NAMES_LIST: List<String> = listOf<String>(TERMUX_API_APP_NAME, TERMUX_BOOT_APP_NAME, TERMUX_FLOAT_APP_NAME, TERMUX_STYLING_APP_NAME, TERMUX_TASKER_APP_NAME, TERMUX_WIDGET_APP_NAME, TERMUX_GUI_APP_NAME)
        @JvmField
        val TERMUX_PLUGIN_APP_PACKAGE_NAMES_LIST: List<String>? = listOf<String>(TERMUX_API_PACKAGE_NAME, TERMUX_BOOT_PACKAGE_NAME, TERMUX_FLOAT_PACKAGE_NAME, TERMUX_STYLING_PACKAGE_NAME, TERMUX_TASKER_PACKAGE_NAME, TERMUX_WIDGET_PACKAGE_NAME, TERMUX_GUI_PACKAGE_NAME)
        const val APK_RELEASE_FDROID = "F-Droid"
        const val APK_RELEASE_FDROID_SIGNING_CERTIFICATE_SHA256_DIGEST = "228FB2CFE90831C1499EC3CCAF61E96E8E1CE70766B9474672CE427334D41C42"
        const val APK_RELEASE_GITHUB = "GitHub"
        const val APK_RELEASE_GITHUB_SIGNING_CERTIFICATE_SHA256_DIGEST = "B6DA01480EEFD5FBF2CD3771B8D1021EC791304BDD6C4BF41D3FAABAD48EE5E1"
        const val APK_RELEASE_GOOGLE_PLAYSTORE = "Google Play Store"
        const val APK_RELEASE_GOOGLE_PLAYSTORE_SIGNING_CERTIFICATE_SHA256_DIGEST = "738F0A30A04D3C8A1BE304AF18D0779BCF3EA88FB60808F657A3521861C2EBF9"
        const val APK_RELEASE_TERMUX_DEVS = "Termux Devs"
        const val APK_RELEASE_TERMUX_DEVS_SIGNING_CERTIFICATE_SHA256_DIGEST = "F7A038EB551F1BE8FDF388686B784ABAB4552A5D82DF423E3D8F1B5CBE1C69AE"
        const val TERMUX_PACKAGES_GITHUB_REPO_NAME = "termux-packages"
        const val TERMUX_PACKAGES_GITHUB_REPO_URL = TERMUX_GITHUB_ORGANIZATION_URL + "/" + TERMUX_PACKAGES_GITHUB_REPO_NAME
        const val TERMUX_PACKAGES_GITHUB_ISSUES_REPO_URL = TERMUX_PACKAGES_GITHUB_REPO_URL + "/issues"
        const val TERMUX_API_APT_PACKAGE_NAME = "termux-api"
        const val TERMUX_API_APT_GITHUB_REPO_NAME = "termux-api-package"
        const val TERMUX_API_APT_GITHUB_REPO_URL = TERMUX_GITHUB_ORGANIZATION_URL + "/" + TERMUX_API_APT_GITHUB_REPO_NAME
        const val TERMUX_API_APT_GITHUB_ISSUES_REPO_URL = TERMUX_API_APT_GITHUB_REPO_URL + "/issues"
        const val TERMUX_SITE = TERMUX_APP_NAME + " Site"
        const val TERMUX_SITE_URL = "https://termux.dev"
        const val TERMUX_WIKI = TERMUX_APP_NAME + " Wiki"
        const val TERMUX_WIKI_URL = "https://wiki.termux.com"
        const val TERMUX_GITHUB_WIKI_REPO_URL = TERMUX_GITHUB_REPO_URL + "/wiki"
        const val TERMUX_PACKAGES_GITHUB_WIKI_REPO_URL = TERMUX_PACKAGES_GITHUB_REPO_URL + "/wiki"
        const val TERMUX_SUPPORT_EMAIL_URL = "support@termux.dev"
        const val TERMUX_SUPPORT_EMAIL_MAILTO_URL = "mailto:" + TERMUX_SUPPORT_EMAIL_URL
        const val TERMUX_REDDIT_SUBREDDIT = "r/termux"
        const val TERMUX_REDDIT_SUBREDDIT_URL = "https://www.reddit.com/r/termux"
        const val TERMUX_DONATE_URL = TERMUX_SITE_URL + "/donate"
    @SuppressLint("SdCardPath")
    // Default: "/data/data/com.termux"
        const val TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH = "/data/data/" + TERMUX_PACKAGE_NAME
        @JvmField
        val TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR: File = File(TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH)
        const val TERMUX_FILES_DIR_PATH = TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH + "/files"
        @JvmField
        val TERMUX_FILES_DIR: File = File(TERMUX_FILES_DIR_PATH)
        const val TERMUX_PREFIX_DIR_PATH = TERMUX_FILES_DIR_PATH + "/usr"
        @JvmField
        val TERMUX_PREFIX_DIR: File = File(TERMUX_PREFIX_DIR_PATH)
        const val TERMUX_BIN_PREFIX_DIR_PATH = TERMUX_PREFIX_DIR_PATH + "/bin"
        @JvmField
        val TERMUX_BIN_PREFIX_DIR: File = File(TERMUX_BIN_PREFIX_DIR_PATH)
        const val TERMUX_ETC_PREFIX_DIR_PATH = TERMUX_PREFIX_DIR_PATH + "/etc"
        @JvmField
        val TERMUX_ETC_PREFIX_DIR: File = File(TERMUX_ETC_PREFIX_DIR_PATH)
        const val TERMUX_INCLUDE_PREFIX_DIR_PATH = TERMUX_PREFIX_DIR_PATH + "/include"
        @JvmField
        val TERMUX_INCLUDE_PREFIX_DIR: File = File(TERMUX_INCLUDE_PREFIX_DIR_PATH)
        const val TERMUX_LIB_PREFIX_DIR_PATH = TERMUX_PREFIX_DIR_PATH + "/lib"
        @JvmField
        val TERMUX_LIB_PREFIX_DIR: File = File(TERMUX_LIB_PREFIX_DIR_PATH)
        const val TERMUX_LIBEXEC_PREFIX_DIR_PATH = TERMUX_PREFIX_DIR_PATH + "/libexec"
        @JvmField
        val TERMUX_LIBEXEC_PREFIX_DIR: File = File(TERMUX_LIBEXEC_PREFIX_DIR_PATH)
        const val TERMUX_SHARE_PREFIX_DIR_PATH = TERMUX_PREFIX_DIR_PATH + "/share"
        @JvmField
        val TERMUX_SHARE_PREFIX_DIR: File = File(TERMUX_SHARE_PREFIX_DIR_PATH)
        const val TERMUX_TMP_PREFIX_DIR_PATH = TERMUX_PREFIX_DIR_PATH + "/tmp"
        @JvmField
        val TERMUX_TMP_PREFIX_DIR: File = File(TERMUX_TMP_PREFIX_DIR_PATH)
        const val TERMUX_VAR_PREFIX_DIR_PATH = TERMUX_PREFIX_DIR_PATH + "/var"
        @JvmField
        val TERMUX_VAR_PREFIX_DIR: File = File(TERMUX_VAR_PREFIX_DIR_PATH)
        const val TERMUX_STAGING_PREFIX_DIR_PATH = TERMUX_FILES_DIR_PATH + "/usr-staging"
        @JvmField
        val TERMUX_STAGING_PREFIX_DIR: File = File(TERMUX_STAGING_PREFIX_DIR_PATH)
        const val TERMUX_HOME_DIR_PATH = TERMUX_FILES_DIR_PATH + "/home"
        @JvmField
        val TERMUX_HOME_DIR: File = File(TERMUX_HOME_DIR_PATH)
        const val TERMUX_CONFIG_HOME_DIR_PATH = TERMUX_HOME_DIR_PATH + "/.config/termux"
        @JvmField
        val TERMUX_CONFIG_HOME_DIR: File = File(TERMUX_CONFIG_HOME_DIR_PATH)
        const val TERMUX_CONFIG_PREFIX_DIR_PATH = TERMUX_ETC_PREFIX_DIR_PATH + "/termux"
        @JvmField
        val TERMUX_CONFIG_PREFIX_DIR: File = File(TERMUX_CONFIG_PREFIX_DIR_PATH)
        const val TERMUX_DATA_HOME_DIR_PATH = TERMUX_HOME_DIR_PATH + "/.termux"
        @JvmField
        val TERMUX_DATA_HOME_DIR: File = File(TERMUX_DATA_HOME_DIR_PATH)
        const val TERMUX_STORAGE_HOME_DIR_PATH = TERMUX_HOME_DIR_PATH + "/storage"
        @JvmField
        val TERMUX_STORAGE_HOME_DIR: File = File(TERMUX_STORAGE_HOME_DIR_PATH)
        const val TERMUX_BACKGROUND_DIR_PATH = TERMUX_DATA_HOME_DIR_PATH + "/background"
        @JvmField
        val TERMUX_BACKGROUND_DIR: File = File(TERMUX_BACKGROUND_DIR_PATH)
        const val TERMUX_BACKGROUND_IMAGE_PATH = TERMUX_BACKGROUND_DIR_PATH + "/background.jpeg"
        @JvmField
        val TERMUX_BACKGROUND_IMAGE_FILE: File = File(TERMUX_BACKGROUND_IMAGE_PATH)
        const val TERMUX_BACKGROUND_IMAGE_PORTRAIT_PATH = TERMUX_BACKGROUND_DIR_PATH + "/background_portrait.jpeg"
        @JvmField
        val TERMUX_BACKGROUND_IMAGE_PORTRAIT_FILE: File = File(TERMUX_BACKGROUND_IMAGE_PORTRAIT_PATH)
        const val TERMUX_BACKGROUND_IMAGE_LANDSCAPE_PATH = TERMUX_BACKGROUND_DIR_PATH + "/background_landscape.jpeg"
        @JvmField
        val TERMUX_BACKGROUND_IMAGE_LANDSCAPE_FILE: File = File(TERMUX_BACKGROUND_IMAGE_LANDSCAPE_PATH)
        const val TERMUX_APPS_DIR_PATH = TERMUX_FILES_DIR_PATH + "/apps"
        @JvmField
        val TERMUX_APPS_DIR: File = File(TERMUX_APPS_DIR_PATH)
        @JvmField
        val TERMUX_PREFIX_DIR_IGNORED_SUB_FILES_PATHS_TO_CONSIDER_AS_EMPTY: List<String> = listOf<String>(TermuxConstants.TERMUX_TMP_PREFIX_DIR_PATH, TermuxConstants.TERMUX_ENV_TEMP_FILE_PATH, TermuxConstants.TERMUX_ENV_FILE_PATH)
        const val TERMUX_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = TERMUX_PACKAGE_NAME + "_preferences"
        const val TERMUX_API_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = TERMUX_API_PACKAGE_NAME + "_preferences"
        const val TERMUX_BOOT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = TERMUX_BOOT_PACKAGE_NAME + "_preferences"
        const val TERMUX_FLOAT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = TERMUX_FLOAT_PACKAGE_NAME + "_preferences"
        const val TERMUX_STYLING_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = TERMUX_STYLING_PACKAGE_NAME + "_preferences"
        const val TERMUX_TASKER_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = TERMUX_TASKER_PACKAGE_NAME + "_preferences"
        const val TERMUX_WIDGET_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = TERMUX_WIDGET_PACKAGE_NAME + "_preferences"
        const val TERMUX_GUI_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = TERMUX_GUI_PACKAGE_NAME + "_preferences"
        const val TERMUX_PROPERTIES_PRIMARY_FILE_PATH = TERMUX_DATA_HOME_DIR_PATH + "/termux.properties"
        @JvmField
        val TERMUX_PROPERTIES_PRIMARY_FILE: File = File(TERMUX_PROPERTIES_PRIMARY_FILE_PATH)
        const val TERMUX_PROPERTIES_SECONDARY_FILE_PATH = TERMUX_CONFIG_HOME_DIR_PATH + "/termux.properties"
        @JvmField
        val TERMUX_PROPERTIES_SECONDARY_FILE: File = File(TERMUX_PROPERTIES_SECONDARY_FILE_PATH)
        @JvmField
        val TERMUX_PROPERTIES_FILE_PATHS_LIST: List<String> = listOf<String>(TERMUX_PROPERTIES_PRIMARY_FILE_PATH, TERMUX_PROPERTIES_SECONDARY_FILE_PATH)
        const val TERMUX_FLOAT_PROPERTIES_PRIMARY_FILE_PATH = TERMUX_DATA_HOME_DIR_PATH + "/termux.float.properties"
        @JvmField
        val TERMUX_FLOAT_PROPERTIES_PRIMARY_FILE: File = File(TERMUX_FLOAT_PROPERTIES_PRIMARY_FILE_PATH)
        const val TERMUX_FLOAT_PROPERTIES_SECONDARY_FILE_PATH = TERMUX_CONFIG_HOME_DIR_PATH + "/termux.float.properties"
        @JvmField
        val TERMUX_FLOAT_PROPERTIES_SECONDARY_FILE: File = File(TERMUX_FLOAT_PROPERTIES_SECONDARY_FILE_PATH)
        @JvmField
        val TERMUX_FLOAT_PROPERTIES_FILE_PATHS_LIST: List<String> = listOf<String>(TERMUX_FLOAT_PROPERTIES_PRIMARY_FILE_PATH, TERMUX_FLOAT_PROPERTIES_SECONDARY_FILE_PATH)
        const val TERMUX_COLOR_PROPERTIES_FILE_PATH = TERMUX_DATA_HOME_DIR_PATH + "/colors.properties"
        @JvmField
        val TERMUX_COLOR_PROPERTIES_FILE: File = File(TERMUX_COLOR_PROPERTIES_FILE_PATH)
        const val TERMUX_FONT_FILE_PATH = TERMUX_DATA_HOME_DIR_PATH + "/font.ttf"
        @JvmField
        val TERMUX_FONT_FILE: File = File(TERMUX_FONT_FILE_PATH)
        const val TERMUX_ITALIC_FONT_FILE_PATH = TERMUX_DATA_HOME_DIR_PATH + "/font-italic.ttf"
        @JvmField
        val TERMUX_ITALIC_FONT_FILE: File = File(TERMUX_ITALIC_FONT_FILE_PATH)
        const val TERMUX_CRASH_LOG_FILE_PATH = TERMUX_HOME_DIR_PATH + "/crash_log.md"
        const val TERMUX_CRASH_LOG_BACKUP_FILE_PATH = TERMUX_HOME_DIR_PATH + "/crash_log_backup.md"
        const val TERMUX_ENV_FILE_PATH = TERMUX_CONFIG_PREFIX_DIR_PATH + "/termux.env"
        const val TERMUX_ENV_TEMP_FILE_PATH = TERMUX_CONFIG_PREFIX_DIR_PATH + "/termux.env.tmp"
        const val TERMUX_BOOT_SCRIPTS_DIR_PATH = TERMUX_DATA_HOME_DIR_PATH + "/boot"
        @JvmField
        val TERMUX_BOOT_SCRIPTS_DIR: File = File(TERMUX_BOOT_SCRIPTS_DIR_PATH)
        const val TERMUX_SHORTCUT_SCRIPTS_DIR_PATH = TERMUX_HOME_DIR_PATH + "/.shortcuts"
        @JvmField
        val TERMUX_SHORTCUT_SCRIPTS_DIR: File = File(TERMUX_SHORTCUT_SCRIPTS_DIR_PATH)
        const val TERMUX_SHORTCUT_TASKS_SCRIPTS_DIR_BASENAME = "tasks"
        const val TERMUX_SHORTCUT_TASKS_SCRIPTS_DIR_PATH = TERMUX_SHORTCUT_SCRIPTS_DIR_PATH + "/" + TERMUX_SHORTCUT_TASKS_SCRIPTS_DIR_BASENAME
        @JvmField
        val TERMUX_SHORTCUT_TASKS_SCRIPTS_DIR: File = File(TERMUX_SHORTCUT_TASKS_SCRIPTS_DIR_PATH)
        const val TERMUX_SHORTCUT_SCRIPT_ICONS_DIR_BASENAME = "icons"
        const val TERMUX_SHORTCUT_SCRIPT_ICONS_DIR_PATH = TERMUX_SHORTCUT_SCRIPTS_DIR_PATH + "/" + TERMUX_SHORTCUT_SCRIPT_ICONS_DIR_BASENAME
        @JvmField
        val TERMUX_SHORTCUT_SCRIPT_ICONS_DIR: File = File(TERMUX_SHORTCUT_SCRIPT_ICONS_DIR_PATH)
        const val TERMUX_TASKER_SCRIPTS_DIR_PATH = TERMUX_DATA_HOME_DIR_PATH + "/tasker"
        @JvmField
        val TERMUX_TASKER_SCRIPTS_DIR: File = File(TERMUX_TASKER_SCRIPTS_DIR_PATH)
        const val TERMUX_APP_NOTIFICATION_CHANNEL_ID = "termux_notification_channel"
        const val TERMUX_APP_NOTIFICATION_CHANNEL_NAME = TermuxConstants.TERMUX_APP_NAME + " App"
        const val TERMUX_APP_NOTIFICATION_ID = 1337
        const val TERMUX_RUN_COMMAND_NOTIFICATION_CHANNEL_ID = "termux_run_command_notification_channel"
        const val TERMUX_RUN_COMMAND_NOTIFICATION_CHANNEL_NAME = TermuxConstants.TERMUX_APP_NAME + " RunCommandService"
        const val TERMUX_RUN_COMMAND_NOTIFICATION_ID = 1338
        const val TERMUX_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_ID = "termux_plugin_command_errors_notification_channel"
        const val TERMUX_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_NAME = TermuxConstants.TERMUX_APP_NAME + " Plugin Commands Errors"
        const val TERMUX_CRASH_REPORTS_NOTIFICATION_CHANNEL_ID = "termux_crash_reports_notification_channel"
        const val TERMUX_CRASH_REPORTS_NOTIFICATION_CHANNEL_NAME = TermuxConstants.TERMUX_APP_NAME + " Crash Reports"
        const val TERMUX_FLOAT_APP_NOTIFICATION_CHANNEL_ID = "termux_float_notification_channel"
        const val TERMUX_FLOAT_APP_NOTIFICATION_CHANNEL_NAME = TermuxConstants.TERMUX_FLOAT_APP_NAME + " App"
        const val TERMUX_FLOAT_APP_NOTIFICATION_ID = 1339
        const val PERMISSION_RUN_COMMAND = TERMUX_PACKAGE_NAME + ".permission.RUN_COMMAND"
        const val PROP_ALLOW_EXTERNAL_APPS = "allow-external-apps"
        const val PROP_DEFAULT_VALUE_ALLOW_EXTERNAL_APPS = "false"
        const val BROADCAST_TERMUX_OPENED = TERMUX_PACKAGE_NAME + ".app.OPENED"
        const val TERMUX_FILE_SHARE_URI_AUTHORITY = TERMUX_PACKAGE_NAME + ".files"
        const val COMMA_NORMAL = ","
        const val COMMA_ALTERNATIVE = "‚"
        const val TERMUX_ENV_PREFIX_ROOT = "TERMUX"
    }

    class TERMUX_APP {

        companion object {
            const val APPS_DIR_PATH = TERMUX_APPS_DIR_PATH + "/" + TERMUX_PACKAGE_NAME
            const val TERMUX_AM_SOCKET_FILE_PATH = APPS_DIR_PATH + "/termux-am/am.sock"
            const val BUILD_CONFIG_CLASS_NAME = TERMUX_PACKAGE_NAME + ".BuildConfig"
            const val FILE_SHARE_RECEIVER_ACTIVITY_CLASS_NAME = TERMUX_PACKAGE_NAME + ".app.api.file.FileShareReceiverActivity"
            const val FILE_VIEW_RECEIVER_ACTIVITY_CLASS_NAME = TERMUX_PACKAGE_NAME + ".app.api.file.FileViewReceiverActivity"
            const val TERMUX_ACTIVITY_NAME = TERMUX_PACKAGE_NAME + ".app.TermuxActivity"
            const val TERMUX_SETTINGS_ACTIVITY_NAME = TERMUX_PACKAGE_NAME + ".app.activities.SettingsActivity"
            const val TERMUX_SERVICE_NAME = TERMUX_PACKAGE_NAME + ".app.TermuxService"
            const val RUN_COMMAND_SERVICE_NAME = TERMUX_PACKAGE_NAME + ".app.RunCommandService"
        }

        class TERMUX_ACTIVITY {

            companion object {
                const val EXTRA_FAILSAFE_SESSION = TermuxConstants.TERMUX_PACKAGE_NAME + ".app.failsafe_session"
                const val ACTION_NOTIFY_APP_CRASH = TermuxConstants.TERMUX_PACKAGE_NAME + ".app.notify_app_crash"
                const val ACTION_RELOAD_STYLE = TermuxConstants.TERMUX_PACKAGE_NAME + ".app.reload_style"
            @Deprecated("")
            // Default: "com.termux.app.reload_style"
                const val EXTRA_RELOAD_STYLE = TermuxConstants.TERMUX_PACKAGE_NAME + ".app.reload_style"
                const val EXTRA_RECREATE_ACTIVITY = TERMUX_APP.TERMUX_ACTIVITY_NAME + ".EXTRA_RECREATE_ACTIVITY"
                const val ACTION_REQUEST_PERMISSIONS = TermuxConstants.TERMUX_PACKAGE_NAME + ".app.request_storage_permissions"
            }
        }

        class TERMUX_SERVICE {

            companion object {
                const val ACTION_STOP_SERVICE = TERMUX_PACKAGE_NAME + ".service_stop"
                const val ACTION_WAKE_LOCK = TERMUX_PACKAGE_NAME + ".service_wake_lock"
                const val ACTION_WAKE_UNLOCK = TERMUX_PACKAGE_NAME + ".service_wake_unlock"
            // Default: "com.termux.service_execute"
                const val ACTION_SERVICE_EXECUTE = TERMUX_PACKAGE_NAME + ".service_execute"
            // Default: "com.termux.service_execution_stop"
                const val ACTION_SERVICE_STOP = TERMUX_PACKAGE_NAME + ".service_execution_stop"
                const val URI_SCHEME_SERVICE_EXECUTE = TERMUX_PACKAGE_NAME + ".file"
                const val EXTRA_ARGUMENTS = TERMUX_PACKAGE_NAME + ".execute.arguments"
                const val EXTRA_STDIN = TERMUX_PACKAGE_NAME + ".execute.stdin"
                const val EXTRA_WORKDIR = TERMUX_PACKAGE_NAME + ".execute.cwd"
            @Deprecated("")
            // Default: "com.termux.execute.background"
                const val EXTRA_BACKGROUND = TERMUX_PACKAGE_NAME + ".execute.background"
                const val EXTRA_RUNNER = TERMUX_PACKAGE_NAME + ".execute.runner"
                const val EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL = TERMUX_PACKAGE_NAME + ".execute.background_custom_log_level"
                const val EXTRA_SESSION_ACTION = TERMUX_PACKAGE_NAME + ".execute.session_action"
                const val EXTRA_SHELL_NAME = TERMUX_PACKAGE_NAME + ".execute.shell_name"
                const val EXTRA_SHELL_CREATE_MODE = TERMUX_PACKAGE_NAME + ".execute.shell_create_mode"
                const val EXTRA_COMMAND_LABEL = TERMUX_PACKAGE_NAME + ".execute.command_label"
                const val EXTRA_COMMAND_DESCRIPTION = TERMUX_PACKAGE_NAME + ".execute.command_description"
                const val EXTRA_COMMAND_HELP = TERMUX_PACKAGE_NAME + ".execute.command_help"
                const val EXTRA_PLUGIN_API_HELP = TERMUX_PACKAGE_NAME + ".execute.plugin_api_help"
                const val EXTRA_PENDING_INTENT = "pendingIntent"
                const val EXTRA_RESULT_DIRECTORY = TERMUX_PACKAGE_NAME + ".execute.result_directory"
                const val EXTRA_RESULT_SINGLE_FILE = TERMUX_PACKAGE_NAME + ".execute.result_single_file"
                const val EXTRA_RESULT_FILE_BASENAME = TERMUX_PACKAGE_NAME + ".execute.result_file_basename"
                const val EXTRA_RESULT_FILE_OUTPUT_FORMAT = TERMUX_PACKAGE_NAME + ".execute.result_file_output_format"
                const val EXTRA_RESULT_FILE_ERROR_FORMAT = TERMUX_PACKAGE_NAME + ".execute.result_file_error_format"
            // Default: "com.termux.execute.result_files_suffix"
                const val EXTRA_RESULT_FILES_SUFFIX = TERMUX_PACKAGE_NAME + ".execute.result_files_suffix"
            // Default: "com.termux.execute.sigkill_delay_on_stop"
                const val EXTRA_SIGKILL_DELAY_ON_STOP = TERMUX_PACKAGE_NAME + ".execute.sigkill_delay_on_stop"
                const val VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY = 0
                const val VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_OPEN_ACTIVITY = 1
                const val VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY = 2
                const val VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_DONT_OPEN_ACTIVITY = 3
                const val MIN_VALUE_EXTRA_SESSION_ACTION = VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY
                const val MAX_VALUE_EXTRA_SESSION_ACTION = VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_DONT_OPEN_ACTIVITY
                const val EXTRA_PLUGIN_RESULT_BUNDLE = "result"
                const val EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT = "stdout"
                const val EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT_ORIGINAL_LENGTH = "stdout_original_length"
                const val EXTRA_PLUGIN_RESULT_BUNDLE_STDERR = "stderr"
                const val EXTRA_PLUGIN_RESULT_BUNDLE_STDERR_ORIGINAL_LENGTH = "stderr_original_length"
                const val EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE = "exitCode"
                const val EXTRA_PLUGIN_RESULT_BUNDLE_ERR = "err"
                const val EXTRA_PLUGIN_RESULT_BUNDLE_ERRMSG = "errmsg"
            }
        }

        class RUN_COMMAND_SERVICE {

            companion object {
                const val RUN_COMMAND_API_HELP_URL = TERMUX_GITHUB_WIKI_REPO_URL + "/RUN_COMMAND-Intent"
                const val ACTION_RUN_COMMAND = TERMUX_PACKAGE_NAME + ".RUN_COMMAND"
                const val EXTRA_COMMAND_PATH = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_PATH"
                const val EXTRA_ARGUMENTS = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_ARGUMENTS"
                const val EXTRA_REPLACE_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_REPLACE_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS"
                const val EXTRA_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS"
                const val EXTRA_STDIN = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_STDIN"
                const val EXTRA_WORKDIR = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_WORKDIR"
            @Deprecated("")
            // Default: "com.termux.RUN_COMMAND_BACKGROUND"
                const val EXTRA_BACKGROUND = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_BACKGROUND"
                const val EXTRA_RUNNER = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_RUNNER"
                const val EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_BACKGROUND_CUSTOM_LOG_LEVEL"
                const val EXTRA_SESSION_ACTION = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_SESSION_ACTION"
                const val EXTRA_SHELL_NAME = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_SHELL_NAME"
                const val EXTRA_SHELL_CREATE_MODE = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_SHELL_CREATE_MODE"
                const val EXTRA_COMMAND_LABEL = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_COMMAND_LABEL"
                const val EXTRA_COMMAND_DESCRIPTION = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_COMMAND_DESCRIPTION"
                const val EXTRA_COMMAND_HELP = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_COMMAND_HELP"
                const val EXTRA_PENDING_INTENT = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_PENDING_INTENT"
                const val EXTRA_RESULT_DIRECTORY = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_RESULT_DIRECTORY"
                const val EXTRA_RESULT_SINGLE_FILE = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_RESULT_SINGLE_FILE"
                const val EXTRA_RESULT_FILE_BASENAME = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_RESULT_FILE_BASENAME"
                const val EXTRA_RESULT_FILE_OUTPUT_FORMAT = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_RESULT_FILE_OUTPUT_FORMAT"
                const val EXTRA_RESULT_FILE_ERROR_FORMAT = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_RESULT_FILE_ERROR_FORMAT"
                const val EXTRA_RESULT_FILES_SUFFIX = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_RESULT_FILES_SUFFIX"
            }
        }
    }

    class TERMUX_API_APP {

        companion object {
            const val TERMUX_API_ACTIVITY_NAME = TERMUX_API_PACKAGE_NAME + ".activities.TermuxAPIActivity"
        // Default: "com.termux.api.activities.TermuxAPIMainActivity"
            const val TERMUX_API_MAIN_ACTIVITY_NAME = TERMUX_API_PACKAGE_NAME + ".activities.TermuxAPIMainActivity"
        // Default: "com.termux.api.activities.TermuxAPILauncherActivity"
            const val TERMUX_API_LAUNCHER_ACTIVITY_NAME = TERMUX_API_PACKAGE_NAME + ".activities.TermuxAPILauncherActivity"
        }

    }

    class TERMUX_BOOT_APP {

        companion object {
        // Default: "com.termux.boot.activities.TermuxBootMainActivity"
            const val TERMUX_BOOT_MAIN_ACTIVITY_NAME = TERMUX_BOOT_PACKAGE_NAME + ".activities.TermuxBootMainActivity"
        // Default: "com.termux.boot.activities.TermuxBootLauncherActivity"
            const val TERMUX_BOOT_LAUNCHER_ACTIVITY_NAME = TERMUX_BOOT_PACKAGE_NAME + ".activities.TermuxBootLauncherActivity"
        }

    }

    class TERMUX_GUI {

        companion object {
            const val TERMUX_GUI_ACTIVITY_NAME = TERMUX_GUI_PACKAGE_NAME + ".activities.TermuxGUIActivity"
        }
    }

    class TERMUX_FLOAT_APP {

        companion object {
        // Default: "com.termux.window.TermuxFloatActivity"
            const val TERMUX_FLOAT_ACTIVITY_NAME = TERMUX_FLOAT_PACKAGE_NAME + ".TermuxFloatActivity"
        // Default: "com.termux.window.TermuxFloatService"
            const val TERMUX_FLOAT_SERVICE_NAME = TERMUX_FLOAT_PACKAGE_NAME + ".TermuxFloatService"
        }

        class TERMUX_FLOAT_SERVICE {

            companion object {
                const val ACTION_STOP_SERVICE = TERMUX_FLOAT_PACKAGE_NAME + ".ACTION_STOP_SERVICE"
                const val ACTION_SHOW = TERMUX_FLOAT_PACKAGE_NAME + ".ACTION_SHOW"
                const val ACTION_HIDE = TERMUX_FLOAT_PACKAGE_NAME + ".ACTION_HIDE"
            }
        }
    }

    class TERMUX_STYLING_APP {

        companion object {
        // Default: "com.termux.styling.TermuxStyleActivity"
            const val TERMUX_STYLING_ACTIVITY_NAME = TERMUX_STYLING_PACKAGE_NAME + ".TermuxStyleActivity"
        // Default: "com.termux.styling.activities.TermuxStylingMainActivity"
            const val TERMUX_STYLING_MAIN_ACTIVITY_NAME = TERMUX_STYLING_PACKAGE_NAME + ".activities.TermuxStylingMainActivity"
        // Default: "com.termux.styling.activities.TermuxStylingLauncherActivity"
            const val TERMUX_STYLING_LAUNCHER_ACTIVITY_NAME = TERMUX_STYLING_PACKAGE_NAME + ".activities.TermuxStylingLauncherActivity"
        }

    }

    class TERMUX_TASKER_APP {

        companion object {
            const val TERMUX_TASKER_ACTIVITY_NAME = TERMUX_TASKER_PACKAGE_NAME + ".activities.TermuxTaskerActivity"
        // Default: "com.termux.tasker.activities.TermuxTaskerMainActivity"
            const val TERMUX_TASKER_MAIN_ACTIVITY_NAME = TERMUX_TASKER_PACKAGE_NAME + ".activities.TermuxTaskerMainActivity"
        // Default: "com.termux.tasker.activities.TermuxTaskerLauncherActivity"
            const val TERMUX_TASKER_LAUNCHER_ACTIVITY_NAME = TERMUX_TASKER_PACKAGE_NAME + ".activities.TermuxTaskerLauncherActivity"
        }

    }

    class TERMUX_WIDGET_APP {

        companion object {
            const val TERMUX_WIDGET_ACTIVITY_NAME = TERMUX_WIDGET_PACKAGE_NAME + ".activities.TermuxWidgetActivity"
        // Default: "com.termux.widget.activities.TermuxWidgetMainActivity"
            const val TERMUX_WIDGET_MAIN_ACTIVITY_NAME = TERMUX_WIDGET_PACKAGE_NAME + ".activities.TermuxWidgetMainActivity"
        // Default: "com.termux.widget.activities.TermuxWidgetLauncherActivity"
            const val TERMUX_WIDGET_LAUNCHER_ACTIVITY_NAME = TERMUX_WIDGET_PACKAGE_NAME + ".activities.TermuxWidgetLauncherActivity"
            const val EXTRA_TOKEN_NAME = TERMUX_PACKAGE_NAME + ".shortcut.token"
        }

        class TERMUX_WIDGET_PROVIDER {

            companion object {
                const val ACTION_WIDGET_ITEM_CLICKED = TERMUX_WIDGET_PACKAGE_NAME + ".ACTION_WIDGET_ITEM_CLICKED"
                const val ACTION_REFRESH_WIDGET = TERMUX_WIDGET_PACKAGE_NAME + ".ACTION_REFRESH_WIDGET"
                const val EXTRA_FILE_CLICKED = TERMUX_WIDGET_PACKAGE_NAME + ".EXTRA_FILE_CLICKED"
            }
        }
    }
}