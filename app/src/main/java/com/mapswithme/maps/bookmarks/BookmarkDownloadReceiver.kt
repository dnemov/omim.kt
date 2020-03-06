package com.mapswithme.maps.bookmarks

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.TextUtils
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.mapswithme.maps.background.AbstractLogBroadcastReceiver
import com.mapswithme.maps.base.Detachable
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.util.log.LoggerFactory

class BookmarkDownloadReceiver : AbstractLogBroadcastReceiver(),
    Detachable<BookmarkDownloadHandler> {
    private var mHandler: BookmarkDownloadHandler? = null
    override fun attach(handler: BookmarkDownloadHandler) {
        mHandler = handler
    }

    override fun detach() {
        mHandler = null
    }

    fun register(application: Application) {
        val filter =
            IntentFilter(SystemDownloadCompletedService.ACTION_DOWNLOAD_COMPLETED)
        LocalBroadcastManager.getInstance(application)
            .registerReceiver(this, filter)
    }

    fun unregister(application: Application) {
        LocalBroadcastManager.getInstance(application)
            .unregisterReceiver(this)
    }

    override val assertAction: String
        protected get() = SystemDownloadCompletedService.ACTION_DOWNLOAD_COMPLETED

    override fun onReceiveInternal(
        context: Context,
        intent: Intent
    ) {
        val logger =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.BILLING)
        val tag = BookmarkDownloadReceiver::class.java.simpleName
        val status: OperationStatus =
            intent.getParcelableExtra(SystemDownloadCompletedService.EXTRA_DOWNLOAD_STATUS)
        val result = status.result
        if (status.isOk && result != null && !TextUtils.isEmpty(result.archiveId)
            && !TextUtils.isEmpty(result.filePath)
        ) {
            logger.i(tag, "Start to import downloaded bookmark")
            BookmarkManager.INSTANCE.importFromCatalog(result.archiveId!!, result.filePath!!)
            return
        }
        logger.i(tag, "Handle download result by handler '$mHandler'")
        val error = status.error
        if (error == null || mHandler == null) return
        if (error.isForbidden) mHandler!!.onAuthorizationRequired() else if (error.isPaymentRequired) mHandler!!.onPaymentRequired()
    }
}