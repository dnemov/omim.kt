package com.mapswithme.maps.routing

import com.mapswithme.maps.routing.RoutePointInfo.RouteMarkType

interface RoutingBottomMenuListener {
    fun onUseMyPositionAsStart()
    fun onSearchRoutePoint(@RouteMarkType type: Int)
    fun onRoutingStart()
}