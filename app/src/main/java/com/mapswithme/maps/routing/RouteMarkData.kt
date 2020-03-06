package com.mapswithme.maps.routing

import com.mapswithme.maps.routing.RoutePointInfo.RouteMarkType

/**
 * Represents RouteMarkData from core.
 */
class RouteMarkData(
    val mTitle: String?, val mSubtitle: String?,
    @field:RouteMarkType @param:RouteMarkType val mPointType: Int,
    val mIntermediateIndex: Int, val mIsVisible: Boolean, val mIsMyPosition: Boolean,
    val mIsPassed: Boolean, val mLat: Double, val mLon: Double
)