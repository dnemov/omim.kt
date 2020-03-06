package com.mapswithme.util

import com.mapswithme.maps.BuildConfig
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language

object Constants {
    const val STORAGE_PATH = "/Android/data/%s/%s/"
    const val OBB_PATH = "/Android/obb/%s/"
    const val KB = 1024
    const val MB = 1024 * 1024
    const val GB = 1024 * 1024 * 1024
    const val CONNECTION_TIMEOUT_MS = 5000
    const val READ_TIMEOUT_MS = 30000
    const val MWM_DIR_POSTFIX = "/MapsWithMe/"
    const val CACHE_DIR = "cache"
    const val FILES_DIR = "files"

    object Url {
        const val GE0_PREFIX = "ge0://"
        const val MAILTO_SCHEME = "mailto:"
        const val MAIL_SUBJECT = "?subject="
        const val MAIL_BODY = "&body="
        const val HTTP_GE0_PREFIX = "http://ge0.me/"
        const val PLAY_MARKET_HTTPS_APP_PREFIX =
            "https://play.google.com/store/apps/details?id="
        const val FB_MAPSME_COMMUNITY_HTTP = "http://www.facebook.com/MapsWithMe"
        // Profile id is taken from http://graph.facebook.com/MapsWithMe
        const val FB_MAPSME_COMMUNITY_NATIVE = "fb://profile/111923085594432"
        const val TWITTER_MAPSME_HTTP = "https://twitter.com/MAPS_ME"
        const val WEB_SITE = "http://maps.me"
        const val COPYRIGHT = "file:///android_asset/copyright.html"
        const val FAQ = "file:///android_asset/faq.html"
        const val OPENING_HOURS_MANUAL =
            "file:///android_asset/opening_hours_how_to_edit.html"
        const val OSM_REGISTER = "https://www.openstreetmap.org/user/new"
        const val OSM_RECOVER_PASSWORD =
            "https://www.openstreetmap.org/user/forgot-password"
        const val OSM_ABOUT = "https://wiki.openstreetmap.org/wiki/About_OpenStreetMap"
    }

    object Email {
        const val FEEDBACK = "android@maps.me"
        val SUPPORT: String = BuildConfig.SUPPORT_MAIL
        const val RATING = "rating@maps.me"
    }

    object Package {
        const val FB_PACKAGE = "com.facebook.katana"
        const val MWM_PRO_PACKAGE = "com.mapswithme.maps.pro"
        const val MWM_LITE_PACKAGE = "com.mapswithme.maps"
        const val MWM_SAMSUNG_PACKAGE = "com.mapswithme.maps.samsung"
        const val TWITTER_PACKAGE = "com.twitter.android"
    }

    object Rating {
        const val RATING_INCORRECT_VALUE = 0.0f
    }
}