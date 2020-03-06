package com.mapswithme.maps.ads

import androidx.annotation.CallSuper

abstract class BaseNativeAdLoader : NativeAdLoader {
    var mAdListener: NativeAdListener? = null
    override fun setAdListener(adListener: NativeAdListener?) {
        mAdListener = adListener
    }

    fun getAdListener(): NativeAdListener? {
        return mAdListener
    }

    @CallSuper
    override fun cancel() {
        setAdListener(null)
    }
}