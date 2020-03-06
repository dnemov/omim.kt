package com.mapswithme.maps.ads

import android.view.View
import android.widget.ImageView
import com.my.target.nativeads.NativeAd

internal class MyTargetNativeAd(
    private val mAd: NativeAd,
    timestamp: Long
) : CachedMwmNativeAd(timestamp) {
    override val bannerId: String

    override val title: String
        get() = mAd.banner!!.title!!

    override val description: String
        get() = mAd.banner!!.description!!

    override val action: String
        get() = mAd.banner!!.ctaText!!

    override fun loadIcon(view: View) {
        val banner = mAd.banner
        val icon = banner!!.icon
        if (icon != null) NativeAd.loadImageToView(
            icon,
            (view as ImageView)
        )
    }

    override fun unregister(view: View) {
        mAd.unregisterView()
    }

    override fun register(view: View) {
        mAd.registerView(view)
    }

    override val provider: String
        get() = Providers.MY_TARGET

    override val privacyInfoUrl: String?
        get() = null

    override fun detachAdListener() {
        mAd.listener = null
    }

    override fun attachAdListener(listener: Any) {
        if (listener !is NativeAd.NativeAdListener) throw AssertionError(
            "A listener for myTarget ad must be instance of " +
                    "NativeAd.NativeAdListener class! Not '" + listener.javaClass + "'!"
        )
        mAd.listener = listener
    }

    override val networkType: NetworkType
        get() = NetworkType.MYTARGET

    init {
        val params = mAd.customParams
        val data = params.data
        bannerId = data[MyTargetAdsLoader.Companion.ZONE_KEY_PARAMETER]!!
    }
}