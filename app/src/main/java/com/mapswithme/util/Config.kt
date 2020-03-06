package com.mapswithme.util

import com.mapswithme.maps.BuildConfig
import com.mapswithme.maps.MwmApplication
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Counters.KEY_APP_FIRST_INSTALL_FLAVOR
import com.mapswithme.util.Counters.KEY_APP_FIRST_INSTALL_VERSION
import com.mapswithme.util.Counters.KEY_APP_LAST_SESSION_TIMESTAMP
import com.mapswithme.util.Counters.KEY_APP_LAUNCH_NUMBER
import com.mapswithme.util.Counters.KEY_APP_SESSION_NUMBER
import com.mapswithme.util.Counters.KEY_LIKES_LAST_RATED_SESSION
import com.mapswithme.util.Counters.KEY_MISC_FIRST_START_DIALOG_SEEN
import com.mapswithme.util.Counters.KEY_MISC_NEWS_LAST_VERSION
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language

object Config {
    private const val KEY_APP_STORAGE = "StoragePath"
    private const val KEY_TTS_ENABLED = "TtsEnabled"
    private const val KEY_TTS_LANGUAGE = "TtsLanguage"
    private const val KEY_DOWNLOADER_AUTO = "AutoDownloadEnabled"
    private const val KEY_PREF_ZOOM_BUTTONS = "ZoomButtonsEnabled"
    const val KEY_PREF_STATISTICS = "StatisticsEnabled"
    private const val KEY_PREF_USE_GS = "UseGoogleServices"
    private const val KEY_MISC_DISCLAIMER_ACCEPTED = "IsDisclaimerApproved"
    private const val KEY_MISC_KITKAT_MIGRATED = "KitKatMigrationCompleted"
    private const val KEY_MISC_UI_THEME = "UiTheme"
    private const val KEY_MISC_UI_THEME_SETTINGS = "UiThemeSettings"
    private const val KEY_MISC_USE_MOBILE_DATA = "UseMobileData"
    private const val KEY_MISC_USE_MOBILE_DATA_TIMESTAMP = "UseMobileDataTimestamp"
    private const val KEY_MISC_USE_MOBILE_DATA_ROAMING = "UseMobileDataRoaming"
    private const val KEY_MISC_AD_FORBIDDEN = "AdForbidden"
    private fun getInt(key: String): Int {
        return getInt(key, 0)
    }

    private fun getInt(key: String, def: Int): Int {
        return nativeGetInt(key, def)
    }

    private fun getLong(key: String): Long {
        return getLong(key, 0L)
    }

    private fun getLong(key: String, def: Long): Long {
        return nativeGetLong(key, def)
    }

    private fun getString(key: String): String {
        return getString(key, "")
    }

    private fun getString(key: String, def: String): String {
        return nativeGetString(key, def)
    }

    private fun getBool(key: String): Boolean {
        return getBool(key, false)
    }

    private fun getBool(key: String, def: Boolean): Boolean {
        return nativeGetBoolean(key, def)
    }

    private fun setInt(key: String, value: Int) {
        nativeSetInt(key, value)
    }

    private fun setLong(key: String, value: Long) {
        nativeSetLong(key, value)
    }

    private fun setString(key: String, value: String) {
        nativeSetString(key, value)
    }

    private fun setBool(key: String) {
        setBool(key, true)
    }

    private fun setBool(key: String, value: Boolean) {
        nativeSetBoolean(key, value)
    }

    fun migrateCountersToSharedPrefs() {
        val version = getInt(
            KEY_APP_FIRST_INSTALL_VERSION,
            BuildConfig.VERSION_CODE
        )
        MwmApplication.prefs()
            .edit()
            .putInt(KEY_APP_LAUNCH_NUMBER, getInt(KEY_APP_LAUNCH_NUMBER))
            .putInt(KEY_APP_FIRST_INSTALL_VERSION, version)
            .putString(
                KEY_APP_FIRST_INSTALL_FLAVOR,
                getString(KEY_APP_FIRST_INSTALL_FLAVOR)
            )
            .putLong(
                KEY_APP_LAST_SESSION_TIMESTAMP,
                getLong(KEY_APP_LAST_SESSION_TIMESTAMP)
            )
            .putInt(
                KEY_APP_SESSION_NUMBER,
                getInt(KEY_APP_SESSION_NUMBER)
            )
            .putBoolean(
                KEY_MISC_FIRST_START_DIALOG_SEEN,
                getBool(KEY_MISC_FIRST_START_DIALOG_SEEN)
            )
            .putInt(
                KEY_MISC_NEWS_LAST_VERSION,
                getInt(KEY_MISC_NEWS_LAST_VERSION)
            )
            .putInt(
                KEY_LIKES_LAST_RATED_SESSION,
                getInt(KEY_LIKES_LAST_RATED_SESSION)
            )
            .apply()
    }

    fun getStoragePath(): String {
        return getString(KEY_APP_STORAGE)
    }

    fun setStoragePath(path: String) {
        setString(KEY_APP_STORAGE, path)
    }

    fun isTtsEnabled(): Boolean {
        return getBool(KEY_TTS_ENABLED, true)
    }

    fun setTtsEnabled(enabled: Boolean) {
        setBool(KEY_TTS_ENABLED, enabled)
    }

    fun getTtsLanguage(): String {
        return getString(KEY_TTS_LANGUAGE)
    }

    fun setTtsLanguage(language: String) {
        setString(KEY_TTS_LANGUAGE, language)
    }

