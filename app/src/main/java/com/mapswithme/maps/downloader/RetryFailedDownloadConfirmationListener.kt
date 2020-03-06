package com.mapswithme.maps.downloader

import android.app.Application
import com.mapswithme.maps.background.Notifier
import com.mapswithme.maps.background.Notifier.Companion.from

open class RetryFailedDownloadConfirmationListener internal constructor(private val mApplication: Application) :
    Runnable {
    override fun run() {
        val notifier =
            from(mApplication)
        notifier.cancelNotification(Notifier.ID_DOWNLOAD_FAILED)
    }

}