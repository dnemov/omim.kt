package com.mapswithme.maps.location

import android.location.Location

internal class FusedLocationFixChecker : DefaultLocationFixChecker() {
    public override fun isLocationBetterThanLast(
        newLocation: Location,
        lastLocation: Location
    ): Boolean { // We believe that google services always return good locations.
        return isFromFusedProvider(newLocation) ||
                !isFromFusedProvider(lastLocation) && super.isLocationBetterThanLast(
            newLocation,
            lastLocation
        )
    }

    companion object {
        private const val GMS_LOCATION_PROVIDER = "fused"
        private fun isFromFusedProvider(location: Location): Boolean {
            return GMS_LOCATION_PROVIDER.equals(
                location.provider,
                ignoreCase = true
            )
        }
    }
}