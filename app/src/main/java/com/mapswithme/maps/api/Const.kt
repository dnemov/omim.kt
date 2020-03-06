package com.mapswithme.maps.api

object Const {
    /* Request extras */
    const val AUTHORITY = "com.mapswithme.maps.api"
    const val EXTRA_URL = "$AUTHORITY.url"
    const val EXTRA_TITLE = "$AUTHORITY.title"
    const val EXTRA_API_VERSION = "$AUTHORITY.version"
    const val EXTRA_CALLER_APP_INFO =
        "$AUTHORITY.caller_app_info"
    const val EXTRA_HAS_PENDING_INTENT =
        "$AUTHORITY.has_pen_intent"
    const val EXTRA_CALLER_PENDING_INTENT =
        "$AUTHORITY.pending_intent"
    const val EXTRA_RETURN_ON_BALLOON_CLICK =
        "$AUTHORITY.return_on_balloon_click"
    const val EXTRA_PICK_POINT = "$AUTHORITY.pick_point"
    const val EXTRA_CUSTOM_BUTTON_NAME =
        "$AUTHORITY.custom_button_name"
    /* Response extras */ /* Point part-by-part*/
    const val EXTRA_MWM_RESPONSE_POINT_NAME =
        "$AUTHORITY.point_name"
    const val EXTRA_MWM_RESPONSE_POINT_LAT =
        "$AUTHORITY.point_lat"
    const val EXTRA_MWM_RESPONSE_POINT_LON =
        "$AUTHORITY.point_lon"
    const val EXTRA_MWM_RESPONSE_POINT_ID =
        "$AUTHORITY.point_id"
    const val EXTRA_MWM_RESPONSE_ZOOM =
        "$AUTHORITY.zoom_level"
    const val ACTION_MWM_REQUEST = "$AUTHORITY.request"
    const val API_VERSION = 2
    const val CALLBACK_PREFIX = "mapswithme.client."
}