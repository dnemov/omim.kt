package com.mapswithme.maps.ads

import android.view.View
import com.mapswithme.maps.R
import com.mapswithme.util.UiUtils
import com.mapswithme.util.log.LoggerFactory

abstract class BaseMwmNativeAd : MwmNativeAd {
    override fun registerView(view: View) {
        val largeAction =
            view.findViewById<View>(R.id.tv__action_large)
        if (UiUtils.isVisible(largeAction)) {
            LOGGER.d(
                TAG,
                "Register the large action button for '$bannerId'"
            )
            register(largeAction)
            return
        }
        val smallAction =
            view.findViewById<View>(R.id.tv__action_small)
        if (UiUtils.isVisible(smallAction)) {
            LOGGER.d(
                TAG,
                "Register the small action button for '$bannerId'"
            )
            register(smallAction)
        }
    }

    override fun unregisterView(bannerView: View) {
        val largeAction =
            bannerView.findViewById<View>(R.id.tv__action_large)
        if (UiUtils.isVisible(largeAction)) {
            LOGGER.d(
                TAG,
                "Unregister the large action button for '$bannerId'"
            )
            unregister(largeAction)
            return
        }
        val smallAction =
            bannerView.findViewById<View>(R.id.tv__action_small)
        if (UiUtils.isVisible(smallAction)) {
            LOGGER.d(
                TAG,
                "Unregister the small action button for '$bannerId'"
            )
            unregister(smallAction)
        }
    }

    abstract fun register(view: View)
    abstract fun unregister(view: View)
    override fun toString(): String {
        return "Ad title: $title"
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = BaseMwmNativeAd::class.java.simpleName
    }
}