    fun isAutodownloadEnabled(): Boolean {
        return getBool(
            KEY_DOWNLOADER_AUTO,
            true
        )
    }

    fun setAutodownloadEnabled(enabled: Boolean) {
        setBool(KEY_DOWNLOADER_AUTO, enabled)
    }

    fun showZoomButtons(): Boolean {
        return getBool(
            KEY_PREF_ZOOM_BUTTONS,
            true
        )
    }

    fun setShowZoomButtons(show: Boolean) {
        setBool(KEY_PREF_ZOOM_BUTTONS, show)
    }

    fun setStatisticsEnabled(enabled: Boolean) {
        setBool(KEY_PREF_STATISTICS, enabled)
    }

    fun useGoogleServices(): Boolean {
        return getBool(KEY_PREF_USE_GS, true)
    }

    fun setUseGoogleService(use: Boolean) {
        setBool(KEY_PREF_USE_GS, use)
    }

    fun isRoutingDisclaimerAccepted(): Boolean {
        return getBool(KEY_MISC_DISCLAIMER_ACCEPTED)
    }

    fun acceptRoutingDisclaimer() {
        setBool(KEY_MISC_DISCLAIMER_ACCEPTED)
    }

    fun isKitKatMigrationComplete(): Boolean {
        return getBool(KEY_MISC_KITKAT_MIGRATED)
    }

    fun setKitKatMigrationComplete() {
        setBool(KEY_MISC_KITKAT_MIGRATED)
    }

    fun getCurrentUiTheme(): String {
        val res = getString(
            KEY_MISC_UI_THEME,
            ThemeUtils.THEME_DEFAULT
        )
        return if (ThemeUtils.isValidTheme(res)) res else ThemeUtils.THEME_DEFAULT
    }

    fun setCurrentUiTheme(theme: String) {
        if (getCurrentUiTheme() == theme) return
        setString(KEY_MISC_UI_THEME, theme)
    }

    fun getUiThemeSettings(): String {
        val res = getString(
            KEY_MISC_UI_THEME_SETTINGS,
            ThemeUtils.THEME_AUTO
        )
        return if (ThemeUtils.isValidTheme(res) || ThemeUtils.isAutoTheme(res)) res else ThemeUtils.THEME_AUTO
    }

    fun setUiThemeSettings(theme: String): Boolean {
        if (getUiThemeSettings() == theme) return false
        setString(
            KEY_MISC_UI_THEME_SETTINGS,
            theme
        )
        return true
    }

    fun isLargeFontsSize(): Boolean {
        return nativeGetLargeFontsSize()
    }

    fun setLargeFontsSize(value: Boolean) {
        nativeSetLargeFontsSize(value)
    }

    fun getUseMobileDataSettings(): NetworkPolicy.Type {
        val value = getInt(
            KEY_MISC_USE_MOBILE_DATA,
            NetworkPolicy.NONE
        )
        if (value != NetworkPolicy.NONE && value < 0 || value >= NetworkPolicy.Type.values().size) throw AssertionError(
            "Wrong NetworkPolicy type : $value"
        )
        return if (value == NetworkPolicy.NONE) NetworkPolicy.Type.NONE else NetworkPolicy.Type.values().get(
            value
        )
    }

    fun setUseMobileDataSettings(value: NetworkPolicy.Type) {
        setInt(
            KEY_MISC_USE_MOBILE_DATA,
            value.ordinal
        )
        setBool(
            KEY_MISC_USE_MOBILE_DATA_ROAMING,
            ConnectionState.isInRoaming
        )
    }

    fun setMobileDataTimeStamp(timestamp: Long) {
        setLong(
            KEY_MISC_USE_MOBILE_DATA_TIMESTAMP,
            timestamp
        )
    }

    fun getMobileDataTimeStamp(): Long {
        return getLong(
            KEY_MISC_USE_MOBILE_DATA_TIMESTAMP,
            0L
        )
    }

    fun getMobileDataRoaming(): Boolean {
        return getBool(
            KEY_MISC_USE_MOBILE_DATA_ROAMING,
            false
        )
    }

    fun isTransliteration(): Boolean {
        return nativeGetTransliteration()
    }

    fun setTransliteration(value: Boolean) {
        nativeSetTransliteration(value)
    }

    @JvmStatic private external fun nativeGetBoolean(
        name: String,
        defaultValue: Boolean
    ): Boolean

    @JvmStatic private external fun nativeSetBoolean(name: String, value: Boolean)
    @JvmStatic private external fun nativeGetInt(name: String, defaultValue: Int): Int
    @JvmStatic private external fun nativeSetInt(name: String, value: Int)
    @JvmStatic private external fun nativeGetLong(name: String, defaultValue: Long): Long
    @JvmStatic private external fun nativeSetLong(name: String, value: Long)
    @JvmStatic private external fun nativeGetDouble(
        name: String,
        defaultValue: Double
    ): Double

    @JvmStatic private external fun nativeSetDouble(name: String, value: Double)
    @JvmStatic private external fun nativeGetString(
        name: String,
        defaultValue: String
    ): String

    @JvmStatic private external fun nativeSetString(name: String, value: String)
    @JvmStatic private external fun nativeGetLargeFontsSize(): Boolean
    @JvmStatic private external fun nativeSetLargeFontsSize(value: Boolean)
    @JvmStatic private external fun nativeGetTransliteration(): Boolean
    @JvmStatic private external fun nativeSetTransliteration(value: Boolean)
}