package com.mapswithme.maps.search

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.HotelUtils
import com.mapswithme.maps.R
import com.mapswithme.maps.bookmarks.data.FeatureId
import com.mapswithme.maps.routing.RoutingController.Companion.get
import com.mapswithme.maps.search.Popularity
import com.mapswithme.maps.search.SearchAdapter.SearchDataViewHolder
import com.mapswithme.maps.ugc.UGC.Companion.nativeFormatRating
import com.mapswithme.util.*
import java.util.*

internal class SearchAdapter(private val mSearchFragment: SearchFragment) :
    RecyclerView.Adapter<SearchDataViewHolder>() {
    private var mResults: Array<SearchResult>? = null
    private val mFilteredHotelIds = FilteredHotelIds()
    private val mClosedMarkerBackground: Drawable

    internal abstract class SearchDataViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        abstract fun bind(
            result: SearchResult,
            position: Int
        )
    }

    private abstract class BaseResultViewHolder internal constructor(view: View) :
        SearchDataViewHolder(view) {
        var mResult: SearchResult? = null
        // Position within search results
        var mOrder = 0

        override fun bind(
            result: SearchResult,
            order: Int
        ) {
            mResult = result
            mOrder = order
            val titleView = titleView
            var title = mResult!!.name
            if (TextUtils.isEmpty(title)) {
                val description =
                    mResult!!.description
                title = if (description != null) Utils.getLocalizedFeatureType(
                    titleView!!.context,
                    description.featureType
                ) else ""
            }
            val builder = SpannableStringBuilder(title)
            if (mResult!!.highlightRanges != null) {
                val size = mResult!!.highlightRanges!!.size / 2
                var index = 0
                for (i in 0 until size) {
                    val start = mResult!!.highlightRanges!![index++]
                    val len = mResult!!.highlightRanges!![index++]
                    builder.setSpan(
                        StyleSpan(Typeface.BOLD),
                        start,
                        start + len,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            if (titleView != null) titleView.text = builder
        }

        @get:AttrRes
        open val tintAttr: Int
            get() = R.attr.colorAccent

        abstract val titleView: TextView?
        abstract fun processClick(
            result: SearchResult?,
            order: Int
        )

        init {
            if (view is TextView) {
                val tintAttr = tintAttr
                if (tintAttr != 0) Graphics.tint(view, tintAttr)
            }
            view.setOnClickListener { processClick(mResult, mOrder) }
        }
    }

    private class FilteredHotelIds {
        private val mFilteredHotelIds =
            SparseArray<Set<FeatureId>>()

        fun put(@BookingFilter.Type type: Int, hotelsId: Array<FeatureId>) {
            mFilteredHotelIds.put(
                type,
                HashSet<FeatureId>(Arrays.asList(*hotelsId))
            )
        }

        fun contains(@BookingFilter.Type type: Int, id: FeatureId): Boolean {
            val ids = mFilteredHotelIds[type]
            return ids != null && ids.contains(id)
        }
    }

    private inner class SuggestViewHolder internal constructor(view: View) :
        BaseResultViewHolder(view) {
        override val titleView: TextView?
            get() = itemView as TextView

        override fun processClick(
            result: SearchResult?,
            order: Int
        ) {
            mSearchFragment.query = result!!.suggestion
        }
    }

    private inner open class ResultViewHolder internal constructor(val mFrame: View) :
        BaseResultViewHolder(mFrame) {
        val mName: TextView
        val mClosedMarker: View
        val mPopularity: View
        val mDescription: TextView
        val mRegion: TextView
        val mDistance: TextView
        val mPriceCategory: TextView
        val mSale: View
        override val tintAttr: Int
            get() = 0

        // FIXME: Better format based on result type
        private fun formatDescription(
            result: SearchResult?,
            isHotelAvailable: Boolean
        ): CharSequence {
            val localizedType = Utils.getLocalizedFeatureType(
                mFrame.context,
                result!!.description!!.featureType
            )
            val res = SpannableStringBuilder(localizedType)
            val tail = SpannableStringBuilder()
            val stars = result.description!!.stars
            if (stars > 0 || result.description!!.rating != Constants.Rating.RATING_INCORRECT_VALUE || isHotelAvailable) {
                if (stars > 0) {
                    tail.append(" • ")
                    tail.append(HotelUtils.formatStars(stars, itemView.resources))
                }
                if (result.description!!.rating != Constants.Rating.RATING_INCORRECT_VALUE) {
                    val rs = itemView.resources
                    val s = rs.getString(
                        R.string.place_page_booking_rating,
                        nativeFormatRating(result.description!!.rating)
                    )
                    tail
                        .append(" • ")
                        .append(colorizeString(s, rs.getColor(R.color.base_green)))
                }
                if (isHotelAvailable) {
                    val rs = itemView.resources
                    val s =
                        itemView.resources.getString(R.string.hotel_available)
                    if (tail.length > 0) tail.append(" • ")
                    tail.append(colorizeString(s, rs.getColor(R.color.base_green)))
                }
            } else if (!TextUtils.isEmpty(result.description!!.airportIata)) {
                tail.append(" • " + result.description!!.airportIata)
            } else {
                if (!TextUtils.isEmpty(result.description!!.brand)) {
                    tail.append(
                        " • " + Utils.getLocalizedBrand(
                            mFrame.context,
                            result.description!!.brand
                        )
                    )
                }
                if (!TextUtils.isEmpty(result.description!!.cuisine)) {
                    tail.append(" • " + result.description!!.cuisine)
                }
            }
            res.append(tail)
            return res
        }

        private fun colorizeString(str: String, @ColorInt color: Int): CharSequence {
            val sb = SpannableStringBuilder(str)
            sb.setSpan(
                ForegroundColorSpan(color),
                0, sb.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
            return sb
        }

        override val titleView: TextView?
            get() = mName

        override fun processClick(result: SearchResult?, order: Int) {
            mSearchFragment.showSingleResultOnMap(result!!, order)
        }

        override fun bind(
            result: SearchResult,
            order: Int
        ) {
            super.bind(result, order)
            setBackground()
            // TODO: Support also "Open Now" mark.
            UiUtils.showIf(isClosedVisible, mClosedMarker)
            val isHotelAvailable = mResult!!.isHotel &&
                    mFilteredHotelIds.contains(
                        BookingFilter.TYPE_AVAILABILITY,
                        mResult!!.description!!.featureId
                    )
            UiUtils.showIf(isPopularVisible, mPopularity)
            UiUtils.setTextAndHideIfEmpty(
                mDescription,
                formatDescription(mResult, isHotelAvailable)
            )
            UiUtils.setTextAndHideIfEmpty(mRegion, mResult!!.description!!.region)
            UiUtils.setTextAndHideIfEmpty(mDistance, mResult!!.description!!.distance)
            UiUtils.setTextAndHideIfEmpty(mPriceCategory, mResult!!.description!!.pricing)
            val hasDeal = mResult!!.isHotel &&
                    mFilteredHotelIds.contains(
                        BookingFilter.TYPE_DEALS,
                        mResult!!.description!!.featureId
                    )
            UiUtils.showIf(hasDeal, mSale)
        }

        private val isClosedVisible: Boolean
            private get() {
                val isClosed =
                    mResult!!.description!!.openNow == SearchResult.Companion.OPEN_NOW_NO
                if (!isClosed) return false
                val isNotPopular =
                    mResult!!.getPopularity().type === Popularity.Type.NOT_POPULAR
                return isNotPopular || !mResult!!.description!!.hasPopularityHigherPriority
            }

        private val isPopularVisible: Boolean
            private get() {
                val isNotPopular =
                    mResult!!.getPopularity().type === Popularity.Type.NOT_POPULAR
                if (isNotPopular) return false
                val isClosed =
                    mResult!!.description!!.openNow == SearchResult.Companion.OPEN_NOW_NO
                return !isClosed || mResult!!.description!!.hasPopularityHigherPriority
            }

        private fun setBackground() {
            val context: Context? = mSearchFragment.activity
            @AttrRes val itemBg =
                ThemeUtils.getResource(context!!, R.attr.clickableBackground)
            val bottomPad = mFrame.paddingBottom
            val topPad = mFrame.paddingTop
            val rightPad = mFrame.paddingRight
            val leftPad = mFrame.paddingLeft
            mFrame.setBackgroundResource(if (needSpecificBackground()) specificBackground else itemBg)
            // On old Android (4.1) after setting the view background the previous paddings
// are discarded for unknown reasons, that's why restoring the previous paddings is needed.
            mFrame.setPadding(leftPad, topPad, rightPad, bottomPad)
        }

        @get:DrawableRes
        open val specificBackground: Int
            get() = R.color.bg_search_available_hotel

        open fun needSpecificBackground(): Boolean {
            return mResult!!.isHotel &&
                    mFilteredHotelIds.contains(
                        BookingFilter.TYPE_AVAILABILITY,
                        mResult!!.description!!.featureId
                    )
        }

        init {
            mName = mFrame.findViewById(R.id.title)
            mClosedMarker = mFrame.findViewById(R.id.closed)
            mPopularity = mFrame.findViewById(R.id.popular_rating_view)
            mDescription = mFrame.findViewById(R.id.description)
            mRegion = mFrame.findViewById(R.id.region)
            mDistance = mFrame.findViewById(R.id.distance)
            mPriceCategory = mFrame.findViewById(R.id.price_category)
            mSale = mFrame.findViewById(R.id.sale)
            mClosedMarker.setBackgroundDrawable(mClosedMarkerBackground)
        }
    }

    private inner class LocalAdsCustomerViewHolder internal constructor(view: View) :
        ResultViewHolder(view) {
        override fun needSpecificBackground(): Boolean {
            return true
        }

        @get:DrawableRes
        override val specificBackground: Int
            get() = if (ThemeUtils.isNightTheme) R.drawable.search_la_customer_result_night else R.drawable.search_la_customer_result
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchDataViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            SearchResult.Companion.TYPE_SUGGEST -> SuggestViewHolder(
                inflater.inflate(R.layout.item_search_suggest, parent, false)
            )
            SearchResult.Companion.TYPE_RESULT -> ResultViewHolder(
                inflater.inflate(R.layout.item_search_result, parent, false)
            )
            SearchResult.Companion.TYPE_LOCAL_ADS_CUSTOMER -> LocalAdsCustomerViewHolder(
                inflater.inflate(R.layout.item_search_result, parent, false)
            )
            else -> throw IllegalArgumentException("Unhandled view type given")
        }
    }

    override fun onBindViewHolder(holder: SearchDataViewHolder, position: Int) {
        holder.bind(mResults!![position], position)
    }

    override fun getItemViewType(position: Int): Int {
        return mResults!![position].type
    }

    fun showPopulateButton(): Boolean {
        return !get().isWaitingPoiPick && mResults != null && mResults!!.size > 0 && mResults!![0].type != SearchResult.Companion.TYPE_SUGGEST
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        var res = 0
        if (mResults == null) return res
        res += mResults!!.size
        return res
    }

    fun clear() {
        refreshData(null)
    }

    fun refreshData(results: Array<SearchResult>?) {
        mResults = results
        notifyDataSetChanged()
    }

    fun setFilteredHotels(@BookingFilter.Type type: Int, hotelsId: Array<FeatureId>) {
        mFilteredHotelIds.put(type, hotelsId)
        notifyDataSetChanged()
    }

    init {
        mClosedMarkerBackground = mSearchFragment.resources
            .getDrawable(if (ThemeUtils.isNightTheme) R.drawable.search_closed_marker_night else R.drawable.search_closed_marker)
    }
}