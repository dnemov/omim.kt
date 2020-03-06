package com.mopub.nativeads

import android.os.SystemClock
import com.mapswithme.maps.ads.AdDataAdapter.StaticAd
import com.mapswithme.maps.ads.CachedMwmNativeAd
import com.mapswithme.maps.ads.MopubNativeAd

object MopubNativeAdFactory {
    @JvmStatic
    fun createNativeAd(ad: NativeAd): CachedMwmNativeAd? {
        val baseAd = ad.baseNativeAd
        return if (baseAd is StaticNativeAd) {
            MopubNativeAd(
                ad, StaticAd(baseAd), null,
                SystemClock.elapsedRealtime()
            )
        } else null
    }
}