package com.mapswithme.util

import android.text.Editable
import android.text.TextWatcher
import android.util.Pair
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language
import java.util.*

object StringUtils {
    fun formatUsingUsLocale(pattern: String?, vararg args: Any?): String {
        return String.format(Locale.US, pattern!!, *args)
    }

    @JvmStatic external fun nativeIsHtml(text: String?): Boolean
    @JvmStatic external fun nativeContainsNormalized(
        str: String?,
        substr: String?
    ): Boolean

    @JvmStatic external fun nativeFilterContainsNormalized(
        strings: Array<String>?,
        substr: String?
    ): Array<String>?

    @JvmStatic external fun nativeFormatSpeedAndUnits(metersPerSecond: Double): Pair<String?, String>?
    /**
     * Removes html tags, generated from edittext content after it's transformed to html.
     * In version 4.3.1 we converted descriptions, entered by users, to html automatically. Later html conversion was cancelled, but those converted descriptions should be converted back to
     * plain text, that's why that ugly util is introduced.
     *
     * @param text source text
     * @return result text
     */
    fun removeEditTextHtmlTags(text: String): String {
        return text.replace("</p>".toRegex(), "").replace("<br>".toRegex(), "")
            .replace("<p dir=\"ltr\">".toRegex(), "")
    }

    /**
     * Formats size in bytes to "x MB" or "x.x GB" format.
     * Small values rounded to 1 MB without fractions.
     *
     * @param size size in bytes
     * @return formatted string
     */
    @JvmStatic
    fun getFileSizeString(size: Long): String {
        if (size < Constants.GB) {
            var value = (size.toFloat() / Constants.MB + 0.5f).toInt()
            if (value == 0) value = 1
            return java.lang.String.format(
                Locale.US,
                "%1\$d %2\$s",
                value,
                MwmApplication.get().getString(R.string.mb)
            )
        }
        val value: Float = size.toFloat() / Constants.GB
        return java.lang.String.format(
            Locale.US,
            "%1$.1f %2\$s",
            value,
            MwmApplication.get().getString(R.string.gb)
        )
    }

    fun isRtl(): Boolean {
        val defLocale = Locale.getDefault()
        return Character.getDirectionality(defLocale.getDisplayName(defLocale)[0]) == Character.DIRECTIONALITY_RIGHT_TO_LEFT
    }

    open class SimpleTextWatcher : TextWatcher {
        override fun beforeTextChanged(
            s: CharSequence,
            start: Int,
            count: Int,
            after: Int
        ) {
        }

        override fun onTextChanged(
            s: CharSequence,
            start: Int,
            before: Int,
            count: Int
        ) {
        }

        override fun afterTextChanged(s: Editable) {}
    }
}