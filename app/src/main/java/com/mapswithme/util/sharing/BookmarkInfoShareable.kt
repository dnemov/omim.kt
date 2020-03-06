package com.mapswithme.util.sharing

import android.app.Activity
import android.text.TextUtils
import com.mapswithme.maps.Framework
import com.mapswithme.maps.R
import com.mapswithme.maps.widget.placepage.Sponsored
import com.mapswithme.util.UiUtils
import com.mapswithme.util.statistics.Statistics
import java.util.*

internal open class BookmarkInfoShareable<T : ShareableInfoProvider?>(
    activity: Activity, val provider: T,
    sponsored: Sponsored?
) : BaseShareable(activity) {
    protected open val emailBodyContent: Iterable<String>
        get() = if (TextUtils.isEmpty(provider?.address)) Arrays.asList(
            provider!!.name,
            geoUrl,
            httpUrl
        ) else Arrays.asList(
            provider!!.name,
            provider.address,
            geoUrl,
            httpUrl
        )

    val geoUrl: String
        get() = Framework.nativeGetGe0Url(
            provider!!.lat, provider.lon,
            provider.scale, provider.name
        )

    val httpUrl: String
        get() = Framework.getHttpGe0Url(
            provider!!.lat, provider.lon,
            provider.scale, provider.name
        )

    override lateinit var mimeType: String

    override fun share(target: SharingTarget) {
        super.share(target)
        Statistics.INSTANCE.trackPlaceShared(target.name)
    }

    companion object {
        private fun makeEmailBody(
            activity: Activity, sponsored: Sponsored?,
            emailBodyContent: Iterable<String>
        ): String {
            var text = TextUtils.join(UiUtils.NEW_STRING_DELIMITER, emailBodyContent)
            if (sponsored != null && sponsored.type === Sponsored.TYPE_BOOKING) text =
                concatSponsoredText(
                    activity,
                    sponsored,
                    text
                )
            return text
        }

        private fun concatSponsoredText(
            activity: Activity, sponsored: Sponsored,
            src: String
        ): String {
            val mainSegment = TextUtils.join(
                UiUtils.NEW_STRING_DELIMITER,
                Arrays.asList(
                    src,
                    activity.getString(R.string.sharing_booking)
                )
            )
            return mainSegment + sponsored.url
        }
    }

    init {
        setSubject(R.string.bookmark_share_email_subject)
        val text =
            makeEmailBody(
                activity,
                sponsored,
                emailBodyContent
            )
        setText(text)
    }
}