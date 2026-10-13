package com.termux.shared.net.url

import androidx.annotation.Nullable
import com.termux.shared.data.DataUtils
import com.termux.shared.logger.Logger
import java.net.MalformedURLException
import java.net.URL

class UrlUtils {

    /**
     * The parts of a {@link URL}.
     */
    enum class UrlPart {

        AUTHORITY,
        FILE,
        HOST,
        REF,
        FRAGMENT,
        PATH,
        PORT,
        PROTOCOL,
        QUERY,
        USER_INFO
    }

    companion object {
        private const val LOG_TAG = "UrlUtils"

        /**
         * Join a url base and destination.
         *
         * @param base The base url to open.
         * @param destination The destination url to open.
         * @param logError If an error message should be logged.
         * @return Returns the joined {@link String} Url, otherwise {@code null}.
         */
        @Nullable
        @JvmStatic
        fun joinUrl(base: String?, destination: String?, logError: Boolean): String? {
            if (DataUtils.isNullOrEmpty(base))
                return null
            try {
                return URL(URL(base), destination).toString()
            } catch (e: MalformedURLException) {
                if (logError)
                    Logger.logError(LOG_TAG, "Failed to join url base \"" + base + "\" and destination \"" + destination + "\": " + e.message)
                return null
            }
        }

        /**
         * Get {@link URL} from url string.
         *
         * @param urlString The urlString string.
         * @return Returns the {@link URL} if a valid urlString, otherwise {@code null}.
         */
        @Nullable
        @JvmStatic
        fun getUrl(urlString: String?): URL? {
            if (DataUtils.isNullOrEmpty(urlString))
                return null
            try {
                return URL(urlString)
            } catch (e: MalformedURLException) {
                return null
            }
        }

        /**
         * Get a {@link URL} part from url string.
         *
         * @param urlString The urlString string.
         * @param urlPart The part to get.
         * @return Returns the {@link URL} part if a valid urlString and part, otherwise {@code null}.
         */
        @Nullable
        @JvmStatic
        fun getUrlPart(urlString: String?, urlPart: UrlPart?): String? {
            val url = getUrl(urlString)
            if (url == null)
                return null
            // A null part threw NPE in the Java switch; !! keeps the throw.
            when (urlPart!!) {
                UrlPart.AUTHORITY -> return url.authority
                UrlPart.FILE -> return url.file
                UrlPart.HOST -> return url.host
                UrlPart.REF, UrlPart.FRAGMENT -> return url.ref
                UrlPart.PATH -> return url.path
                UrlPart.PORT -> return url.port.toString()
                UrlPart.PROTOCOL -> return url.protocol
                UrlPart.QUERY -> return url.query
                UrlPart.USER_INFO -> return url.userInfo
            }
        }

        /**
         * Remove "https://www.", "https://", "www.", etc
         */
        @JvmStatic
        fun removeProtocol(urlString: String?): String? {
            if (urlString == null)
                return null
            return urlString.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)".toRegex(), "")
        }

        @JvmStatic
        fun areUrlsEqual(url1: String?, url2: String?): Boolean {
            if (url1 == null && url2 == null)
                return true
            if (url1 == null || url2 == null)
                return false
            // removeProtocol returns non-null for non-null input, so these
            // assertions never throw for a reached call.
            return removeProtocol(url1)!!.replace("/+$".toRegex(), "") == removeProtocol(url2)!!.replace("/+$".toRegex(), "")
        }
    }
}
