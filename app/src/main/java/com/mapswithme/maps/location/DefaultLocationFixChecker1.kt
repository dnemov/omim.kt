package com.mapswithme.maps.location

import android.location.Location
import com.mapswithme.util.LocationUtils

internal open class DefaultLocationFixChecker : LocationFixChecker {
    override fun isAccuracySatisfied(location: Location): Boolean { // If it's a gps location then we completely ignore an accuracy checking,
// because there are cases on some devices (https://jira.mail.ru/browse/MAPSME-3789)
// when location is good, but it doesn't contain an accuracy for some reasons
        return if (isFromGpsProvider(location)) true else location.accuracy > 0.0f
        // Completely ignore locations without lat and lon
    }

    override fun isLocationBetterThanLast(newLocation: Location): Boolean {
        val lastLocation = LocationHelper.INSTANCE.savedLocation ?: return true
        return if (isFromGpsProvider(lastLocation) && lastLocation.accuracy == 0.0f) true else isLocationBetterThanLast(
            newLocation,
            lastLocation
        )
    }

    open fun isLocationBetterThanLast(
        newLocation: Location,
        lastLocation: Location
    ): Boolean {
        val speed = Math.max(
            DEFAULT_SPEED_MPS,
            (newLocation.speed + lastLocation.speed) / 2.0
        )
        val lastAccuracy =
            lastLocation.accuracy + speed * LocationUtils.getDiff(lastLocation, newLocation)
        return newLocation.accuracy < lastAccuracy
    }

    companion object {
        private const val DEFAULT_SPEED_MPS = 5.0
        private const val GPS_LOCATION_PROVIDER = "gps"
        private fun isFromGpsProvider(location: Location): Boolean {
            return GPS_LOCATION_PROVIDER == location.provider
        }
    }
}