package com.mapswithme.maps.location

import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.background.AppBackgroundTracker.OnTransitionListener

internal class TransitionListener : OnTransitionListener {
    private val mReceiver = GPSCheck()
    private var mReceiverRegistered = false
    override fun onTransit(foreground: Boolean) {
        if (foreground && !mReceiverRegistered) {
            val filter = IntentFilter()
            filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            filter.addCategory(Intent.CATEGORY_DEFAULT)
            MwmApplication.get().registerReceiver(mReceiver, filter)
            mReceiverRegistered = true
            return
        }
        if (!foreground && mReceiverRegistered) {
            MwmApplication.get().unregisterReceiver(mReceiver)
            mReceiverRegistered = false
        }
    }
}