package com.mapswithme.maps.scheduling

import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import com.mapswithme.util.log.LoggerFactory

class FirebaseJobService : JobService() {
    private lateinit var mDelegate: JobServiceDelegate
    override fun onCreate() {
        super.onCreate()
        mDelegate = JobServiceDelegate(application)
    }

    override fun onStartJob(job: JobParameters): Boolean {
        LOGGER.d(
            TAG,
            "onStartJob FirebaseJobService"
        )
        return mDelegate.onStartJob()
    }

    override fun onStopJob(job: JobParameters): Boolean {
        return mDelegate.onStopJob()
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = FirebaseJobService::class.java.simpleName
    }
}