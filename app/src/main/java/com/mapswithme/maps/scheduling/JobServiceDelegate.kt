package com.mapswithme.maps.scheduling

import android.app.Application
import com.mapswithme.maps.background.NotificationService.Companion.startOnConnectivityChanged
import com.mapswithme.util.ConnectionState

internal class JobServiceDelegate(private val mApp: Application) {
    fun onStartJob(): Boolean {
        val type =
            ConnectionState.requestCurrentType(mApp)
        if (type == ConnectionState.Type.WIFI) startOnConnectivityChanged(
            mApp
        )
        retryJob()
        return true
    }

    private fun retryJob() {
        ConnectivityJobScheduler.from(mApp).listen()
    }

    fun onStopJob(): Boolean {
        return false
    }

}