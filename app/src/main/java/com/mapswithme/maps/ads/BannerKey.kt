package com.mapswithme.maps.ads

import net.jcip.annotations.Immutable

@Immutable
data class BannerKey(private val mProvider: String, private val mId: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val bannerKey = other as BannerKey
        return if (mProvider != bannerKey.mProvider) false else mId == bannerKey.mId
    }

    override fun hashCode(): Int {
        var result = mProvider.hashCode()
        result = 31 * result + mId.hashCode()
        return result
    }

}