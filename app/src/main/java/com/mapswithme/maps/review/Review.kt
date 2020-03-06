package com.mapswithme.maps.review

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class Review(
    val date: Long,
    val rating: Float,
    val author: String,
    val pros: String?,
    val cons: String?
) : Parcelable