package com.mapswithme.maps.search

import com.mapswithme.maps.bookmarks.data.FeatureId

/**
 * Native booking filter returns available hotels via this interface.
 */
interface NativeBookingFilterListener {
    /**
     * @param type Filter type which was applied.
     * @param hotels Array of hotels that meet the requirements for the filter.
     */
    fun onFilterHotels(@BookingFilter.Type type: Int, hotels: Array<FeatureId>?)
}