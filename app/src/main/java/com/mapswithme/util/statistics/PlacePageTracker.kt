package com.mapswithme.util.statistics

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import com.mapswithme.maps.R
import com.mapswithme.maps.ads.MwmNativeAd
import com.mapswithme.maps.base.Savable
import com.mapswithme.maps.bookmarks.data.MapObject
import com.mapswithme.maps.taxi.TaxiType
import com.mapswithme.maps.widget.placepage.PlacePageView
import com.mapswithme.maps.widget.placepage.Sponsored
import com.mapswithme.util.UiUtils
import com.mapswithme.util.statistics.Statistics.EventName.PP_BANNER_SHOW

class PlacePageTracker(placePageView: PlacePageView, bottomButtons: View) :
    Savable<Bundle?> {
    private val mPlacePageView: PlacePageView
    private val mBottomButtons: View
    private val mTaxi: View
    private var mMapObject: MapObject? = null
    private var mTaxiTracked = false
    private var mSponsoredTracked = false
    private var mBannerDetailsTracked = false
    private var mBannerPreviewTracked = false
    private var mPpDetailsOpenedTracked = false
    fun setMapObject(mapObject: MapObject?) {
        mMapObject = mapObject
    }

    fun onMove() {
        trackTaxiVisibility()
    }

    fun onHidden() {
        mMapObject = null
        mTaxiTracked = false
        mSponsoredTracked = false
        mBannerDetailsTracked = false
        mBannerPreviewTracked = false
        mPpDetailsOpenedTracked = false
    }

    fun onBannerDetails(ad: MwmNativeAd?) {
        if (mBannerDetailsTracked) return
        Statistics.INSTANCE.trackPPBanner(PP_BANNER_SHOW, ad!!, Statistics.PP_BANNER_STATE_DETAILS)
        mBannerDetailsTracked = true
    }

    fun onBannerPreview(ad: MwmNativeAd?) {
        if (mBannerPreviewTracked) return
        Statistics.INSTANCE.trackPPBanner(PP_BANNER_SHOW, ad!!,  Statistics.PP_BANNER_STATE_PREVIEW)
        mBannerPreviewTracked = true
    }

    fun onDetails() {
        if (!mSponsoredTracked) {
            val sponsored: Sponsored? = mPlacePageView.getSponsored()
            if (sponsored != null) {
                Statistics.INSTANCE.trackSponsoredOpenEvent(sponsored)
                mSponsoredTracked = true
            }
        }
        if (!mPpDetailsOpenedTracked) {
            Statistics.INSTANCE.trackEvent(Statistics.EventName.PP_DETAILS_OPEN)
            mPpDetailsOpenedTracked = true
        }
    }

    private fun trackTaxiVisibility() {
        if (!mTaxiTracked && isViewOnScreen(
                mTaxi,
                VISIBILITY_RATIO_TAXI
            ) && mMapObject != null
        ) {
            val taxiTypes: List<TaxiType>? = mMapObject!!.getReachableByTaxiTypes()
            if (taxiTypes != null && !taxiTypes.isEmpty()) {
                val providerName: String = taxiTypes[0].providerName
                Statistics.INSTANCE.trackTaxiEvent(
                    Statistics.EventName.ROUTING_TAXI_SHOW,
                    providerName
                )
                mTaxiTracked = true
            }
        }
    }

    /**
     *
     * @param visibilityRatio Describes what the portion of view should be visible before
     * the view is considered visible on the screen. It can be from 0 to 1.
     */
    private fun isViewOnScreen(view: View, visibilityRatio: Float): Boolean {
        if (UiUtils.isInvisible(mPlacePageView)) return false
        val localRect = Rect()
        val isVisible = view.getGlobalVisibleRect(localRect)
        if (isVisible) {
            val visibleHeight =
                localRect.height() - (localRect.bottom - mBottomButtons.top)
            if (visibleHeight.toFloat() / view.height >= visibilityRatio) return true
        }
        return false
    }

    override fun onSave(outState: Bundle?) {
        outState?.putBoolean(
            EXTRA_SPONSORED_TRACKED,
            mSponsoredTracked
        )
        outState?.putBoolean(
            EXTRA_TAXI_TRACKED,
            mTaxiTracked
        )
        outState?.putBoolean(
            EXTRA_BANNER_DETAILS_TRACKED,
            mBannerDetailsTracked
        )
        outState?.putBoolean(
            EXTRA_BANNER_PREVIEW_TRACKED,
            mBannerPreviewTracked
        )
        outState?.putBoolean(
            EXTRA_PP_DETAILS_OPENED_TRACKED,
            mPpDetailsOpenedTracked
        )
    }

    override fun onRestore(inState: Bundle?) {
        mSponsoredTracked =
            inState?.getBoolean(EXTRA_SPONSORED_TRACKED) ?: false
        mTaxiTracked =
            inState?.getBoolean(EXTRA_TAXI_TRACKED) ?: false
        mBannerDetailsTracked =
            inState?.getBoolean(EXTRA_BANNER_DETAILS_TRACKED) ?: false
        mBannerPreviewTracked =
            inState?.getBoolean(EXTRA_BANNER_PREVIEW_TRACKED) ?: false
        mPpDetailsOpenedTracked =
            inState?.getBoolean(EXTRA_PP_DETAILS_OPENED_TRACKED) ?: false
    }

    companion object {
        private const val VISIBILITY_RATIO_TAXI = 0.6f
        private const val EXTRA_TAXI_TRACKED = "extra_taxi_tracked"
        private const val EXTRA_SPONSORED_TRACKED = "extra_sponsored_tracked"
        private const val EXTRA_BANNER_DETAILS_TRACKED = "extra_banner_details_tracked"
        private const val EXTRA_BANNER_PREVIEW_TRACKED = "extra_banner_preview_tracked"
        private const val EXTRA_PP_DETAILS_OPENED_TRACKED =
            "extra_pp_details_opened_tracked"
    }

    init {
        mPlacePageView = placePageView
        mBottomButtons = bottomButtons
        mTaxi = mPlacePageView.findViewById(R.id.ll__place_page_taxi)
    }
}