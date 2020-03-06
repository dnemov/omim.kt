package com.mapswithme.maps.routing

import com.mapswithme.maps.settings.RoadType
import java.util.*

object RoutingOptions {
    @JvmStatic
    fun addOption(roadType: RoadType) {
        nativeAddOption(roadType.ordinal)
    }

    @JvmStatic
    fun removeOption(roadType: RoadType) {
        nativeRemoveOption(roadType.ordinal)
    }

    @JvmStatic
    fun hasOption(roadType: RoadType): Boolean {
        return nativeHasOption(roadType.ordinal)
    }

    @JvmStatic private external fun nativeAddOption(option: Int)
    @JvmStatic private external fun nativeRemoveOption(option: Int)
    @JvmStatic private external fun nativeHasOption(option: Int): Boolean
    fun hasAnyOptions(): Boolean {
        for (each in RoadType.values()) {
            if (hasOption(each)) return true
        }
        return false
    }

    @JvmStatic
    val activeRoadTypes: Set<RoadType>
        get() {
            val roadTypes: MutableSet<RoadType> = HashSet()
            for (each in RoadType.values()) {
                if (hasOption(each)) roadTypes.add(each)
            }
            return roadTypes
        }
}