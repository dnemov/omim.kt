package com.mapswithme.maps.background

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager

class ConnectivityChangedReceiver : AbstractLogBroadcastReceiver() {
    override fun onReceiveInternal(
        context: Context,
        intent: Intent
    ) {
        NotificationService.startOnConnectivityChanged(context)
    }

    override val assertAction: String
        get() = ConnectivityManager.CONNECTIVITY_ACTION
}