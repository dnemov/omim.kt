package com.mapswithme.maps.search

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class Popularity(private val popularity: Int) : Parcelable {
    val type: Type
        get() = Type.makeInstance(popularity)

    companion object {
        fun defaultInstance(): Popularity {
            return Popularity(Type.NOT_POPULAR.ordinal)
        }
    }

    enum class Type {
        NOT_POPULAR, POPULAR;

        companion object {
            fun makeInstance(index: Int): Type {
                if (index < 0) throw AssertionError("Incorrect negative index = $index")
                return if (index > 0) POPULAR else NOT_POPULAR
            }
        }
    }
}