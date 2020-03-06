package com.mapswithme.maps.bookmarks

import android.os.Parcelable
import com.mapswithme.maps.bookmarks.data.Error
import com.mapswithme.maps.bookmarks.data.Result
import kotlinx.android.parcel.Parcelize

@Parcelize
class OperationStatus(val result: Result? = null, val error: Error? = null) : Parcelable {
    val isOk: Boolean
        get() = error == null
}