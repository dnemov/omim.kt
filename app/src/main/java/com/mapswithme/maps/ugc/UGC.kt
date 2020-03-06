package com.mapswithme.maps.ugc

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.IntDef
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.background.AppBackgroundTracker.OnTransitionListener
import com.mapswithme.maps.background.WorkerService
import com.mapswithme.maps.bookmarks.data.FeatureId
import kotlinx.android.parcel.Parceler
import kotlinx.android.parcel.Parcelize
import kotlin.annotation.Retention



class UGC constructor(
    private val mRatings: Array<Rating>,
    private val mAverageRating: Float,
    private val mReviews: Array<Review>?,
    val basedOnCount: Int
) {
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @IntDef(
        RATING_NONE,
        RATING_HORRIBLE,
        RATING_BAD,
        RATING_NORMAL,
        RATING_GOOD,
        RATING_EXCELLENT,
        RATING_COMING_SOON
    )
    annotation class Impress

    val ratings: List<Rating>
        get() = mRatings.toList()

    val reviews: List<Review>?
        get() = mReviews?.toList()



    data class Rating internal constructor(var mName: String?, var mValue: Float) :
        Parcelable {

        companion object {
            @JvmField
            val CREATOR = object : Parcelable.Creator<Rating> {
                override fun createFromParcel(parcel: Parcel): Rating {
                    return Rating(parcel)
                }

                override fun newArray(size: Int) = arrayOfNulls<Rating?>(size)
            }
        }

        constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readFloat()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(mName)
            parcel.writeFloat(mValue)
        }

        override fun describeContents(): Int = 0
    }

    @Parcelize
    data class Review internal constructor(
        val text: String, val author: String, val time: Long,
        val rating: Float, @Impress val impress: Int
    ) : Parcelable

    interface ReceiveUGCListener {
        fun onUGCReceived(
            ugc: UGC?, ugcUpdate: UGCUpdate?, @Impress impress: Int,
            rating: String
        )
    }

    interface SaveUGCListener {
        fun onUGCSaved(result: Boolean)
    }

    private class UploadUgcTransitionListener internal constructor(private val mContext: Context) :
        OnTransitionListener {
        override fun onTransit(foreground: Boolean) {
            if (foreground) return
            WorkerService.startActionUploadUGC(mContext)
        }

    }

    companion object {
        const val RATING_NONE = 0
        const val RATING_HORRIBLE = 1
        const val RATING_BAD = 2
        const val RATING_NORMAL = 3
        const val RATING_GOOD = 4
        const val RATING_EXCELLENT = 5
        const val RATING_COMING_SOON = 6
        private var mReceiveListener: ReceiveUGCListener? = null
        private var mSaveListener: SaveUGCListener? = null
        fun init(context: Context) {
            val listener: OnTransitionListener = UploadUgcTransitionListener(context)
            MwmApplication.backgroundTracker(context).addListener(listener)
        }

        @JvmStatic
        fun setReceiveListener(listener: ReceiveUGCListener?) {
            mReceiveListener = listener
        }

        @JvmStatic
        fun setSaveListener(listener: SaveUGCListener?) {
            mSaveListener = listener
        }

        @JvmStatic
        fun setUGCUpdate(fid: FeatureId, update: UGCUpdate?) {
            nativeSetUGCUpdate(fid, update)
        }

        @JvmStatic external fun nativeRequestUGC(fid: FeatureId)

        @JvmStatic external fun nativeSetUGCUpdate(fid: FeatureId, update: UGCUpdate?)
        @JvmStatic external fun nativeUploadUGC()
        @JvmStatic external fun nativeToImpress(rating: Float): Int
        @JvmStatic external fun nativeFormatRating(rating: Float): String
        // Called from JNI.

        @JvmStatic
        fun onUGCReceived(
            ugc: UGC?, ugcUpdate: UGCUpdate?,
            @Impress impress: Int, rating: String
        ) {
            var impress = impress
            if (mReceiveListener == null) return
            if (ugc == null && ugcUpdate != null) impress = RATING_COMING_SOON
            mReceiveListener!!.onUGCReceived(ugc, ugcUpdate, impress, rating)
        }

        // Called from JNI.
        @JvmStatic
        fun onUGCSaved(result: Boolean) {
            if (mSaveListener == null) return
            mSaveListener!!.onUGCSaved(result)
        }
    }

}