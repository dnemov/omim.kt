package com.mapswithme.maps.ads

import android.view.View
import android.widget.ImageView
import com.facebook.ads.AdListener
import com.facebook.ads.NativeAd

internal class FacebookNativeAd : CachedMwmNativeAd {
    private val mAd: NativeAd

    constructor(ad: NativeAd, timestamp: Long) : super(timestamp) {
        mAd = ad
    }

    constructor(ad: NativeAd) : super(0) {
        mAd = ad
    }

    override val bannerId: String
        get() = mAd.placementId

    override val title: String
        get() = mAd.adTitle

    override val description: String
        get() = mAd.adBody

    override val action: String
        get() = mAd.adCallToAction

    override fun loadIcon(view: View) {
        NativeAd.downloadAndDisplayImage(
            mAd.adIcon,
            view as ImageView
        )
    }

    override fun unregister(view: View) {
        mAd.unregisterView()
    }

    override fun register(view: View) {
        mAd.registerViewForInteraction(view)
    }

    override val provider: String
        get() = Providers.FACEBOOK

    override val privacyInfoUrl: String?
        get() = mAd.adChoicesLinkUrl

    public override fun detachAdListener() {
        mAd.setAdListener(null)
    }

    public override fun attachAdListener(listener: Any) {
        if (listener !is AdListener) throw AssertionError(
            "A listener for Facebook ad must be instance of " +
                    "AdListener class! Not '" + listener.javaClass + "'!"
        )
        mAd.setAdListener(listener)
    }

    override val networkType: NetworkType
        get() = NetworkType.FACEBOOK
}