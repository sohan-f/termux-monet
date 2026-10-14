package com.termux.shared.termux.settings.properties

import android.content.Context
import androidx.annotation.NonNull
import com.termux.shared.data.DataUtils
import com.termux.shared.logger.Logger
import com.termux.shared.settings.properties.SharedProperties
import com.termux.shared.settings.properties.SharedPropertiesParser
import com.termux.shared.termux.TermuxConstants
import java.io.File
import java.util.Properties

abstract class TermuxSharedProperties(
    @NonNull context: Context,
    @NonNull label: String,
    propertiesFilePaths: List<String>?,
    @NonNull propertiesList: Set<String>,
    @NonNull sharedPropertiesParser: SharedPropertiesParser
) {

    @JvmField
    protected val mContext: Context = context.applicationContext

    @JvmField
    protected val mLabel: String = label

    @JvmField
    protected val mPropertiesFilePaths: List<String>? = propertiesFilePaths

    @JvmField
    protected val mPropertiesList: Set<String> = propertiesList

    @JvmField
    protected val mSharedPropertiesParser: SharedPropertiesParser = sharedPropertiesParser

    @JvmField
    protected var mPropertiesFile: File? = null

    @JvmField
    protected var mSharedProperties: SharedProperties? = null

    init {
        loadTermuxPropertiesFromDisk()
    }

    /**
     * Reload the termux properties from disk into an in-memory cache.
     */
    @Synchronized
    open fun loadTermuxPropertiesFromDisk() {
        // Properties files must be searched everytime since no file may exist when constructor is
        // called or a higher priority file may have been created afterward. Otherwise, if no file
        // was found, then default props would keep loading, since mSharedProperties would be null. #2836
        mPropertiesFile = SharedProperties.getPropertiesFileFromList(mPropertiesFilePaths, LOG_TAG)
        mSharedProperties = null
        mSharedProperties = SharedProperties(mContext, mPropertiesFile, mPropertiesList, mSharedPropertiesParser)
        // Never null here (just assigned), but the field is nullable so !! keeps the type system honest.
        mSharedProperties!!.loadPropertiesFromDisk()
        dumpPropertiesToLog()
        dumpInternalPropertiesToLog()
    }

    /**
     * Get the {@link Properties} from the {@link #mPropertiesFile} file.
     *
     * @param cached If {@code true}, then the {@link Properties} in-memory cache is returned.
     *               Otherwise the {@link Properties} object is read directly from the
     *               {@link #mPropertiesFile} file.
     * @return Returns the {@link Properties} object. It will be {@code null} if an exception is
     * raised while reading the file.
     */
    open fun getProperties(cached: Boolean): Properties? {
        return mSharedProperties!!.getProperties(cached)
    }

    /**
     * Get the {@link String} value for the key passed from the {@link #mPropertiesFile} file.
     *
     * @param key The key to read.
     * @param def The default value.
     * @param cached If {@code true}, then the value is returned from the the {@link Properties} in-memory cache.
     *               Otherwise the {@link Properties} object is read directly from the file
     *               and value is returned from it against the key.
     * @return Returns the {@link String} object. This will be {@code null} if key is not found.
     */
    open fun getPropertyValue(key: String?, def: String?, cached: Boolean): String? {
        return SharedProperties.getDefaultIfNull(mSharedProperties!!.getProperty(key, cached), def)
    }

    /**
     * A function to check if the value is {@code true} for {@link Properties} key read from
     * the {@link #mPropertiesFile} file.
     *
     * @param key The key to read.
     * @param cached If {@code true}, then the value is checked from the the {@link Properties} in-memory cache.
     *               Otherwise the {@link Properties} object is read directly from the file
     *               and value is checked from it.
     * @param logErrorOnInvalidValue If {@code true}, then an error will be logged if key value
     *                               was found in {@link Properties} but was invalid.
     * @return Returns the {@code true} if the {@link Properties} key {@link String} value equals "true",
     * regardless of case. If the key does not exist in the file or does not equal "true", then
     * {@code false} will be returned.
     */
    open fun isPropertyValueTrue(key: String?, cached: Boolean, logErrorOnInvalidValue: Boolean): Boolean {
        return SharedProperties.getBooleanValueForStringValue(key, getPropertyValue(key, null, cached), false, logErrorOnInvalidValue, LOG_TAG)
    }

    /**
     * A function to check if the value is {@code false} for {@link Properties} key read from
     * the {@link #mPropertiesFile} file.
     *
     * @param key The key to read.
     * @param cached If {@code true}, then the value is checked from the the {@link Properties} in-memory cache.
     *               Otherwise the {@link Properties} object is read directly from the file
     *               and value is checked from it.
     * @param logErrorOnInvalidValue If {@code true}, then an error will be logged if key value
     *                               was found in {@link Properties} but was invalid.
     * @return Returns {@code true} if the {@link Properties} key {@link String} value equals "false",
     * regardless of case. If the key does not exist in the file or does not equal "false", then
     * {@code true} will be returned.
     */
    open fun isPropertyValueFalse(key: String?, cached: Boolean, logErrorOnInvalidValue: Boolean): Boolean {
        return SharedProperties.getInvertedBooleanValueForStringValue(key, getPropertyValue(key, null, cached), true, logErrorOnInvalidValue, LOG_TAG)
    }

    /**
     * Get the internal value {@link Object} {@link HashMap <>} in-memory cache for the
     * {@link #mPropertiesFile} file. A call to {@link #loadTermuxPropertiesFromDisk()} must be made
     * before this.
     *
     * @return Returns a copy of {@link Map} object.
     */
    open fun getInternalProperties(): Map<String, Any?> {
        return mSharedProperties!!.getInternalProperties()
    }

    /**
     * Get the internal {@link Object} value for the key passed from the {@link #mPropertiesFile} file.
     * If cache is {@code true}, then value is returned from the {@link HashMap <>} in-memory cache,
     * so a call to {@link #loadTermuxPropertiesFromDisk()} must be made before this.
     *
     * @param key The key to read from the {@link HashMap<>} in-memory cache.
     * @param cached If {@code true}, then the value is returned from the the {@link HashMap <>} in-memory cache,
     *               but if the value is null, then an attempt is made to return the default value.
     *               If {@code false}, then the {@link Properties} object is read directly from the file
     *               and internal value is returned for the property value against the key.
     * @return Returns the {@link Object} object. This will be {@code null} if key is not found or
     * the object stored against the key is {@code null}.
     */
    open fun getInternalPropertyValue(key: String?, cached: Boolean): Any? {
        var value: Any?
        if (cached) {
            value = mSharedProperties!!.getInternalProperty(key)
            // If the value is not null since key was found or if the value was null since the
            // object stored for the key was itself null, we detect the later by checking if the key
            // exists in the map.
            if (value != null || mSharedProperties!!.getInternalProperties().containsKey(key)) {
                return value
            } else {
                // This should not happen normally unless mMap was modified after the
                // {@link #loadTermuxPropertiesFromDisk()} call
                // A null value can still be returned by
                // {@link #getInternalPropertyValueFromValue(Context,String,String)} for some keys
                value = getInternalTermuxPropertyValueFromValue(mContext, key, null)
                Logger.logWarn(LOG_TAG, "The value for \"" + key + "\" not found in SharedProperties cache, force returning default value: `" + value + "`")
                return value
            }
        } else {
            // We get the property value directly from file and return its internal value
            return getInternalTermuxPropertyValueFromValue(mContext, key, mSharedProperties!!.getProperty(key, false))
        }
    }

    open fun shouldAllowExternalApps(): Boolean {
        return getInternalPropertyValue(TermuxConstants.PROP_ALLOW_EXTERNAL_APPS, true) as Boolean
    }

    open fun isFileShareReceiverDisabled(): Boolean {
        return getInternalPropertyValue(TermuxPropertyConstants.KEY_DISABLE_FILE_SHARE_RECEIVER, true) as Boolean
    }

    open fun isFileViewReceiverDisabled(): Boolean {
        return getInternalPropertyValue(TermuxPropertyConstants.KEY_DISABLE_FILE_VIEW_RECEIVER, true) as Boolean
    }

    open fun areHardwareKeyboardShortcutsDisabled(): Boolean {
        return getInternalPropertyValue(TermuxPropertyConstants.KEY_DISABLE_HARDWARE_KEYBOARD_SHORTCUTS, true) as Boolean
    }

    open fun areTerminalSessionChangeToastsDisabled(): Boolean {
        return getInternalPropertyValue(TermuxPropertyConstants.KEY_DISABLE_TERMINAL_SESSION_CHANGE_TOAST, true) as Boolean
    }

    open fun isEnforcingCharBasedInput(): Boolean {
        return getInternalPropertyValue(TermuxPropertyConstants.KEY_ENFORCE_CHAR_BASED_INPUT, true) as Boolean
    }

    open fun shouldDrawBoldTextWithBrightColors(): Boolean {
        return getInternalPropertyValue(TermuxPropertyConstants.KEY_DRAW_BOLD_TEXT_WITH_BRIGHT_COLORS, true) as Boolean
    }

    open fun shouldExtraKeysTextBeAllCaps(): Boolean {
        return getInternalPropertyValue(TermuxPropertyConstants.KEY_EXTRA_KEYS_TEXT_ALL_CAPS, true) as Boolean
    }

    open fun shouldSoftKeyboardBeHiddenOnStartup(): Boolean {
        return getInternalPropertyValue(TermuxPropertyConstants.KEY_HIDE_SOFT_KEYBOARD_ON_STARTUP, true) as Boolean
    }

    open fun shouldRunTermuxAmSocketServer(): Boolean {
        return getInternalPropertyValue(TermuxPropertyConstants.KEY_RUN_TERMUX_AM_SOCKET_SERVER, true) as Boolean
    }

    open fun shouldOpenTerminalTranscriptURLOnClick(): Boolean {
        return getInternalPropertyValue(TermuxPropertyConstants.KEY_TERMINAL_ONCLICK_URL_OPEN, true) as Boolean
    }

    open fun isUsingCtrlSpaceWorkaround(): Boolean {
        return getInternalPropertyValue(TermuxPropertyConstants.KEY_USE_CTRL_SPACE_WORKAROUND, true) as Boolean
    }

    open fun isUsingFullScreen(): Boolean {
        return getInternalPropertyValue(TermuxPropertyConstants.KEY_USE_FULLSCREEN, true) as Boolean
    }

    open fun isUsingFullScreenWorkAround(): Boolean {
        return getInternalPropertyValue(TermuxPropertyConstants.KEY_USE_FULLSCREEN_WORKAROUND, true) as Boolean
    }

    open fun isRemovingTaskOnActivityFinishEnabled(): Boolean {
        return getInternalPropertyValue(TermuxPropertyConstants.KEY_ACTIVITY_FINISH_REMOVE_TASK, true) as Boolean
    }

    open fun getBellBehaviour(): Int {
        return getInternalPropertyValue(TermuxPropertyConstants.KEY_BELL_BEHAVIOUR, true) as Int
    }

    open fun getDeleteTMPDIRFilesOlderThanXDaysOnExit(): Int {
        return getInternalPropertyValue(TermuxPropertyConstants.KEY_DELETE_TMPDIR_FILES_OLDER_THAN_X_DAYS_ON_EXIT, true) as Int
    }

    open fun getTerminalCursorBlinkRate(): Int {
        return getInternalPropertyValue(TermuxPropertyConstants.KEY_TERMINAL_CURSOR_BLINK_RATE, true) as Int
    }

    open fun getTerminalCursorStyle(): Int {
        return getInternalPropertyValue(TermuxPropertyConstants.KEY_TERMINAL_CURSOR_STYLE, true) as Int
    }

    open fun getTerminalMarginHorizontal(): Int {
        return getInternalPropertyValue(TermuxPropertyConstants.KEY_TERMINAL_MARGIN_HORIZONTAL, true) as Int
    }

    open fun getTerminalMarginVertical(): Int {
        return getInternalPropertyValue(TermuxPropertyConstants.KEY_TERMINAL_MARGIN_VERTICAL, true) as Int
    }

    open fun getTerminalTranscriptRows(): Int {
        return getInternalPropertyValue(TermuxPropertyConstants.KEY_TERMINAL_TRANSCRIPT_ROWS, true) as Int
    }

    open fun getBackgroundOverlayColor(): Int {
        return getInternalPropertyValue(TermuxPropertyConstants.KEY_BACKGROUND_OVERLAY_COLOR, true) as Int
    }

    open fun getTerminalToolbarHeightScaleFactor(): Float {
        return getInternalPropertyValue(TermuxPropertyConstants.KEY_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR, true) as Float
    }

    open fun isBackKeyTheEscapeKey(): Boolean {
        return TermuxPropertyConstants.IVALUE_BACK_KEY_BEHAVIOUR_ESCAPE == getInternalPropertyValue(TermuxPropertyConstants.KEY_BACK_KEY_BEHAVIOUR, true)
    }

    open fun getDefaultWorkingDirectory(): String? {
        return getInternalPropertyValue(TermuxPropertyConstants.KEY_DEFAULT_WORKING_DIRECTORY, true) as String?
    }

    open fun getNightMode(): String? {
        return getInternalPropertyValue(TermuxPropertyConstants.KEY_NIGHT_MODE, true) as String?
    }

    open fun shouldEnableDisableSoftKeyboardOnToggle(): Boolean {
        return TermuxPropertyConstants.IVALUE_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR_ENABLE_DISABLE == getInternalPropertyValue(TermuxPropertyConstants.KEY_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR, true)
    }

    open fun areVirtualVolumeKeysDisabled(): Boolean {
        return TermuxPropertyConstants.IVALUE_VOLUME_KEY_BEHAVIOUR_VOLUME == getInternalPropertyValue(TermuxPropertyConstants.KEY_VOLUME_KEYS_BEHAVIOUR, true)
    }

    open fun dumpPropertiesToLog() {
        val properties = getProperties(true)
        val propertiesDump = StringBuilder()
        propertiesDump.append(mLabel).append(" Termux Properties:")
        if (properties != null) {
            for (key in properties.stringPropertyNames()) {
                propertiesDump.append("\n").append(key).append(": `").append(properties.get(key)).append("`")
            }
        } else {
            propertiesDump.append(" null")
        }
        Logger.logVerbose(LOG_TAG, propertiesDump.toString())
    }

    open fun dumpInternalPropertiesToLog() {
        // getInternalProperties() is never null (like the Java version),
        // so the dead null branch is dropped.
        val internalProperties = getInternalProperties()
        val internalPropertiesDump = StringBuilder()
        internalPropertiesDump.append(mLabel).append(" Internal Properties:")
        for (key in internalProperties.keys) {
            internalPropertiesDump.append("\n").append(key).append(": `").append(internalProperties[key]).append("`")
        }
        Logger.logVerbose(LOG_TAG, internalPropertiesDump.toString())
    }

    companion object {
        const val LOG_TAG = "TermuxSharedProperties"

        /**
         * Get the internal {@link Object} value for the key passed from the first file found in
         * {@link TermuxConstants#TERMUX_PROPERTIES_FILE_PATHS_LIST}. The {@link Properties} object is
         * read directly from the file and internal value is returned for the property value against the key.
         *
         * @param context The context for operations.
         * @param key The key for which the internal object is required.
         * @return Returns the {@link Object} object. This will be {@code null} if key is not found or
         * the object stored against the key is {@code null}.
         */
        @JvmStatic
        fun getTermuxInternalPropertyValue(context: Context?, key: String?): Any? {
            return SharedProperties.getInternalProperty(context, SharedProperties.getPropertiesFileFromList(TermuxConstants.TERMUX_PROPERTIES_FILE_PATHS_LIST, LOG_TAG), key, SharedPropertiesParserClient())
        }

        @JvmStatic
        @Suppress("DEPRECATION") // KEY_USE_BLACK_UI must stay: this migrates it away
        fun replaceUseBlackUIProperty(@NonNull properties: Properties): Properties {
            val useBlackUIStringValue = properties.getProperty(TermuxPropertyConstants.KEY_USE_BLACK_UI)
            if (useBlackUIStringValue == null)
                return properties
            Logger.logWarn(LOG_TAG, "Removing deprecated property " + TermuxPropertyConstants.KEY_USE_BLACK_UI + "=" + useBlackUIStringValue)
            properties.remove(TermuxPropertyConstants.KEY_USE_BLACK_UI)
            // If KEY_NIGHT_MODE is not set
            if (properties.getProperty(TermuxPropertyConstants.KEY_NIGHT_MODE) == null) {
                val useBlackUI = SharedProperties.getBooleanValueForStringValue(useBlackUIStringValue)
                if (useBlackUI != null) {
                    val termuxAppTheme = if (useBlackUI) TermuxPropertyConstants.IVALUE_NIGHT_MODE_TRUE else TermuxPropertyConstants.IVALUE_NIGHT_MODE_FALSE
                    Logger.logWarn(LOG_TAG, "Replacing deprecated property " + TermuxPropertyConstants.KEY_USE_BLACK_UI + "=" + useBlackUI + " with " + TermuxPropertyConstants.KEY_NIGHT_MODE + "=" + termuxAppTheme)
                    properties.put(TermuxPropertyConstants.KEY_NIGHT_MODE, termuxAppTheme)
                }
            }
            return properties
        }

        /**
         * A static function that should return the internal termux {@link Object} for a key/value pair
         * read from properties file.
         *
         * @param context The context for operations.
         * @param key The key for which the internal object is required.
         * @param value The literal value for the property found is the properties file.
         * @return Returns the internal termux {@link Object} object.
         */
        @JvmStatic
        fun getInternalTermuxPropertyValueFromValue(context: Context?, key: String?, value: String?): Any? {
            if (key == null)
                return null
            /*
              For keys where a MAP_* is checked by respective functions. Note that value to this function
              would actually be the key for the MAP_*:
              - If the value is currently null, then searching MAP_* should also return null and internal default value will be used.
              - If the value is not null and does not exist in MAP_*, then internal default value will be used.
              - If the value is not null and does exist in MAP_*, then internal value returned by map will be used.
             */
            when (key) {
                /* int */
                TermuxPropertyConstants.KEY_BELL_BEHAVIOUR ->
                    return getBellBehaviourInternalPropertyValueFromValue(value)
                TermuxPropertyConstants.KEY_DELETE_TMPDIR_FILES_OLDER_THAN_X_DAYS_ON_EXIT ->
                    return getDeleteTMPDIRFilesOlderThanXDaysOnExitInternalPropertyValueFromValue(value)
                TermuxPropertyConstants.KEY_TERMINAL_CURSOR_BLINK_RATE ->
                    return getTerminalCursorBlinkRateInternalPropertyValueFromValue(value)
                TermuxPropertyConstants.KEY_TERMINAL_CURSOR_STYLE ->
                    return getTerminalCursorStyleInternalPropertyValueFromValue(value)
                TermuxPropertyConstants.KEY_TERMINAL_MARGIN_HORIZONTAL ->
                    return getTerminalMarginHorizontalInternalPropertyValueFromValue(value)
                TermuxPropertyConstants.KEY_TERMINAL_MARGIN_VERTICAL ->
                    return getTerminalMarginVerticalInternalPropertyValueFromValue(value)
                TermuxPropertyConstants.KEY_TERMINAL_TRANSCRIPT_ROWS ->
                    return getTerminalTranscriptRowsInternalPropertyValueFromValue(value)
                TermuxPropertyConstants.KEY_BACKGROUND_OVERLAY_COLOR ->
                    return getBackgroundOverlayInternalPropertyValueFromValue(value)
                /* float */
                TermuxPropertyConstants.KEY_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR ->
                    return getTerminalToolbarHeightScaleFactorInternalPropertyValueFromValue(value)
                /* Integer (may be null) */
                TermuxPropertyConstants.KEY_SHORTCUT_CREATE_SESSION,
                TermuxPropertyConstants.KEY_SHORTCUT_NEXT_SESSION,
                TermuxPropertyConstants.KEY_SHORTCUT_PREVIOUS_SESSION,
                TermuxPropertyConstants.KEY_SHORTCUT_RENAME_SESSION ->
                    return getCodePointForSessionShortcuts(key, value)
                /* String (may be null) */
                TermuxPropertyConstants.KEY_BACK_KEY_BEHAVIOUR ->
                    return getBackKeyBehaviourInternalPropertyValueFromValue(value)
                TermuxPropertyConstants.KEY_DEFAULT_WORKING_DIRECTORY ->
                    return getDefaultWorkingDirectoryInternalPropertyValueFromValue(value)
                TermuxPropertyConstants.KEY_EXTRA_KEYS ->
                    return getExtraKeysInternalPropertyValueFromValue(value)
                TermuxPropertyConstants.KEY_EXTRA_KEYS2 ->
                    return getExtraKeys2InternalPropertyValueFromValue(value)
                TermuxPropertyConstants.KEY_EXTRA_KEYS_STYLE ->
                    return getExtraKeysStyleInternalPropertyValueFromValue(value)
                TermuxPropertyConstants.KEY_NIGHT_MODE ->
                    return getNightModeInternalPropertyValueFromValue(value)
                TermuxPropertyConstants.KEY_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR ->
                    return getSoftKeyboardToggleBehaviourInternalPropertyValueFromValue(value)
                TermuxPropertyConstants.KEY_VOLUME_KEYS_BEHAVIOUR ->
                    return getVolumeKeysBehaviourInternalPropertyValueFromValue(value)
                else -> {
                    // default false boolean behaviour
                    if (TermuxPropertyConstants.TERMUX_DEFAULT_FALSE_BOOLEAN_BEHAVIOUR_PROPERTIES_LIST.contains(key))
                        return SharedProperties.getBooleanValueForStringValue(key, value, false, true, LOG_TAG)
                    // default true boolean behaviour
                    if (TermuxPropertyConstants.TERMUX_DEFAULT_TRUE_BOOLEAN_BEHAVIOUR_PROPERTIES_LIST.contains(key))
                        return SharedProperties.getBooleanValueForStringValue(key, value, true, true, LOG_TAG)
                    else
                        // default inverted false boolean behaviour
                        //else if (TermuxPropertyConstants.TERMUX_DEFAULT_INVERETED_FALSE_BOOLEAN_BEHAVIOUR_PROPERTIES_LIST.contains(key))
                        //    return SharedProperties.getInvertedBooleanValueForStringValue(key, value, false, true, LOG_TAG);
                        // default inverted true boolean behaviour
                        // else if (TermuxPropertyConstants.TERMUX_DEFAULT_INVERETED_TRUE_BOOLEAN_BEHAVIOUR_PROPERTIES_LIST.contains(key))
                        //    return SharedProperties.getInvertedBooleanValueForStringValue(key, value, true, true, LOG_TAG);
                        // just use String object as is (may be null)
                        return value
                }
            }
        }

        /**
         * Returns the internal value after mapping it based on
         * {@code TermuxPropertyConstants#MAP_BELL_BEHAVIOUR} if the value is not {@code null}
         * and is valid, otherwise returns {@link TermuxPropertyConstants#DEFAULT_IVALUE_BELL_BEHAVIOUR}.
         *
         * @param value The {@link String} value to convert.
         * @return Returns the internal value for value.
         */
        @JvmStatic
        fun getBellBehaviourInternalPropertyValueFromValue(value: String?): Int {
            return SharedProperties.getDefaultIfNotInMap(TermuxPropertyConstants.KEY_BELL_BEHAVIOUR, TermuxPropertyConstants.MAP_BELL_BEHAVIOUR, SharedProperties.toLowerCase(value), TermuxPropertyConstants.DEFAULT_IVALUE_BELL_BEHAVIOUR, true, LOG_TAG) as Int
        }

        /**
         * Returns the int for the value if its not null and is between
         * {@link TermuxPropertyConstants#IVALUE_DELETE_TMPDIR_FILES_OLDER_THAN_X_DAYS_ON_EXIT_MIN} and
         * {@link TermuxPropertyConstants#IVALUE_DELETE_TMPDIR_FILES_OLDER_THAN_X_DAYS_ON_EXIT_MAX},
         * otherwise returns {@link TermuxPropertyConstants#DEFAULT_IVALUE_DELETE_TMPDIR_FILES_OLDER_THAN_X_DAYS_ON_EXIT}.
         *
         * @param value The {@link String} value to convert.
         * @return Returns the internal value for value.
         */
        @JvmStatic
        fun getDeleteTMPDIRFilesOlderThanXDaysOnExitInternalPropertyValueFromValue(value: String?): Int {
            return SharedProperties.getDefaultIfNotInRange(TermuxPropertyConstants.KEY_DELETE_TMPDIR_FILES_OLDER_THAN_X_DAYS_ON_EXIT, DataUtils.getIntFromString(value, TermuxPropertyConstants.DEFAULT_IVALUE_DELETE_TMPDIR_FILES_OLDER_THAN_X_DAYS_ON_EXIT), TermuxPropertyConstants.DEFAULT_IVALUE_DELETE_TMPDIR_FILES_OLDER_THAN_X_DAYS_ON_EXIT, TermuxPropertyConstants.IVALUE_DELETE_TMPDIR_FILES_OLDER_THAN_X_DAYS_ON_EXIT_MIN, TermuxPropertyConstants.IVALUE_DELETE_TMPDIR_FILES_OLDER_THAN_X_DAYS_ON_EXIT_MAX, true, true, LOG_TAG)
        }

        /**
         * Returns the int for the value if its not null and is between
         * {@link TermuxPropertyConstants#IVALUE_TERMINAL_CURSOR_BLINK_RATE_MIN} and
         * {@link TermuxPropertyConstants#IVALUE_TERMINAL_CURSOR_BLINK_RATE_MAX},
         * otherwise returns {@link TermuxPropertyConstants#DEFAULT_IVALUE_TERMINAL_CURSOR_BLINK_RATE}.
         *
         * @param value The {@link String} value to convert.
         * @return Returns the internal value for value.
         */
        @JvmStatic
        fun getTerminalCursorBlinkRateInternalPropertyValueFromValue(value: String?): Int {
            return SharedProperties.getDefaultIfNotInRange(TermuxPropertyConstants.KEY_TERMINAL_CURSOR_BLINK_RATE, DataUtils.getIntFromString(value, TermuxPropertyConstants.DEFAULT_IVALUE_TERMINAL_CURSOR_BLINK_RATE), TermuxPropertyConstants.DEFAULT_IVALUE_TERMINAL_CURSOR_BLINK_RATE, TermuxPropertyConstants.IVALUE_TERMINAL_CURSOR_BLINK_RATE_MIN, TermuxPropertyConstants.IVALUE_TERMINAL_CURSOR_BLINK_RATE_MAX, true, true, LOG_TAG)
        }

        /**
         * Returns the internal value after mapping it based on
         * {@link TermuxPropertyConstants#MAP_TERMINAL_CURSOR_STYLE} if the value is not {@code null}
         * and is valid, otherwise returns {@link TermuxPropertyConstants#DEFAULT_IVALUE_TERMINAL_CURSOR_STYLE}.
         *
         * @param value The {@link String} value to convert.
         * @return Returns the internal value for value.
         */
        @JvmStatic
        fun getTerminalCursorStyleInternalPropertyValueFromValue(value: String?): Int {
            return SharedProperties.getDefaultIfNotInMap(TermuxPropertyConstants.KEY_TERMINAL_CURSOR_STYLE, TermuxPropertyConstants.MAP_TERMINAL_CURSOR_STYLE, SharedProperties.toLowerCase(value), TermuxPropertyConstants.DEFAULT_IVALUE_TERMINAL_CURSOR_STYLE, true, LOG_TAG) as Int
        }

        /**
         * Returns the int for the value if its not null and is between
         * {@link TermuxPropertyConstants#IVALUE_TERMINAL_MARGIN_HORIZONTAL_MIN} and
         * {@link TermuxPropertyConstants#IVALUE_TERMINAL_MARGIN_HORIZONTAL_MAX},
         * otherwise returns {@link TermuxPropertyConstants#DEFAULT_IVALUE_TERMINAL_MARGIN_HORIZONTAL}.
         *
         * @param value The {@link String} value to convert.
         * @return Returns the internal value for value.
         */
        @JvmStatic
        fun getTerminalMarginHorizontalInternalPropertyValueFromValue(value: String?): Int {
            return SharedProperties.getDefaultIfNotInRange(TermuxPropertyConstants.KEY_TERMINAL_MARGIN_HORIZONTAL, DataUtils.getIntFromString(value, TermuxPropertyConstants.DEFAULT_IVALUE_TERMINAL_MARGIN_HORIZONTAL), TermuxPropertyConstants.DEFAULT_IVALUE_TERMINAL_MARGIN_HORIZONTAL, TermuxPropertyConstants.IVALUE_TERMINAL_MARGIN_HORIZONTAL_MIN, TermuxPropertyConstants.IVALUE_TERMINAL_MARGIN_HORIZONTAL_MAX, true, true, LOG_TAG)
        }

        /**
         * Returns the int for the value if its not null and is between
         * {@link TermuxPropertyConstants#IVALUE_TERMINAL_MARGIN_VERTICAL_MIN} and
         * {@link TermuxPropertyConstants#IVALUE_TERMINAL_MARGIN_VERTICAL_MAX},
         * otherwise returns {@link TermuxPropertyConstants#DEFAULT_IVALUE_TERMINAL_MARGIN_VERTICAL}.
         *
         * @param value The {@link String} value to convert.
         * @return Returns the internal value for value.
         */
        @JvmStatic
        fun getTerminalMarginVerticalInternalPropertyValueFromValue(value: String?): Int {
            return SharedProperties.getDefaultIfNotInRange(TermuxPropertyConstants.KEY_TERMINAL_MARGIN_VERTICAL, DataUtils.getIntFromString(value, TermuxPropertyConstants.DEFAULT_IVALUE_TERMINAL_MARGIN_VERTICAL), TermuxPropertyConstants.DEFAULT_IVALUE_TERMINAL_MARGIN_VERTICAL, TermuxPropertyConstants.IVALUE_TERMINAL_MARGIN_VERTICAL_MIN, TermuxPropertyConstants.IVALUE_TERMINAL_MARGIN_VERTICAL_MAX, true, true, LOG_TAG)
        }

        /**
         * Returns the int for the value if its not null and is between
         * {@link TermuxPropertyConstants#IVALUE_TERMINAL_TRANSCRIPT_ROWS_MIN} and
         * {@link TermuxPropertyConstants#IVALUE_TERMINAL_TRANSCRIPT_ROWS_MAX},
         * otherwise returns {@link TermuxPropertyConstants#DEFAULT_IVALUE_TERMINAL_TRANSCRIPT_ROWS}.
         *
         * @param value The {@link String} value to convert.
         * @return Returns the internal value for value.
         */
        @JvmStatic
        fun getTerminalTranscriptRowsInternalPropertyValueFromValue(value: String?): Int {
            return SharedProperties.getDefaultIfNotInRange(TermuxPropertyConstants.KEY_TERMINAL_TRANSCRIPT_ROWS, DataUtils.getIntFromString(value, TermuxPropertyConstants.DEFAULT_IVALUE_TERMINAL_TRANSCRIPT_ROWS), TermuxPropertyConstants.DEFAULT_IVALUE_TERMINAL_TRANSCRIPT_ROWS, TermuxPropertyConstants.IVALUE_TERMINAL_TRANSCRIPT_ROWS_MIN, TermuxPropertyConstants.IVALUE_TERMINAL_TRANSCRIPT_ROWS_MAX, true, true, LOG_TAG)
        }

        /**
         * Returns the int for the color value if its not null and is in form of {@code #AARRGGBB}.
         * If the value does not contain alpha value then it will borrow it from {@link
         * TermuxPropertyConstants#DEFAULT_IVALUE_BACKGROUND_OVERLAY_COLOR}
         *
         * @param value The {@link String} value to convert.
         * @return Returns the internal value for value.
         */
        @JvmStatic
        fun getBackgroundOverlayInternalPropertyValueFromValue(value: String?): Int {
            return DataUtils.getIntColorFromString(value, TermuxPropertyConstants.DEFAULT_IVALUE_BACKGROUND_OVERLAY_COLOR, true)
        }

        /**
         * Returns the int for the value if its not null and is between
         * {@link TermuxPropertyConstants#IVALUE_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR_MIN} and
         * {@link TermuxPropertyConstants#IVALUE_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR_MAX},
         * otherwise returns {@link TermuxPropertyConstants#DEFAULT_IVALUE_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR}.
         *
         * @param value The {@link String} value to convert.
         * @return Returns the internal value for value.
         */
        @JvmStatic
        fun getTerminalToolbarHeightScaleFactorInternalPropertyValueFromValue(value: String?): Float {
            return SharedProperties.getDefaultIfNotInRange(TermuxPropertyConstants.KEY_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR, DataUtils.getFloatFromString(value, TermuxPropertyConstants.DEFAULT_IVALUE_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR), TermuxPropertyConstants.DEFAULT_IVALUE_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR, TermuxPropertyConstants.IVALUE_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR_MIN, TermuxPropertyConstants.IVALUE_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR_MAX, true, true, LOG_TAG)
        }

        /**
         * Returns the code point for the value if key is not {@code null} and value is not {@code null} and is valid,
         * otherwise returns {@code null}.
         *
         * @param key The key for session shortcut.
         * @param value The {@link String} value to convert.
         * @return Returns the internal value for value.
         */
        @JvmStatic
        fun getCodePointForSessionShortcuts(key: String?, value: String?): Int? {
            if (key == null)
                return null
            if (value == null)
                return null
            // Java split("\\+") drops trailing empties while Kotlin keeps
            // them; dropLastWhile restores exact Java semantics.
            val parts = value.lowercase().trim().split("+").dropLastWhile { it.isEmpty() }
            val input = if (parts.size == 2) parts[1].trim() else null
            if (!(parts.size == 2 && parts[0].trim() == "ctrl") || input.isNullOrEmpty() || input.length > 2) {
                Logger.logError(LOG_TAG, "Keyboard shortcut '" + key + "' is not Ctrl+<something>")
                return null
            }
            val c = input[0]
            var codePoint = c.code
            if (c.isLowSurrogate()) {
                if (input.length != 2 || input[1].isHighSurrogate()) {
                    Logger.logError(LOG_TAG, "Keyboard shortcut '" + key + "' is not Ctrl+<something>")
                    return null
                } else {
                    codePoint = Character.toCodePoint(input[1], c)
                }
            }
            return codePoint
        }

        /**
         * Returns the value itself if it is not {@code null}, otherwise returns {@link TermuxPropertyConstants#DEFAULT_IVALUE_BACK_KEY_BEHAVIOUR}.
         *
         * @param value {@link String} value to convert.
         * @return Returns the internal value for value.
         */
        @JvmStatic
        fun getBackKeyBehaviourInternalPropertyValueFromValue(value: String?): String? {
            return SharedProperties.getDefaultIfNotInMap(TermuxPropertyConstants.KEY_BACK_KEY_BEHAVIOUR, TermuxPropertyConstants.MAP_BACK_KEY_BEHAVIOUR, SharedProperties.toLowerCase(value), TermuxPropertyConstants.DEFAULT_IVALUE_BACK_KEY_BEHAVIOUR, true, LOG_TAG) as String?
        }

        /**
         * Returns the path itself if a directory exists at it and is readable, otherwise returns
         *  {@link TermuxPropertyConstants#DEFAULT_IVALUE_DEFAULT_WORKING_DIRECTORY}.
         *
         * @param path The {@link String} path to check.
         * @return Returns the internal value for value.
         */
        @JvmStatic
        fun getDefaultWorkingDirectoryInternalPropertyValueFromValue(path: String?): String? {
            if (path == null || path.isEmpty())
                return TermuxPropertyConstants.DEFAULT_IVALUE_DEFAULT_WORKING_DIRECTORY
            val workDir = File(path)
            if (!workDir.exists() || !workDir.isDirectory || !workDir.canRead()) {
                // Fallback to default directory if user configured working directory does not exist,
                // is not a directory or is not readable.
                Logger.logError(LOG_TAG, "The path \"" + path + "\" for the key \"" + TermuxPropertyConstants.KEY_DEFAULT_WORKING_DIRECTORY + "\" does not exist, is not a directory or is not readable. Using default value \"" + TermuxPropertyConstants.DEFAULT_IVALUE_DEFAULT_WORKING_DIRECTORY + "\" instead.")
                return TermuxPropertyConstants.DEFAULT_IVALUE_DEFAULT_WORKING_DIRECTORY
            } else {
                return path
            }
        }

        /**
         * Returns the value itself if it is not {@code null}, otherwise returns {@link TermuxPropertyConstants#DEFAULT_IVALUE_EXTRA_KEYS}.
         *
         * @param value The {@link String} value to convert.
         * @return Returns the internal value for value.
         */
        @JvmStatic
        fun getExtraKeysInternalPropertyValueFromValue(value: String?): String? {
            return SharedProperties.getDefaultIfNullOrEmpty(value, TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS)
        }

        @JvmStatic
        fun getExtraKeys2InternalPropertyValueFromValue(value: String?): String? {
            return SharedProperties.getDefaultIfNullOrEmpty(value, TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS2)
        }

        /**
         * Returns the value itself if it is not {@code null}, otherwise returns {@link TermuxPropertyConstants#DEFAULT_IVALUE_EXTRA_KEYS_STYLE}.
         *
         * @param value {@link String} value to convert.
         * @return Returns the internal value for value.
         */
        @JvmStatic
        fun getExtraKeysStyleInternalPropertyValueFromValue(value: String?): String? {
            return SharedProperties.getDefaultIfNullOrEmpty(value, TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE)
        }

        /**
         * Returns the value itself if it is not {@code null}, otherwise returns {@link TermuxPropertyConstants#DEFAULT_IVALUE_NIGHT_MODE}.
         *
         * @param value {@link String} value to convert.
         * @return Returns the internal value for value.
         */
        @JvmStatic
        fun getNightModeInternalPropertyValueFromValue(value: String?): String? {
            return SharedProperties.getDefaultIfNotInMap(TermuxPropertyConstants.KEY_NIGHT_MODE, TermuxPropertyConstants.MAP_NIGHT_MODE, SharedProperties.toLowerCase(value), TermuxPropertyConstants.DEFAULT_IVALUE_NIGHT_MODE, true, LOG_TAG) as String?
        }

        /**
         * Returns the value itself if it is not {@code null}, otherwise returns {@link TermuxPropertyConstants#DEFAULT_IVALUE_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR}.
         *
         * @param value {@link String} value to convert.
         * @return Returns the internal value for value.
         */
        @JvmStatic
        fun getSoftKeyboardToggleBehaviourInternalPropertyValueFromValue(value: String?): String? {
            return SharedProperties.getDefaultIfNotInMap(TermuxPropertyConstants.KEY_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR, TermuxPropertyConstants.MAP_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR, SharedProperties.toLowerCase(value), TermuxPropertyConstants.DEFAULT_IVALUE_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR, true, LOG_TAG) as String?
        }

        /**
         * Returns the value itself if it is not {@code null}, otherwise returns {@link TermuxPropertyConstants#DEFAULT_IVALUE_VOLUME_KEYS_BEHAVIOUR}.
         *
         * @param value {@link String} value to convert.
         * @return Returns the internal value for value.
         */
        @JvmStatic
        fun getVolumeKeysBehaviourInternalPropertyValueFromValue(value: String?): String? {
            return SharedProperties.getDefaultIfNotInMap(TermuxPropertyConstants.KEY_VOLUME_KEYS_BEHAVIOUR, TermuxPropertyConstants.MAP_VOLUME_KEYS_BEHAVIOUR, SharedProperties.toLowerCase(value), TermuxPropertyConstants.DEFAULT_IVALUE_VOLUME_KEYS_BEHAVIOUR, true, LOG_TAG) as String?
        }

        /**
         * Get the {@link TermuxPropertyConstants#KEY_NIGHT_MODE} value from the properties file on disk.
         */
        @JvmStatic
        fun getNightMode(context: Context?): String? {
            return getTermuxInternalPropertyValue(context, TermuxPropertyConstants.KEY_NIGHT_MODE) as String?
        }

        /**
         * Get the {@link TermuxPropertyConstants#KEY_DRAW_BOLD_TEXT_WITH_BRIGHT_COLORS} value from the properties file on disk.
         */
        @JvmStatic
        fun shouldDrawBoldTextWithBrightColors(context: Context?): Boolean {
            return getTermuxInternalPropertyValue(context, TermuxPropertyConstants.KEY_DRAW_BOLD_TEXT_WITH_BRIGHT_COLORS) as Boolean
        }
    }

    /**
     * The class that implements the {@link SharedPropertiesParser} interface.
     */
    class SharedPropertiesParserClient : SharedPropertiesParser {

        @NonNull
        override fun preProcessPropertiesOnReadFromDisk(@NonNull context: Context, @NonNull properties: Properties): Properties {
            return replaceUseBlackUIProperty(properties)
        }

        /**
         * Override the
         * {@link SharedPropertiesParser#getInternalPropertyValueFromValue(Context,String,String)}
         * interface function.
         */
        override fun getInternalPropertyValueFromValue(@NonNull context: Context, key: String?, value: String?): Any? {
            return getInternalTermuxPropertyValueFromValue(context, key, value)
        }
    }
}
