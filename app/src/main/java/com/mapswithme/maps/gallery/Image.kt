package com.mapswithme.maps.gallery

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Image(
    val url: String,
    val smallUrl: String,
    var description: String? = null,
    var userName: String? = null,
    var userAvatar: String? = null,
    var source: String? = null,
    var date: Long? = null
) : Parcelable {
    constructor(url: String, smallUrl: String) : this(
        url, smallUrl, null
    )
}