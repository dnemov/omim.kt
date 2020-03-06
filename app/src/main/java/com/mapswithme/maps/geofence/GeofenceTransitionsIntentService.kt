package com.mapswithme.maps.geofence

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.app.JobIntentService
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.mapswithme.maps.LightFramework
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.location.LocationPermissionNotGrantedException
import com.mapswithme.maps.scheduling.JobIdMap
import com.mapswithme.util.log.LoggerFactory
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class GeofenceTransitionsIntentService : JobIntentService() {
    private val mMainThreadHandler = Handler(Looper.getMainLooper())
    override fun onHandleWork(intent: Intent) {
        LOG.d(
            TAG,
            "onHandleWork. Intent = $intent"
        )
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) onError(geofencingEvent) else onSuccess(geofencingEvent)
    }

    private fun onSuccess(geofencingEvent: GeofencingEvent) {
        val transitionType = geofencingEvent.geofenceTransition
        LOG.d(
            TAG,
            "transitionType = $transitionType"
        )
        if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) onGeofenceEnter(geofencingEvent) else if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) onGeofenceExit(
            geofencingEvent
        )
    }

    private fun onGeofenceExit(geofencingEvent: GeofencingEvent) {
        val geofenceLocation: GeofenceLocation =
            GeofenceLocation.Companion.from(geofencingEvent.triggeringLocation)
        mMainThreadHandler.post(GeofencingEventExitTask(application, geofenceLocation))
    }

    private fun onGeofenceEnter(geofencingEvent: GeofencingEvent) {
        makeLocationProbesBlockingSafely(geofencingEvent)
    }

    private fun makeLocationProbesBlockingSafely(geofencingEvent: GeofencingEvent) {
        try {
            makeLocationProbesBlocking(geofencingEvent)
        } catch (e: InterruptedException) {
            LOG.e(
                TAG,
                "Failed to make location probe for '$geofencingEvent'",
                e
            )
        }
    }

    @Throws(InterruptedException::class)
    private fun makeLocationProbesBlocking(event: GeofencingEvent) {
        val latch =
            CountDownLatch(LOCATION_PROBES_MAX_COUNT)
        for (i in 0 until LOCATION_PROBES_MAX_COUNT) {
            makeSingleLocationProbe(event, i)
        }
        latch.await(
            LOCATION_PROBES_MAX_COUNT.toLong(),
            TimeUnit.MINUTES
        )
    }

    private fun makeSingleLocationProbe(
        event: GeofencingEvent,
        timeoutInMinutes: Int
    ) {
        val geofenceLocation: GeofenceLocation =
            GeofenceLocation.Companion.from(event.triggeringLocation)
        val geofences =
            Collections.unmodifiableList(event.triggeringGeofences)
        val locationTask = CheckLocationTask(
            application,
            geofences,
            geofenceLocation
        )
        mMainThreadHandler.postDelayed(
            locationTask,
            TimeUnit.MINUTES.toMillis(timeoutInMinutes.toLong())
        )
    }

    private fun onError(geofencingEvent: GeofencingEvent) {
        val errorMessage = "Error code = " + geofencingEvent.errorCode
        LOG.e(
            TAG,
            errorMessage
        )
    }

    private class CheckLocationTask internal constructor(
        application: Application, private val mGeofences: List<Geofence>,
        triggeringLocation: GeofenceLocation
    ) : AbstractGeofenceTask(application, triggeringLocation) {
        override fun run() {
            requestLocationCheck()
        }

        private fun requestLocationCheck() {
            LOG.d(
                TAG,
                "Geofences = " + Arrays.toString(mGeofences.toTypedArray())
            )
            val geofenceLocation = geofenceLocation
            for (each in mGeofences) {
                val feature = Factory.from(each)
                LightFramework.logLocalAdsEvent(geofenceLocation, feature)
            }
        }

    }

    private inner class GeofencingEventExitTask internal constructor(
        application: Application,
        location: GeofenceLocation
    ) : AbstractGeofenceTask(application, location) {
        override fun run() {
            val location = geofenceLocation
            val geofenceRegistry: GeofenceRegistry =
                GeofenceRegistryImpl.Companion.from(application)
            LOG.d(
                TAG,
                "Exit event for location = $location"
            )
            try {
                geofenceRegistry.unregisterGeofences()
                geofenceRegistry.registerGeofences(location)
            } catch (e: LocationPermissionNotGrantedException) {
                LOG.e(
                    TAG,
                    "Location permission not granted!",
                    e
                )
            }
        }
    }

    abstract class AbstractGeofenceTask internal constructor(
        application: Application,
        location: GeofenceLocation
    ) : Runnable {
        protected val application: MwmApplication
        private val mGeofenceLocation: GeofenceLocation

        protected val geofenceLocation: GeofenceLocation
            protected get() {
                val lastKnownLocation =
                    LocationHelper.INSTANCE.lastKnownLocation
                return if (lastKnownLocation == null) mGeofenceLocation else GeofenceLocation.Companion.from(
                    lastKnownLocation
                )
            }

        init {
            this.application = application as MwmApplication
            mGeofenceLocation = location
        }
    }

    companion object {
        private val LOG =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG =
            GeofenceTransitionsIntentService::class.java.simpleName
        private const val LOCATION_PROBES_MAX_COUNT = 10
        fun enqueueWork(context: Context, intent: Intent) {
            val id = JobIdMap.getId(GeofenceTransitionsIntentService::class.java)
            enqueueWork(
                context,
                GeofenceTransitionsIntentService::class.java,
                id,
                intent
            )
            LOG.d(
                TAG,
                "Service was enqueued"
            )
        }
    }
}