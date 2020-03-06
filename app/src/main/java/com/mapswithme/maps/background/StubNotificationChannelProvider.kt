package com.mapswithme.maps.background

import android.app.Application

open class StubNotificationChannelProvider @JvmOverloads internal constructor(
    protected val application: Application,
    override val uGCChannel: String = DEFAULT_NOTIFICATION_CHANNEL,
    override val downloadingChannel: String = DEFAULT_NOTIFICATION_CHANNEL
) : NotificationChannelProvider {

    override fun setUGCChannel() { /*Do nothing */
    }

    override fun setDownloadingChannel() { /*Do nothing */
    }

    companion object {
        private const val DEFAULT_NOTIFICATION_CHANNEL = "default_notification_channel"
    }

}