package com.mapswithme.maps.ads

import com.facebook.ads.AdError

internal class FacebookAdError(private val mError: AdError) : NativeAdError {
    override val message: String?
        get() = mError.errorMessage

    override val code: Int
        get() = mError.errorCode

}