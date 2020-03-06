package com.mapswithme.maps.ads

import android.content.Context
import android.os.SystemClock
import androidx.annotation.CallSuper

import com.mapswithme.util.log.Logger
import com.mapswithme.util.log.LoggerFactory
import net.jcip.annotations.NotThreadSafe

import java.util.HashMap
import java.util.HashSet

@NotThreadSafe
internal abstract class CachingNativeAdLoader(
    private val mTracker: AdTracker?,
    private val mCacheListener: OnAdCacheModifiedListener?
) : BaseNativeAdLoader() {

    /**
     * Returns a provider name for this ad.
     */
    internal abstract val provider: String

    /**
     * Loads an ad for a specified banner id. If there is a cached ad, the caller will be notified
     * immediately through [NativeAdListener.onAdLoaded].
     * Otherwise, the caller will be notified once an ad is loaded through the mentioned method.
     *
     * <br></br><br></br>**Important note: ** if there is a cached ad for the requested banner id, and that ad
     * has a good impression indicator, and there is at least [.REQUEST_INTERVAL_MS] between the
     * first time that ad was requested and the current time the new ad will be loaded.
     *
     */
    @CallSuper
    override fun loadAd(context: Context, bannerId: String) {
        LOGGER.d(TAG, "Load the ad for a banner id '$bannerId'")
        val key = BannerKey(provider, bannerId)
        val cachedAd = getAdByIdFromCache(key)

        if (cachedAd == null) {
            LOGGER.d(TAG, "There is no an ad in a cache")
            loadAdInternally(context, bannerId)
            return
        }

        if (isImpressionGood(cachedAd) && canBeReloaded(cachedAd)) {
            LOGGER.d(TAG, "A new ad will be loaded because the previous one has a good impression")
            loadAdInternally(context, bannerId)
        }

        if (getAdListener() != null) {
            LOGGER.d(TAG, "A cached ad '" + cachedAd.title + "' is set immediately")
            getAdListener()!!.onAdLoaded(cachedAd)
        }
    }

    @CallSuper
    override fun cancel() {
        super.cancel()
        PENDING_REQUESTS.clear()
    }

    private fun isImpressionGood(ad: CachedMwmNativeAd): Boolean {
        return mTracker != null && mTracker.isImpressionGood(ad.provider, ad.bannerId)
    }

    private fun loadAdInternally(context: Context, bannerId: String) {
        if (isAdLoading(bannerId)) {
            LOGGER.d(TAG, "The ad request for banner id '$bannerId' hasn't been completed yet.")
            return
        }

        loadAdFromProvider(context, bannerId)
        PENDING_REQUESTS.add(BannerKey(provider, bannerId))
    }

    internal abstract fun loadAdFromProvider(context: Context, bannerId: String)

    fun onError(bannerId: String, provider: String, error: NativeAdError) {
        PENDING_REQUESTS.remove(BannerKey(provider, bannerId))
        if (getAdListener() != null)
            getAdListener()!!.onError(bannerId, provider, error)
    }

    fun onAdLoaded(bannerId: String, ad: CachedMwmNativeAd) {
        val key = BannerKey(provider, bannerId)
        LOGGER.d(TAG, "A new ad for id '" + key + "' is loaded, title = " + ad.title)
        PENDING_REQUESTS.remove(key)

        val isCacheWasEmpty = isCacheEmptyForId(key)

        LOGGER.d(TAG, "Put the ad '" + ad.title + "' to cache, isCacheWasEmpty = " + isCacheWasEmpty)
        putInCache(key, ad)

        if (isCacheWasEmpty && getAdListener() != null)
            getAdListener()!!.onAdLoaded(ad)
    }

    fun onAdClicked(bannerId: String) {
        if (getAdListener() != null) {
            val nativeAd = getAdByIdFromCache(BannerKey(provider, bannerId))
                ?: throw AssertionError("A native ad must be presented in a cache when it's clicked!")

            getAdListener()!!.onClick(nativeAd)
        }
    }

    /**
     * Indicates whether the ad is loading right now or not.
     *
     * @param bannerId A banner id that an ad is loading for.
     * @return true if an ad is loading, otherwise - false.
     */
    override fun isAdLoading(bannerId: String): Boolean {
        return PENDING_REQUESTS.contains(BannerKey(provider, bannerId))
    }

    private fun putInCache(key: BannerKey, value: CachedMwmNativeAd) {
        CACHE[key] = value
        mCacheListener?.onPut(key)
    }

    private fun removeFromCache(key: BannerKey, value: CachedMwmNativeAd) {
        CACHE.remove(key)
        mCacheListener?.onRemoved(key)
    }

    private fun getAdByIdFromCache(key: BannerKey): CachedMwmNativeAd? {
        return CACHE[key]
    }

    private fun isCacheEmptyForId(key: BannerKey): Boolean {
        return getAdByIdFromCache(key) == null
    }

    @CallSuper
    override fun detach() {
        for (ad in CACHE.values)
            ad.detachAdListener()
    }

    @CallSuper
    override fun attach() {
        for (ad in CACHE.values) {
            if (ad.provider == provider)
                ad.attachAdListener(this)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = CachingNativeAdLoader::class.java.simpleName
        private val REQUEST_INTERVAL_MS = (5 * 1000).toLong()
        private val CACHE = HashMap<BannerKey, CachedMwmNativeAd>()
        private val PENDING_REQUESTS = HashSet<BannerKey>()

        private fun canBeReloaded(ad: CachedMwmNativeAd): Boolean {
            return SystemClock.elapsedRealtime() - ad.loadedTime >= REQUEST_INTERVAL_MS
        }
    }
}
