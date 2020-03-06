package com.mapswithme.util

import android.content.Context
import android.preference.PreferenceManager
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.maps.bookmarks.BookmarksPageFactory
import com.mapswithme.util.Config.KEY_PREF_STATISTICS
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language

class SharedPropertiesUtils private constructor() {
    companion object {
        private const val USER_AGREEMENT_TERM_OF_USE = "user_agreement_term_of_use"
        private const val USER_AGREEMENT_PRIVACY_POLICY = "user_agreement_privacy_policy"
        private const val PREFS_SHOW_EMULATE_BAD_STORAGE_SETTING =
            "ShowEmulateBadStorageSetting"
        private const val PREFS_BACKUP_WIDGET_EXPANDED = "BackupWidgetExpanded"
        private const val PREFS_WHATS_NEW_TITLE_CONCATENATION =
            "WhatsNewTitleConcatenation"
        private const val PREFS_CATALOG_CATEGORIES_HEADER_CLOSED =
            "CatalogCategoriesHeaderClosed"
        private const val PREFS_BOOKMARK_CATEGORIES_LAST_VISIBLE_PAGE =
            "BookmarkCategoriesLastVisiblePage"
        private val PREFS =
            PreferenceManager.getDefaultSharedPreferences(MwmApplication.get())

        var isStatisticsEnabled: Boolean
            get() = MwmApplication.prefs().getBoolean(KEY_PREF_STATISTICS, true)
            set(enabled) {
                MwmApplication.prefs().edit().putBoolean(KEY_PREF_STATISTICS, enabled).apply()
            }

        fun setShouldShowEmulateBadStorageSetting(show: Boolean) {
            PREFS.edit().putBoolean(
                PREFS_SHOW_EMULATE_BAD_STORAGE_SETTING,
                show
            ).apply()
        }

        fun shouldShowEmulateBadStorageSetting(): Boolean {
            return PREFS.getBoolean(
                PREFS_SHOW_EMULATE_BAD_STORAGE_SETTING,
                false
            )
        }

        fun shouldEmulateBadExternalStorage(): Boolean {
            val key: String =
                MwmApplication.get().getString(R.string.pref_emulate_bad_external_storage)
            return PREFS.getBoolean(key, false)
        }

        var backupWidgetExpanded: Boolean
            get() = PREFS.getBoolean(
                PREFS_BACKUP_WIDGET_EXPANDED,
                true
            )
            set(expanded) {
                PREFS.edit().putBoolean(
                    PREFS_BACKUP_WIDGET_EXPANDED,
                    expanded
                ).apply()
            }

        val whatsNewTitleConcatenation: String?
            get() = PREFS.getString(
                PREFS_WHATS_NEW_TITLE_CONCATENATION,
                null
            )

        fun setWhatsNewTitleConcatenation(concatenation: String) {
            PREFS.edit().putString(
                PREFS_WHATS_NEW_TITLE_CONCATENATION,
                concatenation
            ).apply()
        }

        fun isCatalogCategoriesHeaderClosed(context: Context): Boolean {
            return MwmApplication.prefs(context)
                .getBoolean(
                    PREFS_CATALOG_CATEGORIES_HEADER_CLOSED,
                    false
                )
        }

        fun setCatalogCategoriesHeaderClosed(
            context: Context,
            value: Boolean
        ) {
            MwmApplication.prefs(context)
                .edit()
                .putBoolean(
                    PREFS_CATALOG_CATEGORIES_HEADER_CLOSED,
                    value
                )
                .apply()
        }

        fun getLastVisibleBookmarkCategoriesPage(context: Context): Int {
            return MwmApplication.prefs(context)
                .getInt(
                    PREFS_BOOKMARK_CATEGORIES_LAST_VISIBLE_PAGE,
                    BookmarksPageFactory.PRIVATE.ordinal
                )
        }

        fun setLastVisibleBookmarkCategoriesPage(
            context: Context,
            pageIndex: Int
        ) {
            MwmApplication.prefs(context)
                .edit()
                .putInt(
                    PREFS_BOOKMARK_CATEGORIES_LAST_VISIBLE_PAGE,
                    pageIndex
                )
                .apply()
        }

        fun isTermOfUseAgreementConfirmed(context: Context): Boolean {
            return getBoolean(
                context,
                USER_AGREEMENT_TERM_OF_USE
            )
        }

        fun isPrivacyPolicyAgreementConfirmed(context: Context): Boolean {
            return getBoolean(
                context,
                USER_AGREEMENT_PRIVACY_POLICY
            )
        }

        fun putPrivacyPolicyAgreement(
            context: Context,
            isChecked: Boolean
        ) {
            putBoolean(
                context,
                USER_AGREEMENT_PRIVACY_POLICY,
                isChecked
            )
        }

        fun putTermOfUseAgreement(
            context: Context,
            isChecked: Boolean
        ) {
            putBoolean(
                context,
                USER_AGREEMENT_TERM_OF_USE,
                isChecked
            )
        }

        private fun getBoolean(
            context: Context,
            key: String
        ): Boolean {
            return MwmApplication.prefs(context).getBoolean(key, false)
        }

        private fun putBoolean(
            context: Context,
            key: String,
            value: Boolean
        ) {
            MwmApplication.prefs(context)
                .edit()
                .putBoolean(key, value)
                .apply()
        }
    }

    //Utils class
    init {
        throw IllegalStateException("Try instantiate utility class SharedPropertiesUtils")
    }
}