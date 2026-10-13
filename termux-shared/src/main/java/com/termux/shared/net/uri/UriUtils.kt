package com.termux.shared.net.uri

import android.net.Uri
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.termux.shared.data.DataUtils
import com.termux.shared.file.FileUtils

class UriUtils {

    companion object {
        /**
         * Get the full file path from a {@link Uri} including the fragment.
         *
         * If the {@link Uri} was created from file path with {@link Uri#parse(String)}, like "am"
         * command "-d" option does, and the path contained a "#", then anything after it would become
         * the fragment and {@link Uri#getPath()} will only return the path before it, which would be
         * invalid. The fragment must be manually appended to the path to get the full path.
         *
         * If the {@link Uri} was created with {@link Uri.Builder} and path was set
         * with {@link Uri.Builder#path(String)}, then "#" will automatically be encoded to "%23"
         * and separate fragment will not exist.
         *
         * @param uri The {@link Uri} to get file path from.
         * @return Returns the file path if found, otherwise {@code null}.
         */
        @Nullable
        @JvmStatic
        fun getUriFilePathWithFragment(uri: Uri?): String? {
            if (uri == null)
                return null
            // Typed nullable so the !! below is provably necessary:
            // isNullOrEmpty carries no contract for smart casts.
            val path: String? = uri.path
            if (DataUtils.isNullOrEmpty(path))
                return null
            val fragment = uri.fragment
            return path!! + if (DataUtils.isNullOrEmpty(fragment)) "" else "#$fragment"
        }

        /**
         * Get the file basename from a {@link Uri}. The file basename is anything after last forward
         * slash "/" in the path, or the path itself if its not found.
         *
         * @param uri The {@link Uri} to get basename from.
         * @param withFragment If the {@link Uri} fragment should be included in basename.
         * @return Returns the file basename if found, otherwise {@code null}.
         */
        @Nullable
        @JvmStatic
        fun getUriFileBasename(uri: Uri?, withFragment: Boolean): String? {
            if (uri == null)
                return null
            val path: String?
            if (withFragment) {
                path = getUriFilePathWithFragment(uri)
            } else {
                path = uri.path
                if (DataUtils.isNullOrEmpty(path))
                    return null
            }
            return FileUtils.getFileBasename(path)
        }

        /**
         * Get {@link UriScheme#SCHEME_FILE} {@link Uri} for path.
         *
         * @param path The path for the {@link Uri}.
         * @return Returns the {@link Uri}.
         */
        @JvmStatic
        fun getFileUri(@NonNull path: String?): Uri {
            // Nullable to preserve the Java null tolerance: Builder.path()
            // accepts null and just builds a pathless Uri instead of throwing.
            return Uri.Builder().scheme(UriScheme.SCHEME_FILE).path(path).build()
        }

        /**
         * Get {@link UriScheme#SCHEME_FILE} {@link Uri} for path.
         *
         * @param authority The authority for the {@link Uri}.
         * @param path The path for the {@link Uri}.
         * @return Returns the {@link Uri}.
         */
        @JvmStatic
        fun getFileUri(@NonNull authority: String?, @NonNull path: String?): Uri {
            return Uri.Builder().scheme(UriScheme.SCHEME_FILE).authority(authority).path(path).build()
        }

        /**
         * Get {@link UriScheme#SCHEME_CONTENT} {@link Uri} for path.
         *
         * @param path The path for the {@link Uri}.
         * @return Returns the {@link Uri}.
         */
        @JvmStatic
        fun getContentUri(@NonNull path: String?): Uri {
            return Uri.Builder().scheme(UriScheme.SCHEME_CONTENT).path(path).build()
        }

        /**
         * Get {@link UriScheme#SCHEME_CONTENT} {@link Uri} for path.
         *
         * @param authority The authority for the {@link Uri}.
         * @param path The path for the {@link Uri}.
         * @return Returns the {@link Uri}.
         */
        @JvmStatic
        fun getContentUri(@NonNull authority: String?, @NonNull path: String?): Uri {
            return Uri.Builder().scheme(UriScheme.SCHEME_CONTENT).authority(authority).path(path).build()
        }
    }
}
