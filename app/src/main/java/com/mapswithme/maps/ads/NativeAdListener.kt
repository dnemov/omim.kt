package com.mapswithme.maps.ads

import androidx.annotation.UiThread

interface NativeAdListener {
    @UiThread
    fun onAdLoaded(ad: MwmNativeAd)

    /**
     * Notifies about a error occurred while loading the ad for the specified banner id from the
     * specified ads provider.
     *
     */
    @UiThread
    fun onError(
        bannerId: String,
        provider: String,
        error: NativeAdError
    )

    @UiThread
    fun onClick(ad: MwmNativeAd)
}