package com.mapswithme.maps.downloader

import android.view.View
import android.widget.Button
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapswithme.maps.R
import com.mapswithme.maps.downloader.MapManager.nativeUpdate
import com.mapswithme.maps.downloader.MapManager.warn3gAndDownload
import com.mapswithme.maps.downloader.MapManager.warnOn3gUpdate
import com.mapswithme.util.StringUtils
import com.mapswithme.util.UiUtils
import com.mapswithme.util.statistics.Statistics
import com.mapswithme.util.statistics.Statistics.EventName
import java.util.*

internal class BottomPanel(private val mFragment: DownloaderFragment, frame: View) {
    private val mFab: FloatingActionButton
    private val mButton: Button


    private val mDownloadListener =
        View.OnClickListener {
            warn3gAndDownload(
                mFragment.activity,
                mFragment.currentRoot,
                Runnable {
                    Statistics.INSTANCE.trackEvent(
                        EventName.DOWNLOADER_ACTION,
                        Statistics.params().add(
                            Statistics.EventParam.ACTION,
                            "download"
                        )
                            .add(
                                Statistics.EventParam.FROM,
                                "downloader"
                            )
                            .add("is_auto", "false")
                            .add("scenario", "download_group")
                    )
                })
        }

    private val mUpdateListener =
        View.OnClickListener {
            val country = mFragment.currentRoot
            warnOn3gUpdate(
                mFragment.activity,
                country,
                Runnable {
                    nativeUpdate(country)
                    Statistics.INSTANCE.trackEvent(
                        EventName.DOWNLOADER_ACTION,
                        Statistics.params().add(
                            Statistics.EventParam.ACTION,
                            "update"
                        )
                            .add(
                                Statistics.EventParam.FROM,
                                "downloader"
                            )
                            .add("is_auto", "false")
                            .add("scenario", "update_all")
                    )
                })
        }


    private val mCancelListener =
        View.OnClickListener {
            MapManager.nativeCancel(mFragment.currentRoot)
            Statistics.INSTANCE.trackEvent(
                EventName.DOWNLOADER_CANCEL,
                Statistics.params().add(
                    Statistics.EventParam.FROM,
                    "downloader"
                )
            )
        }

    private fun setUpdateAllState(info: UpdateInfo?) {
        mButton.text = String.format(
            Locale.US,
            "%s (%s)",
            mFragment.getString(R.string.downloader_update_all_button),
            StringUtils.getFileSizeString(info!!.totalSize)
        )
        mButton.setOnClickListener(mUpdateListener)
    }

    private fun setDownloadAllState() {
        mButton.setText(R.string.downloader_download_all_button)
        mButton.setOnClickListener(mDownloadListener)
    }

    private fun setCancelState() {
        mButton.setText(R.string.downloader_cancel_all)
        mButton.setOnClickListener(mCancelListener)
    }

    fun update() {
        val adapter = mFragment.adapter!!
        val search = adapter.isSearchResultsMode
        var show = !search
        UiUtils.showIf(show && adapter.isMyMapsMode, mFab)
        if (show) {
            val root = adapter.currentRootId
            val status = MapManager.nativeGetStatus(root)
            if (adapter.isMyMapsMode) {
                when (status) {
                    CountryItem.Companion.STATUS_UPDATABLE -> {
                        val info = MapManager.nativeGetUpdateInfo(root)
                        setUpdateAllState(info)
                    }
                    CountryItem.Companion.STATUS_DOWNLOADABLE, CountryItem.Companion.STATUS_DONE, CountryItem.Companion.STATUS_PARTLY -> show =
                        false
                    CountryItem.Companion.STATUS_PROGRESS, CountryItem.Companion.STATUS_APPLYING, CountryItem.Companion.STATUS_ENQUEUED -> setCancelState()
                    CountryItem.Companion.STATUS_FAILED -> setDownloadAllState()
                    else -> throw IllegalArgumentException("Inappropriate status for \"$root\": $status")
                }
            } else {
                show = !CountryItem.Companion.isRoot(root)
                if (show) {
                    when (status) {
                        CountryItem.Companion.STATUS_UPDATABLE -> {
                            val info = MapManager.nativeGetUpdateInfo(root)
                            setUpdateAllState(info)
                        }
                        CountryItem.Companion.STATUS_DONE -> show = false
                        CountryItem.Companion.STATUS_PROGRESS, CountryItem.Companion.STATUS_APPLYING, CountryItem.Companion.STATUS_ENQUEUED -> setCancelState()
                        else -> setDownloadAllState()
                    }
                }
            }
        }
        UiUtils.showIf(show, mButton)
    }

    init {
        mFab = frame.findViewById<View>(R.id.fab) as FloatingActionButton
        mFab.setOnClickListener {
            if (mFragment.adapter != null) mFragment.adapter?.setAvailableMapsMode()
            Statistics.INSTANCE.trackEvent(
                EventName.DOWNLOADER_FAB_CLICK,
                Statistics.params().add(
                    Statistics.EventParam.BUTTON,
                    Statistics.ParamValue.PLUS
                )
            )
            update()
        }
        mButton = frame.findViewById<View>(R.id.action) as Button
    }
}