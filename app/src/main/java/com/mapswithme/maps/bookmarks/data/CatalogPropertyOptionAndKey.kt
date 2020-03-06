package com.mapswithme.maps.bookmarks.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class CatalogPropertyOptionAndKey(
    val key: String,
    val option: CatalogCustomPropertyOption
): Parcelable