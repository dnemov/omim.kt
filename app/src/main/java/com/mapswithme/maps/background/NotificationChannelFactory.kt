package com.mapswithme.maps.background

import android.app.Application
import com.mapswithme.util.Utils

object NotificationChannelFactory {
    @JvmStatic
    fun createProvider(app: Application): NotificationChannelProvider {
        return if (Utils.isOreoOrLater) OreoCompatNotificationChannelProvider(
            app
        ) else StubNotificationChannelProvider(app)
    }
}