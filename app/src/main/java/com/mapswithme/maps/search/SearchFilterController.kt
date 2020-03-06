package com.mapswithme.maps.search

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.mapswithme.maps.R
import com.mapswithme.util.UiUtils

class SearchFilterController(
    private val mFrame: View,
    private val mFilterListener: FilterListener?, @StringRes populateButtonText: Int
) {
    private val mShowOnMap: TextView
    private val mFilterButton: View
    private val mFilterIcon: ImageView
    private val mFilterText: TextView
    private val mDivider: View
    var filter: HotelsFilter? = null
        private set
    var bookingFilterParams: BookingFilterParams? = null
        private set
    private var mHotelMode = false
    private val mClearListener =
        View.OnClickListener {
            setFilterAndParams(null, null)
            mFilterListener?.onFilterClear()
        }

    interface FilterListener {
        fun onShowOnMapClick()
        fun onFilterClick()
        fun onFilterClear()
    }

    internal constructor(
        frame: View,
        listener: FilterListener?
    ) : this(frame, listener, R.string.search_show_on_map) {
    }

    fun show(show: Boolean, showPopulateButton: Boolean) {
        UiUtils.showIf(show && (showPopulateButton || mHotelMode), mFrame)
        showPopulateButton(showPopulateButton)
    }

    fun showPopulateButton(show: Boolean) {
        UiUtils.showIf(show, mShowOnMap)
    }

    fun showDivider(show: Boolean) {
        UiUtils.showIf(show, mDivider)
    }

    fun updateFilterButtonVisibility(isHotel: Boolean) {
        mHotelMode = isHotel
        UiUtils.showIf(isHotel, mFilterButton)
    }

    private fun initListeners() {
        mShowOnMap.setOnClickListener { v: View? -> mFilterListener?.onShowOnMapClick() }
        mFilterButton.setOnClickListener { v: View? -> mFilterListener?.onFilterClick() }
    }

    fun setFilterAndParams(filter: HotelsFilter?, params: BookingFilterParams?) {
        this.filter = filter
        bookingFilterParams = params
        if (this.filter != null || bookingFilterParams != null) {
            mFilterIcon.setOnClickListener(mClearListener)
            mFilterIcon.setImageResource(R.drawable.ic_cancel)
            mFilterIcon.setColorFilter(
                ContextCompat.getColor(
                    mFrame.context,
                    UiUtils.getStyledResourceId(mFrame.context, R.attr.accentButtonTextColor)
                )
            )
            UiUtils.setBackgroundDrawable(mFilterButton, R.attr.accentButtonRoundBackground)
            mFilterText.setTextColor(
                ContextCompat.getColor(
                    mFrame.context,
                    UiUtils.getStyledResourceId(mFrame.context, R.attr.accentButtonTextColor)
                )
            )
        } else {
            mFilterIcon.setOnClickListener(null)
            mFilterIcon.setImageResource(R.drawable.ic_filter_list)
            mFilterIcon.setColorFilter(
                ContextCompat.getColor(
                    mFrame.context,
                    UiUtils.getStyledResourceId(mFrame.context, R.attr.colorAccent)
                )
            )
            UiUtils.setBackgroundDrawable(mFilterButton, R.attr.clickableBackground)
            mFilterText.setTextColor(
                ContextCompat.getColor(
                    mFrame.context,
                    UiUtils.getStyledResourceId(mFrame.context, R.attr.colorAccent)
                )
            )
        }
    }

    fun resetFilter() {
        setFilterAndParams(null, null)
        updateFilterButtonVisibility(false)
    }

    fun onSaveState(outState: Bundle) {
        outState.putParcelable(STATE_HOTEL_FILTER, filter)
        outState.putParcelable(
            STATE_FILTER_PARAMS,
            bookingFilterParams
        )
        outState.putBoolean(
            STATE_HOTEL_FILTER_VISIBILITY,
            mFilterButton.visibility == View.VISIBLE
        )
    }

    fun onRestoreState(state: Bundle) {
        setFilterAndParams(
            state.getParcelable(STATE_HOTEL_FILTER),
            state.getParcelable(STATE_FILTER_PARAMS)
        )
        updateFilterButtonVisibility(
            state.getBoolean(
                STATE_HOTEL_FILTER_VISIBILITY,
                false
            )
        )
    }

    open class DefaultFilterListener :
        FilterListener {
        override fun onShowOnMapClick() {}
        override fun onFilterClick() {}
        override fun onFilterClear() {}
    }

    companion object {
        private const val STATE_HOTEL_FILTER = "state_hotel_filter"
        private const val STATE_FILTER_PARAMS = "state_filter_params"
        private const val STATE_HOTEL_FILTER_VISIBILITY = "state_hotel_filter_visibility"
    }

    init {
        mShowOnMap = mFrame.findViewById(R.id.show_on_map)
        mShowOnMap.setText(populateButtonText)
        mFilterButton = mFrame.findViewById(R.id.filter_button)
        mFilterIcon = mFilterButton.findViewById(R.id.filter_icon)
        mFilterText = mFilterButton.findViewById(R.id.filter_text)
        mDivider = mFrame.findViewById(R.id.divider)
        initListeners()
    }
}