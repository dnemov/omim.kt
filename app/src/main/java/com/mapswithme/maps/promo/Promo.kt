package com.mapswithme.maps.promo

import androidx.annotation.MainThread

import com.mapswithme.util.NetworkPolicy
import com.mapswithme.util.UTM.UTMType
import com.mapswithme.util.concurrency.UiThread

enum class Promo {
    INSTANCE;

    interface Listener {
        fun onCityGalleryReceived(gallery: PromoCityGallery)
        fun onErrorReceived()
    }

    private var mListener: Listener? = null
    fun setListener(listener: Listener?) {
        mListener = listener
    }

    // Called from JNI.
    @MainThread
    fun onCityGalleryReceived(gallery: PromoCityGallery) {
        if (!UiThread.isUiThread) throw AssertionError("Must be called from UI thread!")
        if (mListener != null) mListener!!.onCityGalleryReceived(gallery)
    }

    // Called from JNI.
    @MainThread
    fun onErrorReceived() {
        if (!UiThread.isUiThread) throw AssertionError("Must be called from UI thread!")
        if (mListener != null) mListener!!.onErrorReceived()
    }



    companion object {
        @JvmStatic external fun nativeGetPromoAfterBooking(policy: NetworkPolicy): PromoAfterBooking?
        @JvmStatic external fun nativeGetCityUrl(
            policy: NetworkPolicy,
            lat: Double,
            lon: Double
        ): String?

        @JvmStatic external fun nativeRequestCityGallery(
            policy: NetworkPolicy,
            lat: Double, lon: Double, @UTMType utm: Int
        )

        @JvmStatic external fun nativeRequestPoiGallery(
            policy: NetworkPolicy,
            lat: Double, lon: Double, tags: Array<String>,
            @UTMType utm: Int
        )
    }
}