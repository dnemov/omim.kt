package com.mapswithme.maps.ads

import android.view.View

/**
 * Represents a native ad object which can be obtained from any providers such as Facebook,
 * MyTarget, etc.
 */
interface MwmNativeAd {
    val bannerId: String
    val title: String
    val description: String
    val action: String
    /**
     *
     * @param view A view which the loaded icon should be placed into.
     */
    fun loadIcon(view: View)

    /**
     * Registers the specified banner view in third-party sdk to track the native ad internally.
     * @param bannerView A view which holds all native ad information.
     */
    fun registerView(bannerView: View)

    /**
     * Unregisters the view attached to the current ad.
     * @param bannerView A view which holds all native ad information.
     */
    fun unregisterView(bannerView: View)

    /**
     * Returns a provider name for this ad.
     */
    val provider: String

    /**
     * Returns a privacy information url, or `null` if not set.
     */
    val privacyInfoUrl: String?

    /**
     * Returns a network type which the native ad belongs to.
     */
    val networkType: NetworkType
}