package com.mapswithme.maps.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.mapswithme.maps.Framework
import com.mapswithme.maps.Framework.Params3dMode
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.downloader.MapManager.nativeIsDownloading
import com.mapswithme.maps.downloader.OnmapDownloader.Companion.setAutodownloadLocked
import com.mapswithme.maps.editor.ProfileActivity
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.location.TrackRecorder.duration
import com.mapswithme.maps.location.TrackRecorder.isEnabled
import com.mapswithme.maps.purchase.*
import com.mapswithme.maps.purchase.PurchaseFactory.createAdsRemovalPurchaseController
import com.mapswithme.maps.routing.RoutingController.Companion.get
import com.mapswithme.maps.settings.SettingsPrefsFragment
import com.mapswithme.maps.sound.LanguageData
import com.mapswithme.maps.sound.TtsPlayer
import com.mapswithme.util.*
import com.mapswithme.util.PowerManagment.SchemeType
import com.mapswithme.util.concurrency.UiThread
import com.mapswithme.util.log.LoggerFactory
import com.mapswithme.util.statistics.AlohaHelper
import com.mapswithme.util.statistics.Statistics
import com.mapswithme.util.statistics.Statistics.ParameterBuilder
import java.util.*

class SettingsPrefsFragment : BaseXmlSettingsFragment(), AdsRemovalActivationCallback,
    AdsRemovalPurchaseControllerProvider {
    private val mPathManager = StoragePathManager()
    private var mStoragePref: Preference? = null
    private var mPrefEnabled: TwoStatePreference? = null
    private var mPrefLanguages: ListPreference? = null
    private var mLangInfo: Preference? = null
    private var mLangInfoLink: Preference? = null
    private var mPreferenceScreen: PreferenceScreen? = null
    private val mLanguages: MutableMap<String?, LanguageData?> =
        HashMap()
    private var mCurrentLanguage: LanguageData? = null
    private var mSelectedLanguage: String? = null
    override var adsRemovalPurchaseController: PurchaseController<PurchaseCallback>? = null

    private fun singleStorageOnly(): Boolean {
        return !mPathManager.hasMoreThanOneStorage()
    }

    private fun updateStoragePrefs() {
        val old =
            findPreference<Preference>(getString(R.string.pref_storage))
        if (singleStorageOnly()) {
            if (old != null) {
                removePreference(getString(R.string.pref_settings_general), old)
            }
        } else {
            if (old == null && mStoragePref != null) {
                preferenceScreen.addPreference(mStoragePref)
            }
        }
    }

    private val mEnabledListener =
        Preference.OnPreferenceChangeListener { preference, newValue ->
            Statistics.INSTANCE.trackEvent(
                Statistics.EventName.Settings.VOICE_ENABLED,
                Statistics.params().add(
                    Statistics.EventParam.ENABLED,
                    newValue.toString()
                )
            )
            val root =
                findPreference<Preference>(getString(R.string.pref_tts_screen))
            val set = newValue as Boolean
            if (!set) {
                TtsPlayer.Companion.isEnabled = false
                if (mPrefLanguages != null) mPrefLanguages!!.isEnabled = false
                if (mLangInfo != null) mLangInfo!!.setSummary(R.string.prefs_languages_information_off)
                if (mLangInfoLink != null && isOnTtsScreen) preferenceScreen.addPreference(
                    mLangInfoLink
                )
                root?.setSummary(R.string.off)
                if (mPrefEnabled != null) mPrefEnabled!!.setTitle(R.string.off)
                return@OnPreferenceChangeListener true
            }
            if (mLangInfo != null) mLangInfo!!.setSummary(R.string.prefs_languages_information)
            root?.setSummary(R.string.on)
            if (mPrefEnabled != null) mPrefEnabled!!.setTitle(R.string.on)
            if (mLangInfoLink != null) removePreference(
                getString(R.string.pref_navigation),
                mLangInfoLink!!
            )
            if (mCurrentLanguage != null && mCurrentLanguage!!.downloaded) {
                setLanguage(mCurrentLanguage!!)
                return@OnPreferenceChangeListener true
            }
            false
        }
    private val mLangListener =
        Preference.OnPreferenceChangeListener { preference, newValue ->
            if (newValue == null) return@OnPreferenceChangeListener false
            mSelectedLanguage = newValue as String
            Statistics.INSTANCE.trackEvent(
                Statistics.EventName.Settings.VOICE_LANGUAGE,
                Statistics.params().add(
                    Statistics.EventParam.LANGUAGE,
                    mSelectedLanguage
                )
            )
            val lang = mLanguages[mSelectedLanguage] ?: return@OnPreferenceChangeListener false
            if (lang.downloaded) setLanguage(lang) else startActivityForResult(
                Intent(
                    TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                ), REQUEST_INSTALL_DATA
            )
            false
        }

    private fun enableListeners(enable: Boolean) {
        if (mPrefEnabled != null) mPrefEnabled!!.onPreferenceChangeListener =
            if (enable) mEnabledListener else null
        if (mPrefLanguages != null) mPrefLanguages!!.onPreferenceChangeListener =
            if (enable) mLangListener else null
    }

    private fun setLanguage(lang: LanguageData) {
        Config.setTtsEnabled(true)
        TtsPlayer.INSTANCE.setLanguage(lang)
        if (mPrefLanguages != null) mPrefLanguages!!.summary = lang.name
        updateTts()
    }

    private fun updateTts() {
        if (mPrefEnabled == null || mPrefLanguages == null || mLangInfo == null || mLangInfoLink == null) return
        enableListeners(false)
        val languages =
            TtsPlayer.INSTANCE.refreshLanguages()
        mLanguages.clear()
        mCurrentLanguage = null
        val root =
            findPreference<Preference>(getString(R.string.pref_tts_screen))
        if (languages.isEmpty()) {
            mPrefEnabled!!.isChecked = false
            mPrefEnabled!!.isEnabled = false
            mPrefEnabled!!.setSummary(R.string.pref_tts_unavailable)
            mPrefEnabled!!.setTitle(R.string.off)
            mPrefLanguages!!.isEnabled = false
            mPrefLanguages!!.summary = null
            mLangInfo!!.setSummary(R.string.prefs_languages_information_off)
            if (isOnTtsScreen) preferenceScreen.addPreference(mLangInfoLink)
            root?.setSummary(R.string.off)
            enableListeners(true)
            return
        }
        val enabled: Boolean = TtsPlayer.Companion.isEnabled
        mPrefEnabled!!.isChecked = enabled
        mPrefEnabled!!.summary = null
        mPrefEnabled!!.setTitle(if (enabled) R.string.on else R.string.off)
        mLangInfo!!.setSummary(if (enabled) R.string.prefs_languages_information else R.string.prefs_languages_information_off)
        if (enabled) removePreference(
            getString(R.string.pref_navigation),
            mLangInfoLink!!
        ) else if (isOnTtsScreen) preferenceScreen.addPreference(mLangInfoLink)
        root?.setSummary(if (enabled) R.string.on else R.string.off)
        val entries =
            arrayOfNulls<CharSequence>(languages.size)
        val values =
            arrayOfNulls<CharSequence>(languages.size)
        for (i in languages.indices) {
            val lang = languages[i]
            entries[i] = lang!!.name
            values[i] = lang.internalCode
            mLanguages[lang.internalCode] = lang
        }
        mPrefLanguages!!.entries = entries
        mPrefLanguages!!.entryValues = values
        mCurrentLanguage = TtsPlayer.Companion.getSelectedLanguage(languages)
        val available = mCurrentLanguage != null && mCurrentLanguage!!.downloaded
        mPrefLanguages!!.isEnabled = available && TtsPlayer.Companion.isEnabled
        mPrefLanguages!!.summary = if (available) mCurrentLanguage!!.name else null
        mPrefLanguages!!.value = if (available) mCurrentLanguage!!.internalCode else null
        mPrefEnabled!!.isChecked = available && TtsPlayer.Companion.isEnabled
        enableListeners(true)
    }

    private val isOnTtsScreen: Boolean
        private get() = mPreferenceScreen!!.key != null && mPreferenceScreen!!.key == TTS_SCREEN_KEY

    override fun getCallbackFragment(): Fragment {
        return this
    }

    protected override val xmlResources: Int
        protected get() = R.xml.prefs_main

    private fun onToggleOptOut(newValue: Any): Boolean {
        val isEnabled = newValue as Boolean
        Statistics.INSTANCE.trackSettingsToggle(isEnabled)
        return true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        adsRemovalPurchaseController =
            createAdsRemovalPurchaseController(requireContext())
        adsRemovalPurchaseController!!.initialize(requireActivity())
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onDestroyView() {
        if (adsRemovalPurchaseController != null) adsRemovalPurchaseController!!.destroy()
        super.onDestroyView()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        mPreferenceScreen = preferenceScreen
        mStoragePref =
            findPreference(getString(R.string.pref_storage))
        mPrefEnabled =
            findPreference<Preference>(getString(R.string.pref_tts_enabled)) as TwoStatePreference?
        mPrefLanguages =
            findPreference<Preference>(getString(R.string.pref_tts_language)) as ListPreference?
        mLangInfo =
            findPreference(getString(R.string.pref_tts_info))
        mLangInfoLink =
            findPreference(getString(R.string.pref_tts_info_link))
        initLangInfoLink()
        updateStoragePrefs()
        initStoragePrefCallbacks()
        initMeasureUnitsPrefsCallbacks()
        initZoomPrefsCallbacks()
        initMapStylePrefsCallbacks()
        initSpeedCamerasPrefs()
        initAutoDownloadPrefsCallbacks()
        initBackupBookmarksPrefsCallbacks()
        initLargeFontSizePrefsCallbacks()
        initTransliterationPrefsCallbacks()
        init3dModePrefsCallbacks()
        initPerspectivePrefsCallbacks()
        initTrackRecordPrefsCallbacks()
        initStatisticsPrefsCallback()
        initPlayServicesPrefsCallbacks()
        initAutoZoomPrefsCallbacks()
        initDisplayShowcasePrefs()
        initLoggingEnabledPrefsCallbacks()
        initEmulationBadStorage()
        initUseMobileDataPrefsCallbacks()
        initPowerManagementPrefsCallbacks()
        initOptOut()
        updateTts()
    }

    private fun initSpeedCamerasPrefs() {
        val key = getString(R.string.pref_speed_cameras)
        val pref =
            findPreference<Preference>(key) as ListPreference? ?: return
        pref.summary = pref.entry
        pref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference: Preference, newValue: Any ->
                onSpeedCamerasPrefSelected(
                    preference,
                    newValue
                )
            }
    }

    private fun onSpeedCamerasPrefSelected(
        preference: Preference,
        newValue: Any
    ): Boolean {
        val speedCamModeValue = newValue as String
        val speedCamModeList =
            preference as ListPreference
        val newCamMode =
            SpeedCameraMode.valueOf(
                speedCamModeValue
            )
        val summary = speedCamModeList.entries[newCamMode.ordinal]
        speedCamModeList.summary = summary
        if (speedCamModeList.value == newValue) return true
        val oldCamMode =
            SpeedCameraMode.valueOf(
                speedCamModeList.value
            )
        onSpeedCamerasPrefChanged(oldCamMode, newCamMode)
        return true
    }

    private fun onSpeedCamerasPrefChanged(
        oldCamMode: SpeedCameraMode,
        newCamMode: SpeedCameraMode
    ) {
        val params = ParameterBuilder()
            .add(
                Statistics.EventParam.VALUE,
                newCamMode.name.toLowerCase()
            )
        Statistics.INSTANCE.trackEvent(
            Statistics.EventName.SETTINGS_SPEED_CAMS,
            params
        )
        Framework.setSpeedCamerasMode(newCamMode)
    }

    override fun onResume() {
        super.onResume()
        initTrackRecordPrefsCallbacks()
        updateTts()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) { // Do not check resultCode here as it is always RESULT_CANCELED
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_INSTALL_DATA) {
            updateTts()
            val lang = mLanguages[mSelectedLanguage]
            if (lang != null && lang.downloaded) setLanguage(lang)
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key != null && preference.key == getString(R.string.pref_help)) {
            Statistics.INSTANCE.trackEvent(Statistics.EventName.Settings.HELP)
            AlohaHelper.logClick(AlohaHelper.Settings.HELP)
        } else if (preference.key != null && preference.key == getString(R.string.pref_about)) {
            Statistics.INSTANCE.trackEvent(Statistics.EventName.Settings.ABOUT)
            AlohaHelper.logClick(AlohaHelper.Settings.ABOUT)
        } else if (preference.key != null && preference.key == getString(R.string.pref_osm_profile)) {
            Statistics.INSTANCE.trackEvent(Statistics.EventName.Settings.OSM_PROFILE)
            startActivity(Intent(activity, ProfileActivity::class.java))
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun initDisplayShowcasePrefs() {
        val pref =
            findPreference<Preference>(getString(R.string.pref_showcase_switched_on))
                ?: return
        if (Framework.nativeHasActiveSubscription(Framework.SUBSCRIPTION_TYPE_REMOVE_ADS)) {
            removePreference(getString(R.string.pref_settings_general), pref)
            return
        }
        pref.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { preference: Preference? ->
                AdsRemovalPurchaseDialog.show(this@SettingsPrefsFragment)
                true
            }
    }

    private fun initLangInfoLink() {
        if (mLangInfoLink != null) {
            val link: Spannable =
                SpannableString(getString(R.string.prefs_languages_information_off_link))
            link.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(
                        context!!,
                        UiUtils.getStyledResourceId(context!!, R.attr.colorAccent)
                    )
                ),
                0, link.length, 0
            )
            mLangInfoLink!!.summary = link
            mLangInfoLink!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(TTS_INFO_LINK)
                context!!.startActivity(intent)
                false
            }
            removePreference(getString(R.string.pref_navigation), mLangInfoLink!!)
        }
    }

    private fun initLargeFontSizePrefsCallbacks() {
        val pref =
            findPreference<Preference>(getString(R.string.pref_large_fonts_size))
                ?: return
        (pref as TwoStatePreference).isChecked = Config.isLargeFontsSize()
        pref.setOnPreferenceChangeListener { preference, newValue ->
            val oldVal = Config.isLargeFontsSize()
            val newVal = newValue as Boolean
            if (oldVal != newVal) Config.setLargeFontsSize(newVal)
            true
        }
    }

    private fun initTransliterationPrefsCallbacks() {
        val pref =
            findPreference<Preference>(getString(R.string.pref_transliteration))
                ?: return
        (pref as TwoStatePreference).isChecked = Config.isTransliteration()
        pref.setOnPreferenceChangeListener { preference, newValue ->
            val oldVal = Config.isTransliteration()
            val newVal = newValue as Boolean
            if (oldVal != newVal) Config.setTransliteration(newVal)
            true
        }
    }

    private fun initUseMobileDataPrefsCallbacks() {
        val mobilePref =
            findPreference<Preference>(
                getString(R.string.pref_use_mobile_data)
            ) as ListPreference?
                ?: return
        val curValue = Config.getUseMobileDataSettings()
        if (curValue !== NetworkPolicy.Type.NOT_TODAY && curValue !== NetworkPolicy.Type.TODAY && curValue !== NetworkPolicy.Type.NONE
        ) {
            mobilePref.value = curValue.toString()
            mobilePref.summary = mobilePref.entry
        } else {
            mobilePref.summary = getString(R.string.mobile_data_description)
        }
        mobilePref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                val valueStr = newValue as String
                val value = valueStr.toInt()
                val type = NetworkPolicy.Type.values()[value]
                if (type === NetworkPolicy.Type.ALWAYS || type === NetworkPolicy.Type.ASK || type === NetworkPolicy.Type.NEVER
                ) {
                    Config.setUseMobileDataSettings(type)
                    Statistics.INSTANCE.trackNetworkUsageAlert(
                        Statistics.EventName.SETTINGS_MOBILE_INTERNET_CHANGE,
                        type.toStatisticValue()!!
                    )
                } else {
                    throw AssertionError("Wrong NetworkPolicy type, value = $valueStr")
                }
                UiThread.runLater(kotlinx.coroutines.Runnable { mobilePref.summary = mobilePref.entry  })
                true
            }
    }

    private fun initPowerManagementPrefsCallbacks() {
        val powerManagementPref =
            findPreference<Preference>(
                getString(R.string.pref_power_management)
            ) as ListPreference?
                ?: return
        @SchemeType val curValue = PowerManagment.getScheme()
        powerManagementPref.value = curValue.toString()
        powerManagementPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                @SchemeType val scheme: Int = newValue.toString().toInt()
                PowerManagment.setScheme(scheme)
                Statistics.INSTANCE.trackPowerManagmentSchemeChanged(
                    scheme
                )
                true
            }
    }

    private fun initLoggingEnabledPrefsCallbacks() {
        val pref =
            findPreference<Preference>(getString(R.string.pref_enable_logging))
                ?: return
        val isLoggingEnabled = LoggerFactory.INSTANCE.isFileLoggingEnabled
        (pref as TwoStatePreference).isChecked = isLoggingEnabled
        pref.setOnPreferenceChangeListener { preference: Preference?, newValue: Any ->
            val newVal = newValue as Boolean
            if (isLoggingEnabled != newVal) LoggerFactory.INSTANCE.isFileLoggingEnabled = newVal
            true
        }
    }

    private fun initEmulationBadStorage() {
        val pref =
            findPreference<Preference>(getString(R.string.pref_emulate_bad_external_storage))
                ?: return
        if (!SharedPropertiesUtils.shouldShowEmulateBadStorageSetting()) removePreference(
            getString(
                R.string.pref_settings_general
            ), pref
        )
    }

    private fun initAutoZoomPrefsCallbacks() {
        val pref =
            findPreference<Preference>(getString(R.string.pref_auto_zoom)) as TwoStatePreference?
                ?: return
        val autozoomEnabled = Framework.nativeGetAutoZoomEnabled()
        pref.isChecked = autozoomEnabled
        pref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                Framework.nativeSetAutoZoomEnabled((newValue as Boolean))
                true
            }
    }

    private fun initPlayServicesPrefsCallbacks() {
        val pref =
            findPreference<Preference>(getString(R.string.pref_play_services))
                ?: return
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MwmApplication.get()) != ConnectionResult.SUCCESS) {
            removePreference(getString(R.string.pref_settings_general), pref)
        } else {
            (pref as TwoStatePreference).isChecked = Config.useGoogleServices()
            pref.setOnPreferenceChangeListener { preference, newValue ->
                val oldVal = Config.useGoogleServices()
                val newVal = newValue as Boolean
                if (oldVal != newVal) {
                    Config.setUseGoogleService(newVal)
                    LocationHelper.INSTANCE.restart()
                }
                true
            }
        }
    }

    private fun initStatisticsPrefsCallback() {
        val pref =
            findPreference<Preference>(getString(R.string.pref_send_statistics))
                ?: return
        (pref as TwoStatePreference).isChecked = SharedPropertiesUtils.isStatisticsEnabled
        pref.setOnPreferenceChangeListener { preference, newValue ->
            Statistics.INSTANCE.setStatEnabled((newValue as Boolean))
            true
        }
    }

    private fun initTrackRecordPrefsCallbacks() {
        val trackPref =
            findPreference<Preference>(getString(R.string.pref_track_record)) as ListPreference?
        val pref =
            findPreference<Preference>(getString(R.string.pref_track_record_time))
        val root =
            findPreference<Preference>(getString(R.string.pref_track_screen))
        if (trackPref == null || pref == null) return
        val enabled = isEnabled
        (pref as TwoStatePreference).isChecked = enabled
        trackPref.isEnabled = enabled
        root?.setSummary(if (enabled) R.string.on else R.string.off)
        pref.setTitle(if (enabled) R.string.on else R.string.off)
        pref.setOnPreferenceChangeListener(Preference.OnPreferenceChangeListener { preference, newValue ->
            val enabled = newValue as Boolean
            isEnabled = enabled
            Statistics.INSTANCE.setStatEnabled(enabled)
            trackPref.isEnabled = enabled
            root?.setSummary(if (enabled) R.string.on else R.string.off)
            pref.setTitle(if (enabled) R.string.on else R.string.off)
            trackPref.performClick()
            true
        })
        val value = if (enabled) duration.toString() else "0"
        trackPref.value = value
        trackPref.summary = trackPref.entry
        trackPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                val value = Integer.valueOf((newValue as String))
                val enabled = value != 0
                if (enabled) duration = value
                isEnabled = enabled
                pref.isChecked = enabled
                trackPref.isEnabled = enabled
                root?.setSummary(if (enabled) R.string.on else R.string.off)
                pref.setTitle(if (enabled) R.string.on else R.string.off)
                val builder = ParameterBuilder().add(
                    Statistics.EventParam.VALUE,
                    value
                )
                Statistics.INSTANCE.trackEvent(
                    Statistics.EventName.SETTINGS_RECENT_TRACK_CHANGE,
                    builder
                )
                UiThread.runLater(Runnable { trackPref.summary = trackPref.entry })
                true
            }
    }

    private fun init3dModePrefsCallbacks() {
        val pref =
            findPreference<Preference>(getString(R.string.pref_3d_buildings)) as TwoStatePreference?
                ?: return
        val _3d = Params3dMode()
        Framework.nativeGet3dMode(_3d)
        pref.isChecked = _3d.buildings
        pref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                Framework.nativeSet3dMode(_3d.enabled, (newValue as Boolean))
                true
            }
    }

    private fun initPerspectivePrefsCallbacks() {
        val pref =
            findPreference<Preference>(getString(R.string.pref_3d)) as TwoStatePreference?
                ?: return
        val _3d = Params3dMode()
        Framework.nativeGet3dMode(_3d)
        pref.isChecked = _3d.enabled
        pref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                Framework.nativeSet3dMode((newValue as Boolean), _3d.buildings)
                true
            }
    }

    private fun initAutoDownloadPrefsCallbacks() {
        val pref =
            findPreference<Preference>(getString(R.string.pref_autodownload)) as TwoStatePreference?
                ?: return
        pref.isChecked = Config.isAutodownloadEnabled()
        pref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                val value = newValue as Boolean
                Config.setAutodownloadEnabled(value)
                if (value) setAutodownloadLocked(false)
                true
            }
    }

    private fun initBackupBookmarksPrefsCallbacks() {
        val pref =
            findPreference<Preference>(getString(R.string.pref_backupbookmarks)) as TwoStatePreference?
                ?: return
        val curValue = BookmarkManager.INSTANCE.isCloudEnabled
        pref.isChecked = curValue
        pref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                val value = newValue as Boolean?
                BookmarkManager.INSTANCE.isCloudEnabled = value!!
                Statistics.INSTANCE.trackBmSettingsToggle(value)
                true
            }
    }

    private fun initMapStylePrefsCallbacks() {
        val pref =
            findPreference<Preference>(getString(R.string.pref_map_style)) as ListPreference?
                ?: return
        val curTheme = Config.getUiThemeSettings()
        pref.value = curTheme
        pref.summary = pref.entry
        pref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { pref: Preference, newValue: Any ->
                onMapStylePrefChanged(
                    pref,
                    newValue
                )
            }
    }

    private fun onMapStylePrefChanged(
        pref: Preference,
        newValue: Any
    ): Boolean {
        val themeName = newValue as String
        if (!Config.setUiThemeSettings(themeName)) return true
        ThemeSwitcher.restart(false)
        Statistics.INSTANCE.trackEvent(
            Statistics.EventName.Settings.MAP_STYLE,
            Statistics.params().add(
                Statistics.EventParam.NAME,
                themeName
            )
        )
        val mapStyleModeList =
            pref as ListPreference
        val mode =
            ThemeMode.getInstance(context!!.applicationContext, themeName)
        val summary = mapStyleModeList.entries[mode.ordinal]
        mapStyleModeList.summary = summary
        return true
    }

    private fun initZoomPrefsCallbacks() {
        val pref =
            findPreference<Preference>(getString(R.string.pref_show_zoom_buttons))
                ?: return
        (pref as TwoStatePreference).isChecked = Config.showZoomButtons()
        pref.setOnPreferenceChangeListener { preference, newValue ->
            Statistics.INSTANCE.trackEvent(Statistics.EventName.Settings.ZOOM)
            Config.setShowZoomButtons((newValue as Boolean))
            true
        }
    }

    private fun initMeasureUnitsPrefsCallbacks() {
        val pref =
            findPreference<Preference>(getString(R.string.pref_munits)) ?: return
        (pref as ListPreference).value = UnitLocale.units.toString()
        pref.setOnPreferenceChangeListener { preference, newValue ->
            UnitLocale.units = (newValue.toString().toInt())
            Statistics.INSTANCE.trackEvent(Statistics.EventName.Settings.UNITS)
            AlohaHelper.logClick(AlohaHelper.Settings.CHANGE_UNITS)
            true
        }
    }

    private fun initStoragePrefCallbacks() {
        if (mStoragePref == null) return
        mStoragePref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (nativeIsDownloading()) AlertDialog.Builder(
                activity!!
            )
                .setTitle(getString(R.string.downloading_is_active))
                .setMessage(getString(R.string.cant_change_this_setting))
                .setPositiveButton(getString(R.string.ok), null)
                .show() else  //          getSettingsActivity().switchToFragment(StoragePathFragment.class, R.string.maps_storage);
                settingsActivity?.replaceFragment(
                    StoragePathFragment::class.java,
                    getString(R.string.maps_storage), null
                )
            true
        }
    }

    private fun initOptOut() {
        val key = getString(R.string.pref_opt_out_fabric_activated)
        val pref =
            findPreference<Preference>(key) ?: return
        pref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                onToggleOptOut(newValue)
            }
    }

    private fun removePreference(
        categoryKey: String,
        preference: Preference
    ) {
        val category =
            findPreference<Preference>(categoryKey) as PreferenceCategory?
                ?: return
        category.removePreference(preference)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context !is Activity) return
        mPathManager.startExternalStorageWatching(
            context,
            object : StoragePathManager.OnStorageListChangedListener {
                override fun onStorageListChanged(
                    storageItems: List<StorageItem>?,
                    currentStorageIndex: Int
                ) {
                    updateStoragePrefs()
                }
            },
            null
        )
    }

    override fun onDetach() {
        super.onDetach()
        mPathManager.stopExternalStorageWatching()
    }

    override fun onAdsRemovalActivation() {
        initDisplayShowcasePrefs()
    }

    internal enum class ThemeMode(@param:StringRes private val mModeStringId: Int) {
        DEFAULT(R.string.theme_default), NIGHT(R.string.theme_night), AUTO(R.string.theme_auto);

        companion object {
            fun getInstance(context: Context, src: String): ThemeMode {
                for (each in values()) {
                    if (context.resources.getString(each.mModeStringId) == src) return each
                }
                return AUTO
            }
        }

    }

    enum class SpeedCameraMode {
        AUTO, ALWAYS, NEVER
    }

    companion object {
        private const val REQUEST_INSTALL_DATA = 1
        private val TTS_SCREEN_KEY = MwmApplication.get()
            .getString(R.string.pref_tts_screen)
        private val TTS_INFO_LINK = MwmApplication.get()
            .getString(R.string.tts_info_link)
    }
}