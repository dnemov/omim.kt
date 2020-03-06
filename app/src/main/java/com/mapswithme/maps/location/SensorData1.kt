package com.mapswithme.maps.location

internal class SensorData {
    var gravity: FloatArray? = null
    var geomagnetic: FloatArray? = null

    val isAbsent: Boolean
        get() = gravity == null || geomagnetic == null
}