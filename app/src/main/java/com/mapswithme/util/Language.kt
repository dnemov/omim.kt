package com.mapswithme.util

import android.content.Context
import android.text.TextUtils
import android.view.inputmethod.InputMethodManager
import com.mapswithme.maps.MwmApplication
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import java.util.*

object Language {
    // Replace deprecated language codes:
// in* -> id
// iw* -> he
// Locale.getLanguage() returns even 3-letter codes, not that we need in the C++ core,
// so we use locale itself, like zh_CN

    @JvmStatic
    val defaultLocale: String
        get() {
            val lang = Locale.getDefault().toString()
            if (TextUtils.isEmpty(lang)) return Locale.US.toString()
            // Replace deprecated language codes:
// in* -> id
// iw* -> he
            if (lang.startsWith("in")) return "id"
            return if (lang.startsWith("iw")) "he" else lang
        }

    // After some testing on Galaxy S4, looks like this method doesn't work on all devices:
// sometime it always returns the same value as getDefaultLocale()
    val keyboardLocale: String
        get() {
            val context: Context = MwmApplication.get()
            val imm = context
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                ?: return defaultLocale
            val ims = imm.currentInputMethodSubtype ?: return defaultLocale
            val locale = ims.locale
            return if (TextUtils.isEmpty(locale.trim { it <= ' ' })) defaultLocale else locale
        }

    @JvmStatic external fun nativeNormalize(locale: String): String
}