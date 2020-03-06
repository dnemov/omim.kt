package com.mapswithme.maps.location

import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import com.mapswithme.util.log.LoggerFactory

internal class BaseLocationListener(private val mLocationFixChecker: LocationFixChecker) :
    LocationListener, com.google.android.gms.location.LocationListener {
    override fun onLocationChanged(location: Location) {
        LOGGER.d(
            TAG,
            "onLocationChanged, location = $location"
        )
        if (location == null) return
        if (!mLocationFixChecker.isAccuracySatisfied(location)) return
        if (mLocationFixChecker.isLocationBetterThanLast(location)) {
            LocationHelper.INSTANCE.resetMagneticField(location)
            LocationHelper.INSTANCE.onLocationUpdated(location)
            LocationHelper.INSTANCE.notifyLocationUpdated()
        } else {
            val last = LocationHelper.INSTANCE.savedLocation
            if (last != null) {
                LOGGER.d(
                    TAG,
                    "The new location from '" + location.provider
                            + "' is worse than the last one from '" + last.provider + "'"
                )
            }
        }
    }

    override fun onProviderDisabled(provider: String) {
        LOGGER.d(
            TAG,
            "Disabled location provider: $provider"
        )
    }

    override fun onProviderEnabled(provider: String) {
        LOGGER.d(
            TAG,
            "Enabled location provider: $provider"
        )
    }

    override fun onStatusChanged(
        provider: String,
        status: Int,
        extras: Bundle
    ) {
        LOGGER.d(
            TAG,
            "Status changed for location provider: $provider; new status = $status"
        )
    }

    companion object {
        private val TAG = BaseLocationListener::class.java.simpleName
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.LOCATION)
    }

}