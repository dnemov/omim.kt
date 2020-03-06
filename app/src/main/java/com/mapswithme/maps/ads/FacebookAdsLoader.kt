package com.mapswithme.maps.ads

import android.content.Context
import android.os.SystemClock
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.AdListener
import com.facebook.ads.NativeAd
import com.mapswithme.util.log.LoggerFactory
import net.jcip.annotations.NotThreadSafe
import java.util.*

@NotThreadSafe
internal class FacebookAdsLoader(
    cacheListener: OnAdCacheModifiedListener?,
    tracker: AdTracker?
) : CachingNativeAdLoader(tracker, cacheListener), AdListener {
    override fun onError(ad: Ad, adError: AdError) {
        LOGGER.w(
            TAG,
            "A error '" + adError.errorMessage + "' is occurred while loading " +
                    "an ad for banner id '" + ad.placementId + "'"
        )
        onError(ad.placementId, provider, FacebookAdError(adError))
    }

    override fun onAdLoaded(ad: Ad) {
        val nativeAd: CachedMwmNativeAd = FacebookNativeAd(
            ad as NativeAd,
            SystemClock.elapsedRealtime()
        )
        onAdLoaded(nativeAd.bannerId, nativeAd)
    }

    override fun onAdClicked(ad: Ad) {
        onAdClicked(ad.placementId)
    }

    override fun onLoggingImpression(ad: Ad) {
        LOGGER.i(TAG, "onLoggingImpression")
    }

    public override fun loadAdFromProvider(
        context: Context,
        bannerId: String
    ) {
        val ad = NativeAd(context, bannerId)
        ad.setAdListener(this)
        LOGGER.d(TAG, "Loading is started")
        ad.loadAd(EnumSet.of(NativeAd.MediaCacheFlag.ICON))
    }

    override val provider: String
        get() = Providers.FACEBOOK

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = FacebookAdsLoader::class.java.simpleName
    }
}