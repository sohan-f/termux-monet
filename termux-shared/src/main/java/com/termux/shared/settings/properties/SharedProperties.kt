package com.termux.shared.settings.properties

import android.content.Context
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.google.common.collect.BiMap
import com.google.common.collect.ImmutableBiMap
import com.google.common.primitives.Primitives
import com.termux.shared.file.FileUtils
import com.termux.shared.file.filesystem.FileType
import com.termux.shared.logger.Logger
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Properties

/**
 * An implementation similar to android's {@link android.content.SharedPreferences} interface for
 * reading and writing to and from ".properties" files which also maintains an in-memory cache for
 * the key/value pairs when an instance object is used. Operations are done under
 * synchronization locks and should be thread safe.
 *
 * If {@link SharedProperties} instance object is used, then two types of in-memory cache maps are
 * maintained, one for the literal {@link String} values found in the file for the keys and an
 * additional one that stores (near) primitive {@link Object} values for internal use by the caller.
 *
 * The {@link SharedProperties} also provides static functions that can be used to read properties
 * from files or individual key values or even their internal values. An automatic mapping to a
 * boolean as internal value can also be done. An in-memory cache is not maintained, nor are locks used.
 *
 * This currently only has read support, write support can/will be added later if needed. Check android's
 * SharedPreferencesImpl class for reference implementation.
 *
 * https://cs.android.com/android/platform/superproject/+/android-11.0.0_r3:frameworks/base/core/java/android/app/SharedPreferencesImpl.java
 */
