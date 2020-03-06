package com.mapswithme.maps.purchase

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize class ProductDetails(
    val productId: String,
    val price: Float,
    val currencyCode: String,
    val title: String
) : Parcelable