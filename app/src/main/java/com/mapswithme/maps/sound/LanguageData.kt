package com.mapswithme.maps.sound

import android.speech.tts.TextToSpeech
import java.util.*

/**
 * `LanguageData` describes single voice language managed by [TtsPlayer].
 * Supported languages are listed in `strings-tts.xml` file, for details see comments there.
 */
class LanguageData internal constructor(
    line: String,
    val name: String,
    tts: TextToSpeech?
) {
    internal class NotAvailableException(locale: Locale) :
        Exception("Locale \"$locale\" is not supported by current TTS engine")

    val locale: Locale
    val internalCode: String
    val downloaded: Boolean
    fun matchesLocale(locale: Locale): Boolean {
        val lang = locale.language
        if (lang != this.locale.language) return false
        if ("zh" == lang && "zh-Hant" == internalCode) { // Chinese is a special case
            val country = locale.country
            return "TW" == country || "MO" == country || "HK" == country
        }
        return true
    }

    fun matchesInternalCode(internalCode: String): Boolean {
        return this.internalCode == internalCode
    }

    override fun toString(): String {
        return name + ": " + locale + ", internal: " + internalCode + (if (downloaded) " - " else " - NOT ") + "downloaded"
    }

    init {
        var parts = line.split(":").toTypedArray()
        val code = if (parts.size > 1) parts[1] else null
        parts = parts[0].split("-").toTypedArray()
        val language = parts[0]
        internalCode = code ?: language
        val country = if (parts.size > 1) parts[1] else ""
        locale = Locale(language, country)
        // tts.isLanguageAvailable() may throw IllegalArgumentException if the TTS is corrupted internally.
        val status = tts!!.isLanguageAvailable(locale)
        if (status < TextToSpeech.LANG_MISSING_DATA) throw NotAvailableException(locale)
        downloaded = status >= TextToSpeech.LANG_AVAILABLE
    }
}