package com.mapswithme.maps.location

import com.mapswithme.util.log.LoggerFactory

internal abstract class BaseLocationProvider(val locationFixChecker: LocationFixChecker) {
    private var mActive = false

    abstract fun start()
    abstract fun stop()
    /**
     * Indicates whether this provider is providing location updates or not
     * @return true - if locations are actively coming from this provider, false - otherwise
     */
    var isActive: Boolean
        get() = mActive
        set(active) {
            LOGGER.d(
                TAG,
                "setActive active = $active"
            )
            mActive = active
        }

    companion object {
        val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.LOCATION)
        private val TAG = BaseLocationProvider::class.java.simpleName
    }

}