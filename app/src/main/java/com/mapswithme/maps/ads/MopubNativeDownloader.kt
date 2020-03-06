package com.mapswithme.maps.ads

import android.content.Context
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.util.Language
import com.mapswithme.util.log.LoggerFactory
import com.mopub.nativeads.*
import com.mopub.nativeads.MoPubNative.MoPubNativeNetworkListener
import com.mopub.nativeads.MopubNativeAdFactory.createNativeAd
import com.mopub.nativeads.NativeAd.MoPubNativeEventListener
import com.mopub.nativeads.RequestParameters.NativeAdAsset
import java.util.*

internal class MopubNativeDownloader(
    listener: OnAdCacheModifiedListener?,
    tracker: AdTracker?
) : CachingNativeAdLoader(tracker, listener), MoPubNativeNetworkListener, MoPubNativeEventListener {
    private var mBannerId: String? = null
    override fun loadAd(
        context: Context,
        bannerId: String
    ) {
        mBannerId = bannerId
        super.loadAd(context, bannerId)
    }

    public override fun loadAdFromProvider(
        context: Context,
        bannerId: String
    ) {
        val nativeAd = MoPubNative(context, bannerId, this)
        nativeAd.registerAdRenderer(DummyRenderer())
        val requestParameters = RequestParameters.Builder()
        val assetsSet = EnumSet.of(
            NativeAdAsset.TITLE,
            NativeAdAsset.TEXT,
            NativeAdAsset.CALL_TO_ACTION_TEXT,
            NativeAdAsset.ICON_IMAGE
        )
        requestParameters.desiredAssets(assetsSet)
        val l = LocationHelper.INSTANCE.savedLocation
        if (l != null) requestParameters.location(l)
        val locale =
            Language.nativeNormalize(Language.defaultLocale)
        requestParameters.keywords("user_lang:$locale")
        nativeAd.makeRequest(requestParameters.build())
    }

    override val provider: String
        get() = Providers.MOPUB

    override fun onNativeLoad(nativeAd: NativeAd) {
        nativeAd.setMoPubNativeEventListener(this)
        LOGGER.d(
            TAG,
            "onNativeLoad nativeAd = $nativeAd"
        )
        val ad = createNativeAd(nativeAd)
        if (ad != null) onAdLoaded(nativeAd.adUnitId, ad)
    }

    override fun onNativeFail(errorCode: NativeErrorCode) {
        LOGGER.w(
            TAG,
            "onNativeFail $errorCode"
        )
        if (mBannerId == null) throw AssertionError("A banner id must be non-null if a error is occurred")
        onError(mBannerId!!, provider, MopubAdError(errorCode.toString()))
    }

    override fun onImpression(view: View?) {
        LOGGER.d(
            TAG,
            "on MoPub Ad impressed"
        )
    }

    override fun onClick(view: View) {
        if (!TextUtils.isEmpty(mBannerId)) onAdClicked(mBannerId!!)
    }

    private class DummyRenderer : MoPubAdRenderer<StaticNativeAd> {

        override fun renderAdView(
            view: View,
            ad: StaticNativeAd
        ) { // No op.
        }

        override fun supports(nativeAd: BaseNativeAd): Boolean {
            return true
        }

        override fun createAdView(context: Context, parent: ViewGroup?): View {
            return View(context)
        }
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = MopubNativeDownloader::class.java.simpleName
    }
}