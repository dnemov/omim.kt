package com.mapswithme.maps.bookmarks.data

import android.graphics.Point
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class ParcelablePoint(val point: Point?) : Parcelable {
    constructor(x: Int, y: Int) : this(Point(x, y))
}