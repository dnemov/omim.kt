package com.mapswithme.util

import androidx.annotation.IntDef
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

object UTM {
    // The order of these constants must correspond to C++ enumeration in partners_api/utm.hpp.
    const val UTM_NONE = 0
    const val UTM_BOOKMARKS_PAGE_CATALOG_BUTTON = 1
    const val UTM_TOOLBAR_BUTTON = 2
    const val UTM_DOWNLOAD_MWM_BANNER = 3
    const val UTM_LARGE_TOPONYMS_PLACEPAGE_GALLERY = 4
    const val UTM_SIGHTSEEINGS_PLACEPAGE_GALLERY = 5
    const val UTM_DISCOVERY_PAGE_GALLERY = 6
    const val UTM_TIPS_AND_TRICKS = 7
    const val UTM_BOOKING_PROMO = 8
    const val UTM_DISCOVER_CATALOG_ONBOARDING = 9
    const val UTM_FREE_SAMPLES_ONBOADING = 10
    const val UTM_OUTDOOR_PLACEPAGE_GALLERY = 11
    // The order of these constants must correspond to C++ enumeration in partners_api/utm.hpp.
    const val UTM_CONTENT_DESCRIPTION = 0
    const val UTM_CONTENT_VIEW = 1
    const val UTM_CONTENT_DETAILS = 2
    const val UTM_CONTENT_MORE = 3

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        UTM_NONE,
        UTM_BOOKMARKS_PAGE_CATALOG_BUTTON,
        UTM_TOOLBAR_BUTTON,
        UTM_DOWNLOAD_MWM_BANNER,
        UTM_LARGE_TOPONYMS_PLACEPAGE_GALLERY,
        UTM_SIGHTSEEINGS_PLACEPAGE_GALLERY,
        UTM_DISCOVERY_PAGE_GALLERY,
        UTM_TIPS_AND_TRICKS,
        UTM_BOOKING_PROMO,
        UTM_DISCOVER_CATALOG_ONBOARDING,
        UTM_FREE_SAMPLES_ONBOADING,
        UTM_OUTDOOR_PLACEPAGE_GALLERY
    )
    annotation class UTMType

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        UTM_CONTENT_DESCRIPTION,
        UTM_CONTENT_VIEW,
        UTM_CONTENT_DETAILS,
        UTM_CONTENT_MORE
    )
    annotation class UTMContentType
}