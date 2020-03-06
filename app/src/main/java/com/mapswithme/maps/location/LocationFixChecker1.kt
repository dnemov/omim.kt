package com.mapswithme.maps.location

import android.location.Location

internal interface LocationFixChecker {
    fun isLocationBetterThanLast(newLocation: Location): Boolean
    fun isAccuracySatisfied(location: Location): Boolean
}