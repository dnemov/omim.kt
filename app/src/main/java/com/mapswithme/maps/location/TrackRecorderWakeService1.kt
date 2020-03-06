package com.mapswithme.maps.location

import android.app.job.JobScheduler
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.scheduling.JobIdMap
import com.mapswithme.util.CrashlyticsUtils
import com.mapswithme.util.Utils
import com.mapswithme.util.log.LoggerFactory
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TrackRecorderWakeService : JobIntentService() {
    private val mWaitMonitor =
        CountDownLatch(1)

    override fun onHandleWork(intent: Intent) {
        val msg = ("onHandleIntent: " + intent + " app in background = "
                + !MwmApplication.backgroundTracker()!!.isForeground)
        LOGGER.i(TAG, msg)
        CrashlyticsUtils.log(Log.INFO, TAG, msg)
        synchronized(
            sLock
        ) { sService = this }
        TrackRecorder.onServiceStarted()
        try {
            val timeout = TrackRecorder.awaitTimeout
            LOGGER.d(
                TAG,
                "Timeout: $timeout"
            )
            if (!mWaitMonitor.await(timeout, TimeUnit.MILLISECONDS)) {
                LOGGER.d(
                    TAG,
                    "TIMEOUT awaiting coordinates"
                )
                TrackRecorder.incrementAwaitTimeout()
            }
        } catch (ignored: InterruptedException) {
        }
        synchronized(
            sLock
        ) { sService = null }
        TrackRecorder.onServiceStopped()
    }

    companion object {
        private val TAG = TrackRecorderWakeService::class.java.simpleName
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.TRACK_RECORDER)
        private val sLock = Any()
        private var sService: TrackRecorderWakeService? = null
        fun start(context: Context) {
            val app = context.applicationContext
            val intent = Intent(app, TrackRecorderWakeService::class.java)
            val jobId = JobIdMap.getId(TrackRecorderWakeService::class.java)
            if (Utils.isLollipopOrLater) {
                val scheduler =
                    context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                Objects.requireNonNull(scheduler)
                val pendingJobs = scheduler.allPendingJobs
                val jobsRepresentation =
                    Arrays.toString(pendingJobs.toTypedArray())
                for (each in pendingJobs) {
                    if (TrackRecorderWakeService::class.java.name == each.service.className) {
                        scheduler.cancel(each.id)
                        val logMsg =
                            "Canceled job: $each. All jobs: $jobsRepresentation"
                        CrashlyticsUtils.log(
                            Log.INFO,
                            TAG,
                            logMsg
                        )
                    }
                }
            }
            enqueueWork(app, TrackRecorderWakeService::class.java, jobId, intent)
        }

        fun stop() {
            LOGGER.d(
                TAG,
                "SVC.stop()"
            )
            synchronized(
                sLock
            ) {
                if (sService != null) sService!!.mWaitMonitor.countDown() else LOGGER.d(
                    TAG,
                    "SVC.stop() SKIPPED because (sService == null)"
                )
            }
        }
    }
}