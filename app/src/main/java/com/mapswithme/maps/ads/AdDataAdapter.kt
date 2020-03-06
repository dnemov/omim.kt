package com.mapswithme.maps.ads

import com.mopub.nativeads.BaseNativeAd
import com.mopub.nativeads.StaticNativeAd

abstract class AdDataAdapter<T : BaseNativeAd> protected constructor(protected val ad: T) {

    abstract val title: String?
    abstract val text: String?
    abstract val iconImageUrl: String?
    abstract val callToAction: String?
    abstract val privacyInfoUrl: String?
    abstract val type: NetworkType

    class StaticAd(ad: StaticNativeAd) : AdDataAdapter<StaticNativeAd>(ad) {

        override val title: String?
            get() = ad.title
        override val text: String?
            get() = ad.text
        override val iconImageUrl: String?
            get() = ad.iconImageUrl
        override val callToAction: String?
            get() = ad.callToAction
        override val privacyInfoUrl: String?
            get() = ad.privacyInformationIconClickThroughUrl
        override val type: NetworkType
            get() = NetworkType.MOPUB
    }

}