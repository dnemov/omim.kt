package com.mapswithme.maps.search

import android.os.Parcel
import android.os.Parcelable
import com.mapswithme.util.ConnectionState
import kotlinx.android.parcel.Parcelize

@Parcelize
class BookingFilterParams (val mCheckinMillisec: Long,
                           val mCheckoutMillisec: Long,
                           vararg val mRooms: Room) : Parcelable {
    @Parcelize
    class Room(val mAdultsCount: Int, val mAgeOfChild: Int) : Parcelable {
        constructor(adultsCount: Int) : this(adultsCount, NO_CHILDREN)

        companion object {
            // This value is corresponds to AvailabilityParams::Room::kNoChildren in core.
            const val NO_CHILDREN = -1
            @JvmField
            val DEFAULT = Room(2)
        }
    }

    class Factory {
        fun createParams(
            checkIn: Long,
            checkOut: Long,
            vararg rooms: Room
        ): BookingFilterParams? {
            return if (ConnectionState.isConnected) BookingFilterParams(
                checkIn,
                checkOut,
                *rooms
            ) else null
        }
    }
}