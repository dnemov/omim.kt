package com.mapswithme.maps.geofence

import com.google.android.gms.location.Geofence
import com.mapswithme.maps.bookmarks.data.FeatureId
import com.mapswithme.maps.bookmarks.data.FeatureId.Companion.fromFeatureIdString

internal object Factory {
    fun from(geofence: Geofence): FeatureId {
        val requestId = geofence.requestId
        return fromFeatureIdString(requestId)
    }
}