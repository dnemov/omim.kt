package com.mapswithme.maps.api

import com.mapswithme.maps.Framework.RouterType

/**
 * Represents Framework::ParsedRoutingData from core.
 */
class ParsedRoutingData(val mPoints: Array<RoutePoint>, @RouterType val mRouterType: Int)