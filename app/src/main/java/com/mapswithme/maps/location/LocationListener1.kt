package com.mapswithme.maps.location

import android.location.Location

interface LocationListener {
    open class Simple : LocationListener {
        override fun onLocationUpdated(location: Location) {}
        override fun onCompassUpdated(
            time: Long,
            magneticNorth: Double,
            trueNorth: Double,
            accuracy: Double
        ) {
        }

        override fun onLocationError(errorCode: Int) {}
    }

    fun onLocationUpdated(location: Location)
    fun onCompassUpdated(
        time: Long,
        magneticNorth: Double,
        trueNorth: Double,
        accuracy: Double
    )

    fun onLocationError(errorCode: Int)
}