package com.mapswithme.maps

import android.util.Log
import com.mapswithme.maps.background.AppBackgroundTracker.OnTransitionListener
import com.mapswithme.maps.geofence.GeofenceLocation.Companion.from
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.location.LocationPermissionNotGrantedException
import com.mapswithme.util.log.LoggerFactory

internal class AppBaseTransitionListener(private val mApplication: MwmApplication) :
    OnTransitionListener {
    override fun onTransit(foreground: Boolean) {
        if (!foreground && LoggerFactory.INSTANCE.isFileLoggingEnabled) {
            Log.i(
                MwmApplication.TAG,
                "The app goes to background. All logs are going to be zipped."
            )
            LoggerFactory.INSTANCE.zipLogs(null)
        }
        if (foreground) return
        updateGeofences()
    }

    private fun updateGeofences() {
        val lastKnownLocation =
            LocationHelper.INSTANCE.lastKnownLocation ?: return
        val geofenceRegistry = mApplication.geofenceRegistry
        try {
            geofenceRegistry.unregisterGeofences()
            geofenceRegistry.registerGeofences(from(lastKnownLocation))
        } catch (e: LocationPermissionNotGrantedException) {
            mApplication.logger.d(MwmApplication.TAG, "Location permission not granted!", e)
        }
    }

}