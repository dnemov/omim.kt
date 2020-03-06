package com.mapswithme.maps.routing

import java.util.*

/**
 * Represents TransitRouteInfo from core.
 */
class TransitRouteInfo(
    private val mTotalDistance: String, private val mTotalDistanceUnits: String, val totalTime: Int,
    val totalPedestrianDistance: String, val totalPedestrianDistanceUnits: String,
    val totalPedestrianTimeInSec: Int, private val mSteps: Array<TransitStepInfo>
) {

    val transitSteps: List<TransitStepInfo>
        get() = ArrayList(Arrays.asList(*mSteps))

}