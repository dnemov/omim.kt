package com.mapswithme.maps.geofence

import android.location.Location

class GeofenceLocation private constructor(
    val lat: Double,
    val lon: Double,
    val radiusInMeters: Float
) {
    override fun toString(): String {
        val sb = StringBuilder("GeofenceLocation{")
        sb.append("mLat=").append(lat)
        sb.append(", mLon=").append(lon)
        sb.append(", mRadiusInMeters=").append(radiusInMeters)
        sb.append('}')
        return sb.toString()
    }

    companion object {
        @JvmStatic
        fun from(location: Location): GeofenceLocation {
            return GeofenceLocation(
                location.latitude,
                location.longitude,
                location.accuracy
            )
        }
    }

}