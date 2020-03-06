package com.mapswithme.maps.ads

internal class MopubAdError(override val message: String?) : NativeAdError {

    override val code: Int
        get() = 0

}