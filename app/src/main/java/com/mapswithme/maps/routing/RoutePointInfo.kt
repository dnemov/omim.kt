package com.mapswithme.maps.routing

import android.os.Parcelable
import androidx.annotation.IntDef
import kotlinx.android.parcel.Parcelize
import kotlin.annotation.Retention

@Parcelize
class RoutePointInfo(@RouteMarkType val markType: Int, val intermediateIndex: Int) : Parcelable {
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        ROUTE_MARK_START,
        ROUTE_MARK_INTERMEDIATE,
        ROUTE_MARK_FINISH
    )
    annotation class RouteMarkType

    val isIntermediatePoint: Boolean
        get() = markType == ROUTE_MARK_INTERMEDIATE

    val isFinishPoint: Boolean
        get() = markType == ROUTE_MARK_FINISH

    val isStartPoint: Boolean
        get() = markType == ROUTE_MARK_START

    companion object {
        const val ROUTE_MARK_START = 0
        const val ROUTE_MARK_INTERMEDIATE = 1
        const val ROUTE_MARK_FINISH = 2
    }
}