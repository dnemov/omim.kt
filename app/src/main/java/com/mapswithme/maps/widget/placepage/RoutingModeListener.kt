package com.mapswithme.maps.widget.placepage

import com.mapswithme.maps.settings.RoadType

interface RoutingModeListener {
    fun toggleRouteSettings(roadType: RoadType)
}