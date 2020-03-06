package com.mapswithme.maps

import com.mapswithme.maps.Framework.LocalAdsEventType
import com.mapswithme.maps.background.NotificationCandidate
import com.mapswithme.maps.bookmarks.data.FeatureId
import com.mapswithme.maps.geofence.GeoFenceFeature
import com.mapswithme.maps.geofence.GeofenceLocation
import java.util.*

object LightFramework {
    @JvmStatic external fun nativeIsAuthenticated(): Boolean
    @JvmStatic external fun nativeGetNumberUnsentUGC(): Int
    @JvmStatic private external fun nativeGetLocalAdsFeatures(
        lat: Double, lon: Double,
        radiusInMeters: Double,
        maxCount: Int
    ): Array<GeoFenceFeature>

    @JvmStatic external fun nativeMakeFeatureId(
        mwmName: String, mwmVersion: Long,
        featureIndex: Int
    ): String

    fun getLocalAdsFeatures(
        lat: Double, lon: Double,
        radiusInMeters: Double,
        maxCount: Int
    ): List<GeoFenceFeature> {
        return nativeGetLocalAdsFeatures(
            lat,
            lon,
            radiusInMeters,
            maxCount
        ).asList()
    }

    fun logLocalAdsEvent(
        location: GeofenceLocation,
        feature: FeatureId
    ) {
        nativeLogLocalAdsEvent(
            LocalAdsEventType.LOCAL_ADS_EVENT_VISIT.ordinal,
            location.lat, location.lon,
            location.radiusInMeters.toInt(), feature.mMwmVersion,
            feature.mMwmName, feature.mFeatureIndex
        )
    }

    @JvmStatic
    private external fun nativeLogLocalAdsEvent(
        type: Int, lat: Double, lon: Double,
        accuracyInMeters: Int, mwmVersion: Long,
        countryId: String, featureIndex: Int
    )

    @JvmStatic external fun nativeGetNotification(): NotificationCandidate?
}