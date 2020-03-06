package com.mapswithme.maps.background

import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.IntDef
import androidx.core.app.NotificationCompat
import com.mapswithme.maps.MwmActivity
import com.mapswithme.maps.R
import com.mapswithme.maps.background.NotificationCandidate.UgcReview
import com.mapswithme.util.StringUtils
import com.mapswithme.util.UiUtils
import com.mapswithme.util.statistics.Statistics

class Notifier private constructor(private val mContext: Application) {

    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @IntDef(
        ID_NONE,
        ID_DOWNLOAD_FAILED,
        ID_IS_NOT_AUTHENTICATED,
        ID_LEAVE_REVIEW
    )
    annotation class NotificationId

    fun notifyDownloadFailed(id: String?, name: String?) {
        val title = mContext.getString(R.string.app_name)
        val content = mContext.getString(R.string.download_country_failed, name)
        val intent = MwmActivity.createShowMapIntent(mContext, id)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pi = PendingIntent.getActivity(
            mContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val channel =
            NotificationChannelFactory.createProvider(mContext).downloadingChannel
        placeNotification(
            title,
            content,
            pi,
            ID_DOWNLOAD_FAILED,
            channel
        )
        Statistics.INSTANCE.trackEvent(Statistics.EventName.DOWNLOAD_COUNTRY_NOTIFICATION_SHOWN)
    }

    fun notifyAuthentication() {
        val authIntent = MwmActivity.createAuthenticateIntent(mContext)
        authIntent.putExtra(
            EXTRA_CANCEL_NOTIFICATION,
            ID_IS_NOT_AUTHENTICATED
        )
        authIntent.putExtra(
            EXTRA_NOTIFICATION_CLICKED,
            Statistics.EventName.UGC_NOT_AUTH_NOTIFICATION_CLICKED
        )
        val pi = PendingIntent.getActivity(
            mContext, 0, authIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val channel = NotificationChannelFactory.createProvider(mContext).uGCChannel
        val builder = getBuilder(
            mContext.getString(R.string.notification_unsent_reviews_title),
            mContext.getString(R.string.notification_unsent_reviews_message),
            pi, channel
        )
        builder.addAction(0, mContext.getString(R.string.authorization_button_sign_in), pi)
        notificationManager.notify(
            ID_IS_NOT_AUTHENTICATED,
            builder.build()
        )
        Statistics.INSTANCE.trackEvent(Statistics.EventName.UGC_NOT_AUTH_NOTIFICATION_SHOWN)
    }

    fun notifyLeaveReview(source: UgcReview) {
        val reviewIntent = MwmActivity.createLeaveReviewIntent(mContext, source)
        reviewIntent.putExtra(
            EXTRA_CANCEL_NOTIFICATION,
            ID_LEAVE_REVIEW
        )
        reviewIntent.putExtra(
            EXTRA_NOTIFICATION_CLICKED,
            Statistics.EventName.UGC_REVIEW_NOTIFICATION_CLICKED
        )
        val pi =
            PendingIntent.getActivity(mContext, 0, reviewIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val channel = NotificationChannelFactory.createProvider(mContext).uGCChannel
        val content =
            if (source.address.isEmpty()) source.readableName else source.readableName + ", " + source.address
        val builder = getBuilder(
            mContext.getString(R.string.notification_leave_review_v2_android_short_title),
            content, pi, channel
        )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(
                        mContext.getString(R.string.notification_leave_review_v2_title)
                    )
                    .bigText(content)
            )
            .addAction(0, mContext.getString(R.string.leave_a_review), pi)
        notificationManager.notify(
            ID_LEAVE_REVIEW,
            builder.build()
        )
        Statistics.INSTANCE.trackEvent(Statistics.EventName.UGC_REVIEW_NOTIFICATION_SHOWN)
    }

    fun cancelNotification(@NotificationId id: Int) {
        if (id == ID_NONE) return
        notificationManager.cancel(id)
    }

    fun processNotificationExtras(intent: Intent?) {
        if (intent == null) return
        if (intent.hasExtra(EXTRA_CANCEL_NOTIFICATION)) {
            @NotificationId val notificationId = intent.getIntExtra(
                EXTRA_CANCEL_NOTIFICATION,
                ID_NONE
            )
            cancelNotification(notificationId)
        }
        if (intent.hasExtra(EXTRA_NOTIFICATION_CLICKED)) {
            val eventName =
                intent.getStringExtra(EXTRA_NOTIFICATION_CLICKED)
            Statistics.INSTANCE.trackEvent(eventName)
        }
    }

    private fun placeNotification(
        title: String, content: String, pendingIntent: PendingIntent,
        notificationId: Int, channel: String
    ) {
        val notification =
            getBuilder(title, content, pendingIntent, channel).build()
        notificationManager.notify(notificationId, notification)
    }

    private fun getBuilder(
        title: String, content: String,
        pendingIntent: PendingIntent, channel: String
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(mContext, channel)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.pw_notification)
            .setColor(UiUtils.getNotificationColor(mContext))
            .setContentTitle(title)
            .setContentText(content)
            .setTicker(getTicker(title, content))
            .setContentIntent(pendingIntent)
    }

    private fun getTicker(title: String, content: String): CharSequence {
        val templateResId =
            if (StringUtils.isRtl()) R.string.notification_ticker_rtl else R.string.notification_ticker_ltr
        return mContext.getString(templateResId, title, content)
    }

    private val notificationManager: NotificationManager
        get() = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val EXTRA_CANCEL_NOTIFICATION = "extra_cancel_notification"
        private const val EXTRA_NOTIFICATION_CLICKED = "extra_notification_clicked"
        const val ID_NONE = 0
        const val ID_DOWNLOAD_FAILED = 1
        const val ID_IS_NOT_AUTHENTICATED = 2
        const val ID_LEAVE_REVIEW = 3


        @JvmStatic
        fun from(application: Application): Notifier {
            return Notifier(application)
        }
    }

}