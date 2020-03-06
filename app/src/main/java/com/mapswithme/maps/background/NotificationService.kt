package com.mapswithme.maps.background

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.core.app.JobIntentService
import com.mapswithme.maps.LightFramework
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.background.NotificationCandidate.UgcReview
import com.mapswithme.maps.routing.RoutingController
import com.mapswithme.maps.scheduling.JobIdMap
import com.mapswithme.util.PermissionsUtils
import com.mapswithme.util.log.LoggerFactory
import java.util.concurrent.TimeUnit

class NotificationService : JobIntentService() {
    private interface NotificationExecutor {
        fun tryToNotify(): Boolean
    }

    private fun notifyIsNotAuthenticated(): Boolean {
        if (LightFramework.nativeIsAuthenticated()
            || LightFramework.nativeGetNumberUnsentUGC() < MIN_COUNT_UNSENT_UGC
        ) {
            LOGGER.d(
                TAG,
                "Authentication notification is rejected. Is user authenticated: " +
                        LightFramework.nativeIsAuthenticated() + ". Number of unsent UGC: " +
                        LightFramework.nativeGetNumberUnsentUGC()
            )
            return false
        }
        val lastEventTimestamp = MwmApplication.prefs()?.getLong(LAST_AUTH_NOTIFICATION_TIMESTAMP, 0) ?: 0
        if (System.currentTimeMillis() - lastEventTimestamp > MIN_AUTH_EVENT_DELTA_MILLIS) {
            LOGGER.d(
                TAG,
                "Authentication notification will be sent."
            )
            MwmApplication.prefs()?.edit()
                ?.putLong(
                    LAST_AUTH_NOTIFICATION_TIMESTAMP,
                    System.currentTimeMillis()
                )
                ?.apply()
            val notifier: Notifier =
                Notifier.Companion.from(application)
            notifier.notifyAuthentication()
            return true
        }
        LOGGER.d(
            TAG,
            "Authentication notification is rejected. Last event timestamp: " +
                    lastEventTimestamp + "Current time milliseconds: " + System.currentTimeMillis()
        )
        return false
    }

    private fun notifySmart(): Boolean {
        if (MwmApplication.backgroundTracker(application).isForeground) return false
        val candidate = LightFramework.nativeGetNotification() ?: return false
        if (candidate.type == NotificationCandidate.Companion.TYPE_UGC_REVIEW) {
            val notifier: Notifier =
                Notifier.Companion.from(application)
            notifier.notifyLeaveReview(candidate as UgcReview)
            return true
        }
        return false
    }

    override fun onHandleWork(intent: Intent) {
        val action = intent.action
        if (ConnectivityManager.CONNECTIVITY_ACTION == action) tryToShowNotification()
    }

    private fun tryToShowNotification() {
        if (!PermissionsUtils.isExternalStorageGranted()) {
            LOGGER.d(
                TAG,
                "Notification is rejected. External storage is not granted."
            )
            return
        }
        // Do not show push when user is in the navigation mode.
        if (MwmApplication.get().arePlatformAndCoreInitialized()
            && RoutingController.get().isNavigating
        ) {
            LOGGER.d(
                TAG,
                "Notification is rejected. The user is in navigation mode."
            )
            return
        }
        val notifyOrder = arrayOf<NotificationExecutor>(object : NotificationExecutor {
            override fun tryToNotify(): Boolean {
                return notifyIsNotAuthenticated()
            }
        }
            ,
            object : NotificationExecutor {
                override fun tryToNotify(): Boolean {
                    return notifySmart()
                }
            }
        )
        // Only one notification should be shown at a time.
        for (executor in notifyOrder) {
            if (executor.tryToNotify()) return
        }
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = NotificationService::class.java.simpleName
        private const val LAST_AUTH_NOTIFICATION_TIMESTAMP = "DownloadOrUpdateTimestamp"
        private const val MIN_COUNT_UNSENT_UGC = 2
        private val MIN_AUTH_EVENT_DELTA_MILLIS =
            TimeUnit.DAYS.toMillis(5)

        @JvmStatic
        fun startOnConnectivityChanged(context: Context) {
            val intent = Intent(context, NotificationService::class.java)
                .setAction(ConnectivityManager.CONNECTIVITY_ACTION)
            val id = JobIdMap.getId(NotificationService::class.java)
            enqueueWork(context, NotificationService::class.java, id, intent)
        }
    }
}