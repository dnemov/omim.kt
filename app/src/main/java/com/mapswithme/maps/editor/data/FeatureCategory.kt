package com.mapswithme.maps.editor.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class FeatureCategory(
    val type: String,
    val localizedTypeName: String
) : Parcelable