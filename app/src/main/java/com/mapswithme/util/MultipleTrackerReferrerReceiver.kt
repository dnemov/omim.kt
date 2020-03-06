package com.mapswithme.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mapswithme.maps.MwmApplication
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language
import com.mapswithme.util.log.Logger
import com.mapswithme.util.log.LoggerFactory
import com.mapswithme.util.statistics.AlohaHelper
import com.my.tracker.campaign.CampaignReceiver

/**
 * Custom broadcast receiver to send intent to MyTracker & Alohalytics at the same time
 */
class MultipleTrackerReferrerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val msg = ("onReceive: " + intent + " app in background = "
                + !(MwmApplication.backgroundTracker()?.isForeground ?: false))
        LOGGER.i(
            TAG,
            msg
        )
        CrashlyticsUtils.log(
            Log.INFO,
            TAG,
            msg
        )
        Counters.initCounters(context)
        // parse & send referrer to Aloha
        try {
            if (intent.hasExtra("referrer")) {
                val referrer = intent.getStringExtra("referrer")
                val referrerSplitted =
                    referrer.split("&".toRegex()).toTypedArray()
                if (referrerSplitted.isNotEmpty()) {
                    val parsedValues: MutableList<String> = mutableListOf()
                    var i = 0
                    for (referrerValue in referrerSplitted) {
                        val keyValue =
                            referrerValue.split("=".toRegex()).toTypedArray()
                        parsedValues[i++] = keyValue[0]
                        parsedValues[i++] = if (keyValue.size == 2) keyValue[1] else ""
                    }
                    org.alohalytics.Statistics.logEvent(
                        AlohaHelper.GPLAY_INSTALL_REFERRER,
                        parsedValues.toTypedArray()
                    )
                } else org.alohalytics.Statistics.logEvent(
                    AlohaHelper.GPLAY_INSTALL_REFERRER,
                    referrer
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        intent.component = null
        // now send intent to myTracker
        val receiver = CampaignReceiver()
        receiver.onReceive(context, intent)
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG =
            MultipleTrackerReferrerReceiver::class.java.simpleName
    }
}