package com.mapswithme.maps.scheduling

import android.annotation.TargetApi
import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import com.mapswithme.util.log.LoggerFactory

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class NativeJobService : JobService() {
    private lateinit var mDelegate: JobServiceDelegate
    override fun onCreate() {
        super.onCreate()
        mDelegate = JobServiceDelegate(application)
    }

    override fun onStartJob(params: JobParameters): Boolean {
        val logger =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        logger.d(TAG, "onStartJob")
        return mDelegate.onStartJob()
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return mDelegate.onStopJob()
    }

    companion object {
        private val TAG = NativeJobService::class.java.simpleName
    }
}