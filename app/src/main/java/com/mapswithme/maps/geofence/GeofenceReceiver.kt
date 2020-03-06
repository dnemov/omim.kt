package com.mapswithme.maps.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        GeofenceTransitionsIntentService.Companion.enqueueWork(context, intent)
    }
}