package com.mapswithme.maps.scheduling

import android.annotation.TargetApi
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import com.crashlytics.android.Crashlytics
import com.firebase.jobdispatcher.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.mapswithme.maps.MwmApplication
import com.mapswithme.util.Utils
import java.util.*
import java.util.concurrent.TimeUnit

class ConnectivityJobScheduler(context: MwmApplication) :
    ConnectivityListener {
    private val mMasterConnectivityListener: ConnectivityListener
    private fun createCompatJobScheduler(context: MwmApplication): ConnectivityListener {
        val status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        val isAvailable = status == ConnectionResult.SUCCESS
        return if (isAvailable) ConnectivityListenerCompat(context) else ConnectivityListenerStub()
    }

    private fun createNativeJobScheduler(context: MwmApplication): ConnectivityListener {
        return NativeConnectivityListener(context)
    }

    override fun listen() {
        mMasterConnectivityListener.listen()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private class NativeConnectivityListener internal constructor(private val mContext: Context) :
        ConnectivityListener {
        private val mJobScheduler = mContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

        override fun listen() {
            val component = ComponentName(mContext, NativeJobService::class.java)
            val jobId = JobIdMap.getId(NativeJobService::class.java)
            val jobInfo = JobInfo.Builder(jobId, component)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setMinimumLatency(
                    TimeUnit.HOURS.toMillis(
                        SCHEDULE_PERIOD_IN_HOURS.toLong()
                    )
                )
                .build()
            mJobScheduler.schedule(jobInfo)
        }

    }

    private class ConnectivityListenerCompat internal constructor(context: MwmApplication) :
        ConnectivityListener {
        private val mJobDispatcher: FirebaseJobDispatcher
        override fun listen() {
            val tag = JobIdMap.getId(FirebaseJobService::class.java).toString()
            var executionWindowStart =
                TimeUnit.HOURS.toSeconds(SCHEDULE_PERIOD_IN_HOURS.toLong()).toInt()
            val job = mJobDispatcher.newJobBuilder()
                .setTag(tag)
                .setService(FirebaseJobService::class.java)
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .setLifetime(Lifetime.FOREVER)
                .setTrigger(
                    Trigger.executionWindow(
                        executionWindowStart,
                        ++executionWindowStart
                    )
                )
                .build()
            mJobDispatcher.mustSchedule(job)
        }

        init {
            mJobDispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))
        }
    }

    private class ConnectivityListenerStub internal constructor() :
        ConnectivityListener {
        override fun listen() { /* Do nothing */
        }

        init {
            val exception = IllegalStateException(
                "Play services doesn't exist on" +
                        " the device"
            )
            Crashlytics.logException(exception)
        }
    }

    companion object {
        private const val SCHEDULE_PERIOD_IN_HOURS = 1
        fun from(context: Context): ConnectivityJobScheduler {
            val application = context.applicationContext as MwmApplication
            return application.connectivityListener as ConnectivityJobScheduler
        }
    }

    init {
        mMasterConnectivityListener =
            if (Utils.isLollipopOrLater) createNativeJobScheduler(context) else createCompatJobScheduler(
                context
            )
    }
}