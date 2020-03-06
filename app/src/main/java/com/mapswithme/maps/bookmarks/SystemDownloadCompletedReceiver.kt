package com.mapswithme.maps.bookmarks

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import com.mapswithme.maps.background.AbstractLogBroadcastReceiver
import com.mapswithme.maps.scheduling.JobIdMap

class SystemDownloadCompletedReceiver : AbstractLogBroadcastReceiver() {
    override val assertAction: String
        protected get() = DownloadManager.ACTION_DOWNLOAD_COMPLETE

    override fun onReceiveInternal(
        context: Context,
        intent: Intent
    ) {
        val manager =
            context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                ?: return
        intent.setClass(context, SystemDownloadCompletedService::class.java)
        val jobId = JobIdMap.getId(SystemDownloadCompletedService::class.java)
        JobIntentService.enqueueWork(
            context,
            SystemDownloadCompletedService::class.java,
            jobId,
            intent
        )
    }
}