package com.mapswithme.maps.sound

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.text.TextUtils
import com.mapswithme.maps.Framework
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.maps.base.MediaPlayerWrapper.Companion.from
import com.mapswithme.maps.sound.LanguageData.NotAvailableException
import com.mapswithme.util.Config
import com.mapswithme.util.log.LoggerFactory
import com.mapswithme.util.statistics.Statistics
import java.util.*

/**
 * `TtsPlayer` class manages available TTS voice languages.
 * Single TTS language is described by [LanguageData] item.
 *
 *
 * We support a set of languages listed in `strings-tts.xml` file.
 * During loading each item in this list is marked as `downloaded` or `not downloaded`,
 * unsupported voices are excluded.
 *
 *
 * At startup we check whether currently selected language is in our list of supported voices and its data is downloaded.
 * If not, we check system default locale. If failed, the same check is made for English language.
 * Finally, if mentioned checks fail we manually disable TTS, so the user must go to the settings and select
 * preferred voice language by hand.
 *
 *
 * If no core supported languages can be used by the system, TTS is locked down and can not be enabled and used.
 */
enum class TtsPlayer {
    INSTANCE;

    private var mTts: TextToSpeech? = null
    private var mInitializing = false
    // TTS is locked down due to absence of supported languages
    private var mUnavailable = false

    private fun setLanguageInternal(lang: LanguageData): Boolean {
        return try {
            mTts!!.language = lang.locale
            nativeSetTurnNotificationsLocale(lang.internalCode)
            Config.setTtsLanguage(lang.internalCode)
            true
        } catch (e: IllegalArgumentException) {
            reportFailure(e, "setLanguageInternal(): " + lang.locale)
            lockDown()
            false
        }
    }

    fun setLanguage(lang: LanguageData?): Boolean {
        return lang != null && setLanguageInternal(lang)
    }

    private fun lockDown() {
        mUnavailable = true
        isEnabled = false
    }

    fun init(context: Context?) {
        if (mTts != null || mInitializing || mUnavailable) return
        mInitializing = true
        mTts = TextToSpeech(context, OnInitListener { status ->
            if (status == TextToSpeech.ERROR) {
                LOGGER.e(
                    TAG,
                    "Failed to initialize TextToSpeach"
                )
                lockDown()
                mInitializing = false
                return@OnInitListener
            }
            refreshLanguages()
            mTts!!.setSpeechRate(SPEECH_RATE)
            mInitializing = false
        })
    }

    val isSpeaking: Boolean
        get() = mTts != null && mTts!!.isSpeaking

    private fun speak(textToSpeak: String) {
        if (Config.isTtsEnabled()) try {
            mTts!!.speak(textToSpeak, TextToSpeech.QUEUE_ADD, null)
        } catch (e: IllegalArgumentException) {
            reportFailure(e, "speak()")
            lockDown()
        }
    }

    fun playTurnNotifications(context: Context) {
        if (from(context).isPlaying) return
        // It's necessary to call Framework.nativeGenerateTurnNotifications() even if TtsPlayer is invalid.
        val turnNotifications =
            Framework.nativeGenerateNotifications()
        if (turnNotifications != null && isReady) for (textToSpeak in turnNotifications) speak(
            textToSpeak
        )
    }

    fun stop() {
        if (isReady) try {
            mTts!!.stop()
        } catch (e: IllegalArgumentException) {
            reportFailure(e, "stop()")
            lockDown()
        }
    }

    private fun getUsableLanguages(outList: MutableList<LanguageData?>): Boolean {
        val resources = MwmApplication.get().resources
        val codes =
            resources.getStringArray(R.array.tts_languages_supported)
        val names =
            resources.getStringArray(R.array.tts_language_names)
        for (i in codes.indices) {
            try {
                outList.add(LanguageData(codes[i], names[i], mTts))
            } catch (ignored: NotAvailableException) {
                LOGGER.e(
                    TAG,
                    "Failed to get usable languages " + ignored.message
                )
            } catch (e: IllegalArgumentException) {
                LOGGER.e(
                    TAG,
                    "Failed to get usable languages",
                    e
                )
                reportFailure(e, "getUsableLanguages()")
                lockDown()
                return false
            }
        }
        return true
    }

    private fun refreshLanguagesInternal(outList: MutableList<LanguageData?>): LanguageData? {
        if (!getUsableLanguages(outList)) return null
        if (outList.isEmpty()) { // No supported languages found, lock down TTS :(
            lockDown()
            return null
        }
        var res = getSelectedLanguage(outList)
        if (res == null || !res.downloaded) // Selected locale is not available or not downloaded
            res = getDefaultLanguage(outList)
        if (res == null || !res.downloaded) { // Default locale can not be used too
            Config.setTtsEnabled(false)
            return null
        }
        return res
    }

    fun refreshLanguages(): List<LanguageData?> {
        val res: MutableList<LanguageData?> =
            ArrayList()
        if (mUnavailable || mTts == null) return res
        val lang = refreshLanguagesInternal(res)
        setLanguage(lang)
        isEnabled = Config.isTtsEnabled()
        return res
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = TtsPlayer::class.java.simpleName
        private val DEFAULT_LOCALE = Locale.US
        private const val SPEECH_RATE = 1.2f
        private fun reportFailure(
            e: IllegalArgumentException,
            location: String
        ) {
            Statistics.INSTANCE.trackEvent(
                Statistics.EventName.TTS_FAILURE_LOCATION,
                Statistics.params().add(
                    Statistics.EventParam.ERR_MSG,
                    e.message
                )
                    .add(Statistics.EventParam.FROM, location)
            )
        }

        private fun findSupportedLanguage(
            internalCode: String,
            langs: List<LanguageData?>
        ): LanguageData? {
            if (TextUtils.isEmpty(internalCode)) return null
            for (lang in langs) if (lang!!.matchesInternalCode(internalCode)) return lang
            return null
        }

        private fun findSupportedLanguage(
            locale: Locale?,
            langs: List<LanguageData?>
        ): LanguageData? {
            if (locale == null) return null
            for (lang in langs) if (lang!!.matchesLocale(locale)) return lang
            return null
        }

        private fun getDefaultLanguage(langs: List<LanguageData?>): LanguageData? {
            var res: LanguageData?
            val defLocale = Locale.getDefault()
            if (defLocale != null) {
                res = findSupportedLanguage(defLocale, langs)
                if (res != null && res.downloaded) return res
            }
            res =
                findSupportedLanguage(DEFAULT_LOCALE, langs)
            return if (res != null && res.downloaded) res else null
        }

        fun getSelectedLanguage(langs: List<LanguageData?>): LanguageData? {
            return findSupportedLanguage(
                Config.getTtsLanguage(),
                langs
            )
        }

        private val isReady: Boolean
            private get() = INSTANCE.mTts != null && !INSTANCE.mUnavailable && !INSTANCE.mInitializing

        var isEnabled: Boolean
            get() = isReady && nativeAreTurnNotificationsEnabled()
            set(enabled) {
                Config.setTtsEnabled(enabled)
                nativeEnableTurnNotifications(enabled)
            }

        @JvmStatic private external fun nativeEnableTurnNotifications(enable: Boolean)
        @JvmStatic private external fun nativeAreTurnNotificationsEnabled(): Boolean
        @JvmStatic private external fun nativeSetTurnNotificationsLocale(code: String?)
        @JvmStatic private external fun nativeGetTurnNotificationsLocale(): String?
    }
}