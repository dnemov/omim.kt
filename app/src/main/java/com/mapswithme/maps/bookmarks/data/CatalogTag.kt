package com.mapswithme.maps.bookmarks.data

import android.graphics.Color
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class CatalogTag(
    val id: String,
    val localizedName: String,
    private val r: Float,
    private val g: Float,
    private val b: Float
): Parcelable {
    val color: Int
        get() = Color.rgb((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
}