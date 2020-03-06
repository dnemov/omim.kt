package com.mapswithme.maps.ads

import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import com.mopub.nativeads.NativeAd
import com.mopub.nativeads.NativeAd.MoPubNativeEventListener
import com.mopub.nativeads.NativeImageHelper

class MopubNativeAd(
    private val mNativeAd: NativeAd, private val mDataAdapter: AdDataAdapter<*>,
    private val mAdRegistrator: AdRegistrator?, timestamp: Long
) : CachedMwmNativeAd(timestamp) {
    override val bannerId: String
        get() = mNativeAd.adUnitId

    override val title: String
        get() = if (TextUtils.isEmpty(mDataAdapter.title)) "" else mDataAdapter.title!!

    override val description: String
        get() = if (TextUtils.isEmpty(mDataAdapter.text)) "" else mDataAdapter.text!!

    override val action: String
        get() = if (TextUtils.isEmpty(mDataAdapter.callToAction)) "" else mDataAdapter.callToAction!!

    override fun loadIcon(view: View) {
        NativeImageHelper.loadImageView(
            mDataAdapter.iconImageUrl,
            view as ImageView
        )
    }

    override fun register(view: View) {
        mNativeAd.prepare(view)
    }

    override fun unregister(view: View) {
        mNativeAd.clear(view)
    }

    override fun registerView(view: View) {
        super.registerView(view)
        mAdRegistrator?.registerView(mNativeAd.baseNativeAd, view)
    }

    override fun unregisterView(view: View) {
        super.unregisterView(view)
        mAdRegistrator?.unregisterView(mNativeAd.baseNativeAd, view)
    }

    override val provider: String
        get() = Providers.MOPUB

    override val privacyInfoUrl: String?
        get() = mDataAdapter.privacyInfoUrl

    override fun detachAdListener() {
        mNativeAd.setMoPubNativeEventListener(null)
    }

    override fun attachAdListener(listener: Any) {
        if (listener !is MoPubNativeEventListener) throw AssertionError(
            "A listener for MoPub ad must be instance of " +
                    "NativeAd.MoPubNativeEventListener class! Not '"
                    + listener.javaClass + "'!"
        )
        mNativeAd.setMoPubNativeEventListener(listener)
    }

    override val networkType: NetworkType
        get() = mDataAdapter.type

    override fun toString(): String {
        return super.toString() + ", mediated ad: " + mNativeAd.baseNativeAd
    }

}