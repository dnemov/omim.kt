package com.mapswithme.maps.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mapswithme.maps.MwmApplication
import com.mapswithme.util.CrashlyticsUtils
import com.mapswithme.util.log.LoggerFactory

class UpgradeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val msg = ("onReceive: " + intent + " app in background = "
                + !(MwmApplication.backgroundTracker()?.isForeground ?: false))
        LOGGER.i(TAG, msg)
        CrashlyticsUtils.log(Log.INFO, TAG, msg)
        MwmApplication.onUpgrade()
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = UpgradeReceiver::class.java.simpleName
    }
}