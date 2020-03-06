package com.mapswithme.maps.background

import android.annotation.TargetApi
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.mapswithme.maps.R
import java.util.*

@TargetApi(Build.VERSION_CODES.O)
class OreoCompatNotificationChannelProvider internal constructor(app: Application) :
    StubNotificationChannelProvider(
        app,
        AUTH_NOTIFICATION_CHANNEL,
        DOWNLOADING_NOTIFICATION_CHANNEL
    ) {
    override fun setUGCChannel() {
        val name = application.getString(R.string.notification_channel_ugc)
        setChannelInternal(uGCChannel, name)
    }

    private fun setChannelInternal(id: String, name: String) {
        val notificationManager =
            application.getSystemService(
                NotificationManager::class.java
            )
        var channel =
            notificationManager?.getNotificationChannel(id)
        if (channel == null) channel = NotificationChannel(
            id,
            name,
            NotificationManager.IMPORTANCE_DEFAULT
        ) else channel.name = name
        notificationManager?.createNotificationChannel(channel)
    }

    override fun setDownloadingChannel() {
        val name = application.getString(R.string.notification_channel_downloader)
        setChannelInternal(downloadingChannel, name)
    }

    companion object {
        private const val AUTH_NOTIFICATION_CHANNEL = "auth_notification_channel"
        private const val DOWNLOADING_NOTIFICATION_CHANNEL =
            "downloading_notification_channel"
    }
}