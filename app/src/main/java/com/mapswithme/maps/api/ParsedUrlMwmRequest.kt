package com.mapswithme.maps.api

import androidx.annotation.IntDef

/**
 * Represents url_scheme::ParsedMapApi::ParsingResult from core.
 */
class ParsedUrlMwmRequest(
    val mRoutePoints: Array<RoutePoint>,
    val mGlobalUrl: String,
    val mAppTitle: String,
    val mVersion: Int,
    val mZoomLevel: Double,
    val mGoBackOnBalloonClick: Boolean,
    val mIsValid: Boolean
) {
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @IntDef(
        RESULT_INCORRECT,
        RESULT_MAP,
        RESULT_ROUTE,
        RESULT_SEARCH,
        RESULT_LEAD
    )
    annotation class ParsingResult

    companion object {
        const val RESULT_INCORRECT = 0
        const val RESULT_MAP = 1
        const val RESULT_ROUTE = 2
        const val RESULT_SEARCH = 3
        const val RESULT_LEAD = 4
    }

}