package com.mapswithme.maps.downloader

import android.text.TextUtils
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Button
import android.widget.TextView
import com.mapswithme.maps.Framework
import com.mapswithme.maps.MwmActivity
import com.mapswithme.maps.MwmActivity.LeftAnimationTrackListener
import com.mapswithme.maps.R
import com.mapswithme.maps.background.Notifier
import com.mapswithme.maps.bookmarks.BookmarksCatalogActivity
import com.mapswithme.maps.downloader.MapManager.CurrentCountryChangedListener
import com.mapswithme.maps.downloader.MapManager.nativeCancel
import com.mapswithme.maps.downloader.MapManager.nativeDownload
import com.mapswithme.maps.downloader.MapManager.nativeFindCountry
import com.mapswithme.maps.downloader.MapManager.nativeHasSpaceToDownloadCountry
import com.mapswithme.maps.downloader.MapManager.nativeRetry
import com.mapswithme.maps.downloader.MapManager.nativeSubscribe
import com.mapswithme.maps.downloader.MapManager.nativeSubscribeOnCountryChanged
import com.mapswithme.maps.downloader.MapManager.nativeUnsubscribe
import com.mapswithme.maps.downloader.MapManager.nativeUnsubscribeOnCountryChanged
import com.mapswithme.maps.downloader.MapManager.showError
import com.mapswithme.maps.downloader.MapManager.warnOn3g
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.routing.RoutingController
import com.mapswithme.maps.widget.WheelProgressView
import com.mapswithme.util.*
import com.mapswithme.util.statistics.Statistics
import com.mapswithme.util.statistics.Statistics.EventName
import java.util.*

class OnmapDownloader(private val mActivity: MwmActivity) : LeftAnimationTrackListener {
    private val mFrame: View
    private val mParent: TextView
    private val mTitle: TextView
    private val mSize: TextView
    private val mProgress: WheelProgressView
    private val mButton: Button
    private val mCatalogCallToActionContainer: View
    private val mPromoContentDivider: View
    private var mStorageSubscriptionSlot = 0
    private var mCurrentCountry: CountryItem? = null
    private var mPromoBanner: DownloaderPromoBanner? = null
    private val mStorageCallback: MapManager.StorageCallback = object : MapManager.StorageCallback {
        override fun onStatusChanged(data: List<MapManager.StorageCallbackData>) {
            if (mCurrentCountry == null) return
            for (item in data) {
                if (!item.isLeafNode) continue
                if (item.newStatus == CountryItem.STATUS_FAILED) showError(
                    mActivity,
                    item,
                    null
                )
                if (mCurrentCountry!!.id == item.countryId) {
                    mCurrentCountry!!.update()
                    updateState(false)
                    return
                }
            }
        }

        override fun onProgress(
            countryId: String,
            localSize: Long,
            remoteSize: Long
        ) {
            if (mCurrentCountry != null && mCurrentCountry!!.id == countryId) {
                mCurrentCountry!!.update()
                updateState(false)
            }
        }
    }
    private val mCountryChangedListener: CurrentCountryChangedListener =
        object : CurrentCountryChangedListener {
            override fun onCurrentCountryChanged(countryId: String?) {
                mCurrentCountry =
                    if (TextUtils.isEmpty(countryId)) null else CountryItem.fill(countryId)
                updateState(true)
            }
        }

