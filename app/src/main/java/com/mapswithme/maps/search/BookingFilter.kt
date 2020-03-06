package com.mapswithme.maps.search

import androidx.annotation.IntDef
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

internal object BookingFilter {
    // This list should correspond to the booking::filter::Type enum on c++ side.
    const val TYPE_DEALS = 0
    const val TYPE_AVAILABILITY = 1

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(TYPE_DEALS, TYPE_AVAILABILITY)
    annotation class Type
}