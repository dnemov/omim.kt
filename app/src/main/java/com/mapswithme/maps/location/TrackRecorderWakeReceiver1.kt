package com.mapswithme.maps.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mapswithme.maps.MwmApplication
import com.mapswithme.util.CrashlyticsUtils
import com.mapswithme.util.log.LoggerFactory

class TrackRecorderWakeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val msg = ("onReceive: " + intent + " app in background = "
                + (MwmApplication.backgroundTracker()?.isForeground == false))
        LOGGER.i(TAG, msg)
        CrashlyticsUtils.log(Log.INFO, TAG, msg)
        TrackRecorder.onWakeAlarm(context)
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = TrackRecorderWakeReceiver::class.java.simpleName
    }
}