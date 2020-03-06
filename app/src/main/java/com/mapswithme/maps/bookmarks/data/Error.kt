package com.mapswithme.maps.bookmarks.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.net.HttpURLConnection

@Parcelize
class Error(private val mHttpCode: Int, val message: String?) : Parcelable {

    constructor(message: String?) : this(
        HttpURLConnection.HTTP_UNAVAILABLE,
        message
    )

    val isForbidden: Boolean
        get() = mHttpCode == HttpURLConnection.HTTP_FORBIDDEN

    val isPaymentRequired: Boolean
        get() = mHttpCode == HttpURLConnection.HTTP_PAYMENT_REQUIRED

}