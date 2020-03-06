package com.mapswithme.util

import com.mapswithme.maps.bookmarks.data.ParcelablePointD

object GeoUtils {
    fun toLatLon(merX: Double, merY: Double): ParcelablePointD {
        return nativeToLatLon(merX, merY)
    }

    @JvmStatic private external fun nativeToLatLon(merX: Double, merY: Double): ParcelablePointD
}