package com.mapswithme.maps.ads

import android.view.View
import com.mopub.nativeads.BaseNativeAd

interface AdRegistrator {
    fun registerView(ad: BaseNativeAd, view: View)
    fun unregisterView(ad: BaseNativeAd, view: View)
}