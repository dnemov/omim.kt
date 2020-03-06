package com.mapswithme.maps.location

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.SystemClock
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.background.AppBackgroundTracker.OnTransitionListener
import com.mapswithme.util.concurrency.UiThread
import com.mapswithme.util.log.LoggerFactory

object TrackRecorder {
    private val TAG = TrackRecorder::class.java.simpleName
    private val sAlarmManager =
        MwmApplication.get().getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private const val WAKEUP_INTERVAL_MS: Long = 20000
    private const val STARTUP_AWAIT_INTERVAL_MS: Long = 5000
    private const val LOCATION_TIMEOUT_STORED_KEY = "TrackRecordLastAwaitTimeout"
    private const val LOCATION_TIMEOUT_MIN_MS: Long = 5000
    private const val LOCATION_TIMEOUT_MAX_MS: Long = 80000
    private val sStartupAwaitProc = Runnable { restartAlarmIfEnabled() }
    private val LOGGER =
        LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.TRACK_RECORDER)
    private val sLocationListener: LocationListener =
        object : LocationListener.Simple() {
            override fun onLocationUpdated(location: Location) {
                LOGGER.d(TAG, "onLocationUpdated()")
                awaitTimeout = LOCATION_TIMEOUT_MIN_MS
                LocationHelper.INSTANCE.onLocationUpdated(location)
                TrackRecorderWakeService.Companion.stop()
            }

            override fun onLocationError(errorCode: Int) {
                LOGGER.e(
                    TAG,
                    "onLocationError() errorCode: $errorCode"
                )
                // Unrecoverable error occured: GPS disabled or inaccessible
                isEnabled = false
            }
        }

    @JvmStatic
    fun init() {
        LOGGER.d(TAG, "--------------------------------")
        LOGGER.d(TAG, "init()")
        MwmApplication.backgroundTracker()?.addListener(object : OnTransitionListener {
            override fun onTransit(foreground: Boolean) {
                LOGGER.d(TAG, "Transit to foreground: $foreground")
                UiThread.cancelDelayedTasks(sStartupAwaitProc)
                if (foreground) TrackRecorderWakeService.Companion.stop() else restartAlarmIfEnabled()
            }
        })
        if (nativeIsEnabled()) UiThread.runLater(
            sStartupAwaitProc,
            STARTUP_AWAIT_INTERVAL_MS
        ) else stop()
    }

    private val alarmIntent: PendingIntent
        private get() {
            val intent = Intent(MwmApplication.get(), TrackRecorderWakeReceiver::class.java)
            return PendingIntent.getBroadcast(MwmApplication.get(), 0, intent, 0)
        }

    private fun restartAlarmIfEnabled() {
        LOGGER.d(TAG, "restartAlarmIfEnabled()")
        if (nativeIsEnabled()) sAlarmManager[AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + WAKEUP_INTERVAL_MS] =
            alarmIntent
    }

    private fun stop() {
        LOGGER.d(TAG, "stop(). Cancel awake timer")
        sAlarmManager.cancel(alarmIntent)
        TrackRecorderWakeService.Companion.stop()
    }

    @kotlin.jvm.JvmStatic
    var isEnabled: Boolean
        get() = nativeIsEnabled()
        set(enabled) {
            LOGGER.d(TAG, "setEnabled(): $enabled")
            awaitTimeout = LOCATION_TIMEOUT_MIN_MS
            nativeSetEnabled(enabled)
            if (enabled) restartAlarmIfEnabled() else stop()
        }

    @kotlin.jvm.JvmStatic
    var duration: Int
        get() = nativeGetDuration()
        set(hours) {
            nativeSetDuration(hours)
        }

    fun onWakeAlarm(context: Context) {
        LOGGER.d(
            TAG,
            "onWakeAlarm(). Enabled: " + nativeIsEnabled()
        )
        UiThread.cancelDelayedTasks(sStartupAwaitProc)
        if (nativeIsEnabled() && !MwmApplication.backgroundTracker()!!.isForeground) TrackRecorderWakeService.Companion.start(
            context
        ) else stop()
    }

    var awaitTimeout: Long
        get() = MwmApplication.prefs()!!.getLong(
            LOCATION_TIMEOUT_STORED_KEY,
            LOCATION_TIMEOUT_MIN_MS
        )
        private set(timeout) {
            LOGGER.d(TAG, "setAwaitTimeout(): $timeout")
            if (timeout != awaitTimeout) MwmApplication.prefs()!!.edit().putLong(
                LOCATION_TIMEOUT_STORED_KEY,
                timeout
            ).apply()
        }

    fun incrementAwaitTimeout() {
        val current = awaitTimeout
        var next = current * 2
        if (next > LOCATION_TIMEOUT_MAX_MS) next =
            LOCATION_TIMEOUT_MAX_MS
        if (next != current) awaitTimeout = next
    }

    fun onServiceStarted() {
        LOGGER.d(
            TAG,
            "onServiceStarted(). Scheduled to be run on UI thread..."
        )
        UiThread.run {
            LOGGER.d(TAG, "onServiceStarted(): actually runs here")
            LocationHelper.INSTANCE.addListener(sLocationListener, false)
        }
    }

    fun onServiceStopped() {
        LOGGER.d(
            TAG,
            "onServiceStopped(). Scheduled to be run on UI thread..."
        )
        UiThread.run {
            LOGGER.d(TAG, "onServiceStopped(): actually runs here")
            LocationHelper.INSTANCE.removeListener(sLocationListener)
            if (MwmApplication.backgroundTracker()?.isForeground == false) restartAlarmIfEnabled()
        }
    }

    @JvmStatic private external fun nativeSetEnabled(enable: Boolean)
    @JvmStatic private external fun nativeIsEnabled(): Boolean
    @JvmStatic private external fun nativeSetDuration(hours: Int)
    @JvmStatic private external fun nativeGetDuration(): Int
}