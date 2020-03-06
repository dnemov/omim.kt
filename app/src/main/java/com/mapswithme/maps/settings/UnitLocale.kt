package com.mapswithme.maps.settings

import java.util.*

object UnitLocale {
    // This constants should be equal with platform/settings.hpp
    const val UNITS_UNDEFINED = -1
    const val UNITS_METRIC = 0
    const val UNITS_FOOT = 1
    // USA, UK, Liberia, Burma
    private val defaultUnits: Int
        private get() {
            val code = Locale.getDefault().country
            // USA, UK, Liberia, Burma
            val arr = arrayOf("US", "GB", "LR", "MM")
            for (s in arr) if (s.equals(
                    code,
                    ignoreCase = true
                )
            ) return UNITS_FOOT
            return UNITS_METRIC
        }

    private var currentUnits: Int private external get private external set
    var units: Int
        get() = currentUnits
        set(units) {
            currentUnits = units
        }

    @kotlin.jvm.JvmStatic
    fun initializeCurrentUnits() {
        val u = currentUnits
        currentUnits = if (u == UNITS_UNDEFINED) defaultUnits else u
    }
}