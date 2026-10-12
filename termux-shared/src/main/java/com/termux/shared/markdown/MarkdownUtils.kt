package com.termux.shared.markdown

import android.content.Context
import android.graphics.Typeface
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.BulletSpan
import android.text.style.QuoteSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.util.Linkify
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import com.google.common.base.Strings
import com.termux.shared.R
import com.termux.shared.theme.ThemeUtils
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.node.BlockQuote
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.ListItem
import org.commonmark.node.StrongEmphasis
import java.util.regex.Matcher
import java.util.regex.Pattern

class MarkdownUtils {

    companion object {
        const val backtick = "`"

        @JvmField
        val backticksPattern: Pattern = Pattern.compile("($backtick+)")

        /**
         * Get the markdown code {@link String} for a {@link String}. This ensures all backticks "`" are
         * properly escaped so that markdown does not break.
         *
         * @param string The {@link String} to convert.
         * @param codeBlock If the {@link String} is to be converted to a code block or inline code.
         * @return Returns the markdown code {@link String}.
         */
        @JvmStatic
        fun getMarkdownCodeForString(string: String?, codeBlock: Boolean): String? {
            if (string == null)
                return null
            if (string.isEmpty())
                return ""
            val maxConsecutiveBackTicksCount = getMaxConsecutiveBackTicksCount(string)
            // markdown requires surrounding backticks count to be at least one more than the count
            // of consecutive ticks in the string itself
            val backticksCountToUse: Int
            if (codeBlock)
                backticksCountToUse = maxConsecutiveBackTicksCount + 3
            else
                backticksCountToUse = maxConsecutiveBackTicksCount + 1
            // create a string with n backticks where n==backticksCountToUse
            val backticksToUse = Strings.repeat(backtick, backticksCountToUse)
            if (codeBlock)
                return backticksToUse + "\n" + string + "\n" + backticksToUse
            else {
                // add a space to any prefixed or suffixed backtick characters
                var result = string
                if (result.startsWith(backtick))
                    result = " " + result
                if (result.endsWith(backtick))
                    result = result + " "
                return backticksToUse + result + backticksToUse
            }
        }

        /**
         * Get the max consecutive backticks "`" in a {@link String}.
         *
         * @param string The {@link String} to check.
         * @return Returns the max consecutive backticks count.
         */
        @JvmStatic
        fun getMaxConsecutiveBackTicksCount(string: String?): Int {
            if (string.isNullOrEmpty())
                return 0
            var maxCount = 0
            var matchCount: Int
            var match: String?
            val matcher: Matcher = backticksPattern.matcher(string)
            while (matcher.find()) {
                match = matcher.group(1)
                matchCount = match?.length ?: 0
                if (matchCount > maxCount)
                    maxCount = matchCount
            }
            return maxCount
        }

        @JvmStatic
        fun getLiteralSingleLineMarkdownStringEntry(label: String?, `object`: Any?, def: String?): String {
            return "**" + label + "**: " + (if (`object` != null) `object`.toString() else def) + "  "
        }

        @JvmStatic
        fun getSingleLineMarkdownStringEntry(label: String?, `object`: Any?, def: String?): String {
            if (`object` != null)
                return "**" + label + "**: " + getMarkdownCodeForString(`object`.toString(), false) + "  "
            else
                return "**" + label + "**: " + def + "  "
        }

        @JvmStatic
        fun getMultiLineMarkdownStringEntry(label: String?, `object`: Any?, def: String?): String {
            if (`object` != null)
                return "**" + label + "**:\n" + getMarkdownCodeForString(`object`.toString(), true) + "\n"
            else
                return "**" + label + "**: " + def + "\n"
        }

        @JvmStatic
        fun getLinkMarkdownString(label: String, url: String?): String {
            if (url != null)
                // Literal replaces: ] and ) are matched literally by the
                // original regexes too, so the output is identical without one.
                return "[" + label.replace("]", "\\]") + "](" + url.replace(")", "\\)") + ")"
            else
                return label
        }

        /**
         * Check following for more info:
         * https://github.com/noties/Markwon/tree/v4.6.2/app-sample
         * https://noties.io/Markwon/docs/v4/recycler/
         * https://github.com/noties/Markwon/blob/v4.6.2/app-sample/src/main/java/io/noties/markwon/app/readme/ReadMeActivity.kt
         */
        @JvmStatic
        fun getRecyclerMarkwonBuilder(context: Context): Markwon {
            return Markwon.builder(context).usePlugin(LinkifyPlugin.create(Linkify.EMAIL_ADDRESSES or Linkify.WEB_URLS)).usePlugin(object : AbstractMarkwonPlugin() {

                override fun configureVisitor(@NonNull builder: MarkwonVisitor.Builder) {
                    builder.on(FencedCodeBlock::class.java) { visitor, fencedCodeBlock ->
                        // we actually won't be applying code spans here, as our custom xml view will
                        // draw background and apply mono typeface
                        //
                        // NB the `trim` operation on literal (as code will have a new line at the end)
                        val code = visitor.configuration().syntaxHighlight().highlight(fencedCodeBlock.getInfo(), fencedCodeBlock.getLiteral().trim())
                        visitor.builder().append(code)
                    }
                }

                override fun configureSpansFactory(@NonNull builder: MarkwonSpansFactory.Builder) {
                    // Do not change color for night themes
                    if (!ThemeUtils.isNightModeEnabled(context)) {
                        builder.// set color for inline code
                        // Explicit Any: heterogeneous arrayOf would otherwise infer
                    // an intersection type (future compiler error).
                    setFactory(Code::class.java) { configuration, props -> arrayOf<Any>(BackgroundColorSpan(ContextCompat.getColor(context, R.color.background_markdown_code_inline))) }
                    }
                }
            }).build()
        }

        /**
         * Check following for more info:
         * https://github.com/noties/Markwon/tree/v4.6.2/app-sample
         * https://github.com/noties/Markwon/blob/v4.6.2/app-sample/src/main/java/io/noties/markwon/app/samples/notification/NotificationSample.java
         */
        @JvmStatic
        fun getSpannedMarkwonBuilder(context: Context): Markwon {
            return Markwon.builder(context).usePlugin(StrikethroughPlugin.create()).usePlugin(object : AbstractMarkwonPlugin() {

                override fun configureSpansFactory(@NonNull builder: MarkwonSpansFactory.Builder) {
                    builder.setFactory(Emphasis::class.java) { configuration, props -> StyleSpan(Typeface.ITALIC) }.setFactory(StrongEmphasis::class.java) { configuration, props -> StyleSpan(Typeface.BOLD) }.setFactory(BlockQuote::class.java) { configuration, props -> QuoteSpan() }.setFactory(Strikethrough::class.java) { configuration, props -> StrikethroughSpan() }.// NB! notification does not handle background color
                    // Explicit Any: heterogeneous arrayOf would otherwise infer
                    // an intersection type (future compiler error).
                    setFactory(Code::class.java) { configuration, props -> arrayOf<Any>(BackgroundColorSpan(ContextCompat.getColor(context, R.color.background_markdown_code_inline)), TypefaceSpan("monospace"), AbsoluteSizeSpan(48)) }.// NB! both ordered and bullet list items
                    setFactory(ListItem::class.java) { configuration, props -> BulletSpan() }
                }
            }).build()
        }

        @JvmStatic
        fun getSpannedMarkdownText(context: Context?, string: String?): Spanned? {
            if (context == null || string == null)
                return null
            val markwon = getSpannedMarkwonBuilder(context)
            return markwon.toMarkdown(string)
        }
    }
}
