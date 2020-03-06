package com.mapswithme.util.statistics

object AlohaHelper {
    fun logClick(element: String?) {
        org.alohalytics.Statistics.logEvent(
            ALOHA_CLICK,
            element
        )
    }

    fun logLongClick(element: String?) {
        org.alohalytics.Statistics.logEvent(
            ALOHA_LONG_CLICK,
            element
        )
    }

    fun logException(e: Exception) {
        org.alohalytics.Statistics.logEvent(
            ALOHA_EXCEPTION,
            arrayOf(e.javaClass.simpleName, e.message!!)
        )
    }

    // for aloha stats
    const val ALOHA_CLICK = "\$onClick"
    const val ALOHA_LONG_CLICK = "\$onLongClick"
    const val ALOHA_EXCEPTION = "exceptionAndroid"
    const val ZOOM_IN = "+"
    const val ZOOM_OUT = "-"
    // toolbar actions
    const val TOOLBAR_MY_POSITION = "MyPosition"
    const val TOOLBAR_SEARCH = "search"
    const val TOOLBAR_MENU = "menu"
    const val TOOLBAR_BOOKMARKS = "bookmarks"
    // menu actions
    const val MENU_DOWNLOADER = "downloader"
    const val MENU_SETTINGS = "settingsAndMore"
    const val MENU_SHARE = "share@"
    const val MENU_POINT2POINT = "point2point"
    const val MENU_ADD_PLACE = "addPlace"
    // place page
    const val PP_OPEN = "ppOpen"
    const val PP_CLOSE = "ppClose"
    const val PP_SHARE = "ppShare"
    const val PP_BOOKMARK = "ppBookmark"
    const val PP_ROUTE = "ppRoute"
    // place page details
    const val PP_DIRECTION_ARROW = "ppDirectionArrow"
    const val PP_DIRECTION_ARROW_CLOSE = "ppDirectionArrowClose"
    const val PP_METADATA_COPY = "ppCopyMetadata"
    // routing
    const val ROUTING_BUILD = "routeBuild"
    const val ROUTING_CLOSE = "routeClose"
    const val ROUTING_START = "routeGo"
    const val ROUTING_START_SUGGEST_REBUILD = "routeGoRebuild"
    const val ROUTING_CANCEL = "routeCancel"
    const val ROUTING_VEHICLE_SET = "routerSetVehicle"
    const val ROUTING_PEDESTRIAN_SET = "routerSetPedestrian"
    const val ROUTING_BICYCLE_SET = "routerSetBicycle"
    const val ROUTING_TAXI_SET = "routerSetTaxi"
    const val ROUTING_TRANSIT_SET = "routerSetTransit"
    const val ROUTING_SWAP_POINTS = "routeSwapPoints"
    const val ROUTING_TOGGLE = "routeToggle"
    const val ROUTING_SEARCH_POINT = "routSearchPoint"
    const val ROUTING_SETTINGS = "routingSettings"
    // search
    const val SEARCH_CANCEL = "searchCancel"
    // installation
    const val GPLAY_INSTALL_REFERRER = "\$googlePlayInstallReferrer"

    object Settings {
        const val WEB_SITE = "webSite"
        const val FEEDBACK_GENERAL = "generalFeedback"
        const val REPORT_BUG = "reportABug"
        const val RATE = "rate"
        const val TELL_FRIEND = "tellFriend"
        const val FACEBOOK = "likeOnFb"
        const val TWITTER = "followOnTwitter"
        const val HELP = "help"
        const val ABOUT = "about"
        const val COPYRIGHT = "copyright"
        const val CHANGE_UNITS = "settingsMiles"
    }
}