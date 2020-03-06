package com.mapswithme.maps.bookmarks.data

import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import com.mapswithme.maps.LightFramework
import kotlinx.android.parcel.Parcelize

@Parcelize
class FeatureId(
    val mMwmName: String,
    val mMwmVersion: Long,
    val mFeatureIndex: Int
) : Parcelable {

    fun toFeatureIdString(): String {
        return LightFramework.nativeMakeFeatureId(mMwmName, mMwmVersion, mFeatureIndex)
    }

    companion object {
        @kotlin.jvm.JvmField
        var EMPTY: FeatureId = FeatureId("", 0L, 0)

        @JvmStatic
        public fun fromFeatureIdString(id: String): FeatureId {
            if (TextUtils.isEmpty(id)) throw AssertionError("Feature id string is empty")
            val parts = id.split(":").toTypedArray()
            if (parts.size != 3) throw AssertionError("Wrong feature id string format")
            return FeatureId(parts[1], parts[0].toLong(), parts[2].toInt())
        }
    }
}