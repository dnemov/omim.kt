package com.mapswithme.maps.geofence

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.mapswithme.maps.LightFramework
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.location.LocationPermissionNotGrantedException
import com.mapswithme.util.PermissionsUtils
import com.mapswithme.util.concurrency.UiThread
import com.mapswithme.util.log.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit

class GeofenceRegistryImpl(private val mApplication: Application) : GeofenceRegistry {
    private val mGeofencingClient: GeofencingClient
    @Throws(LocationPermissionNotGrantedException::class)
    override fun registerGeofences(location: GeofenceLocation) {
        checkThread()
        checkPermission()
        val features = LightFramework.getLocalAdsFeatures(
            location.lat,
            location.lon,
            location.radiusInMeters.toDouble(),
            GEOFENCE_MAX_COUNT
        )
        LOG.d(
            TAG,
            "GeoFenceFeatures = " + Arrays.toString(features.toTypedArray())
        )
        if (features.isEmpty()) return
        val geofences: MutableList<Geofence> = ArrayList()
        for (each in features) {
            val geofence = Geofence.Builder()
                .setRequestId(each.id.toFeatureIdString())
                .setCircularRegion(
                    each.latitude,
                    each.longitude,
                    PREFERRED_GEOFENCE_RADIUS
                )
                .setExpirationDuration(
                    TimeUnit.DAYS.toMillis(
                        GEOFENCE_TTL_IN_DAYS.toLong()
                    )
                )
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
            geofences.add(geofence)
        }
        val geofencingRequest = makeGeofencingRequest(geofences)
        val intent = makeGeofencePendingIntent()
        mGeofencingClient.addGeofences(geofencingRequest, intent)
            .addOnSuccessListener { params: Void? -> onAddSucceeded() }
            .addOnFailureListener { params: Exception? -> onAddFailed() }
    }

    @Throws(LocationPermissionNotGrantedException::class)
    override fun unregisterGeofences() {
        checkThread()
        checkPermission()
        mGeofencingClient.removeGeofences(makeGeofencePendingIntent())
            .addOnSuccessListener { params: Void? -> onRemoveFailed() }
            .addOnSuccessListener { params: Void? -> onRemoveSucceeded() }
    }

    private fun onAddSucceeded() {
        LOG.d(TAG, "onAddSucceeded")
    }

    private fun onAddFailed() {
        LOG.d(TAG, "onAddFailed")
    }

    private fun onRemoveSucceeded() {
        LOG.d(
            TAG,
            "onRemoveSucceeded"
        )
    }

    private fun onRemoveFailed() {
        LOG.d(TAG, "onRemoveFailed")
    }

    @Throws(LocationPermissionNotGrantedException::class)
    private fun checkPermission() {
        if (!PermissionsUtils.isLocationGranted(mApplication)) throw LocationPermissionNotGrantedException
    }

    private fun makeGeofencePendingIntent(): PendingIntent {
        val intent = Intent(mApplication, GeofenceReceiver::class.java)
        return PendingIntent.getBroadcast(
            mApplication,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun makeGeofencingRequest(geofences: List<Geofence>): GeofencingRequest {
        val builder = GeofencingRequest.Builder()
        return builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()
    }

    companion object {
        private const val GEOFENCE_MAX_COUNT = 100
        private const val GEOFENCE_TTL_IN_DAYS = 3
        private const val PREFERRED_GEOFENCE_RADIUS = 100.0f
        private val LOG =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = GeofenceRegistryImpl::class.java.simpleName
        private fun checkThread() {
            check(UiThread.isUiThread) { "Must be call from Ui thread" }
        }

        fun from(application: Application): GeofenceRegistry {
            val app = application as MwmApplication
            return app.geofenceRegistry
        }
    }

    init {
        mGeofencingClient = LocationServices.getGeofencingClient(mApplication)
    }
}