package com.mapswithme.maps.geofence

import com.mapswithme.maps.location.LocationPermissionNotGrantedException

interface GeofenceRegistry {
    @Throws(LocationPermissionNotGrantedException::class)
    fun registerGeofences(location: GeofenceLocation)

    @Throws(LocationPermissionNotGrantedException::class)
    fun unregisterGeofences()
}