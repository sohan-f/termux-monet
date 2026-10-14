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

    /*
     * Termux organization variables.
     */
    /**
     * Termux GitHub organization name
     */
    // Default: "termux"
        const val TERMUX_GITHUB_ORGANIZATION_NAME = "termux"

    /**
     * Termux GitHub organization url
     */
    // Default: "https://github.com/termux"
        const val TERMUX_GITHUB_ORGANIZATION_URL = "https://github.com" + "/" + TERMUX_GITHUB_ORGANIZATION_NAME

    /**
     * F-Droid packages base url
     */
    // Default: "https://f-droid.org/en/packages"
        const val FDROID_PACKAGES_BASE_URL = "https://f-droid.org/en/packages"

    /*
     * Termux and its plugin app and package names and urls.
     */
    /**
     * Termux app name
     */
    // Default: "Termux"
        const val TERMUX_APP_NAME = "Termux"

    /**
     * Termux package name
     */
    // Default: "com.termux"
        const val TERMUX_PACKAGE_NAME = "com.termux"

    /**
     * Termux GitHub repo name
     */
    // Default: "termux-app"
        const val TERMUX_GITHUB_REPO_NAME = "termux-app"

    /**
     * Termux GitHub repo url
     */
    // Default: "https://github.com/termux/termux-app"
        const val TERMUX_GITHUB_REPO_URL = TERMUX_GITHUB_ORGANIZATION_URL + "/" + TERMUX_GITHUB_REPO_NAME

    /**
     * Termux GitHub issues repo url
     */
    // Default: "https://github.com/termux/termux-app/issues"
        const val TERMUX_GITHUB_ISSUES_REPO_URL = TERMUX_GITHUB_REPO_URL + "/issues"

    /**
     * Termux F-Droid package url
     */
    // Default: "https://f-droid.org/en/packages/com.termux"
        const val TERMUX_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + TERMUX_PACKAGE_NAME

    /**
     * Termux:API app name
     */
    // Default: "Termux:API"
        const val TERMUX_API_APP_NAME = "Termux:API"

    /**
     * Termux:API app package name
     */
    // Default: "com.termux.api"
        const val TERMUX_API_PACKAGE_NAME = TERMUX_PACKAGE_NAME + ".api"

    /**
     * Termux:API GitHub repo name
     */
    // Default: "termux-api"
        const val TERMUX_API_GITHUB_REPO_NAME = "termux-api"

    /**
     * Termux:API GitHub repo url
     */
    // Default: "https://github.com/termux/termux-api"
        const val TERMUX_API_GITHUB_REPO_URL = TERMUX_GITHUB_ORGANIZATION_URL + "/" + TERMUX_API_GITHUB_REPO_NAME

    /**
     * Termux:API GitHub issues repo url
     */
    // Default: "https://github.com/termux/termux-api/issues"
        const val TERMUX_API_GITHUB_ISSUES_REPO_URL = TERMUX_API_GITHUB_REPO_URL + "/issues"

    /**
     * Termux:API F-Droid package url
     */
    // Default: "https://f-droid.org/en/packages/com.termux.api"
        const val TERMUX_API_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + TERMUX_API_PACKAGE_NAME

    /**
     * Termux:Boot app name
     */
    // Default: "Termux:Boot"
        const val TERMUX_BOOT_APP_NAME = "Termux:Boot"

    /**
     * Termux:Boot app package name
     */
    // Default: "com.termux.boot"
        const val TERMUX_BOOT_PACKAGE_NAME = TERMUX_PACKAGE_NAME + ".boot"

    /**
     * Termux:Boot GitHub repo name
     */
    // Default: "termux-boot"
        const val TERMUX_BOOT_GITHUB_REPO_NAME = "termux-boot"

    /**
     * Termux:Boot GitHub repo url
     */
    // Default: "https://github.com/termux/termux-boot"
        const val TERMUX_BOOT_GITHUB_REPO_URL = TERMUX_GITHUB_ORGANIZATION_URL + "/" + TERMUX_BOOT_GITHUB_REPO_NAME

    /**
     * Termux:Boot GitHub issues repo url
     */
    // Default: "https://github.com/termux/termux-boot/issues"
        const val TERMUX_BOOT_GITHUB_ISSUES_REPO_URL = TERMUX_BOOT_GITHUB_REPO_URL + "/issues"

    /**
     * Termux:Boot F-Droid package url
     */
    // Default: "https://f-droid.org/en/packages/com.termux.boot"
        const val TERMUX_BOOT_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + TERMUX_BOOT_PACKAGE_NAME

    /**
     * Termux:Float app name
     */
    // Default: "Termux:Float"
        const val TERMUX_FLOAT_APP_NAME = "Termux:Float"

    /**
     * Termux:Float app package name
     */
    // Default: "com.termux.window"
        const val TERMUX_FLOAT_PACKAGE_NAME = TERMUX_PACKAGE_NAME + ".window"

    /**
     * Termux:Float GitHub repo name
     */
    // Default: "termux-float"
        const val TERMUX_FLOAT_GITHUB_REPO_NAME = "termux-float"

    /**
     * Termux:Float GitHub repo url
     */
    // Default: "https://github.com/termux/termux-float"
        const val TERMUX_FLOAT_GITHUB_REPO_URL = TERMUX_GITHUB_ORGANIZATION_URL + "/" + TERMUX_FLOAT_GITHUB_REPO_NAME

    /**
     * Termux:Float GitHub issues repo url
     */
    // Default: "https://github.com/termux/termux-float/issues"
        const val TERMUX_FLOAT_GITHUB_ISSUES_REPO_URL = TERMUX_FLOAT_GITHUB_REPO_URL + "/issues"

    /**
     * Termux:Float F-Droid package url
     */
    // Default: "https://f-droid.org/en/packages/com.termux.window"
        const val TERMUX_FLOAT_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + TERMUX_FLOAT_PACKAGE_NAME

    /**
     * Termux:Styling app name
     */
    // Default: "Termux:Styling"
        const val TERMUX_STYLING_APP_NAME = "Termux:Styling"

    /**
     * Termux:Styling app package name
     */
    // Default: "com.termux.styling"
        const val TERMUX_STYLING_PACKAGE_NAME = TERMUX_PACKAGE_NAME + ".styling"

    /**
     * Termux:Styling GitHub repo name
     */
    // Default: "termux-styling"
        const val TERMUX_STYLING_GITHUB_REPO_NAME = "termux-styling"

    /**
     * Termux:Styling GitHub repo url
     */
    // Default: "https://github.com/termux/termux-styling"
        const val TERMUX_STYLING_GITHUB_REPO_URL = TERMUX_GITHUB_ORGANIZATION_URL + "/" + TERMUX_STYLING_GITHUB_REPO_NAME

    /**
     * Termux:Styling GitHub issues repo url
     */
    // Default: "https://github.com/termux/termux-styling/issues"
        const val TERMUX_STYLING_GITHUB_ISSUES_REPO_URL = TERMUX_STYLING_GITHUB_REPO_URL + "/issues"

    /**
     * Termux:Styling F-Droid package url
     */
    // Default: "https://f-droid.org/en/packages/com.termux.styling"
        const val TERMUX_STYLING_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + TERMUX_STYLING_PACKAGE_NAME

    /**
     * Termux:Tasker app name
     */
    // Default: "Termux:Tasker"
        const val TERMUX_TASKER_APP_NAME = "Termux:Tasker"

    /**
     * Termux:GUI app name
     */
    // Default: "Termux:GUI"
        const val TERMUX_GUI_APP_NAME = "Termux:GUI"

    /**
     * Termux:Tasker app package name
     */
    // Default: "com.termux.tasker"
        const val TERMUX_TASKER_PACKAGE_NAME = TERMUX_PACKAGE_NAME + ".tasker"

    /**
     * Termux:GUI app package name
     */
    // Default: "com.termux.gui"
        const val TERMUX_GUI_PACKAGE_NAME = TERMUX_PACKAGE_NAME + ".gui"

    /**
     * Termux:Tasker GitHub repo name
     */
    // Default: "termux-tasker"
        const val TERMUX_TASKER_GITHUB_REPO_NAME = "termux-tasker"

    /**
     * Termux:GUI GitHub repo name
     */
    // Default: "termux-gui"
        const val TERMUX_GUI_GITHUB_REPO_NAME = "termux-gui"

    /**
     * Termux:Tasker GitHub repo url
     */
    // Default: "https://github.com/termux/termux-tasker"
        const val TERMUX_TASKER_GITHUB_REPO_URL = TERMUX_GITHUB_ORGANIZATION_URL + "/" + TERMUX_TASKER_GITHUB_REPO_NAME

    /**
     * Termux:GUI GitHub repo url
     */
    // Default: "https://github.com/termux/termux-gui"
        const val TERMUX_GUI_GITHUB_REPO_URL = TERMUX_GITHUB_ORGANIZATION_URL + "/" + TERMUX_GUI_GITHUB_REPO_NAME

    /**
     * Termux:Tasker GitHub issues repo url
     */
    // Default: "https://github.com/termux/termux-tasker/issues"
        const val TERMUX_TASKER_GITHUB_ISSUES_REPO_URL = TERMUX_TASKER_GITHUB_REPO_URL + "/issues"

    /**
     * Termux:GUI GitHub issues repo url
     */
    // Default: "https://github.com/termux/termux-gui/issues"
        const val TERMUX_GUI_GITHUB_ISSUES_REPO_URL = TERMUX_GUI_GITHUB_REPO_URL + "/issues"

    /**
     * Termux:Tasker F-Droid package url
     */
    // Default: "https://f-droid.org/en/packages/com.termux.tasker"
        const val TERMUX_TASKER_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + TERMUX_TASKER_PACKAGE_NAME

    /**
     * Termux:GUI F-Droid package url
     */
    // Default: "https://f-droid.org/en/packages/com.termux.gui"
        const val TERMUX_GUI_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + TERMUX_GUI_PACKAGE_NAME

    /**
     * Termux:Widget app name
     */
    // Default: "Termux:Widget"
        const val TERMUX_WIDGET_APP_NAME = "Termux:Widget"

    /**
     * Termux:Widget app package name
     */
    // Default: "com.termux.widget"
        const val TERMUX_WIDGET_PACKAGE_NAME = TERMUX_PACKAGE_NAME + ".widget"

    /**
     * Termux:Widget GitHub repo name
     */
    // Default: "termux-widget"
        const val TERMUX_WIDGET_GITHUB_REPO_NAME = "termux-widget"

    /**
     * Termux:Widget GitHub repo url
     */
    // Default: "https://github.com/termux/termux-widget"
        const val TERMUX_WIDGET_GITHUB_REPO_URL = TERMUX_GITHUB_ORGANIZATION_URL + "/" + TERMUX_WIDGET_GITHUB_REPO_NAME

    /**
     * Termux:Widget GitHub issues repo url
     */
    // Default: "https://github.com/termux/termux-widget/issues"
        const val TERMUX_WIDGET_GITHUB_ISSUES_REPO_URL = TERMUX_WIDGET_GITHUB_REPO_URL + "/issues"

    /**
     * Termux:Widget F-Droid package url
     */
    // Default: "https://f-droid.org/en/packages/com.termux.widget"
        const val TERMUX_WIDGET_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + TERMUX_WIDGET_PACKAGE_NAME

    /*
     * Termux plugin apps lists.
     */
        @JvmField
        val TERMUX_PLUGIN_APP_NAMES_LIST: List<String> = listOf<String>(TERMUX_API_APP_NAME, TERMUX_BOOT_APP_NAME, TERMUX_FLOAT_APP_NAME, TERMUX_STYLING_APP_NAME, TERMUX_TASKER_APP_NAME, TERMUX_WIDGET_APP_NAME, TERMUX_GUI_APP_NAME)

        @JvmField
        val TERMUX_PLUGIN_APP_PACKAGE_NAMES_LIST: List<String>? = listOf<String>(TERMUX_API_PACKAGE_NAME, TERMUX_BOOT_PACKAGE_NAME, TERMUX_FLOAT_PACKAGE_NAME, TERMUX_STYLING_PACKAGE_NAME, TERMUX_TASKER_PACKAGE_NAME, TERMUX_WIDGET_PACKAGE_NAME, TERMUX_GUI_PACKAGE_NAME)

    /*
     * Termux APK releases.
     */
    /**
     * F-Droid APK release
     */
    // Default: "F-Droid"
        const val APK_RELEASE_FDROID = "F-Droid"

    /**
     * F-Droid APK release signing certificate SHA-256 digest
     */
    // Default: "228FB2CFE90831C1499EC3CCAF61E96E8E1CE70766B9474672CE427334D41C42"
        const val APK_RELEASE_FDROID_SIGNING_CERTIFICATE_SHA256_DIGEST = "228FB2CFE90831C1499EC3CCAF61E96E8E1CE70766B9474672CE427334D41C42"

    /**
     * GitHub APK release
     */
    // Default: "GitHub"
        const val APK_RELEASE_GITHUB = "GitHub"

    /**
     * GitHub APK release signing certificate SHA-256 digest
     */
    // Default: "B6DA01480EEFD5FBF2CD3771B8D1021EC791304BDD6C4BF41D3FAABAD48EE5E1"
        const val APK_RELEASE_GITHUB_SIGNING_CERTIFICATE_SHA256_DIGEST = "B6DA01480EEFD5FBF2CD3771B8D1021EC791304BDD6C4BF41D3FAABAD48EE5E1"

    /**
     * Google Play Store APK release
     */
    // Default: "Google Play Store"
        const val APK_RELEASE_GOOGLE_PLAYSTORE = "Google Play Store"

    /**
     * Google Play Store APK release signing certificate SHA-256 digest
     */
    // Default: "738F0A30A04D3C8A1BE304AF18D0779BCF3EA88FB60808F657A3521861C2EBF9"
        const val APK_RELEASE_GOOGLE_PLAYSTORE_SIGNING_CERTIFICATE_SHA256_DIGEST = "738F0A30A04D3C8A1BE304AF18D0779BCF3EA88FB60808F657A3521861C2EBF9"

    /**
     * Termux Devs APK release
     */
    // Default: "Termux Devs"
        const val APK_RELEASE_TERMUX_DEVS = "Termux Devs"

    /**
     * Termux Devs APK release signing certificate SHA-256 digest
     */
    // Default: "F7A038EB551F1BE8FDF388686B784ABAB4552A5D82DF423E3D8F1B5CBE1C69AE"
        const val APK_RELEASE_TERMUX_DEVS_SIGNING_CERTIFICATE_SHA256_DIGEST = "F7A038EB551F1BE8FDF388686B784ABAB4552A5D82DF423E3D8F1B5CBE1C69AE"

    /*
     * Termux packages urls.
     */
    /**
     * Termux Packages GitHub repo name
     */
    // Default: "termux-packages"
        const val TERMUX_PACKAGES_GITHUB_REPO_NAME = "termux-packages"

    /**
     * Termux Packages GitHub repo url
     */
    // Default: "https://github.com/termux/termux-packages"
        const val TERMUX_PACKAGES_GITHUB_REPO_URL = TERMUX_GITHUB_ORGANIZATION_URL + "/" + TERMUX_PACKAGES_GITHUB_REPO_NAME

    /**
     * Termux Packages GitHub issues repo url
     */
    // Default: "https://github.com/termux/termux-packages/issues"
        const val TERMUX_PACKAGES_GITHUB_ISSUES_REPO_URL = TERMUX_PACKAGES_GITHUB_REPO_URL + "/issues"

    /**
     * Termux API apt package name
     */
    // Default: "termux-api"
        const val TERMUX_API_APT_PACKAGE_NAME = "termux-api"

    /**
     * Termux API apt GitHub repo name
     */
    // Default: "termux-api-package"
        const val TERMUX_API_APT_GITHUB_REPO_NAME = "termux-api-package"

    /**
     * Termux API apt GitHub repo url
     */
    // Default: "https://github.com/termux/termux-api-package"
        const val TERMUX_API_APT_GITHUB_REPO_URL = TERMUX_GITHUB_ORGANIZATION_URL + "/" + TERMUX_API_APT_GITHUB_REPO_NAME

    /**
     * Termux API apt GitHub issues repo url
     */
    // Default: "https://github.com/termux/termux-api-package/issues"
        const val TERMUX_API_APT_GITHUB_ISSUES_REPO_URL = TERMUX_API_APT_GITHUB_REPO_URL + "/issues"

    /*
     * Termux miscellaneous urls.
     */
    /**
     * Termux Site
     */
    // Default: "Termux Site"
        const val TERMUX_SITE = TERMUX_APP_NAME + " Site"

    /**
     * Termux Site url
     */
    // Default: "https://termux.dev"
        const val TERMUX_SITE_URL = "https://termux.dev"

    /**
     * Termux Wiki
     */
    // Default: "Termux Wiki"
        const val TERMUX_WIKI = TERMUX_APP_NAME + " Wiki"

    /**
     * Termux Wiki url
     */
    // Default: "https://wiki.termux.com"
        const val TERMUX_WIKI_URL = "https://wiki.termux.com"

    /**
     * Termux GitHub wiki repo url
     */
    // Default: "https://github.com/termux/termux-app/wiki"
        const val TERMUX_GITHUB_WIKI_REPO_URL = TERMUX_GITHUB_REPO_URL + "/wiki"

    /**
     * Termux Packages wiki repo url
     */
    // Default: "https://github.com/termux/termux-packages/wiki"
        const val TERMUX_PACKAGES_GITHUB_WIKI_REPO_URL = TERMUX_PACKAGES_GITHUB_REPO_URL + "/wiki"

    /**
     * Termux support email url
     */
    // Default: "support@termux.dev"
        const val TERMUX_SUPPORT_EMAIL_URL = "support@termux.dev"

    /**
     * Termux support email mailto url
     */
    // Default: "mailto:support@termux.dev"
        const val TERMUX_SUPPORT_EMAIL_MAILTO_URL = "mailto:" + TERMUX_SUPPORT_EMAIL_URL

    /**
     * Termux Reddit subreddit
     */
    // Default: "r/termux"
        const val TERMUX_REDDIT_SUBREDDIT = "r/termux"

    /**
     * Termux Reddit subreddit url
     */
    // Default: "https://www.reddit.com/r/termux"
        const val TERMUX_REDDIT_SUBREDDIT_URL = "https://www.reddit.com/r/termux"

    /**
     * Termux donate url
     */
    // Default: "https://termux.dev/donate"
        const val TERMUX_DONATE_URL = TERMUX_SITE_URL + "/donate"

    /*
     * Termux app core directory paths.
     */
    /**
     * Termux app internal private app data directory path
     */
    @SuppressLint("SdCardPath")
    // Default: "/data/data/com.termux"
        const val TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH = "/data/data/" + TERMUX_PACKAGE_NAME

    /**
     * Termux app internal private app data directory
     */
        @JvmField
        val TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR: File = File(TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH)

    /**
     * Termux app Files directory path
     */
    // Default: "/data/data/com.termux/files"
        const val TERMUX_FILES_DIR_PATH = TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH + "/files"

    /**
     * Termux app Files directory
     */
        @JvmField
        val TERMUX_FILES_DIR: File = File(TERMUX_FILES_DIR_PATH)

    /**
     * Termux app $PREFIX directory path
     */
    // Default: "/data/data/com.termux/files/usr"
        const val TERMUX_PREFIX_DIR_PATH = TERMUX_FILES_DIR_PATH + "/usr"

    /**
     * Termux app $PREFIX directory
     */
        @JvmField
        val TERMUX_PREFIX_DIR: File = File(TERMUX_PREFIX_DIR_PATH)

    /**
     * Termux app $PREFIX/bin directory path
     */
    // Default: "/data/data/com.termux/files/usr/bin"
        const val TERMUX_BIN_PREFIX_DIR_PATH = TERMUX_PREFIX_DIR_PATH + "/bin"

    /**
     * Termux app $PREFIX/bin directory
     */
        @JvmField
        val TERMUX_BIN_PREFIX_DIR: File = File(TERMUX_BIN_PREFIX_DIR_PATH)

    /**
     * Termux app $PREFIX/etc directory path
     */
    // Default: "/data/data/com.termux/files/usr/etc"
        const val TERMUX_ETC_PREFIX_DIR_PATH = TERMUX_PREFIX_DIR_PATH + "/etc"

    /**
     * Termux app $PREFIX/etc directory
     */
        @JvmField
        val TERMUX_ETC_PREFIX_DIR: File = File(TERMUX_ETC_PREFIX_DIR_PATH)

    /**
     * Termux app $PREFIX/include directory path
     */
    // Default: "/data/data/com.termux/files/usr/include"
        const val TERMUX_INCLUDE_PREFIX_DIR_PATH = TERMUX_PREFIX_DIR_PATH + "/include"

    /**
     * Termux app $PREFIX/include directory
     */
        @JvmField
        val TERMUX_INCLUDE_PREFIX_DIR: File = File(TERMUX_INCLUDE_PREFIX_DIR_PATH)

    /**
     * Termux app $PREFIX/lib directory path
     */
    // Default: "/data/data/com.termux/files/usr/lib"
        const val TERMUX_LIB_PREFIX_DIR_PATH = TERMUX_PREFIX_DIR_PATH + "/lib"

    /**
     * Termux app $PREFIX/lib directory
     */
        @JvmField
        val TERMUX_LIB_PREFIX_DIR: File = File(TERMUX_LIB_PREFIX_DIR_PATH)

    /**
     * Termux app $PREFIX/libexec directory path
     */
    // Default: "/data/data/com.termux/files/usr/libexec"
        const val TERMUX_LIBEXEC_PREFIX_DIR_PATH = TERMUX_PREFIX_DIR_PATH + "/libexec"

    /**
     * Termux app $PREFIX/libexec directory
     */
        @JvmField
        val TERMUX_LIBEXEC_PREFIX_DIR: File = File(TERMUX_LIBEXEC_PREFIX_DIR_PATH)

    /**
     * Termux app $PREFIX/share directory path
     */
    // Default: "/data/data/com.termux/files/usr/share"
        const val TERMUX_SHARE_PREFIX_DIR_PATH = TERMUX_PREFIX_DIR_PATH + "/share"

    /**
     * Termux app $PREFIX/share directory
     */
        @JvmField
        val TERMUX_SHARE_PREFIX_DIR: File = File(TERMUX_SHARE_PREFIX_DIR_PATH)

    /**
     * Termux app $PREFIX/tmp and $TMPDIR directory path
     */
    // Default: "/data/data/com.termux/files/usr/tmp"
        const val TERMUX_TMP_PREFIX_DIR_PATH = TERMUX_PREFIX_DIR_PATH + "/tmp"

    /**
     * Termux app $PREFIX/tmp and $TMPDIR directory
     */
        @JvmField
        val TERMUX_TMP_PREFIX_DIR: File = File(TERMUX_TMP_PREFIX_DIR_PATH)

    /**
     * Termux app $PREFIX/var directory path
     */
    // Default: "/data/data/com.termux/files/usr/var"
        const val TERMUX_VAR_PREFIX_DIR_PATH = TERMUX_PREFIX_DIR_PATH + "/var"

    /**
     * Termux app $PREFIX/var directory
     */
        @JvmField
        val TERMUX_VAR_PREFIX_DIR: File = File(TERMUX_VAR_PREFIX_DIR_PATH)

    /**
     * Termux app usr-staging directory path
     */
    // Default: "/data/data/com.termux/files/usr-staging"
        const val TERMUX_STAGING_PREFIX_DIR_PATH = TERMUX_FILES_DIR_PATH + "/usr-staging"

    /**
     * Termux app usr-staging directory
     */
        @JvmField
        val TERMUX_STAGING_PREFIX_DIR: File = File(TERMUX_STAGING_PREFIX_DIR_PATH)

    /**
     * Termux app $HOME directory path
     */
    // Default: "/data/data/com.termux/files/home"
        const val TERMUX_HOME_DIR_PATH = TERMUX_FILES_DIR_PATH + "/home"

    /**
     * Termux app $HOME directory
     */
        @JvmField
        val TERMUX_HOME_DIR: File = File(TERMUX_HOME_DIR_PATH)

    /**
     * Termux app config home directory path
     */
    // Default: "/data/data/com.termux/files/home/.config/termux"
        const val TERMUX_CONFIG_HOME_DIR_PATH = TERMUX_HOME_DIR_PATH + "/.config/termux"

    /**
     * Termux app config home directory
     */
        @JvmField
        val TERMUX_CONFIG_HOME_DIR: File = File(TERMUX_CONFIG_HOME_DIR_PATH)

    /**
     * Termux app config $PREFIX directory path
     */
    // Default: "/data/data/com.termux/files/usr/etc/termux"
        const val TERMUX_CONFIG_PREFIX_DIR_PATH = TERMUX_ETC_PREFIX_DIR_PATH + "/termux"

    /**
     * Termux app config $PREFIX directory
     */
        @JvmField
        val TERMUX_CONFIG_PREFIX_DIR: File = File(TERMUX_CONFIG_PREFIX_DIR_PATH)

    /**
     * Termux app data home directory path
     */
    // Default: "/data/data/com.termux/files/home/.termux"
        const val TERMUX_DATA_HOME_DIR_PATH = TERMUX_HOME_DIR_PATH + "/.termux"

    /**
     * Termux app data home directory
     */
        @JvmField
        val TERMUX_DATA_HOME_DIR: File = File(TERMUX_DATA_HOME_DIR_PATH)

    /**
     * Termux app storage home directory path
     */
    // Default: "/data/data/com.termux/files/home/storage"
        const val TERMUX_STORAGE_HOME_DIR_PATH = TERMUX_HOME_DIR_PATH + "/storage"

    /**
     * Termux app storage home directory
     */
        @JvmField
        val TERMUX_STORAGE_HOME_DIR: File = File(TERMUX_STORAGE_HOME_DIR_PATH)

    /**
     * Termux app background directory path
     */
    // Default: "/data/data/com.termux/files/.termux/background"
        const val TERMUX_BACKGROUND_DIR_PATH = TERMUX_DATA_HOME_DIR_PATH + "/background"

    /**
     * Termux app background directory
     */
        @JvmField
        val TERMUX_BACKGROUND_DIR: File = File(TERMUX_BACKGROUND_DIR_PATH)

    /**
     * Termux app backgorund original image file path
     */
    // Default: "/data/data/com.termux/files/home/.termux/background.jpeg"
        const val TERMUX_BACKGROUND_IMAGE_PATH = TERMUX_BACKGROUND_DIR_PATH + "/background.jpeg"

    /**
     * Termux app backgorund original image file
     */
        @JvmField
        val TERMUX_BACKGROUND_IMAGE_FILE: File = File(TERMUX_BACKGROUND_IMAGE_PATH)

    /**
     * Termux app portrait backgorund image file path
     */
    // Default: "/data/data/com.termux/files/home/.termux/background/background_portrait.jpeg"
        const val TERMUX_BACKGROUND_IMAGE_PORTRAIT_PATH = TERMUX_BACKGROUND_DIR_PATH + "/background_portrait.jpeg"

    /**
     * Termux app portrait backgorund image file
     */
        @JvmField
        val TERMUX_BACKGROUND_IMAGE_PORTRAIT_FILE: File = File(TERMUX_BACKGROUND_IMAGE_PORTRAIT_PATH)

    /**
     * Termux app landscape backgorund image file path
     */
    // Default: "/data/data/com.termux/files/home/.termux/background/background_landscape.jpeg"
        const val TERMUX_BACKGROUND_IMAGE_LANDSCAPE_PATH = TERMUX_BACKGROUND_DIR_PATH + "/background_landscape.jpeg"

    /**
     * Termux app landscape backgorund image file
     */
        @JvmField
        val TERMUX_BACKGROUND_IMAGE_LANDSCAPE_FILE: File = File(TERMUX_BACKGROUND_IMAGE_LANDSCAPE_PATH)

    /**
     * Termux and plugin apps directory path
     */
    // Default: "/data/data/com.termux/files/apps"
        const val TERMUX_APPS_DIR_PATH = TERMUX_FILES_DIR_PATH + "/apps"

    /**
     * Termux and plugin apps directory
     */
        @JvmField
        val TERMUX_APPS_DIR: File = File(TERMUX_APPS_DIR_PATH)

    /**
     * Termux app $PREFIX directory path ignored sub file paths to consider it empty
     */
        @JvmField
        val TERMUX_PREFIX_DIR_IGNORED_SUB_FILES_PATHS_TO_CONSIDER_AS_EMPTY: List<String> = listOf<String>(TermuxConstants.TERMUX_TMP_PREFIX_DIR_PATH, TermuxConstants.TERMUX_ENV_TEMP_FILE_PATH, TermuxConstants.TERMUX_ENV_FILE_PATH)

    /*
     * Termux app and plugin preferences and properties file paths.
     */
    /**
     * Termux app default SharedPreferences file basename without extension
     */
    // Default: "com.termux_preferences"
        const val TERMUX_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = TERMUX_PACKAGE_NAME + "_preferences"

    /**
     * Termux:API app default SharedPreferences file basename without extension
     */
    // Default: "com.termux.api_preferences"
        const val TERMUX_API_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = TERMUX_API_PACKAGE_NAME + "_preferences"

    /**
     * Termux:Boot app default SharedPreferences file basename without extension
     */
    // Default: "com.termux.boot_preferences"
        const val TERMUX_BOOT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = TERMUX_BOOT_PACKAGE_NAME + "_preferences"

    /**
     * Termux:Float app default SharedPreferences file basename without extension
     */
    // Default: "com.termux.window_preferences"
        const val TERMUX_FLOAT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = TERMUX_FLOAT_PACKAGE_NAME + "_preferences"

    /**
     * Termux:Styling app default SharedPreferences file basename without extension
     */
    // Default: "com.termux.styling_preferences"
        const val TERMUX_STYLING_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = TERMUX_STYLING_PACKAGE_NAME + "_preferences"

    /**
     * Termux:Tasker app default SharedPreferences file basename without extension
     */
    // Default: "com.termux.tasker_preferences"
        const val TERMUX_TASKER_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = TERMUX_TASKER_PACKAGE_NAME + "_preferences"

    /**
     * Termux:Widget app default SharedPreferences file basename without extension
     */
    // Default: "com.termux.widget_preferences"
        const val TERMUX_WIDGET_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = TERMUX_WIDGET_PACKAGE_NAME + "_preferences"

    /**
     * Termux:GUI app default SharedPreferences file basename without extension
     */
    // Default: "com.termux.gui_preferences"
        const val TERMUX_GUI_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = TERMUX_GUI_PACKAGE_NAME + "_preferences"

    /**
     * Termux app properties primary file path
     */
    // Default: "/data/data/com.termux/files/home/.termux/termux.properties"
        const val TERMUX_PROPERTIES_PRIMARY_FILE_PATH = TERMUX_DATA_HOME_DIR_PATH + "/termux.properties"

    /**
     * Termux app properties primary file
     */
        @JvmField
        val TERMUX_PROPERTIES_PRIMARY_FILE: File = File(TERMUX_PROPERTIES_PRIMARY_FILE_PATH)

    /**
     * Termux app properties secondary file path
     */
    // Default: "/data/data/com.termux/files/home/.config/termux/termux.properties"
        const val TERMUX_PROPERTIES_SECONDARY_FILE_PATH = TERMUX_CONFIG_HOME_DIR_PATH + "/termux.properties"

    /**
     * Termux app properties secondary file
     */
        @JvmField
        val TERMUX_PROPERTIES_SECONDARY_FILE: File = File(TERMUX_PROPERTIES_SECONDARY_FILE_PATH)

    /**
     * Termux app properties file paths list. **DO NOT** allow these files to be modified by
     * {@link android.content.ContentProvider} exposed to external apps, since they may silently
     * modify the values for security properties like {@link #PROP_ALLOW_EXTERNAL_APPS} set by users
     * without their explicit consent.
     */
        @JvmField
        val TERMUX_PROPERTIES_FILE_PATHS_LIST: List<String> = listOf<String>(TERMUX_PROPERTIES_PRIMARY_FILE_PATH, TERMUX_PROPERTIES_SECONDARY_FILE_PATH)

    /**
     * Termux:Float app properties primary file path
     */
    // Default: "/data/data/com.termux/files/home/.termux/termux.float.properties"
        const val TERMUX_FLOAT_PROPERTIES_PRIMARY_FILE_PATH = TERMUX_DATA_HOME_DIR_PATH + "/termux.float.properties"

    /**
     * Termux:Float app properties primary file
     */
        @JvmField
        val TERMUX_FLOAT_PROPERTIES_PRIMARY_FILE: File = File(TERMUX_FLOAT_PROPERTIES_PRIMARY_FILE_PATH)

    /**
     * Termux:Float app properties secondary file path
     */
    // Default: "/data/data/com.termux/files/home/.config/termux/termux.float.properties"
        const val TERMUX_FLOAT_PROPERTIES_SECONDARY_FILE_PATH = TERMUX_CONFIG_HOME_DIR_PATH + "/termux.float.properties"

    /**
     * Termux:Float app properties secondary file
     */
        @JvmField
        val TERMUX_FLOAT_PROPERTIES_SECONDARY_FILE: File = File(TERMUX_FLOAT_PROPERTIES_SECONDARY_FILE_PATH)

    /**
     * Termux:Float app properties file paths list. **DO NOT** allow these files to be modified by
     * {@link android.content.ContentProvider} exposed to external apps, since they may silently
     * modify the values for security properties like {@link #PROP_ALLOW_EXTERNAL_APPS} set by users
     * without their explicit consent.
     */
        @JvmField
        val TERMUX_FLOAT_PROPERTIES_FILE_PATHS_LIST: List<String> = listOf<String>(TERMUX_FLOAT_PROPERTIES_PRIMARY_FILE_PATH, TERMUX_FLOAT_PROPERTIES_SECONDARY_FILE_PATH)

    /**
     * Termux app and Termux:Styling colors.properties file path
     */
    // Default: "/data/data/com.termux/files/home/.termux/colors.properties"
        const val TERMUX_COLOR_PROPERTIES_FILE_PATH = TERMUX_DATA_HOME_DIR_PATH + "/colors.properties"

    /**
     * Termux app and Termux:Styling colors.properties file
     */
        @JvmField
        val TERMUX_COLOR_PROPERTIES_FILE: File = File(TERMUX_COLOR_PROPERTIES_FILE_PATH)

    /**
     * Termux app and Termux:Styling font.ttf file path
     */
    // Default: "/data/data/com.termux/files/home/.termux/font.ttf"
        const val TERMUX_FONT_FILE_PATH = TERMUX_DATA_HOME_DIR_PATH + "/font.ttf"

    /**
     * Termux app and Termux:Styling font.ttf file
     */
        @JvmField
        val TERMUX_FONT_FILE: File = File(TERMUX_FONT_FILE_PATH)

    /**
     * Termux app only font-italic.ttf file path
     */
    // Default: "/data/data/com.termux/files/home/.termux/font-italic.ttf"
        const val TERMUX_ITALIC_FONT_FILE_PATH = TERMUX_DATA_HOME_DIR_PATH + "/font-italic.ttf"

    /**
     * Termux app only font-italic.ttf file
     */
        @JvmField
        val TERMUX_ITALIC_FONT_FILE: File = File(TERMUX_ITALIC_FONT_FILE_PATH)

    /**
     * Termux app and plugins crash log file path
     */
    // Default: "/data/data/com.termux/files/home/crash_log.md"
        const val TERMUX_CRASH_LOG_FILE_PATH = TERMUX_HOME_DIR_PATH + "/crash_log.md"

    /**
     * Termux app and plugins crash log backup file path
     */
    // Default: "/data/data/com.termux/files/home/crash_log_backup.md"
        const val TERMUX_CRASH_LOG_BACKUP_FILE_PATH = TERMUX_HOME_DIR_PATH + "/crash_log_backup.md"

    /**
     * Termux app environment file path
     */
    // Default: "/data/data/com.termux/files/usr/etc/termux/termux.env"
        const val TERMUX_ENV_FILE_PATH = TERMUX_CONFIG_PREFIX_DIR_PATH + "/termux.env"

    /**
     * Termux app environment temp file path
     */
    // Default: "/data/data/com.termux/files/usr/etc/termux/termux.env.tmp"
        const val TERMUX_ENV_TEMP_FILE_PATH = TERMUX_CONFIG_PREFIX_DIR_PATH + "/termux.env.tmp"

    /*
     * Termux app plugin specific paths.
     */
    /**
     * Termux app directory path to store scripts to be run at boot by Termux:Boot
     */
    // Default: "/data/data/com.termux/files/home/.termux/boot"
        const val TERMUX_BOOT_SCRIPTS_DIR_PATH = TERMUX_DATA_HOME_DIR_PATH + "/boot"

    /**
     * Termux app directory to store scripts to be run at boot by Termux:Boot
     */
        @JvmField
        val TERMUX_BOOT_SCRIPTS_DIR: File = File(TERMUX_BOOT_SCRIPTS_DIR_PATH)

    /**
     * Termux app directory path to store foreground scripts that can be run by the termux launcher
     * widget provided by Termux:Widget
     */
    // Default: "/data/data/com.termux/files/home/.shortcuts"
        const val TERMUX_SHORTCUT_SCRIPTS_DIR_PATH = TERMUX_HOME_DIR_PATH + "/.shortcuts"

    /**
     * Termux app directory to store foreground scripts that can be run by the termux launcher widget provided by Termux:Widget
     */
        @JvmField
        val TERMUX_SHORTCUT_SCRIPTS_DIR: File = File(TERMUX_SHORTCUT_SCRIPTS_DIR_PATH)

    /**
     * Termux app directory basename that stores background scripts that can be run by the termux
     * launcher widget provided by Termux:Widget
     */
    // Default: "tasks"
        const val TERMUX_SHORTCUT_TASKS_SCRIPTS_DIR_BASENAME = "tasks"

    /**
     * Termux app directory path to store background scripts that can be run by the termux launcher
     * widget provided by Termux:Widget
     */
    // Default: "/data/data/com.termux/files/home/.shortcuts/tasks"
        const val TERMUX_SHORTCUT_TASKS_SCRIPTS_DIR_PATH = TERMUX_SHORTCUT_SCRIPTS_DIR_PATH + "/" + TERMUX_SHORTCUT_TASKS_SCRIPTS_DIR_BASENAME

    /**
     * Termux app directory to store background scripts that can be run by the termux launcher widget provided by Termux:Widget
     */
        @JvmField
        val TERMUX_SHORTCUT_TASKS_SCRIPTS_DIR: File = File(TERMUX_SHORTCUT_TASKS_SCRIPTS_DIR_PATH)

    /**
     * Termux app directory basename that stores icons for the foreground and background scripts
     * that can be run by the termux launcher widget provided by Termux:Widget
     */
    // Default: "icons"
        const val TERMUX_SHORTCUT_SCRIPT_ICONS_DIR_BASENAME = "icons"

    /**
     * Termux app directory path to store icons for the foreground and background scripts that can
     * be run by the termux launcher widget provided by Termux:Widget
     */
    // Default: "/data/data/com.termux/files/home/.shortcuts/icons"
        const val TERMUX_SHORTCUT_SCRIPT_ICONS_DIR_PATH = TERMUX_SHORTCUT_SCRIPTS_DIR_PATH + "/" + TERMUX_SHORTCUT_SCRIPT_ICONS_DIR_BASENAME

    /**
     * Termux app directory to store icons for the foreground and background scripts that can be
     * run by the termux launcher widget provided by Termux:Widget
     */
        @JvmField
        val TERMUX_SHORTCUT_SCRIPT_ICONS_DIR: File = File(TERMUX_SHORTCUT_SCRIPT_ICONS_DIR_PATH)

    /**
     * Termux app directory path to store scripts to be run by 3rd party twofortyfouram locale plugin
     * host apps like Tasker app via the Termux:Tasker plugin client
     */
    // Default: "/data/data/com.termux/files/home/.termux/tasker"
        const val TERMUX_TASKER_SCRIPTS_DIR_PATH = TERMUX_DATA_HOME_DIR_PATH + "/tasker"

    /**
     * Termux app directory to store scripts to be run by 3rd party twofortyfouram locale plugin host apps like Tasker app via the Termux:Tasker plugin client
     */
        @JvmField
        val TERMUX_TASKER_SCRIPTS_DIR: File = File(TERMUX_TASKER_SCRIPTS_DIR_PATH)

    /*
     * Termux app and plugins notification variables.
     */
    /**
     * Termux app notification channel id used by {@link TERMUX_APP.TERMUX_SERVICE}
     */
        const val TERMUX_APP_NOTIFICATION_CHANNEL_ID = "termux_notification_channel"

    /**
     * Termux app notification channel name used by {@link TERMUX_APP.TERMUX_SERVICE}
     */
        const val TERMUX_APP_NOTIFICATION_CHANNEL_NAME = TermuxConstants.TERMUX_APP_NAME + " App"

    /**
     * Termux app unique notification id used by {@link TERMUX_APP.TERMUX_SERVICE}
     */
        const val TERMUX_APP_NOTIFICATION_ID = 1337

    /**
     * Termux app notification channel id used by {@link TERMUX_APP.RUN_COMMAND_SERVICE}
     */
        const val TERMUX_RUN_COMMAND_NOTIFICATION_CHANNEL_ID = "termux_run_command_notification_channel"

    /**
     * Termux app notification channel name used by {@link TERMUX_APP.RUN_COMMAND_SERVICE}
     */
        const val TERMUX_RUN_COMMAND_NOTIFICATION_CHANNEL_NAME = TermuxConstants.TERMUX_APP_NAME + " RunCommandService"

    /**
     * Termux app unique notification id used by {@link TERMUX_APP.RUN_COMMAND_SERVICE}
     */
        const val TERMUX_RUN_COMMAND_NOTIFICATION_ID = 1338

    /**
     * Termux app notification channel id used for plugin command errors
     */
        const val TERMUX_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_ID = "termux_plugin_command_errors_notification_channel"

    /**
     * Termux app notification channel name used for plugin command errors
     */
        const val TERMUX_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_NAME = TermuxConstants.TERMUX_APP_NAME + " Plugin Commands Errors"

    /**
     * Termux app notification channel id used for crash reports
     */
        const val TERMUX_CRASH_REPORTS_NOTIFICATION_CHANNEL_ID = "termux_crash_reports_notification_channel"

    /**
     * Termux app notification channel name used for crash reports
     */
        const val TERMUX_CRASH_REPORTS_NOTIFICATION_CHANNEL_NAME = TermuxConstants.TERMUX_APP_NAME + " Crash Reports"

    /**
     * Termux app notification channel id used by {@link TERMUX_FLOAT_APP.TERMUX_FLOAT_SERVICE}
     */
        const val TERMUX_FLOAT_APP_NOTIFICATION_CHANNEL_ID = "termux_float_notification_channel"

    /**
     * Termux app notification channel name used by {@link TERMUX_FLOAT_APP.TERMUX_FLOAT_SERVICE}
     */
        const val TERMUX_FLOAT_APP_NOTIFICATION_CHANNEL_NAME = TermuxConstants.TERMUX_FLOAT_APP_NAME + " App"

    /**
     * Termux app unique notification id used by {@link TERMUX_APP.TERMUX_SERVICE}
     */
        const val TERMUX_FLOAT_APP_NOTIFICATION_ID = 1339

    /*
     * Termux app and plugins miscellaneous variables.
     */
    /**
     * Android OS permission declared by Termux app in AndroidManifest.xml which can be requested by
     * 3rd party apps to run various commands in Termux app context
     */
    // Default: "com.termux.permission.RUN_COMMAND"
        const val PERMISSION_RUN_COMMAND = TERMUX_PACKAGE_NAME + ".permission.RUN_COMMAND"

    /**
     * Termux property defined in termux.properties file as a secondary check to PERMISSION_RUN_COMMAND
     * to allow 3rd party apps to run various commands in Termux app context
     */
    // Default: "allow-external-apps"
        const val PROP_ALLOW_EXTERNAL_APPS = "allow-external-apps"

    /**
     * Default value for {@link #PROP_ALLOW_EXTERNAL_APPS}
     */
    // Default: "false"
        const val PROP_DEFAULT_VALUE_ALLOW_EXTERNAL_APPS = "false"

    /**
     * The broadcast action sent when Termux App opens
     */
        const val BROADCAST_TERMUX_OPENED = TERMUX_PACKAGE_NAME + ".app.OPENED"

    /**
     * The Uri authority for Termux app file shares
     */
    // Default: "com.termux.files"
        const val TERMUX_FILE_SHARE_URI_AUTHORITY = TERMUX_PACKAGE_NAME + ".files"

    /**
     * The normal comma character (U+002C, &comma;, &#44;, comma)
     */
    // Default: ","
        const val COMMA_NORMAL = ","

    /**
     * The alternate comma character (U+201A, &sbquo;, &#8218;, single low-9 quotation mark) that
     * may be used instead of {@link #COMMA_NORMAL}
     */
    // Default: "‚"
        const val COMMA_ALTERNATIVE = "‚"

    /**
     * Environment variable prefix root for the Termux app.
     */
        const val TERMUX_ENV_PREFIX_ROOT = "TERMUX"
    }






    /**
     * Termux app constants.
     */

    class TERMUX_APP {

        companion object {

        /**
         * Termux apps directory path
         */
        // Default: "/data/data/com.termux/files/apps/com.termux"
            const val APPS_DIR_PATH = TERMUX_APPS_DIR_PATH + "/" + TERMUX_PACKAGE_NAME

        /**
         * termux-am socket file path
         */
        // Default: "/data/data/com.termux/files/apps/com.termux/termux-am/am.sock"
            const val TERMUX_AM_SOCKET_FILE_PATH = APPS_DIR_PATH + "/termux-am/am.sock"

        /**
         * Termux app BuildConfig class name
         */
        // Default: "com.termux.BuildConfig"
            const val BUILD_CONFIG_CLASS_NAME = TERMUX_PACKAGE_NAME + ".BuildConfig"

        /**
         * Termux app FileShareReceiverActivity class name
         */
        // Default: "com.termux.app.api.file.FileShareReceiverActivity"
            const val FILE_SHARE_RECEIVER_ACTIVITY_CLASS_NAME = TERMUX_PACKAGE_NAME + ".app.api.file.FileShareReceiverActivity"

        /**
         * Termux app FileViewReceiverActivity class name
         */
        // Default: "com.termux.app.api.file.FileViewReceiverActivity"
            const val FILE_VIEW_RECEIVER_ACTIVITY_CLASS_NAME = TERMUX_PACKAGE_NAME + ".app.api.file.FileViewReceiverActivity"

        /**
         * Termux app core activity name.
         */
        // Default: "com.termux.app.TermuxActivity"
            const val TERMUX_ACTIVITY_NAME = TERMUX_PACKAGE_NAME + ".app.TermuxActivity"

        /**
         * Termux app settings activity name.
         */
        // Default: "com.termux.app.activities.SettingsActivity"
            const val TERMUX_SETTINGS_ACTIVITY_NAME = TERMUX_PACKAGE_NAME + ".app.activities.SettingsActivity"

        /**
         * Termux app core service name.
         */
        // Default: "com.termux.app.TermuxService"
            const val TERMUX_SERVICE_NAME = TERMUX_PACKAGE_NAME + ".app.TermuxService"

        /**
         * Termux app run command service name.
         */
        // Termux app service to receive commands from 3rd party apps "com.termux.app.RunCommandService"
            const val RUN_COMMAND_SERVICE_NAME = TERMUX_PACKAGE_NAME + ".app.RunCommandService"
        }

        /**
         * Termux app core activity.
         */

        class TERMUX_ACTIVITY {

            companion object {

            /**
             * Intent extra for if termux failsafe session needs to be started and is used by {@link TERMUX_ACTIVITY} and {@link TERMUX_SERVICE#ACTION_STOP_SERVICE}
             */
            // Default: "com.termux.app.failsafe_session"
                const val EXTRA_FAILSAFE_SESSION = TermuxConstants.TERMUX_PACKAGE_NAME + ".app.failsafe_session"

            /**
             * Intent action to make termux app notify user that a crash happened.
             */
            // Default: "com.termux.app.notify_app_crash"
                const val ACTION_NOTIFY_APP_CRASH = TermuxConstants.TERMUX_PACKAGE_NAME + ".app.notify_app_crash"

            /**
             * Intent action to make termux reload its termux session styling
             */
            // Default: "com.termux.app.reload_style"
                const val ACTION_RELOAD_STYLE = TermuxConstants.TERMUX_PACKAGE_NAME + ".app.reload_style"

            /**
             * Intent {@code String} extra for what to reload for the TERMUX_ACTIVITY.ACTION_RELOAD_STYLE intent. This has been deperecated.
             */
            @Deprecated("")
            // Default: "com.termux.app.reload_style"
                const val EXTRA_RELOAD_STYLE = TermuxConstants.TERMUX_PACKAGE_NAME + ".app.reload_style"

            /**
             *  Intent {@code boolean} extra for whether to recreate activity for the TERMUX_ACTIVITY.ACTION_RELOAD_STYLE intent.
             */
            // Default: "com.termux.app.TermuxActivity.EXTRA_RECREATE_ACTIVITY"
                const val EXTRA_RECREATE_ACTIVITY = TERMUX_APP.TERMUX_ACTIVITY_NAME + ".EXTRA_RECREATE_ACTIVITY"

            /**
             * Intent action to make termux request storage permissions
             */
            // Default: "com.termux.app.request_storage_permissions"
                const val ACTION_REQUEST_PERMISSIONS = TermuxConstants.TERMUX_PACKAGE_NAME + ".app.request_storage_permissions"
            }
        }

        /**
         * Termux app core service.
         */

        class TERMUX_SERVICE {

            companion object {

            /**
             * Intent action to stop TERMUX_SERVICE
             */
            // Default: "com.termux.service_stop"
                const val ACTION_STOP_SERVICE = TERMUX_PACKAGE_NAME + ".service_stop"

            /**
             * Intent action to make TERMUX_SERVICE acquire a wakelock
             */
            // Default: "com.termux.service_wake_lock"
                const val ACTION_WAKE_LOCK = TERMUX_PACKAGE_NAME + ".service_wake_lock"

            /**
             * Intent action to make TERMUX_SERVICE release wakelock
             */
            // Default: "com.termux.service_wake_unlock"
                const val ACTION_WAKE_UNLOCK = TERMUX_PACKAGE_NAME + ".service_wake_unlock"

            /** Intent action to execute command with TERMUX_SERVICE */
            // Default: "com.termux.service_execute"
                const val ACTION_SERVICE_EXECUTE = TERMUX_PACKAGE_NAME + ".service_execute"

            /** Intent action to stop command execution with TERMUX_SERVICE */
            // Default: "com.termux.service_execution_stop"
                const val ACTION_SERVICE_STOP = TERMUX_PACKAGE_NAME + ".service_execution_stop"

            /**
             * Uri scheme for paths sent via intent to TERMUX_SERVICE
             */
            // Default: "com.termux.file"
                const val URI_SCHEME_SERVICE_EXECUTE = TERMUX_PACKAGE_NAME + ".file"

            /**
             * Intent {@code String[]} extra for arguments to the executable of the command for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent
             */
            // Default: "com.termux.execute.arguments"
                const val EXTRA_ARGUMENTS = TERMUX_PACKAGE_NAME + ".execute.arguments"

            /**
             * Intent {@code String} extra for stdin of the command for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent
             */
            // Default: "com.termux.execute.stdin"
                const val EXTRA_STDIN = TERMUX_PACKAGE_NAME + ".execute.stdin"

            /**
             * Intent {@code String} extra for command current working directory for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent
             */
            // Default: "com.termux.execute.cwd"
                const val EXTRA_WORKDIR = TERMUX_PACKAGE_NAME + ".execute.cwd"

            /**
             * Intent {@code boolean} extra for whether to run command in background {@link Runner#APP_SHELL} or foreground {@link Runner#TERMINAL_SESSION} for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent
             */
            @Deprecated("")
            // Default: "com.termux.execute.background"
                const val EXTRA_BACKGROUND = TERMUX_PACKAGE_NAME + ".execute.background"

            /**
             * Intent {@code String} extra for command the {@link Runner} for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent
             */
            // Default: "com.termux.execute.runner"
                const val EXTRA_RUNNER = TERMUX_PACKAGE_NAME + ".execute.runner"

            /**
             * Intent {@code String} extra for custom log level for background commands defined by {@link com.termux.shared.logger.Logger} for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent
             */
            // Default: "com.termux.execute.background_custom_log_level"
                const val EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL = TERMUX_PACKAGE_NAME + ".execute.background_custom_log_level"

            /**
             * Intent {@code String} extra for session action for {@link Runner#TERMINAL_SESSION} commands for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent
             */
            // Default: "com.termux.execute.session_action"
                const val EXTRA_SESSION_ACTION = TERMUX_PACKAGE_NAME + ".execute.session_action"

            /**
             * Intent {@code String} extra for shell name for commands for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent
             */
            // Default: "com.termux.execute.shell_name"
                const val EXTRA_SHELL_NAME = TERMUX_PACKAGE_NAME + ".execute.shell_name"

            /**
             * Intent {@code String} extra for the {@link ExecutionCommand.ShellCreateMode}  for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent.
             */
            // Default: "com.termux.execute.shell_create_mode"
                const val EXTRA_SHELL_CREATE_MODE = TERMUX_PACKAGE_NAME + ".execute.shell_create_mode"

            /**
             * Intent {@code String} extra for label of the command for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent
             */
            // Default: "com.termux.execute.command_label"
                const val EXTRA_COMMAND_LABEL = TERMUX_PACKAGE_NAME + ".execute.command_label"

            /**
             * Intent markdown {@code String} extra for description of the command for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent
             */
            // Default: "com.termux.execute.command_description"
                const val EXTRA_COMMAND_DESCRIPTION = TERMUX_PACKAGE_NAME + ".execute.command_description"

            /**
             * Intent markdown {@code String} extra for help of the command for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent
             */
            // Default: "com.termux.execute.command_help"
                const val EXTRA_COMMAND_HELP = TERMUX_PACKAGE_NAME + ".execute.command_help"

            /**
             * Intent markdown {@code String} extra for help of the plugin API for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent (Internal Use Only)
             */
            // Default: "com.termux.execute.plugin_help"
                const val EXTRA_PLUGIN_API_HELP = TERMUX_PACKAGE_NAME + ".execute.plugin_api_help"

            /**
             * Intent {@code Parcelable} extra for the pending intent that should be sent with the
             * result of the execution command to the execute command caller for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent
             */
            // Default: "pendingIntent"
                const val EXTRA_PENDING_INTENT = "pendingIntent"

            /**
             * Intent {@code String} extra for the directory path in which to write the result of the
             * execution command for the execute command caller for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent
             */
            // Default: "com.termux.execute.result_directory"
                const val EXTRA_RESULT_DIRECTORY = TERMUX_PACKAGE_NAME + ".execute.result_directory"

            /**
             * Intent {@code boolean} extra for whether the result should be written to a single file
             * or multiple files (err, errmsg, stdout, stderr, exit_code) in
             * {@link #EXTRA_RESULT_DIRECTORY} for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent
             */
            // Default: "com.termux.execute.result_single_file"
                const val EXTRA_RESULT_SINGLE_FILE = TERMUX_PACKAGE_NAME + ".execute.result_single_file"

            /**
             * Intent {@code String} extra for the basename of the result file that should be created
             * in {@link #EXTRA_RESULT_DIRECTORY} if {@link #EXTRA_RESULT_SINGLE_FILE} is {@code true}
             * for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent
             */
            // Default: "com.termux.execute.result_file_basename"
                const val EXTRA_RESULT_FILE_BASENAME = TERMUX_PACKAGE_NAME + ".execute.result_file_basename"

            /**
             * Intent {@code String} extra for the output {@link Formatter} format of the
             * {@link #EXTRA_RESULT_FILE_BASENAME} result file for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent
             */
            // Default: "com.termux.execute.result_file_output_format"
                const val EXTRA_RESULT_FILE_OUTPUT_FORMAT = TERMUX_PACKAGE_NAME + ".execute.result_file_output_format"

            /**
             * Intent {@code String} extra for the error {@link Formatter} format of the
             * {@link #EXTRA_RESULT_FILE_BASENAME} result file for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent
             */
            // Default: "com.termux.execute.result_file_error_format"
                const val EXTRA_RESULT_FILE_ERROR_FORMAT = TERMUX_PACKAGE_NAME + ".execute.result_file_error_format"

            /**
             * Intent {@code String} extra for the optional suffix of the result files that should
             * be created in {@link #EXTRA_RESULT_DIRECTORY} if {@link #EXTRA_RESULT_SINGLE_FILE} is
             * {@code false} for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            // Default: "com.termux.execute.result_files_suffix"
                const val EXTRA_RESULT_FILES_SUFFIX = TERMUX_PACKAGE_NAME + ".execute.result_files_suffix"
            /** Intent {@code long} extra for the delay between SIGTERM and SIGKILL
             * for the TERMUX_SERVICE.ACTION_SERVICE_STOP intent */
            // Default: "com.termux.execute.sigkill_delay_on_stop"
                const val EXTRA_SIGKILL_DELAY_ON_STOP = TERMUX_PACKAGE_NAME + ".execute.sigkill_delay_on_stop"



            /**
             * The value for {@link #EXTRA_SESSION_ACTION} extra that will set the new session as
             * the current session and will start {@link TERMUX_ACTIVITY} if its not running to bring
             * the new session to foreground.
             */
                const val VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY = 0

            /**
             * The value for {@link #EXTRA_SESSION_ACTION} extra that will keep any existing session
             * as the current session and will start {@link TERMUX_ACTIVITY} if its not running to
             * bring the existing session to foreground. The new session will be added to the left
             * sidebar in the sessions list.
             */
                const val VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_OPEN_ACTIVITY = 1

            /**
             * The value for {@link #EXTRA_SESSION_ACTION} extra that will set the new session as
             * the current session but will not start {@link TERMUX_ACTIVITY} if its not running
             * and session(s) will be seen in Termux notification and can be clicked to bring new
             * session to foreground. If the {@link TERMUX_ACTIVITY} is already running, then this
             * will behave like {@link #VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_OPEN_ACTIVITY}.
             */
                const val VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY = 2

            /**
             * The value for {@link #EXTRA_SESSION_ACTION} extra that will keep any existing session
             * as the current session but will not start {@link TERMUX_ACTIVITY} if its not running
             * and session(s) will be seen in Termux notification and can be clicked to bring
             * existing session to foreground. If the {@link TERMUX_ACTIVITY} is already running,
             * then this will behave like {@link #VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_OPEN_ACTIVITY}.
             */
                const val VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_DONT_OPEN_ACTIVITY = 3

            /**
             * The minimum allowed value for {@link #EXTRA_SESSION_ACTION}.
             */
                const val MIN_VALUE_EXTRA_SESSION_ACTION = VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY

            /**
             * The maximum allowed value for {@link #EXTRA_SESSION_ACTION}.
             */
                const val MAX_VALUE_EXTRA_SESSION_ACTION = VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_DONT_OPEN_ACTIVITY

            /**
             * Intent {@code Bundle} extra to store result of execute command that is sent back for the
             * TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent if the {@link #EXTRA_PENDING_INTENT} is not
             * {@code null}
             */
            // Default: "result"
                const val EXTRA_PLUGIN_RESULT_BUNDLE = "result"

            /**
             * Intent {@code String} extra for stdout value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE}
             */
            // Default: "stdout"
                const val EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT = "stdout"

            /**
             * Intent {@code String} extra for original length of stdout value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE}
             */
            // Default: "stdout_original_length"
                const val EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT_ORIGINAL_LENGTH = "stdout_original_length"

            /**
             * Intent {@code String} extra for stderr value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE}
             */
            // Default: "stderr"
                const val EXTRA_PLUGIN_RESULT_BUNDLE_STDERR = "stderr"

            /**
             * Intent {@code String} extra for original length of stderr value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE}
             */
            // Default: "stderr_original_length"
                const val EXTRA_PLUGIN_RESULT_BUNDLE_STDERR_ORIGINAL_LENGTH = "stderr_original_length"

            /**
             * Intent {@code int} extra for exit code value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE}
             */
            // Default: "exitCode"
                const val EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE = "exitCode"

            /**
             * Intent {@code int} extra for err value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE}
             */
            // Default: "err"
                const val EXTRA_PLUGIN_RESULT_BUNDLE_ERR = "err"

            /**
             * Intent {@code String} extra for errmsg value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE}
             */
            // Default: "errmsg"
                const val EXTRA_PLUGIN_RESULT_BUNDLE_ERRMSG = "errmsg"
            }
        }

        /**
         * Termux app run command service to receive commands sent by 3rd party apps.
         */

        class RUN_COMMAND_SERVICE {

            companion object {

            /**
             * Termux RUN_COMMAND Intent help url
             */
            // Default: "https://github.com/termux/termux-app/wiki/RUN_COMMAND-Intent"
                const val RUN_COMMAND_API_HELP_URL = TERMUX_GITHUB_WIKI_REPO_URL + "/RUN_COMMAND-Intent"

            /**
             * Intent action to execute command with RUN_COMMAND_SERVICE
             */
            // Default: "com.termux.RUN_COMMAND"
                const val ACTION_RUN_COMMAND = TERMUX_PACKAGE_NAME + ".RUN_COMMAND"

            /**
             * Intent {@code String} extra for absolute path of command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent
             */
            // Default: "com.termux.RUN_COMMAND_PATH"
                const val EXTRA_COMMAND_PATH = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_PATH"

            /**
             * Intent {@code String[]} extra for arguments to the executable of the command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent
             */
            // Default: "com.termux.RUN_COMMAND_ARGUMENTS"
                const val EXTRA_ARGUMENTS = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_ARGUMENTS"

            /**
             * Intent {@code boolean} extra for whether to replace comma alternative characters in arguments with comma characters for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent
             */
            // Default: "com.termux.RUN_COMMAND_REPLACE_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS"
                const val EXTRA_REPLACE_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_REPLACE_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS"

            /**
             * Intent {@code String} extra for the comma alternative characters in arguments that should be replaced instead of the default {@link #COMMA_ALTERNATIVE} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent
             */
            // Default: "com.termux.RUN_COMMAND_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS"
                const val EXTRA_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS"

            /**
             * Intent {@code String} extra for stdin of the command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent
             */
            // Default: "com.termux.RUN_COMMAND_STDIN"
                const val EXTRA_STDIN = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_STDIN"

            /**
             * Intent {@code String} extra for current working directory of command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent
             */
            // Default: "com.termux.RUN_COMMAND_WORKDIR"
                const val EXTRA_WORKDIR = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_WORKDIR"

            /**
             * Intent {@code boolean} extra for whether to run command in background {@link Runner#APP_SHELL} or foreground {@link Runner#TERMINAL_SESSION} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent
             */
            @Deprecated("")
            // Default: "com.termux.RUN_COMMAND_BACKGROUND"
                const val EXTRA_BACKGROUND = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_BACKGROUND"

            /**
             * Intent {@code String} extra for command the {@link Runner} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent
             */
            // Default: "com.termux.RUN_COMMAND_RUNNER"
                const val EXTRA_RUNNER = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_RUNNER"

            /**
             * Intent {@code String} extra for custom log level for background commands defined by {@link com.termux.shared.logger.Logger} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent
             */
            // Default: "com.termux.RUN_COMMAND_BACKGROUND_CUSTOM_LOG_LEVEL"
                const val EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_BACKGROUND_CUSTOM_LOG_LEVEL"

            /**
             * Intent {@code String} extra for session action of {@link Runner#TERMINAL_SESSION} commands for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent
             */
            // Default: "com.termux.RUN_COMMAND_SESSION_ACTION"
                const val EXTRA_SESSION_ACTION = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_SESSION_ACTION"

            /**
             * Intent {@code String} extra for shell name of commands for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent
             */
            // Default: "com.termux.RUN_COMMAND_SHELL_NAME"
                const val EXTRA_SHELL_NAME = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_SHELL_NAME"

            /**
             * Intent {@code String} extra for the {@link ExecutionCommand.ShellCreateMode}  for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent.
             */
            // Default: "com.termux.RUN_COMMAND_SHELL_CREATE_MODE"
                const val EXTRA_SHELL_CREATE_MODE = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_SHELL_CREATE_MODE"

            /**
             * Intent {@code String} extra for label of the command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent
             */
            // Default: "com.termux.RUN_COMMAND_COMMAND_LABEL"
                const val EXTRA_COMMAND_LABEL = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_COMMAND_LABEL"

            /**
             * Intent markdown {@code String} extra for description of the command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent
             */
            // Default: "com.termux.RUN_COMMAND_COMMAND_DESCRIPTION"
                const val EXTRA_COMMAND_DESCRIPTION = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_COMMAND_DESCRIPTION"

            /**
             * Intent markdown {@code String} extra for help of the command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent
             */
            // Default: "com.termux.RUN_COMMAND_COMMAND_HELP"
                const val EXTRA_COMMAND_HELP = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_COMMAND_HELP"

            /**
             * Intent {@code Parcelable} extra for the pending intent that should be sent with the result of the execution command to the execute command caller for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent
             */
            // Default: "com.termux.RUN_COMMAND_PENDING_INTENT"
                const val EXTRA_PENDING_INTENT = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_PENDING_INTENT"

            /**
             * Intent {@code String} extra for the directory path in which to write the result of
             * the execution command for the execute command caller for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent
             */
            // Default: "com.termux.RUN_COMMAND_RESULT_DIRECTORY"
                const val EXTRA_RESULT_DIRECTORY = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_RESULT_DIRECTORY"

            /**
             * Intent {@code boolean} extra for whether the result should be written to a single file
             * or multiple files (err, errmsg, stdout, stderr, exit_code) in
             * {@link #EXTRA_RESULT_DIRECTORY} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent
             */
            // Default: "com.termux.RUN_COMMAND_RESULT_SINGLE_FILE"
                const val EXTRA_RESULT_SINGLE_FILE = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_RESULT_SINGLE_FILE"

            /**
             * Intent {@code String} extra for the basename of the result file that should be created
             * in {@link #EXTRA_RESULT_DIRECTORY} if {@link #EXTRA_RESULT_SINGLE_FILE} is {@code true}
             * for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent
             */
            // Default: "com.termux.RUN_COMMAND_RESULT_FILE_BASENAME"
                const val EXTRA_RESULT_FILE_BASENAME = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_RESULT_FILE_BASENAME"

            /**
             * Intent {@code String} extra for the output {@link Formatter} format of the
             * {@link #EXTRA_RESULT_FILE_BASENAME} result file for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent
             */
            // Default: "com.termux.RUN_COMMAND_RESULT_FILE_OUTPUT_FORMAT"
                const val EXTRA_RESULT_FILE_OUTPUT_FORMAT = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_RESULT_FILE_OUTPUT_FORMAT"

            /**
             * Intent {@code String} extra for the error {@link Formatter} format of the
             * {@link #EXTRA_RESULT_FILE_BASENAME} result file for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent
             */
            // Default: "com.termux.RUN_COMMAND_RESULT_FILE_ERROR_FORMAT"
                const val EXTRA_RESULT_FILE_ERROR_FORMAT = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_RESULT_FILE_ERROR_FORMAT"

            /**
             * Intent {@code String} extra for the optional suffix of the result files that should be
             * created in {@link #EXTRA_RESULT_DIRECTORY} if {@link #EXTRA_RESULT_SINGLE_FILE} is
             * {@code false} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent
             */
            // Default: "com.termux.RUN_COMMAND_RESULT_FILES_SUFFIX"
                const val EXTRA_RESULT_FILES_SUFFIX = TERMUX_PACKAGE_NAME + ".RUN_COMMAND_RESULT_FILES_SUFFIX"
            }
        }
    }

    /**
     * Termux:API app constants.
     */

    class TERMUX_API_APP {

        companion object {

        /**
         * Termux:API app core activity name.
         * @deprecated For backward compatibility with old Termux:API app. Use TERMUX_API_MAIN_ACTIVITY_NAME.
         */
        // Default: "com.termux.api.activities.TermuxAPIActivity"
            const val TERMUX_API_ACTIVITY_NAME = TERMUX_API_PACKAGE_NAME + ".activities.TermuxAPIActivity"
        /** Termux:API app main activity name. */
        // Default: "com.termux.api.activities.TermuxAPIMainActivity"
            const val TERMUX_API_MAIN_ACTIVITY_NAME = TERMUX_API_PACKAGE_NAME + ".activities.TermuxAPIMainActivity"

        /** Termux:API app launcher activity name. This is an `activity-alias` for {@link #TERMUX_API_MAIN_ACTIVITY_NAME} used for launchers with {@link Intent#CATEGORY_LAUNCHER}. */
        // Default: "com.termux.api.activities.TermuxAPILauncherActivity"
            const val TERMUX_API_LAUNCHER_ACTIVITY_NAME = TERMUX_API_PACKAGE_NAME + ".activities.TermuxAPILauncherActivity"
        }

    }





    /**
     * Termux:Boot app constants.
     */

    class TERMUX_BOOT_APP {

        companion object {

        /** Termux:Boot app main activity name. */
        // Default: "com.termux.boot.activities.TermuxBootMainActivity"
            const val TERMUX_BOOT_MAIN_ACTIVITY_NAME = TERMUX_BOOT_PACKAGE_NAME + ".activities.TermuxBootMainActivity"

        /** Termux:Boot app launcher activity name. This is an `activity-alias` for {@link #TERMUX_BOOT_MAIN_ACTIVITY_NAME} used for launchers with {@link Intent#CATEGORY_LAUNCHER}. */
        // Default: "com.termux.boot.activities.TermuxBootLauncherActivity"
            const val TERMUX_BOOT_LAUNCHER_ACTIVITY_NAME = TERMUX_BOOT_PACKAGE_NAME + ".activities.TermuxBootLauncherActivity"
        }

    }

    /**
     * Termux:GUI app constants.
     */

    class TERMUX_GUI {

        companion object {

        /**
         * Termux:GUI app core activity name.
         */
        // Default: "com.termux.gui.activities.TermuxGUIActivity"
            const val TERMUX_GUI_ACTIVITY_NAME = TERMUX_GUI_PACKAGE_NAME + ".activities.TermuxGUIActivity"
        }
    }

    /**
     * Termux:Float app constants.
     */

    class TERMUX_FLOAT_APP {

        companion object {

        /** Termux:Float app core activity name. */
        // Default: "com.termux.window.TermuxFloatActivity"
            const val TERMUX_FLOAT_ACTIVITY_NAME = TERMUX_FLOAT_PACKAGE_NAME + ".TermuxFloatActivity"

        /** Termux:Float app core service name. */
        // Default: "com.termux.window.TermuxFloatService"
            const val TERMUX_FLOAT_SERVICE_NAME = TERMUX_FLOAT_PACKAGE_NAME + ".TermuxFloatService"
        }

        /**
         * Termux:Float app core service.
         */

        class TERMUX_FLOAT_SERVICE {

            companion object {

            /**
             * Intent action to stop TERMUX_FLOAT_SERVICE.
             */
            // Default: "com.termux.float.ACTION_STOP_SERVICE"
                const val ACTION_STOP_SERVICE = TERMUX_FLOAT_PACKAGE_NAME + ".ACTION_STOP_SERVICE"

            /**
             * Intent action to show float window.
             */
            // Default: "com.termux.float.ACTION_SHOW"
                const val ACTION_SHOW = TERMUX_FLOAT_PACKAGE_NAME + ".ACTION_SHOW"

            /**
             * Intent action to hide float window.
             */
            // Default: "com.termux.float.ACTION_HIDE"
                const val ACTION_HIDE = TERMUX_FLOAT_PACKAGE_NAME + ".ACTION_HIDE"
            }
        }
    }

    /**
     * Termux:Styling app constants.
     */

    class TERMUX_STYLING_APP {

        companion object {

        /** Termux:Styling app core activity name. */
        // Default: "com.termux.styling.TermuxStyleActivity"
            const val TERMUX_STYLING_ACTIVITY_NAME = TERMUX_STYLING_PACKAGE_NAME + ".TermuxStyleActivity"


        /** Termux:Styling app main activity name. */
        // Default: "com.termux.styling.activities.TermuxStylingMainActivity"
            const val TERMUX_STYLING_MAIN_ACTIVITY_NAME = TERMUX_STYLING_PACKAGE_NAME + ".activities.TermuxStylingMainActivity"

        /** Termux:Styling app launcher activity name. This is an `activity-alias` for {@link #TERMUX_STYLING_MAIN_ACTIVITY_NAME} used for launchers with {@link Intent#CATEGORY_LAUNCHER}. */
        // Default: "com.termux.styling.activities.TermuxStylingLauncherActivity"
            const val TERMUX_STYLING_LAUNCHER_ACTIVITY_NAME = TERMUX_STYLING_PACKAGE_NAME + ".activities.TermuxStylingLauncherActivity"
        }

    }

    /**
     * Termux:Tasker app constants.
     */

    class TERMUX_TASKER_APP {

        companion object {

        /**
         * Termux:Tasker app core activity name.
         * @deprecated For backward compatibility. Use TERMUX_TASKER_MAIN_ACTIVITY_NAME.
         */
            const val TERMUX_TASKER_ACTIVITY_NAME = TERMUX_TASKER_PACKAGE_NAME + ".activities.TermuxTaskerActivity"
        /** Termux:Tasker app main activity name. */
        // Default: "com.termux.tasker.activities.TermuxTaskerMainActivity"
            const val TERMUX_TASKER_MAIN_ACTIVITY_NAME = TERMUX_TASKER_PACKAGE_NAME + ".activities.TermuxTaskerMainActivity"

        /** Termux:Tasker app launcher activity name. This is an `activity-alias` for {@link #TERMUX_TASKER_MAIN_ACTIVITY_NAME} used for launchers with {@link Intent#CATEGORY_LAUNCHER}. */
        // Default: "com.termux.tasker.activities.TermuxTaskerLauncherActivity"
            const val TERMUX_TASKER_LAUNCHER_ACTIVITY_NAME = TERMUX_TASKER_PACKAGE_NAME + ".activities.TermuxTaskerLauncherActivity"
        }

    }

    /**
     * Termux:Widget app constants.
     */

    class TERMUX_WIDGET_APP {

        companion object {

        /**
         * Termux:Widget app core activity name.
         * @deprecated For backward compatibility. Use TERMUX_WIDGET_MAIN_ACTIVITY_NAME.
         */
            const val TERMUX_WIDGET_ACTIVITY_NAME = TERMUX_WIDGET_PACKAGE_NAME + ".activities.TermuxWidgetActivity"
        /** Termux:Widget app main activity name. */
        // Default: "com.termux.widget.activities.TermuxWidgetMainActivity"
            const val TERMUX_WIDGET_MAIN_ACTIVITY_NAME = TERMUX_WIDGET_PACKAGE_NAME + ".activities.TermuxWidgetMainActivity"

        /** Termux:Widget app launcher activity name. This is an `activity-alias` for {@link #TERMUX_WIDGET_MAIN_ACTIVITY_NAME} used for launchers with {@link Intent#CATEGORY_LAUNCHER}. */
        // Default: "com.termux.widget.activities.TermuxWidgetLauncherActivity"
            const val TERMUX_WIDGET_LAUNCHER_ACTIVITY_NAME = TERMUX_WIDGET_PACKAGE_NAME + ".activities.TermuxWidgetLauncherActivity"

        /**
         *  Intent {@code String} extra for the token of the Termux:Widget app shortcuts.
         */
        // Default: "com.termux.shortcut.token"
            const val EXTRA_TOKEN_NAME = TERMUX_PACKAGE_NAME + ".shortcut.token"
        }


        /**
         * Termux:Widget app {@link android.appwidget.AppWidgetProvider} class.
         */

        class TERMUX_WIDGET_PROVIDER {

            companion object {

            /**
             * Intent action for if an item is clicked in the widget.
             */
            // Default: "com.termux.widget.ACTION_WIDGET_ITEM_CLICKED"
                const val ACTION_WIDGET_ITEM_CLICKED = TERMUX_WIDGET_PACKAGE_NAME + ".ACTION_WIDGET_ITEM_CLICKED"

            /**
             * Intent action to refresh files in the widget.
             */
            // Default: "com.termux.widget.ACTION_REFRESH_WIDGET"
                const val ACTION_REFRESH_WIDGET = TERMUX_WIDGET_PACKAGE_NAME + ".ACTION_REFRESH_WIDGET"

            /**
             *  Intent {@code String} extra for the file clicked for the TERMUX_WIDGET_PROVIDER.ACTION_WIDGET_ITEM_CLICKED intent.
             */
            // Default: "com.termux.widget.EXTRA_FILE_CLICKED"
                const val EXTRA_FILE_CLICKED = TERMUX_WIDGET_PACKAGE_NAME + ".EXTRA_FILE_CLICKED"
            }
        }
    }
}