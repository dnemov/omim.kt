package com.mapswithme.maps.ugc

import java.util.*

class UGCUpdate(
    private val mRatings: Array<UGC.Rating>?,
    var mText: String?,
    val mTimeMillis: Long,
    private val mDeviceLocale: String,
    private val mKeyboardLocale: String
) {

    val ratings: List<UGC.Rating>
        get() = mRatings?.asList() ?: ArrayList()

}