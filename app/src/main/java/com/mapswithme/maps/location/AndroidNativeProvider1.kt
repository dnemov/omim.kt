package com.mapswithme.maps.location

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import com.mapswithme.maps.MwmApplication
import java.util.*

internal class AndroidNativeProvider(locationFixChecker: LocationFixChecker) :
    BaseLocationProvider(locationFixChecker) {
    private val mLocationManager: LocationManager
    private val mListeners: MutableList<LocationListener> =
        ArrayList()

    override fun start() {
        BaseLocationProvider.Companion.LOGGER.d(
            TAG,
            "Android native provider is started"
        )
        if (isActive) return
        val providers =
            getAvailableProviders(mLocationManager)
        if (providers.isEmpty()) {
            isActive = false
            return
        }
        isActive = true
        for (provider in providers) {
            val listener: LocationListener =
                BaseLocationListener(locationFixChecker)
            val interval = LocationHelper.INSTANCE.interval
            BaseLocationProvider.Companion.LOGGER.d(
                TAG, "Request Android native provider '" + provider
                        + "' to get locations at this interval = " + interval + " ms"
            )
            mLocationManager.requestLocationUpdates(provider, interval, 0f, listener)
            mListeners.add(listener)
        }
        LocationHelper.INSTANCE.startSensors()
        var location =
            findBestLocation(mLocationManager, providers)
        if (location != null && !locationFixChecker.isLocationBetterThanLast(location)) location =
            LocationHelper.INSTANCE.savedLocation
        location?.let { onLocationChanged(it) }
    }

    private fun onLocationChanged(location: Location) {
        val iterator: ListIterator<LocationListener> =
            mListeners.listIterator()
        // All listeners have to be notified only through safe list iterator interface,
// otherwise ConcurrentModificationException will be obtained, because each listener can
// cause 'stop' method calling and modifying the collection during this iteration.
// noinspection WhileLoopReplaceableByForEach
        while (iterator.hasNext()) iterator.next().onLocationChanged(location)
    }

    override fun stop() {
        BaseLocationProvider.Companion.LOGGER.d(
            TAG,
            "Android native provider is stopped"
        )
        val iterator: ListIterator<LocationListener> =
            mListeners.listIterator()
        // noinspection WhileLoopReplaceableByForEach
        while (iterator.hasNext()) mLocationManager.removeUpdates(iterator.next())
        mListeners.clear()
        isActive = false
    }

    companion object {
        private val TAG = AndroidNativeProvider::class.java.simpleName
        private val TRUSTED_PROVIDERS = arrayOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER
        )

        fun findBestLocation(): Location? {
            val manager =
                MwmApplication.get().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return findBestLocation(
                manager,
                getAvailableProviders(manager)
            )
        }

        private fun findBestLocation(
            manager: LocationManager,
            providers: List<String>
        ): Location? {
            var res: Location? = null
            try {
                for (pr in providers) {
                    val last = manager.getLastKnownLocation(pr) ?: continue
                    if (res == null || res.accuracy > last.accuracy) res = last
                }
            } catch (e: SecurityException) {
                BaseLocationProvider.Companion.LOGGER.e(
                    TAG,
                    "Dynamic permission ACCESS_COARSE_LOCATION/ACCESS_FINE_LOCATION is not granted",
                    e
                )
            }
            return res
        }

        private fun getAvailableProviders(locationManager: LocationManager): List<String> {
            val res: MutableList<String> =
                ArrayList()
            for (provider in TRUSTED_PROVIDERS) {
                if (locationManager.isProviderEnabled(provider)) res.add(provider)
            }
            return res
        }
    }

    init {
        mLocationManager =
            MwmApplication.get().getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
}