package com.mapswithme.maps.bookmarks.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class PaymentData(
    val serverId: String, val productId: String, val name: String,
    val imgUrl: String?, val authorName: String, val group: String
) : Parcelable