    fun updateState(shouldAutoDownload: Boolean) {
        var showFrame = mCurrentCountry != null &&
                !mCurrentCountry!!.present &&
                !RoutingController.get().isNavigating
        if (showFrame) {
            val enqueued =
                mCurrentCountry!!.status == CountryItem.STATUS_ENQUEUED
            val progress =
                mCurrentCountry!!.status == CountryItem.STATUS_PROGRESS ||
                        mCurrentCountry!!.status == CountryItem.STATUS_APPLYING
            val failed =
                mCurrentCountry!!.status == CountryItem.STATUS_FAILED
            showFrame =
                enqueued || progress || failed || mCurrentCountry!!.status == CountryItem.STATUS_DOWNLOADABLE
            if (showFrame) {
                val hasParent: Boolean =
                    !CountryItem.isRoot(mCurrentCountry!!.topmostParentId)
                UiUtils.showIf(progress || enqueued, mProgress)
                UiUtils.showIf(!progress && !enqueued, mButton)
                UiUtils.showIf(hasParent, mParent)
                if (hasParent) mParent.text = mCurrentCountry!!.topmostParentName
                mTitle.text = mCurrentCountry!!.name
                val sizeText: String
                if (progress) {
                    mProgress.isPending = false
                    mProgress.progress = mCurrentCountry!!.progress
                    sizeText = String.format(
                        Locale.US,
                        "%1\$s %2\$d%%",
                        mActivity.getString(R.string.downloader_downloading),
                        mCurrentCountry!!.progress
                    )
                } else {
                    if (enqueued) {
                        sizeText = mActivity.getString(R.string.downloader_queued)
                        mProgress.isPending = true
                    } else {
                        sizeText =
                            StringUtils.getFileSizeString(mCurrentCountry!!.totalSize)
                        if (shouldAutoDownload &&
                            Config.isAutodownloadEnabled() &&
                            !sAutodownloadLocked &&
                            !failed &&
                            ConnectionState.isWifiConnected
                        ) {
                            val loc =
                                LocationHelper.INSTANCE.savedLocation
                            if (loc != null) {
                                val country = nativeFindCountry(
                                    loc.latitude,
                                    loc.longitude
                                )
                                if (TextUtils.equals(mCurrentCountry!!.id, country) &&
                                    nativeHasSpaceToDownloadCountry(country)
                                ) {
                                    nativeDownload(mCurrentCountry!!.id)
                                    Statistics.INSTANCE.trackEvent(
                                        EventName.DOWNLOADER_ACTION,
                                        Statistics.params().add(
                                            Statistics.EventParam.ACTION,
                                            "download"
                                        )
                                            .add(
                                                Statistics.EventParam.FROM,
                                                "map"
                                            )
                                            .add("is_auto", "true")
                                            .add("scenario", "download")
                                    )
                                }
                            }
                        }
                        mButton.setText(if (failed) R.string.downloader_retry else R.string.download)
                    }
                }
                mSize.text = sizeText
            }
        }
        UiUtils.showIf(showFrame, mFrame)
        updateBannerVisibility()
    }

    private fun updateBannerVisibility() {
        if (mCurrentCountry == null || TextUtils.isEmpty(mCurrentCountry!!.id)) return
        mPromoBanner = Framework.nativeGetDownloaderPromoBanner(mCurrentCountry!!.id!!)
        val isPromoFound =
            mPromoBanner!!.type != DownloaderPromoBanner.DOWNLOADER_PROMO_TYPE_NO_PROMO
        val enqueued =
            mCurrentCountry!!.status == CountryItem.STATUS_ENQUEUED
        val progress =
            mCurrentCountry!!.status == CountryItem.STATUS_PROGRESS
        val applying =
            mCurrentCountry!!.status == CountryItem.STATUS_APPLYING
        val isDownloading = enqueued || progress || applying
        UiUtils.showIf(isPromoFound && isDownloading, mPromoContentDivider)
        val hasMegafonPromo =
            mPromoBanner!!.type == DownloaderPromoBanner.DOWNLOADER_PROMO_TYPE_MEGAFON
        val hasCatalogPromo =
            mPromoBanner!!.type == DownloaderPromoBanner.DOWNLOADER_PROMO_TYPE_BOOKMARK_CATALOG
        UiUtils.showIf(isDownloading && hasMegafonPromo, mFrame, R.id.banner)
        UiUtils.showIf(isDownloading && hasCatalogPromo, mCatalogCallToActionContainer)
        if (!isPromoFound) return
        val builder =
            Statistics.makeDownloaderBannerParamBuilder(mPromoBanner!!.toStatisticValue())
        Statistics.INSTANCE.trackEvent(
            EventName.DOWNLOADER_BANNER_SHOW,
            builder
        )
    }

    override fun onTrackStarted(collapsed: Boolean) {}
    override fun onTrackFinished(collapsed: Boolean) {}
    override fun onTrackLeftAnimation(offset: Float) {
        val lp = mFrame.layoutParams as MarginLayoutParams
        lp.leftMargin = offset.toInt()
        mFrame.layoutParams = lp
    }

