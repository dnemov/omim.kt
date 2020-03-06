package com.mapswithme.maps.discovery

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class LocalExpert(
    val id: Int = 0,
    val name: String,
    val country: String,
    val city: String,
    var rating: Double = 0.0,
    var reviewCount: Int = 0,
    val price: Double = 0.0,
    val currency: String,
    val motto: String,
    val aboutExpert: String,
    val offerDescription: String,
    val pageUrl: String,
    val photoUrl: String
): Parcelable