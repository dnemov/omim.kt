package com.mapswithme.maps.bookmarks.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*
import kotlin.collections.ArrayList

@Parcelize
class CatalogCustomProperty(
    val key: String, val localizedName: String,
    val required: Boolean, val mOptions: Array<CatalogCustomPropertyOption>
): Parcelable {
    val options: List<CatalogCustomPropertyOption>
        get() = mOptions.toList()
}