package com.mapswithme.maps.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import com.mapswithme.maps.MwmApplication
import com.mapswithme.util.CrashlyticsUtils
import com.mapswithme.util.log.LoggerFactory

abstract class AbstractLogBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (!TextUtils.equals(assertAction, action)) {
            LOGGER.w(
                tag,
                "An intent with wrong action detected: $action"
            )
            return
        }
        val msg = ("onReceive: " + intent + " app in background = "
                + !(MwmApplication.backgroundTracker()?.isForeground ?: false))
        LOGGER.i(tag, msg)
        CrashlyticsUtils.log(Log.INFO, tag, msg)
        onReceiveInternal(context, intent)
    }

    protected val tag: String
        get() = javaClass.simpleName

    protected abstract val assertAction: String
    abstract fun onReceiveInternal(context: Context, intent: Intent)

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
    }
}