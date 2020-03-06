package com.mapswithme.maps.ads

/**
 * A common listener to make all interested observers be able to watch for ads cache modifications.
 */
interface OnAdCacheModifiedListener {
    fun onRemoved(key: BannerKey)
    fun onPut(key: BannerKey)
}