    fun onPause() {
        if (mStorageSubscriptionSlot > 0) {
            nativeUnsubscribe(mStorageSubscriptionSlot)
            mStorageSubscriptionSlot = 0
            nativeUnsubscribeOnCountryChanged()
        }
    }

    fun onResume() {
        if (mStorageSubscriptionSlot == 0) {
            mStorageSubscriptionSlot = nativeSubscribe(mStorageCallback)
            nativeSubscribeOnCountryChanged(mCountryChangedListener)
        }
    }

    private inner class CatalogCallToActionListener :
        View.OnClickListener {
        override fun onClick(v: View) {
            if (mPromoBanner != null) {
                BookmarksCatalogActivity.start(mActivity, mPromoBanner!!.url)
                val builder =
                    Statistics.makeDownloaderBannerParamBuilder(Statistics.ParamValue.MAPSME_GUIDES)
                Statistics.INSTANCE.trackEvent(
                    EventName.DOWNLOADER_BANNER_CLICK,
                    builder
                )
            }
        }
    }

    companion object {
        private var sAutodownloadLocked = false
        @kotlin.jvm.JvmStatic
        fun setAutodownloadLocked(locked: Boolean) {
            sAutodownloadLocked = locked
        }
    }

    init {
        mFrame = mActivity.findViewById(R.id.onmap_downloader)
        mParent = mFrame.findViewById<View>(R.id.downloader_parent) as TextView
        mTitle = mFrame.findViewById<View>(R.id.downloader_title) as TextView
        mSize = mFrame.findViewById<View>(R.id.downloader_size) as TextView
        val controls =
            mFrame.findViewById<View>(R.id.downloader_controls_frame)
        mProgress =
            controls.findViewById<View>(R.id.wheel_downloader_progress) as WheelProgressView
        mButton =
            controls.findViewById<View>(R.id.downloader_button) as Button
        mProgress.setOnClickListener {
            nativeCancel(mCurrentCountry!!.id)
            Statistics.INSTANCE.trackEvent(
                EventName.DOWNLOADER_CANCEL,
                Statistics.params().add(
                    Statistics.EventParam.FROM,
                    "map"
                )
            )
            setAutodownloadLocked(true)
        }
        val notifier =
            Notifier.from(mActivity.application)
        mButton.setOnClickListener {
            warnOn3g(mActivity, mCurrentCountry!!.id, Runnable {
                if (mCurrentCountry == null) return@Runnable
                val retry =
                    mCurrentCountry!!.status == CountryItem.STATUS_FAILED
                if (retry) {
                    notifier.cancelNotification(Notifier.ID_DOWNLOAD_FAILED)
                    nativeRetry(mCurrentCountry!!.id)
                } else nativeDownload(mCurrentCountry!!.id)
                Statistics.INSTANCE.trackEvent(
                    EventName.DOWNLOADER_ACTION,
                    Statistics.params().add(
                        Statistics.EventParam.ACTION,
                        if (retry) "retry" else "download"
                    )
                        .add(
                            Statistics.EventParam.FROM,
                            "map"
                        )
                        .add("is_auto", "false")
                        .add("scenario", "download")
                )
            })
        }
        mFrame.findViewById<View>(R.id.banner_button)
            .setOnClickListener { v: View? ->
                if (mPromoBanner != null && mPromoBanner?.type != DownloaderPromoBanner.DOWNLOADER_PROMO_TYPE_NO_PROMO) Utils.openUrl(
                    mActivity,
                    mPromoBanner!!.url
                )
                val builder =
                    Statistics.makeDownloaderBannerParamBuilder(Statistics.ParamValue.MEGAFON)
                Statistics.INSTANCE.trackEvent(
                    EventName.DOWNLOADER_BANNER_CLICK,
                    builder
                )
            }
        val downloadGuidesBtn =
            mFrame.findViewById<View>(R.id.catalog_call_to_action_btn)
        mCatalogCallToActionContainer =
            mFrame.findViewById(R.id.catalog_call_to_action_container)
        downloadGuidesBtn.setOnClickListener(CatalogCallToActionListener())
        mPromoContentDivider = mFrame.findViewById(R.id.onmap_downloader_divider)
    }
}