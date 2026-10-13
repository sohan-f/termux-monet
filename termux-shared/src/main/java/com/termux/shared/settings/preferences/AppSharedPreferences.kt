package com.termux.shared.settings.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.NonNull
import androidx.annotation.Nullable

/**
 * A class that holds {@link SharedPreferences} objects for apps.
 */
open class AppSharedPreferences protected constructor(
    /**
     * The {@link Context} for operations.
     */
    @JvmField protected val mContext: Context,

    /**
     * The {@link SharedPreferences} that ideally should be created with {@link SharedPreferenceUtils#getPrivateSharedPreferences(Context, String)}.
     */
    @JvmField protected val mSharedPreferences: SharedPreferences?,

    /**
     * The {@link SharedPreferences}that ideally should be created with {@link SharedPreferenceUtils#getPrivateAndMultiProcessSharedPreferences(Context, String)}.
     */
    @JvmField protected val mMultiProcessSharedPreferences: SharedPreferences?
) {

    protected constructor(@NonNull context: Context, @Nullable sharedPreferences: SharedPreferences?) : this(context, sharedPreferences, null)

    /**
     * Get {@link #mContext}.
     */
    fun getContext(): Context {
        return mContext
    }

    /**
     * Get {@link #mSharedPreferences}.
     */
    fun getSharedPreferences(): SharedPreferences? {
        return mSharedPreferences
    }

    /**
     * Get {@link #mMultiProcessSharedPreferences}.
     */
    fun getMultiProcessSharedPreferences(): SharedPreferences? {
        return mMultiProcessSharedPreferences
    }
}
