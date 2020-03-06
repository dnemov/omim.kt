package com.mapswithme.maps.bookmarks.data

import android.graphics.Point
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

// TODO consider removal and usage of platform PointF
@Parcelize
class ParcelablePointD(var x: Double, var y: Double) : Parcelable {
    val roundX: Int
        get() = Math.round(x).toInt()

    val roundY: Int
        get() = Math.round(y).toInt()

    val roundedPoint: Point
        get() = Point(roundX, roundY)
}