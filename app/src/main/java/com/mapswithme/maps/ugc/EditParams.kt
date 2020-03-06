package com.mapswithme.maps.ugc

import com.mapswithme.maps.bookmarks.data.FeatureId
import com.mapswithme.maps.bookmarks.data.MapObject
import java.util.*

class EditParams private constructor(builder: Builder) {
    val title: String
    val featureId: FeatureId
    val ratings: ArrayList<UGC.Rating>?
    @UGC.Impress
    val defaultRating: Int
    private val mCanBeReviewed: Boolean
    val isFromPP: Boolean
    val isFromNotification: Boolean
    // TODO: mLat, mLon, mAddress are added just for debugging null feature id for ugc object.
// Remove they after problem is fixed.
    val lat: Double
    val lon: Double
    val address: String?

    fun canBeReviewed(): Boolean {
        return mCanBeReviewed
    }

    class Builder(val mTitle: String, val mFeatureId: FeatureId) {
        var mRatings: ArrayList<UGC.Rating>? = null
        @UGC.Impress
        var mDefaultRating = 0
        var mCanBeReviewed = false
        var mFromPP = false
        var mFromNotification = false
        var mLat = 0.0
        var mLon = 0.0
        var mAddress: String? = null
        fun setRatings(ratings: ArrayList<UGC.Rating>?): Builder {
            mRatings = ratings
            return this
        }

        fun setDefaultRating(@UGC.Impress defaultRating: Int): Builder {
            mDefaultRating = defaultRating
            return this
        }

        fun setCanBeReviewed(value: Boolean): Builder {
            mCanBeReviewed = value
            return this
        }

        fun setFromPP(value: Boolean): Builder {
            mFromPP = value
            return this
        }

        fun setFromNotification(value: Boolean): Builder {
            mFromNotification = value
            return this
        }

        fun setLat(lat: Double): Builder {
            mLat = lat
            return this
        }

        fun setLon(lon: Double): Builder {
            mLon = lon
            return this
        }

        fun setAddress(address: String?): Builder {
            mAddress = address
            return this
        }

        fun build(): EditParams {
            return EditParams(this)
        }

        companion object {
            fun fromMapObject(mapObject: MapObject): Builder {
                return Builder(
                    mapObject.title,
                    mapObject.featureId
                )
                    .setRatings(mapObject.defaultRatings)
                    .setCanBeReviewed(mapObject.canBeReviewed())
                    .setLat(mapObject.lat)
                    .setLon(mapObject.lon)
                    .setAddress(mapObject.address)
            }
        }

    }

    init {
        title = builder.mTitle
        featureId = builder.mFeatureId
        ratings = builder.mRatings
        defaultRating = builder.mDefaultRating
        mCanBeReviewed = builder.mCanBeReviewed
        isFromPP = builder.mFromPP
        isFromNotification = builder.mFromNotification
        lat = builder.mLat
        lon = builder.mLon
        address = builder.mAddress
    }
}