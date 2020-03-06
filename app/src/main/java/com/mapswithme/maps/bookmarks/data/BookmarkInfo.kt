package com.mapswithme.maps.bookmarks.data

import androidx.annotation.IntRange
import com.mapswithme.maps.Framework
import com.mapswithme.util.GeoUtils
import com.mapswithme.util.sharing.ShareableInfoProvider

class BookmarkInfo(
    @param:IntRange(from = 0) val categoryId: Long, @param:IntRange(
        from = 0
    ) val bookmarkId: Long
) : ShareableInfoProvider {
    override val name: String
    val featureType: String
    val icon: Icon
    private val mMerX: Double
    private val mMerY: Double
    override val scale: Double
    override val address: String
    private val mLatLonPoint: ParcelablePointD

    fun getDistanceAndAzimuth(
        cLat: Double,
        cLon: Double,
        north: Double
    ): DistanceAndAzimut {
        return Framework.nativeGetDistanceAndAzimuth(mMerX, mMerY, cLat, cLon, north)
    }

    fun getDistance(
        latitude: Double,
        longitude: Double,
        v: Double
    ): String {
        return getDistanceAndAzimuth(latitude, longitude, v).distance
    }

    override val lat: Double
        get() = mLatLonPoint.x

    override val lon: Double
        get() = mLatLonPoint.y

    init {
        name = BookmarkManager.INSTANCE.getBookmarkName(bookmarkId)
        featureType = BookmarkManager.INSTANCE.getBookmarkFeatureType(bookmarkId)
        icon = Icon(
            BookmarkManager.INSTANCE.getBookmarkColor(bookmarkId),
            BookmarkManager.INSTANCE.getBookmarkIcon(bookmarkId)
        )
        val ll = BookmarkManager.INSTANCE.getBookmarkXY(bookmarkId)
        mMerX = ll.x
        mMerY = ll.y
        scale = BookmarkManager.INSTANCE.getBookmarkScale(bookmarkId)
        address = BookmarkManager.INSTANCE.getBookmarkAddress(bookmarkId)
        mLatLonPoint = GeoUtils.toLatLon(mMerX, mMerY)
    }
}