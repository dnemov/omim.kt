package com.mapswithme.maps.ads

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class Banner(val id: String, private val mType: Int) : Parcelable {

    val provider: String
        get() = when (Type.values()[mType]) {
            Type.TYPE_FACEBOOK -> Providers.FACEBOOK
            Type.TYPE_RB -> Providers.MY_TARGET
            Type.TYPE_MOPUB -> Providers.MOPUB
            Type.TYPE_GOOGLE -> Providers.GOOGLE
            else -> throw AssertionError("Unsupported banner type: $mType")
        }

    enum class Type {
        TYPE_NONE, TYPE_FACEBOOK, TYPE_RB, TYPE_MOPUB, TYPE_GOOGLE
    }

    enum class Place {
        SEARCH, DEFAULT
    }

}