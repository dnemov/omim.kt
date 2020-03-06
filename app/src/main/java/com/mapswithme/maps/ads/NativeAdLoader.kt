package com.mapswithme.maps.ads

import android.content.Context

interface NativeAdLoader {
    /**
     * Loads an ad for the specified banner id. A caller will be notified about loading through
     * [NativeAdListener] interface.
     *
     * @param context An activity context.
     * @param bannerId A banner id that ad will be loaded for.
     */
    fun loadAd(context: Context, bannerId: String)

    /**
     * Caller should set this listener to be informed about status of an ad loading.
     *
     * @see NativeAdListener
     */
    fun setAdListener(adListener: NativeAdListener?)

    /**
     * Indicated whether the ad for the specified banner is loading right now or not.
     *
     * @param bannerId A specified banner id.
     * @return `true` if loading is in a progress, otherwise - `false`.
     */
    fun isAdLoading(bannerId: String): Boolean

    /**
     * Cancels the loading process.
     *
     */
    fun cancel()

    /**
     * Detaches this loader from UI context. Must be called every time when current UI context is going
     * to be destroyed. Otherwise, memory leaks are possible.
     */
    fun detach()

    /**
     * Attaches this loader to UI context.
     */
    fun attach()
}