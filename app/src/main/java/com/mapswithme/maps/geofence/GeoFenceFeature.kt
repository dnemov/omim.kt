package com.mapswithme.maps.geofence

import android.os.Parcelable
import com.mapswithme.maps.bookmarks.data.FeatureId
import kotlinx.android.parcel.Parcelize

/**
 * Represents CampaignFeature from core.
 */
@Parcelize
class GeoFenceFeature(
    val id: FeatureId,
    val latitude: Double,
    val longitude: Double
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as GeoFenceFeature
        return id == that.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}