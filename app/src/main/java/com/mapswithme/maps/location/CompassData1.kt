package com.mapswithme.maps.location

import com.mapswithme.maps.MwmApplication
import com.mapswithme.util.LocationUtils

class CompassData {
    var magneticNorth = 0.0
        private set
    var trueNorth = 0.0
        private set
    var north = 0.0
        private set

    fun update(magneticNorth: Double, trueNorth: Double) {
        val top = MwmApplication.backgroundTracker()?.topActivity ?: return
        val rotation = top.windowManager.defaultDisplay.rotation
        this.magneticNorth = LocationUtils.correctCompassAngle(rotation, magneticNorth)
        this.trueNorth = LocationUtils.correctCompassAngle(rotation, trueNorth)
        north = if (this.trueNorth >= 0.0) this.trueNorth else this.magneticNorth
    }

}