package com.mapswithme.maps.promo

import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmDialogFragment

import com.mapswithme.maps.bookmarks.BookmarksCatalogActivity.Companion.start
import com.mapswithme.util.statistics.Statistics

class PromoBookingDialogFragment : BaseMwmDialogFragment() {
    private var mCityGuidesUrl: String? = null
    private var mCityImageUrl: String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_promo_after_booking_dialog, container, false)
    }

    override val style: Int
        protected get() = STYLE_NO_TITLE

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        val cancel = view.findViewById<View>(R.id.cancel)
        cancel.setOnClickListener(CancelClickListener())
        if (!readArguments()) return
        loadImage()
        val cityGuides = view.findViewById<View>(R.id.city_guides)
        cityGuides.setOnClickListener { v: View? -> onCityGuidesClick() }
    }

    private fun readArguments(): Boolean {
        val arguments = arguments ?: return false
        mCityGuidesUrl =
            arguments.getString(EXTRA_CITY_GUIDES_URL)
        mCityImageUrl =
            arguments.getString(EXTRA_CITY_IMAGE_URL)
        return !TextUtils.isEmpty(mCityGuidesUrl) && !TextUtils.isEmpty(mCityImageUrl)
    }

    private fun loadImage() {
        if (mCityImageUrl == null) return
        val imageView =
            viewOrThrow.findViewById<ImageView>(R.id.city_picture)
        Glide.with(imageView.context)
            .load(mCityImageUrl)
            .centerCrop()
            .into(imageView)
    }

    private fun onCityGuidesClick() {
        if (mCityGuidesUrl == null) return
        start(requireActivity(), mCityGuidesUrl!!)
        dismissAllowingStateLoss()
        val builder =
            Statistics.makeInAppSuggestionParamBuilder()
        Statistics.INSTANCE.trackEvent(
            Statistics.EventName.INAPP_SUGGESTION_CLICKED,
            builder
        )
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        trackCancelStats(Statistics.ParamValue.OFFSCREEEN)
    }

    private inner class CancelClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            dismissAllowingStateLoss()
            trackCancelStats(Statistics.ParamValue.CANCEL)
        }
    }

    companion object {
        const val EXTRA_CITY_GUIDES_URL = "city_guides_url"
        const val EXTRA_CITY_IMAGE_URL = "city_image_url"
        private fun trackCancelStats(value: String) {
            val builder =
                Statistics.makeInAppSuggestionParamBuilder()
                    .add(Statistics.EventParam.OPTION, value)
            Statistics.INSTANCE.trackEvent(
                Statistics.EventName.INAPP_SUGGESTION_CLOSED,
                builder
            )
        }
    }
}