class SharedProperties(
    @NonNull context: Context,
    @Nullable propertiesFile: File?,
    propertiesList: Set<String>?,
    @NonNull sharedPropertiesParser: SharedPropertiesParser
) {

    /**
     * The {@link Properties} object that maintains an in-memory cache of values loaded from the
     * {@link #mPropertiesFile} file. The key/value pairs are of any keys that are found in the file
     * against their literal values in the file.
     */
    private var mProperties: Properties?

    /**
     * The {@link HashMap<>} object that maintains an in-memory cache of internal values for the values
     * loaded from the {@link #mPropertiesFile} file. The key/value pairs are of any keys defined by
     * {@link #mPropertiesList} that are found in the file against their internal {@link Object} values
     * returned by the call to
     * {@link SharedPropertiesParser#getInternalPropertyValueFromValue(Context, String, String)} interface.
     */
    private var mMap: Map<String, Any?>?

    private val mContext: Context

    private val mPropertiesFile: File?

    private val mPropertiesList: Set<String>?

    private val mSharedPropertiesParser: SharedPropertiesParser

    private val mLock = Any()

    init {
        mContext = context.applicationContext
        mPropertiesFile = propertiesFile
        mPropertiesList = propertiesList
        mSharedPropertiesParser = sharedPropertiesParser
        mProperties = Properties()
        mMap = HashMap()
    }

    /**
     * Load the properties defined by {@link #mPropertiesList} or all properties if its {@code null}
     * from the {@link #mPropertiesFile} file to update the {@link #mProperties} and {@link #mMap}
     * in-memory cache.
     * Properties are not loading automatically when constructor is called and must be manually called.
     */
    fun loadPropertiesFromDisk() {
        synchronized(mLock) {
            // Get properties from mPropertiesFile
            var properties = getProperties(false)
            // We still need to load default values into mMap, so we assume no properties defined if
            // reading from mPropertiesFile failed
            if (properties == null)
                properties = Properties()
            val map = HashMap<String, Any?>()
            val newProperties = Properties()
            // Collect the keys explicitly: the platform set returned by
            // stringPropertyNames() carries a flexible element type that
            // never conforms as a type argument, so it is only iterated.
            // Order is irrelevant since keys are processed independently.
            val keys = HashSet<String>()
            val list = mPropertiesList
            if (list != null)
                keys.addAll(list)
            else
                for (k in properties.stringPropertyNames()) keys.add(k)
            for (key in keys) {
                // value will be null if key does not exist in propertiesFile
                val value = properties.getProperty(key)
                // Logger.logVerbose(LOG_TAG, key + " : " + value);
                // Call the {@link SharedPropertiesParser#getInternalPropertyValueFromValue(Context,String,String)}
                // interface method to get the internal value to store in the {@link #mMap}.
                val internalValue = mSharedPropertiesParser.getInternalPropertyValueFromValue(mContext, key, value)
                // If the internal value was successfully added to map, then also add value to newProperties
                // We only store values in-memory defined by propertiesList
                if (putToMap(map, key, internalValue)) {
                    // null internalValue will be put into map
                    // null value will **not** be into properties
                    putToProperties(newProperties, key, value)
                }
            }
            mMap = map
            mProperties = newProperties
        }
    }

    /**
     * Get the {@link Properties} object for the {@link #mPropertiesFile}. The {@link Properties}
     * object will also contain properties not defined by the {@link #mPropertiesList} if cache
     * value is {@code false}.
     *
     * @param cached If {@code true}, then the {@link #mProperties} in-memory cache is returned. Otherwise
     *               the {@link Properties} object is directly read from the {@link #mPropertiesFile}.
     * @return Returns the {@link Properties} object if read from file, otherwise a copy of {@link #mProperties}.
     */
    fun getProperties(cached: Boolean): Properties? {
        synchronized(mLock) {
            return if (cached) {
                if (mProperties == null)
                    mProperties = Properties()
                getPropertiesCopy(mProperties)
            } else {
                getPropertiesFromFile(mContext, mPropertiesFile, mSharedPropertiesParser)
            }
        }
    }

    /**
     * Get the {@link String} value for the key passed from the {@link #mPropertiesFile}.
     *
     * @param key The key to read from the {@link Properties} object.
     * @param cached If {@code true}, then the value is returned from the {@link #mProperties} in-memory cache.
     *               Otherwise the {@link Properties} object is read directly from the {@link #mPropertiesFile}
     *               and value is returned from it against the key.
     * @return Returns the {@link String} object. This will be {@code null} if key is not found.
     */
    fun getProperty(key: String?, cached: Boolean): String? {
        synchronized(mLock) {
            // NPE parity: the Java version threw if getProperties() was
            // null (unreadable file with cached=false), so keep the throw.
            return getProperties(cached)!!.get(key) as String?
        }
    }

    /**
     * Get the {@link #mMap} object for the {@link #mPropertiesFile}. A call to
     * {@link #loadPropertiesFromDisk()} must be made before this.
     *
     * @return Returns a copy of {@link #mMap} object.
     */
    fun getInternalProperties(): Map<String, Any?> {
        synchronized(mLock) {
            if (mMap == null)
                mMap = HashMap()
            // getMapCopy of a non-null map is non-null.
            return getMapCopy(mMap)!!
        }
    }

    /**
     * Get the internal {@link Object} value for the key passed from the {@link #mPropertiesFile}.
     * The value is returned from the {@link #mMap} in-memory cache, so a call to
     * {@link #loadPropertiesFromDisk()} must be made before this.
     *
     * @param key The key to read from the {@link #mMap} object.
     * @return Returns the {@link Object} object. This will be {@code null} if key is not found or
     * if object was {@code null}. Use {@link HashMap#containsKey(Object)} to detect the later.
     * situation.
     */
    fun getInternalProperty(key: String?): Any? {
        synchronized(mLock) {
            // null keys are not allowed to be stored in mMap
            return if (key != null)
                getInternalProperties()[key]
            else
                null
        }
    }

    companion object {
        /**
         * Defines the bidirectional map for boolean values and their internal values
         */
        @JvmField
        val MAP_GENERIC_BOOLEAN: ImmutableBiMap<String, Boolean> = ImmutableBiMap.Builder<String, Boolean>().put("true", true).put("false", false).build()

        /**
         * Defines the bidirectional map for inverted boolean values and their internal values
         */
        @JvmField
        val MAP_GENERIC_INVERTED_BOOLEAN: ImmutableBiMap<String, Boolean> = ImmutableBiMap.Builder<String, Boolean>().put("true", false).put("false", true).build()

        private const val LOG_TAG = "SharedProperties"

        /**
         * A static function to get the {@link Properties} object for the propertiesFile. A lock is not
         * taken when this function is called.
         *
         * @param context The {@link Context} to use to show a flash if an exception is raised while
         *                reading the file. If context is {@code null}, then flash will not be shown.
         * @param propertiesFile The {@link File} to read the {@link Properties} from.
         * @return Returns the {@link Properties} object. It will be {@code null} if an exception is
         * raised while reading the file.
         */
        @JvmStatic
        fun getPropertiesFromFile(context: Context?, propertiesFile: File?, @Nullable sharedPropertiesParser: SharedPropertiesParser?): Properties? {
            val properties = Properties()
            if (propertiesFile == null) {
                Logger.logWarn(LOG_TAG, "Not loading properties since file is null")
                return properties
            }
            try {
                FileInputStream(propertiesFile).use { input ->
                    Logger.logVerbose(LOG_TAG, "Loading properties from \"" + propertiesFile.absolutePath + "\" file")
                    properties.load(InputStreamReader(input, StandardCharsets.UTF_8))
                }
            } catch (e: Exception) {
                if (context != null)
                    Toast.makeText(context, "Could not open properties file \"" + propertiesFile.absolutePath + "\": " + e.message, Toast.LENGTH_LONG).show()
                Logger.logStackTraceWithMessage(LOG_TAG, "Error loading properties file \"" + propertiesFile.absolutePath + "\"", e)
                return null
            }
            return if (sharedPropertiesParser != null && context != null)
                sharedPropertiesParser.preProcessPropertiesOnReadFromDisk(context, properties)
            else
                properties
        }

        /**
         * Returns the first {@link File} found in
         * {@code propertiesFilePaths} from which app properties can be loaded. If the {@link File} found
         * is not a regular file or is not readable, then {@code null} is returned. Symlinks **will not**
         * be followed for potential security reasons.
         *
         * @param propertiesFilePaths The {@link List<String>} containing properties file paths.
         * @param logTag If log tag to use for logging errors.
         * @return Returns the {@link File} object for Termux:Float app properties.
         */
        @JvmStatic
        fun getPropertiesFileFromList(propertiesFilePaths: List<String>?, @NonNull logTag: String): File? {
            if (propertiesFilePaths == null || propertiesFilePaths.size == 0)
                return null
            for (propertiesFilePath in propertiesFilePaths) {
                val propertiesFile = File(propertiesFilePath)
                // Symlinks **will not** be followed.
                val fileType = FileUtils.getFileType(propertiesFilePath, false)
                if (fileType == FileType.REGULAR) {
                    if (propertiesFile.canRead())
                        return propertiesFile
                    else
                        Logger.logWarn(logTag, "Ignoring properties file at \"" + propertiesFilePath + "\" since it is not readable")
                } else if (fileType != FileType.NO_EXIST) {
                    Logger.logWarn(logTag, "Ignoring properties file at \"" + propertiesFilePath + "\" of type: \"" + fileType.getName() + "\"")
                }
            }
            Logger.logDebug(logTag, "No readable properties file found at: " + propertiesFilePaths)
            return null
        }

        @JvmStatic
        fun getProperty(context: Context?, propertiesFile: File?, key: String?, def: String?): String? {
            return getProperty(context, propertiesFile, key, def, null)
        }

        /**
         * A static function to get the {@link String} value for the {@link Properties} key read from
         * the propertiesFile file.
         *
         * @param context The {@link Context} for the {@link #getPropertiesFromFile(Context,File,SharedPropertiesParser)} call.
         * @param propertiesFile The {@link File} to read the {@link Properties} from.
         * @param key The key to read.
         * @param def The default value.
         * @param sharedPropertiesParser The implementation of the {@link SharedPropertiesParser} interface.
         * @return Returns the {@link String} object. This will be {@code null} if key is not found.
         */
        @JvmStatic
        fun getProperty(context: Context?, propertiesFile: File?, key: String?, def: String?, @Nullable sharedPropertiesParser: SharedPropertiesParser?): String? {
            return getDefaultIfNull(getDefaultIfNull(getPropertiesFromFile(context, propertiesFile, sharedPropertiesParser), Properties())?.get(key), def) as String?
        }

        /**
         * A static function to get the internal {@link Object} value for the {@link String} value for
         * the {@link Properties} key read from the propertiesFile file.
         *
         * @param context The {@link Context} for the {@link #getPropertiesFromFile(Context,File,SharedPropertiesParser)} call.
         * @param propertiesFile The {@link File} to read the {@link Properties} from.
         * @param key The key to read.
         * @param sharedPropertiesParser The implementation of the {@link SharedPropertiesParser} interface.
         * @return Returns the {@link String} Object returned by the call to
         * {@link SharedPropertiesParser#getInternalPropertyValueFromValue(Context,String,String)}.
         */
        @JvmStatic
        fun getInternalProperty(context: Context?, propertiesFile: File?, key: String?, @NonNull sharedPropertiesParser: SharedPropertiesParser): Any? {
            val value = getDefaultIfNull(getPropertiesFromFile(context, propertiesFile, sharedPropertiesParser), Properties())?.get(key) as String?
            // Call the {@link SharedPropertiesParser#getInternalPropertyValueFromValue(Context,String,String)}
            // interface method to get the internal value to return.
            // The interface contract is non-null context; !! keeps the NPE
            // instead of failing to compile against the stub.
            return sharedPropertiesParser.getInternalPropertyValueFromValue(context!!, key, value)
        }

        @JvmStatic
        fun isPropertyValueTrue(context: Context?, propertiesFile: File?, key: String?, logErrorOnInvalidValue: Boolean): Boolean {
            return isPropertyValueTrue(context, propertiesFile, key, logErrorOnInvalidValue, null)
        }

        /**
         * A static function to check if the value is {@code true} for {@link Properties} key read from
         * the propertiesFile file.
         *
         * @param context The {@link Context} for the {@link #getPropertiesFromFile(Context,File,SharedPropertiesParser)} call.
         * @param propertiesFile The {@link File} to read the {@link Properties} from.
         * @param key The key to read.
         * @param logErrorOnInvalidValue If {@code true}, then an error will be logged if key value
         *                               was found in {@link Properties} but was invalid.
         * @param sharedPropertiesParser The implementation of the {@link SharedPropertiesParser} interface.
         * @return Returns the {@code true} if the {@link Properties} key {@link String} value equals "true",
         * regardless of case. If the key does not exist in the file or does not equal "true", then
         * {@code false} will be returned.
         */
        @JvmStatic
        fun isPropertyValueTrue(context: Context?, propertiesFile: File?, key: String?, logErrorOnInvalidValue: Boolean, @Nullable sharedPropertiesParser: SharedPropertiesParser?): Boolean {
            return getBooleanValueForStringValue(key, getProperty(context, propertiesFile, key, null, sharedPropertiesParser), false, logErrorOnInvalidValue, LOG_TAG)
        }

        @JvmStatic
        fun isPropertyValueFalse(context: Context?, propertiesFile: File?, key: String?, logErrorOnInvalidValue: Boolean): Boolean {
            return isPropertyValueFalse(context, propertiesFile, key, logErrorOnInvalidValue, null)
        }

        /**
         * A static function to check if the value is {@code false} for {@link Properties} key read from
         * the propertiesFile file.
         *
         * @param context The {@link Context} for the {@link #getPropertiesFromFile(Context,File,SharedPropertiesParser)} call.
         * @param propertiesFile The {@link File} to read the {@link Properties} from.
         * @param key The key to read.
         * @param logErrorOnInvalidValue If {@code true}, then an error will be logged if key value
         *                               was found in {@link Properties} but was invalid.
         * @param sharedPropertiesParser The implementation of the {@link SharedPropertiesParser} interface.
         * @return Returns the {@code true} if the {@link Properties} key {@link String} value equals "false",
         * regardless of case. If the key does not exist in the file or does not equal "false", then
         * {@code true} will be returned.
         */
        @JvmStatic
        fun isPropertyValueFalse(context: Context?, propertiesFile: File?, key: String?, logErrorOnInvalidValue: Boolean, @Nullable sharedPropertiesParser: SharedPropertiesParser?): Boolean {
            return getInvertedBooleanValueForStringValue(key, getProperty(context, propertiesFile, key, null, sharedPropertiesParser), true, logErrorOnInvalidValue, LOG_TAG)
        }

        /**
         * Put a value in a {@link #mMap}.
         * The key cannot be {@code null}.
         * Only {@code null}, primitive or their wrapper classes or String class objects are allowed to be added to
         * the map, although this limitation may be changed.
         *
         * @param map The {@link Map} object to add value to.
         * @param key The key for which to add the value to the map.
         * @param value The {@link Object} to add to the map.
         * @return Returns {@code true} if value was successfully added, otherwise {@code false}.
         */
        @JvmStatic
        fun putToMap(map: HashMap<String, Any?>?, key: String?, value: Any?): Boolean {
            if (map == null) {
                // Fixed: message named the wrong function (putToProperties).
                Logger.logError(LOG_TAG, "Map passed to SharedProperties.putToMap() is null")
                return false
            }
            // null keys are not allowed to be stored in mMap
            if (key == null) {
                Logger.logError(LOG_TAG, "Cannot put a null key into properties map")
                return false
            }
            var put = false
            if (value != null) {
                val clazz = value.javaClass
                if (clazz.isPrimitive || Primitives.isWrapperType(clazz) || value is String) {
                    put = true
                }
            } else {
                put = true
            }
            if (put) {
                map.put(key, value)
                return true
            } else {
                Logger.logError(LOG_TAG, "Cannot put a non-primitive value for the key \"" + key + "\" into properties map")
                return false
            }
        }

        /**
         * Put a value in a {@link Map}.
         * The key cannot be {@code null}.
         * Passing {@code null} as the value argument is equivalent to removing the key from the
         * properties.
         *
         * @param properties The {@link Properties} object to add value to.
         * @param key The key for which to add the value to the properties.
         * @param value The {@link String} to add to the properties.
         * @return Returns {@code true} if value was successfully added, otherwise {@code false}.
         */
        @JvmStatic
        fun putToProperties(properties: Properties?, key: String?, value: String?): Boolean {
            if (properties == null) {
                Logger.logError(LOG_TAG, "Properties passed to SharedProperties.putToProperties() is null")
                return false
            }
            // null keys are not allowed to be stored in mMap
            if (key == null) {
                Logger.logError(LOG_TAG, "Cannot put a null key into properties")
                return false
            }
            if (value != null) {
                properties.put(key, value)
                return true
            } else {
                properties.remove(key)
            }
            return true
        }

        @JvmStatic
        fun getPropertiesCopy(inputProperties: Properties?): Properties? {
            if (inputProperties == null)
                return null
            val outputProperties = Properties()
            for (key in inputProperties.stringPropertyNames()) {
                outputProperties.put(key, inputProperties.get(key))
            }
            return outputProperties
        }

        @JvmStatic
        fun getMapCopy(map: Map<String, Any?>?): Map<String, Any?>? {
            if (map == null)
                return null
            return HashMap(map)
        }

        /**
         * Get the boolean value for the {@link String} value.
         *
         * @param value The {@link String} value to convert.
         * @return Returns {@code true} or {@code false} if value is the literal string "true" or "false" respectively,
         * regardless of case. Otherwise returns {@code null}.
         */
        @JvmStatic
        fun getBooleanValueForStringValue(value: String?): Boolean? {
            return MAP_GENERIC_BOOLEAN[toLowerCase(value)]
        }

        /**
         * Get the boolean value for the {@link String} value.
         *
         * @param value The {@link String} value to convert.
         * @param def The default {@code boolean} value to return.
         * @param logErrorOnInvalidValue If {@code true}, then an error will be logged if {@code value}
         *                               was not {@code null} and was invalid.
         * @param logTag If log tag to use for logging errors.
         * @return Returns {@code true} or {@code false} if value is the literal string "true" or "false" respectively,
         * regardless of case. Otherwise returns default value.
         */
        @JvmStatic
        fun getBooleanValueForStringValue(key: String?, value: String?, def: Boolean, logErrorOnInvalidValue: Boolean, logTag: String?): Boolean {
            return getDefaultIfNotInMap(key, MAP_GENERIC_BOOLEAN, toLowerCase(value), def, logErrorOnInvalidValue, logTag) as Boolean
        }

        /**
         * Get the inverted boolean value for the {@link String} value.
         *
         * @param value The {@link String} value to convert.
         * @param def The default {@code boolean} value to return.
         * @param logErrorOnInvalidValue If {@code true}, then an error will be logged if {@code value}
         *                               was not {@code null} and was invalid.
         * @param logTag If log tag to use for logging errors.
         * @return Returns {@code true} or {@code false} if value is the literal string "false" or "true" respectively,
         * regardless of case. Otherwise returns default value.
         */
        @JvmStatic
        fun getInvertedBooleanValueForStringValue(key: String?, value: String?, def: Boolean, logErrorOnInvalidValue: Boolean, logTag: String?): Boolean {
            return getDefaultIfNotInMap(key, MAP_GENERIC_INVERTED_BOOLEAN, toLowerCase(value), def, logErrorOnInvalidValue, logTag) as Boolean
        }

        /**
         * Get the value for the {@code inputValue} {@link Object} key from a {@link BiMap<>}, otherwise
         * default value if key not found in {@code map}.
         *
         * @param key The shared properties {@link String} key value for which the value is being returned.
         * @param map The {@link BiMap<>} value to get the value from.
         * @param inputValue The {@link Object} key value of the map.
         * @param defaultOutputValue The default {@link boolean} value to return if {@code inputValue} not found in map.
         *            The default value must exist as a value in the {@link BiMap<>} passed.
         * @param logErrorOnInvalidValue If {@code true}, then an error will be logged if {@code inputValue}
         *                               was not {@code null} and was not found in the map.
         * @param logTag If log tag to use for logging errors.
         * @return Returns the value for the {@code inputValue} key from the map if it exists. Otherwise
         * returns default value.
         */
        @JvmStatic
        fun getDefaultIfNotInMap(key: String?, @NonNull map: BiMap<*, *>, inputValue: Any?, defaultOutputValue: Any?, logErrorOnInvalidValue: Boolean, logTag: String?): Any? {
            val outputValue: Any? = map[inputValue]
            if (outputValue == null) {
                val defaultInputValue: Any? = map.inverse()[defaultOutputValue]
                if (defaultInputValue == null)
                    Logger.logError(LOG_TAG, "The default output value \"" + defaultOutputValue + "\" for the key \"" + key + "\" does not exist as a value in the BiMap passed to getDefaultIfNotInMap(): " + map.values)
                if (logErrorOnInvalidValue && inputValue != null) {
                    if (key != null)
                        Logger.logError(logTag, "The value \"" + inputValue + "\" for the key \"" + key + "\" is invalid. Using default value \"" + defaultInputValue + "\" instead.")
                    else
                        Logger.logError(logTag, "The value \"" + inputValue + "\" is invalid. Using default value \"" + defaultInputValue + "\" instead.")
                }
                return defaultOutputValue
            } else {
                return outputValue
            }
        }

        /**
         * Get the {@code int} {@code value} as is if between {@code min} and {@code max} (inclusive), otherwise
         * return default value.
         *
         * @param key The shared properties {@link String} key value for which the value is being returned.
         * @param value The {@code int} value to check.
         * @param def The default {@code int} value if {@code value} not in range.
         * @param min The min allowed {@code int} value.
         * @param max The max allowed {@code int} value.
         * @param logErrorOnInvalidValue If {@code true}, then an error will be logged if {@code value}
         *                               not in range.
         * @param ignoreErrorIfValueZero If logging error should be ignored if value equals 0.
         * @param logTag If log tag to use for logging errors.
         * @return Returns the {@code value} as is if within range. Otherwise returns default value.
         */
        @JvmStatic
        fun getDefaultIfNotInRange(key: String?, value: Int, def: Int, min: Int, max: Int, logErrorOnInvalidValue: Boolean, ignoreErrorIfValueZero: Boolean, logTag: String?): Int {
            if (value < min || value > max) {
                if (logErrorOnInvalidValue && (!ignoreErrorIfValueZero || value != 0)) {
                    if (key != null)
                        Logger.logError(logTag, "The value \"" + value + "\" for the key \"" + key + "\" is not within the range " + min + "-" + max + " (inclusive). Using default value \"" + def + "\" instead.")
                    else
                        Logger.logError(logTag, "The value \"" + value + "\" is not within the range " + min + "-" + max + " (inclusive). Using default value \"" + def + "\" instead.")
                }
                return def
            } else {
                return value
            }
        }

        /**
         * Get the {@code float} {@code value} as is if between {@code min} and {@code max} (inclusive), otherwise
         * return default value.
         *
         * @param key The shared properties {@link String} key value for which the value is being returned.
         * @param value The {@code float} value to check.
         * @param def The default {@code float} value if {@code value} not in range.
         * @param min The min allowed {@code float} value.
         * @param max The max allowed {@code float} value.
         * @param logErrorOnInvalidValue If {@code true}, then an error will be logged if {@code value}
         *                               not in range.
         * @param ignoreErrorIfValueZero If logging error should be ignored if value equals 0.
         * @param logTag If log tag to use for logging errors.
         * @return Returns the {@code value} as is if within range. Otherwise returns default value.
         */
        @JvmStatic
        fun getDefaultIfNotInRange(key: String?, value: Float, def: Float, min: Float, max: Float, logErrorOnInvalidValue: Boolean, ignoreErrorIfValueZero: Boolean, logTag: String?): Float {
            if (value < min || value > max) {
                if (logErrorOnInvalidValue && (!ignoreErrorIfValueZero || value != 0f)) {
                    if (key != null)
                        Logger.logError(logTag, "The value \"" + value + "\" for the key \"" + key + "\" is not within the range " + min + "-" + max + " (inclusive). Using default value \"" + def + "\" instead.")
                    else
                        Logger.logError(logTag, "The value \"" + value + "\" is not within the range " + min + "-" + max + " (inclusive). Using default value \"" + def + "\" instead.")
                }
                return def
            } else {
                return value
            }
        }

        /**
         * Get the object itself if it is not {@code null}, otherwise default.
         *
         * @param object The {@link Object} to check.
         * @param def The default {@link Object}.
         * @return Returns {@code object} if it is not {@code null}, otherwise returns {@code def}.
         */
        @JvmStatic
        fun <T> getDefaultIfNull(obj: T?, def: T?): T? {
            return if (obj == null) def else obj
        }

        /**
         * Get the {@link String} object itself if it is not {@code null} or empty, otherwise default.
         *
         * @param object The {@link String} to check.
         * @param def The default {@link String}.
         * @return Returns {@code object} if it is not {@code null}, otherwise returns {@code def}.
         */
        @JvmStatic
        fun getDefaultIfNullOrEmpty(obj: String?, def: String?): String? {
            return if (obj == null || obj.isEmpty()) def else obj
        }

        /**
         * Covert the {@link String} value to lowercase.
         *
         * @param value The {@link String} value to convert.
         * @return Returns the lowercased value.
         */
        @JvmStatic
        fun toLowerCase(value: String?): String? {
            return if (value == null)
                null
            else
                // lowercase() keeps the default-locale behaviour of Java toLowerCase().
                value.lowercase()
        }
    }
}
