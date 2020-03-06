package com.mapswithme.maps.ads

import android.content.Context
import android.os.SystemClock
import com.mapswithme.maps.PrivateVariables
import com.mapswithme.util.log.LoggerFactory
import com.my.target.nativeads.NativeAd
import net.jcip.annotations.NotThreadSafe

@NotThreadSafe
internal class MyTargetAdsLoader(
    listener: OnAdCacheModifiedListener?,
    tracker: AdTracker?
) : CachingNativeAdLoader(tracker, listener), NativeAd.NativeAdListener {
    public override fun loadAdFromProvider(
        context: Context,
        bannerId: String
    ) {
        val ad =
            NativeAd(SLOT, context)
        ad.listener = this
        ad.customParams
            .setCustomParam(ZONE_KEY_PARAMETER, bannerId)
        ad.load()
    }

    override fun onLoad(nativeAd: NativeAd) {
        val ad: CachedMwmNativeAd =
            MyTargetNativeAd(nativeAd, SystemClock.elapsedRealtime())
        onAdLoaded(ad.bannerId, ad)
    }

    override fun onNoAd(
        s: String,
        nativeAd: NativeAd
    ) {
        LOGGER.w(TAG, "onNoAd s = $s")
        val params = nativeAd.customParams
        val bannerId =
            params.getCustomParam(ZONE_KEY_PARAMETER)
        onError(bannerId!!, provider, MyTargetAdError(s))
    }

    override fun onClick(nativeAd: NativeAd) {
        val params = nativeAd.customParams
        val bannerId =
            params.data[ZONE_KEY_PARAMETER]
        onAdClicked(bannerId!!)
    }

    override fun onShow(nativeAd: NativeAd) { // No op.
    }

    override fun onVideoPlay(nativeAd: NativeAd) { /* Do nothing */
    }

    override fun onVideoPause(nativeAd: NativeAd) { /* Do nothing */
    }

    override fun onVideoComplete(nativeAd: NativeAd) { /* Do nothing */
    }

    override val provider: String
        get() = Providers.MY_TARGET

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = MyTargetAdsLoader::class.java.simpleName
        private val SLOT = PrivateVariables.myTargetRbSlot()
        const val ZONE_KEY_PARAMETER = "_SITEZONE"
    }
}