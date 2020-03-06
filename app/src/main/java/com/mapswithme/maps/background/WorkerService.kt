package com.mapswithme.maps.background

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import androidx.core.app.JobIntentService
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.editor.Editor
import com.mapswithme.maps.scheduling.JobIdMap
import com.mapswithme.maps.ugc.UGC.Companion.nativeUploadUGC
import com.mapswithme.util.CrashlyticsUtils
import com.mapswithme.util.log.LoggerFactory

class WorkerService : JobIntentService() {
    private val mArePlatformAndCoreInitialized =
        MwmApplication.get().arePlatformAndCoreInitialized()

    override fun onHandleWork(intent: Intent) {
        val msg = ("onHandleIntent: " + intent + " app in background = "
                + MwmApplication.backgroundTracker()?.isForeground)
        LOGGER.i(TAG, msg)
        CrashlyticsUtils.log(Log.INFO, TAG, msg)
        val action = intent.action
        if (TextUtils.isEmpty(action)) return
        if (!mArePlatformAndCoreInitialized) return
        when (action) {
            ACTION_UPLOAD_OSM_CHANGES -> handleActionUploadOsmChanges()
            ACTION_UPLOAD_UGC -> handleUploadUGC()
        }
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = WorkerService::class.java.simpleName
        private const val ACTION_UPLOAD_OSM_CHANGES =
            "com.mapswithme.maps.action.upload_osm_changes"
        private const val ACTION_UPLOAD_UGC = "com.mapswithme.maps.action.upload_ugc"
        /**
         * Starts this service to upload map edits to osm servers.
         */
        @JvmStatic
        fun startActionUploadOsmChanges(context: Context) {
            val intent = Intent(context, WorkerService::class.java)
            intent.action = ACTION_UPLOAD_OSM_CHANGES
            enqueueWork(
                context.applicationContext, WorkerService::class.java,
                JobIdMap.getId(WorkerService::class.java), intent
            )
        }

        /**
         * Starts this service to upload UGC to our servers.
         */
        fun startActionUploadUGC(context: Context) {
            val intent = Intent(context, WorkerService::class.java)
            intent.action = ACTION_UPLOAD_UGC
            val jobId = JobIdMap.getId(WorkerService::class.java)
            enqueueWork(context, WorkerService::class.java, jobId, intent)
        }

        private fun handleActionUploadOsmChanges() {
            Editor.uploadChanges()
        }

        private fun handleUploadUGC() {
            nativeUploadUGC()
        }
    }
}