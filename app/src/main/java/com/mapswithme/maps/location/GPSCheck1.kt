package com.mapswithme.maps.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mapswithme.maps.MwmApplication
import com.mapswithme.util.log.LoggerFactory

class GPSCheck : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val msg = ("onReceive: " + intent + " app in background = "
                + !MwmApplication.backgroundTracker()!!.isForeground)
        LOGGER.i(TAG, msg)
        if (MwmApplication.get().arePlatformAndCoreInitialized() && MwmApplication.backgroundTracker()!!.isForeground) {
            LocationHelper.INSTANCE.restart()
        }
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.LOCATION)
        private val TAG = GPSCheck::class.java.simpleName
    }
}