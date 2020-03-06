package com.mapswithme.maps.ads

object Factory {
    internal fun createLoaderForBanner(
        banner: Banner,
        cacheListener: OnAdCacheModifiedListener?,
        tracker: AdTracker?
    ): NativeAdLoader {
        val provider = banner.provider
        when (provider) {
            Providers.FACEBOOK -> return FacebookAdsLoader(cacheListener, tracker)
            Providers.MY_TARGET -> return MyTargetAdsLoader(cacheListener, tracker)
            Providers.MOPUB -> return MopubNativeDownloader(cacheListener, tracker)
            Providers.GOOGLE -> throw AssertionError("Not implemented yet")
            else -> throw AssertionError("Unknown ads provider: $provider")
        }
    }

    fun createCompoundLoader(
        cacheListener: OnAdCacheModifiedListener?, tracker: AdTracker?
    ): CompoundNativeAdLoader {
        return CompoundNativeAdLoader(cacheListener, tracker)
    }
}
