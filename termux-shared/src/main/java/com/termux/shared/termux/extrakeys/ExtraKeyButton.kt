package com.termux.shared.termux.extrakeys

import android.text.TextUtils
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import org.json.JSONException
import org.json.JSONObject

class ExtraKeyButton {

    /**
     * The key name for the name of the extra key if using a dict to define the extra key. {key: name, ...}
     */
    companion object {
        const val KEY_KEY_NAME = "key"

        /**
         * The key name for the macro value of the extra key if using a dict to define the extra key. {macro: value, ...}
         */
        const val KEY_MACRO = "macro"

        /**
         * The key name for the alternate display name of the extra key if using a dict to define the extra key. {display: name, ...}
         */
        const val KEY_DISPLAY_NAME = "display"

        /**
         * The key name for the nested dict to define popup extra key info if using a dict to define the extra key. {popup: {key: name, ...}, ...}
         */
        const val KEY_POPUP = "popup"

        /**
         * Replace the alias with its actual key name if found in extraKeyAliasMap.
         */
        @JvmStatic
        fun replaceAlias(@NonNull extraKeyAliasMap: ExtraKeysConstants.ExtraKeyDisplayMap, key: String?): String {
            return extraKeyAliasMap.get(key, key)
        }
    }

    /**
     * The key that will be sent to the terminal, either a control character, like defined in
     * {@link ExtraKeysConstants#PRIMARY_KEY_CODES_FOR_STRINGS} (LEFT, RIGHT, PGUP...) or some text.
     */
    private val key: String

    /**
     * If the key is a macro, i.e. a sequence of keys separated by space.
     */
    private val macro: Boolean

    /**
     * The text that will be displayed on the button.
     */
    private val display: String

    /**
     * The {@link ExtraKeyButton} containing the information of the popup button (triggered by swipe up).
     */
    @Nullable
    private val popup: ExtraKeyButton?

    /**
     * Initialize a {@link ExtraKeyButton}.
     *
     * @param config The {@link JSONObject} containing the info to create the {@link ExtraKeyButton}.
     * @param extraKeyDisplayMap The {@link ExtraKeysConstants.ExtraKeyDisplayMap} that defines the
     *                           display text mapping for the keys if a custom value is not defined
     *                           by {@link #KEY_DISPLAY_NAME}.
     * @param extraKeyAliasMap The {@link ExtraKeysConstants.ExtraKeyDisplayMap} that defines the
     *                           aliases for the actual key names.
     */
    @Throws(JSONException::class)
    constructor(@NonNull config: JSONObject, @NonNull extraKeyDisplayMap: ExtraKeysConstants.ExtraKeyDisplayMap, @NonNull extraKeyAliasMap: ExtraKeysConstants.ExtraKeyDisplayMap) : this(config, null, extraKeyDisplayMap, extraKeyAliasMap)

    /**
     * Initialize a {@link ExtraKeyButton}.
     *
     * @param config The {@link JSONObject} containing the info to create the {@link ExtraKeyButton}.
     * @param popup The {@link ExtraKeyButton} optional {@link #popup} button.
     * @param extraKeyDisplayMap The {@link ExtraKeysConstants.ExtraKeyDisplayMap} that defines the
     *                           display text mapping for the keys if a custom value is not defined
     *                           by {@link #KEY_DISPLAY_NAME}.
     * @param extraKeyAliasMap The {@link ExtraKeysConstants.ExtraKeyDisplayMap} that defines the
     *                           aliases for the actual key names.
     */
    @Throws(JSONException::class)
    constructor(@NonNull config: JSONObject, @Nullable popup: ExtraKeyButton?, @NonNull extraKeyDisplayMap: ExtraKeysConstants.ExtraKeyDisplayMap, @NonNull extraKeyAliasMap: ExtraKeysConstants.ExtraKeyDisplayMap) {
        val keyFromConfig = getStringFromJson(config, KEY_KEY_NAME)
        val macroFromConfig = getStringFromJson(config, KEY_MACRO)
        val keys: Array<String>
        if (keyFromConfig != null && macroFromConfig != null) {
            throw JSONException("Both key and macro can't be set for the same key. key: \"" + keyFromConfig + "\", macro: \"" + macroFromConfig + "\"")
        } else if (keyFromConfig != null) {
            keys = arrayOf(keyFromConfig)
            this.macro = false
        } else if (macroFromConfig != null) {
            // Java split(" ") dropped trailing empties while Kotlin keeps
            // them; dropLastWhile restores exact Java semantics (middle
            // empties are kept by both).
            keys = macroFromConfig.split(" ").dropLastWhile { it.isEmpty() }.toTypedArray()
            this.macro = true
        } else {
            throw JSONException("All keys have to specify either key or macro")
        }
        for (i in keys.indices) {
            keys[i] = replaceAlias(extraKeyAliasMap, keys[i])
        }
        this.key = TextUtils.join(" ", keys)
        val displayFromConfig = getStringFromJson(config, KEY_DISPLAY_NAME)
        if (displayFromConfig != null) {
            this.display = displayFromConfig
        } else {
            this.display = keys.map { key -> extraKeyDisplayMap.get(key, key) }.joinToString(" ")
        }
        this.popup = popup
    }

    fun getStringFromJson(@NonNull config: JSONObject, @NonNull key: String): String? {
        return try {
            config.getString(key)
        } catch (e: JSONException) {
            null
        }
    }

    /**
     * Get {@link #key}.
     */
    fun getKey(): String {
        return key
    }

    /**
     * Check whether a {@link #macro} is defined or not.
     */
    fun isMacro(): Boolean {
        return macro
    }

    /**
     * Get {@link #display}.
     */
    fun getDisplay(): String {
        return display
    }

    /**
     * Get {@link #popup}.
     */
    @Nullable
    fun getPopup(): ExtraKeyButton? {
        return popup
    }
}
