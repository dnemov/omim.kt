package com.mapswithme.maps.widget.placepage

import android.view.View
import androidx.annotation.LayoutRes
import com.mapswithme.maps.R
import com.mapswithme.maps.ads.MwmNativeAd
import com.mapswithme.maps.ads.NetworkType
import java.util.*

class NativeAdWrapper internal constructor(private val mNativeAd: MwmNativeAd) : MwmNativeAd {
    val type: UiType
    override val bannerId: String
        get() = mNativeAd.bannerId

    override val title: String
        get() = mNativeAd.title

    override val description: String
        get() = mNativeAd.description

    override val action: String
        get() = mNativeAd.action

    override fun loadIcon(view: View) {
        mNativeAd.loadIcon(view)
    }

    override fun registerView(bannerView: View) {
        mNativeAd.registerView(bannerView)
    }

    override fun unregisterView(bannerView: View) {
        mNativeAd.unregisterView(bannerView)
    }

    override val provider: String
        get() = mNativeAd.provider

    override val privacyInfoUrl: String?
        get() = mNativeAd.privacyInfoUrl

    override val networkType: NetworkType
        get() {
            throw UnsupportedOperationException("It's not supported for UI!")
        }

    enum class UiType(@field:LayoutRes @get:LayoutRes val layoutId: Int, private val mShowAdChoiceIcon: Boolean) {
        DEFAULT(R.layout.place_page_banner, true);

        fun showAdChoiceIcon(): Boolean {
            return mShowAdChoiceIcon
        }

    }

    companion object {
        private val TYPES: Map<NetworkType, UiType> =
            object : EnumMap<NetworkType, UiType>(
                NetworkType::class.java
            ) {
                init {
                    put(NetworkType.MOPUB, UiType.DEFAULT)
                    put(NetworkType.FACEBOOK, UiType.DEFAULT)
                    put(NetworkType.MYTARGET, UiType.DEFAULT)
                }
            }
    }

    init {
        type = TYPES[mNativeAd.networkType]!!
    }
}