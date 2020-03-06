package com.mapswithme.maps.routing

import androidx.annotation.IntDef
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Represents TransitStepInfo from core.
 */
class TransitStepInfo internal constructor(
    @TransitType type: Int, distance: String?, distanceUnits: String?,
    timeInSec: Int, number: String?, color: Int, intermediateIndex: Int
) {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        TRANSIT_TYPE_INTERMEDIATE_POINT,
        TRANSIT_TYPE_PEDESTRIAN,
        TRANSIT_TYPE_SUBWAY,
        TRANSIT_TYPE_TRAIN,
        TRANSIT_TYPE_LIGHT_RAIL,
        TRANSIT_TYPE_MONORAIL
    )
    internal annotation class TransitType

    val type: TransitStepType
    val distance: String?
    val distanceUnits: String?
    val timeInSec: Int
    val number: String?
    val color: Int
    val intermediateIndex: Int

    companion object {
        private const val TRANSIT_TYPE_INTERMEDIATE_POINT = 0
        private const val TRANSIT_TYPE_PEDESTRIAN = 1
        private const val TRANSIT_TYPE_SUBWAY = 2
        private const val TRANSIT_TYPE_TRAIN = 3
        private const val TRANSIT_TYPE_LIGHT_RAIL = 4
        private const val TRANSIT_TYPE_MONORAIL = 5
    }

    init {
        this.type = TransitStepType.values()[type]
        this.distance = distance
        this.distanceUnits = distanceUnits
        this.timeInSec = timeInSec
        this.number = number
        this.color = color
        this.intermediateIndex = intermediateIndex
    }
}