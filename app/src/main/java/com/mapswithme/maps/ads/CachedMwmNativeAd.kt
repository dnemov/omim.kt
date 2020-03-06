package com.mapswithme.maps.ads

abstract class CachedMwmNativeAd internal constructor(val loadedTime: Long) : BaseMwmNativeAd() {

    abstract fun detachAdListener()
    abstract fun attachAdListener(listener: Any)

}