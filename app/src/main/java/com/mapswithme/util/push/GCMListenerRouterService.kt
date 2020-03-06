package com.mapswithme.util.push

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import com.google.android.gms.gcm.GcmListenerService
import com.mapswithme.util.log.Logger
import com.mapswithme.util.log.LoggerFactory

// It's temporary class, it may be deleted along with Pushwoosh sdk.
// The base of this code is taken from https://www.pushwoosh.com/docs/gcm-integration-legacy.
class GCMListenerRouterService : GcmListenerService() {
    override fun onMessageReceived(from: String?, data: Bundle?) {
        LOGGER.i(
            TAG,
            "Gcm router service received message: "
                    + (data?.toString() ?: "<null>") + " from: " + from
        )
        if (data == null || TextUtils.isEmpty(from)) return
        // Base GCM listener service removes this extra before calling onMessageReceived.
// Need to set it again to pass intent to another service.
        data.putString("from", from)
        val pwProjectId =
            getPWProjectId(
                getApplicationContext()
            )
    }

    companion object {
        private val LOGGER: Logger =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.THIRD_PARTY)
        private val TAG =
            GCMListenerRouterService::class.java.simpleName

        fun getPWProjectId(context: Context): String? {
            return null
        }
    }
}