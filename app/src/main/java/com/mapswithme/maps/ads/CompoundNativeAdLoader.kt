package com.mapswithme.maps.ads

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils

import com.mapswithme.util.concurrency.UiThread
import com.mapswithme.util.log.Logger
import com.mapswithme.util.log.LoggerFactory

import java.util.ArrayList
import java.util.HashSet

/**
 * Represents a native loader that provides interface to load a few banners simultaneously, i.e.
 * concurrently. This loader makes a decision about which native ad should be posted to the listener
 * based on obtained results. If all native ads for requested banners are obtained, the native ad
 * with the highest priority will be post. Now, MyTarget banner has a high priority. If there is no
 * MyTarget banner in the requested banner list, the first obtained native ad will be posted
 * immediately. Otherwise, this loader will try to obtain/wait for MyTarget native ad even if another
 * provider already give theirs ads.
 *
 */
class CompoundNativeAdLoader internal constructor(
    private val mCacheListener: OnAdCacheModifiedListener?,
    private val mAdTracker: AdTracker?
) : BaseNativeAdLoader(), NativeAdListener {
    private val mLoaders = ArrayList<NativeAdLoader>()
    private val mFailedProviders = HashSet<String>()
    private var mDelayedNotification: Runnable? = null
    /**
     * Indicates about whether the composite loading can be considered as completed or not.
     */
    private var mLoadingCompleted: Boolean = false

    val isAdLoading: Boolean
        get() = !mLoadingCompleted

    @androidx.annotation.UiThread
    fun loadAd(context: Context, banners: List<Banner>) {
        LOGGER.i(TAG, "Load ads for $banners")
        detach()
        cancel()
        mLoadingCompleted = false
        mFailedProviders.clear()

        if (banners.size == 0)
            return

        for (banner in banners) {
            if (TextUtils.isEmpty(banner.id))
                throw AssertionError("A banner id mustn't be empty!")

            val loader = Factory.createLoaderForBanner(banner, mCacheListener, mAdTracker)
            mLoaders.add(loader)
            attach()
            loader.setAdListener(this)
            // TODO: this workaround is need to avoid memory leak of activity context in MyTarget SDK.
            // The fix of myTarged sdk will be done in this issue https://jira.mail.ru/browse/MOBADS-207.
            // After the mentioned issued is fixed, this workaround should be removed. Also, we can't use
            // the application context for all providers, because some of them (e.g. Mopub) requires an
            // activity context and can't work with application context correctly.
            if (loader is MyTargetAdsLoader)
                loader.loadAd(context.applicationContext, banner.id)
            else
                loader.loadAd(context, banner.id)
        }
    }

    override fun loadAd(context: Context, bannerId: String) {
        throw UnsupportedOperationException("A compound loader doesn't support this operation!")
    }

    override fun isAdLoading(bannerId: String): Boolean {
        throw UnsupportedOperationException("A compound loader doesn't support this operation!")
    }

    @SuppressLint("MissingSuperCall")
    override// Don't need to call super here, because we don't need to null the mAdListener from the
    // CompoundNativeAdLoader
    fun cancel() {
        for (loader in mLoaders)
            loader.cancel()
        mLoaders.clear()
    }

    override fun detach() {
        for (loader in mLoaders)
            loader.detach()
    }

    override fun attach() {
        for (loader in mLoaders)
            loader.attach()
    }

    override fun onAdLoaded(ad: MwmNativeAd) {
        if (mDelayedNotification != null) {
            UiThread.cancelDelayedTasks(mDelayedNotification)
            mDelayedNotification = null
        }

        if (mLoadingCompleted)
            return

        // If only one banner is requested and obtained, it will be posted immediately to the listener.
        if (mLoaders.size == 1) {
            onAdLoadingCompleted(ad)
            return
        }

        val provider = ad.provider
        // MyTarget ad has the highest priority, so we notify the listener as soon as that ad is obtained.
        if (Providers.MY_TARGET == provider) {
            onAdLoadingCompleted(ad)
            return
        }

        // If MyTarget ad is failed, the ad from another provider should be posted to the listener.
        if (mFailedProviders.contains(Providers.MY_TARGET)) {
            onAdLoadingCompleted(ad)
            return
        }

        // Otherwise, we must wait a TIMEOUT_MS for the high priority ad.
        // If the high priority ad is not obtained in TIMEOUT_MS, the last obtained ad will be posted
        // to the listener.
        mDelayedNotification = DelayedNotification(ad)
        UiThread.runLater(mDelayedNotification, TIMEOUT_MS.toLong())
    }

    override fun onError(bannerId: String, provider: String, error: NativeAdError) {
        mFailedProviders.add(provider)

        // If all providers give nothing, the listener will be notified about the error.
        if (mFailedProviders.size == mLoaders.size) {
            if (getAdListener() != null)
                getAdListener()!!.onError(bannerId, provider, error)
            return
        }

        // If the high priority ad is just failed, the timer should be forced if it's started.
        if (Providers.MY_TARGET == provider && mDelayedNotification != null) {
            mDelayedNotification!!.run()
            UiThread.cancelDelayedTasks(mDelayedNotification)
            mDelayedNotification = null
            mLoadingCompleted = true
        }
    }

    override fun onClick(ad: MwmNativeAd) {
        if (getAdListener() != null)
            getAdListener()!!.onClick(ad)
    }

    private fun onAdLoadingCompleted(ad: MwmNativeAd) {
        if (getAdListener() != null)
            getAdListener()!!.onAdLoaded(ad)
        mLoadingCompleted = true
    }

    private inner class DelayedNotification internal constructor(private val mAd: MwmNativeAd) : Runnable {

        override fun run() {
            onAdLoadingCompleted(mAd)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = CompoundNativeAdLoader::class.java.simpleName
        private val TIMEOUT_MS = 5000
    }
}
