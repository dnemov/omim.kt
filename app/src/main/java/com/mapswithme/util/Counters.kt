package com.mapswithme.util

import android.content.Context
import android.text.TextUtils
import android.text.format.DateUtils
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.mapswithme.maps.BuildConfig
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language

object Counters {
    const val KEY_APP_LAUNCH_NUMBER = "LaunchNumber"
    const val KEY_APP_FIRST_INSTALL_VERSION = "FirstInstallVersion"
    const val KEY_APP_FIRST_INSTALL_FLAVOR = "FirstInstallFlavor"
    const val KEY_APP_LAST_SESSION_TIMESTAMP = "LastSessionTimestamp"
    const val KEY_APP_SESSION_NUMBER = "SessionNumber"
    const val KEY_MISC_FIRST_START_DIALOG_SEEN = "FirstStartDialogSeen"
    const val KEY_MISC_NEWS_LAST_VERSION = "WhatsNewShownVersion"
    const val KEY_LIKES_LAST_RATED_SESSION = "LastRatedSession"
    private const val KEY_LIKES_RATED_DIALOG = "RatedDialog"
    private const val KEY_SHOW_REVIEW_FOR_OLD_USER = "ShowReviewForOldUser"
    private const val KEY_MIGRATION_EXECUTED = "MigrationExecuted"
    fun initCounters(context: Context) {
        PreferenceManager.setDefaultValues(context, R.xml.prefs_main, false)
        updateLaunchCounter()
    }

    fun getFirstInstallVersion(): Int {
        return MwmApplication.prefs()
            .getInt(KEY_APP_FIRST_INSTALL_VERSION, 0)
    }

    fun isFirstStartDialogSeen(context: Context): Boolean {
        return MwmApplication.prefs(context)
            .getBoolean(KEY_MISC_FIRST_START_DIALOG_SEEN, false)
    }

    fun setFirstStartDialogSeen(context: Context) {
        MwmApplication.prefs(context)
            .edit()
            .putBoolean(KEY_MISC_FIRST_START_DIALOG_SEEN, true)
            .apply()
    }

    fun setWhatsNewShown() {
        MwmApplication.prefs()
            .edit()
            .putInt(
                KEY_MISC_NEWS_LAST_VERSION,
                BuildConfig.VERSION_CODE
            )
            .apply()
    }

    fun resetAppSessionCounters() {
        MwmApplication.prefs()
            .edit()
            .putInt(KEY_APP_LAUNCH_NUMBER, 0)
            .putInt(KEY_APP_SESSION_NUMBER, 0)
            .putLong(KEY_APP_LAST_SESSION_TIMESTAMP, 0L)
            .putInt(KEY_LIKES_LAST_RATED_SESSION, 0)
            .apply()
        incrementSessionNumber()
    }

    fun isSessionRated(session: Int): Boolean {
        return MwmApplication.prefs().getInt(
            KEY_LIKES_LAST_RATED_SESSION,
            0
        ) >= session
    }

    fun setRatedSession(session: Int) {
        MwmApplication.prefs()
            .edit()
            .putInt(KEY_LIKES_LAST_RATED_SESSION, session)
            .apply()
    }

    /**
     * Session = single day, when app was started any number of times.
     */
    fun getSessionCount(): Int {
        return MwmApplication.prefs().getInt(KEY_APP_SESSION_NUMBER, 0)
    }

    fun isRatingApplied(dialogFragmentClass: Class<out DialogFragment?>): Boolean {
        return MwmApplication.prefs()
            .getBoolean(
                KEY_LIKES_RATED_DIALOG + dialogFragmentClass.simpleName,
                false
            )
    }

    fun setRatingApplied(dialogFragmentClass: Class<out DialogFragment?>) {
        MwmApplication.prefs()
            .edit()
            .putBoolean(
                KEY_LIKES_RATED_DIALOG + dialogFragmentClass.simpleName,
                true
            )
            .apply()
    }

    fun getInstallFlavor(): String {
        return MwmApplication.prefs()
            .getString(KEY_APP_FIRST_INSTALL_FLAVOR, "").orEmpty()
    }

    private fun updateLaunchCounter() {
        if (incrementLaunchNumber() == 0) {
            if (getFirstInstallVersion() == 0) {
                MwmApplication.prefs()
                    .edit()
                    .putInt(
                        KEY_APP_FIRST_INSTALL_VERSION,
                        BuildConfig.VERSION_CODE
                    )
                    .apply()
            }
            updateInstallFlavor()
        }
        incrementSessionNumber()
    }

    private fun incrementLaunchNumber(): Int {
        return increment(KEY_APP_LAUNCH_NUMBER)
    }

    private fun updateInstallFlavor() {
        val installedFlavor = getInstallFlavor()
        if (TextUtils.isEmpty(installedFlavor)) {
            MwmApplication.prefs()
                .edit()
                .putString(
                    KEY_APP_FIRST_INSTALL_FLAVOR,
                    BuildConfig.FLAVOR
                )
                .apply()
        }
    }

    private fun incrementSessionNumber() {
        val lastSessionTimestamp: Long = MwmApplication.prefs()
            .getLong(KEY_APP_LAST_SESSION_TIMESTAMP, 0)
        if (DateUtils.isToday(lastSessionTimestamp)) return
        MwmApplication.prefs()
            .edit()
            .putLong(
                KEY_APP_LAST_SESSION_TIMESTAMP,
                System.currentTimeMillis()
            )
            .apply()
        increment(KEY_APP_SESSION_NUMBER)
    }

    private fun increment(key: String): Int {
        var value: Int = MwmApplication.prefs().getInt(key, 0)
        MwmApplication.prefs()
            .edit()
            .putInt(key, ++value)
            .apply()
        return value
    }

    fun setShowReviewForOldUser(value: Boolean) {
        MwmApplication.prefs()
            .edit()
            .putBoolean(KEY_SHOW_REVIEW_FOR_OLD_USER, value)
            .apply()
    }

    fun isShowReviewForOldUser(): Boolean {
        return MwmApplication.prefs()
            .getBoolean(KEY_SHOW_REVIEW_FOR_OLD_USER, false)
    }

    fun isMigrationNeeded(): Boolean {
        return !MwmApplication.prefs().getBoolean(
            KEY_MIGRATION_EXECUTED,
            false
        )
    }

    fun setMigrationExecuted() {
        MwmApplication.prefs()
            .edit()
            .putBoolean(KEY_MIGRATION_EXECUTED, true)
            .apply()
    }
}