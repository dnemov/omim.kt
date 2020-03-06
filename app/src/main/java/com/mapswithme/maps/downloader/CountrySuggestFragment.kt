package com.mapswithme.maps.downloader

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmFragment
import com.mapswithme.maps.downloader.MapManager.StorageCallback
import com.mapswithme.maps.downloader.MapManager.StorageCallbackData
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.widget.WheelProgressView
import com.mapswithme.util.StringUtils
import com.mapswithme.util.UiUtils
import com.mapswithme.util.statistics.Statistics
import com.mapswithme.util.statistics.Statistics.EventName
import java.util.*

class CountrySuggestFragment : BaseMwmFragment(), View.OnClickListener {
    private var mLlWithLocation: LinearLayout? = null
    private var mLlNoLocation: LinearLayout? = null
    private var mLlSelectDownload: LinearLayout? = null
    private var mLlActiveDownload: LinearLayout? = null
    private var mWpvDownloadProgress: WheelProgressView? = null
    private var mTvCountry: TextView? = null
    private var mTvActiveCountry: TextView? = null
    private var mTvProgress: TextView? = null
    private var mBtnDownloadMap: Button? = null
    private var mCurrentCountry: CountryItem? = null
    private var mDownloadingCountry: CountryItem? = null
    private var mListenerSlot = 0
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_suggest_country_download, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        mListenerSlot = MapManager.nativeSubscribe(object : StorageCallback {
            override fun onStatusChanged(data: List<StorageCallbackData>) {
                if (!isAdded) return
                for (item in data) {
                    if (!item.isLeafNode) continue
                    if (mDownloadingCountry == null) mDownloadingCountry =
                        CountryItem.Companion.fill(item.countryId) else if (item.countryId != mDownloadingCountry!!.id) continue
                    when (item.newStatus) {
                        CountryItem.Companion.STATUS_FAILED -> {
                            updateViews()
                            return
                        }
                        CountryItem.Companion.STATUS_DONE -> {
                            exitFragment()
                            return
                        }
                    }
                    break
                }
                updateViews()
            }

            override fun onProgress(
                countryId: String,
                localSize: Long,
                remoteSize: Long
            ) {
                if (!isAdded) return
                if (mDownloadingCountry == null) mDownloadingCountry =
                    CountryItem.Companion.fill(countryId) else mDownloadingCountry!!.update()
                updateProgress()
            }
        })
    }

    private fun exitFragment() { // TODO find more elegant way
        parentFragment!!.childFragmentManager.beginTransaction().remove(this)
            .commitAllowingStateLoss()
    }

    override fun onResume() {
        super.onResume()
        val loc = LocationHelper.INSTANCE.savedLocation
        if (loc != null) {
            val id =
                MapManager.nativeFindCountry(loc.latitude, loc.longitude)
            if (!TextUtils.isEmpty(id)) mCurrentCountry = CountryItem.Companion.fill(id)
        }
        updateViews()
    }

    override fun onDestroy() {
        super.onDestroy()
        MapManager.nativeUnsubscribe(mListenerSlot)
    }

    private fun refreshDownloadButton() {
        if (mCurrentCountry == null || !isAdded) return
        mBtnDownloadMap!!.text = String.format(
            Locale.US, "%1\$s (%2\$s)",
            getString(R.string.downloader_download_map),
            StringUtils.getFileSizeString(mCurrentCountry!!.totalSize)
        )
    }

    private fun initViews(view: View) {
        mLlSelectDownload =
            view.findViewById<View>(R.id.ll__select_download) as LinearLayout
        mLlActiveDownload =
            view.findViewById<View>(R.id.ll__active_download) as LinearLayout
        mLlWithLocation =
            view.findViewById<View>(R.id.ll__location_determined) as LinearLayout
        mLlNoLocation =
            view.findViewById<View>(R.id.ll__location_unknown) as LinearLayout
        mBtnDownloadMap =
            view.findViewById<View>(R.id.btn__download_map) as Button
        mBtnDownloadMap!!.setOnClickListener(this)
        val selectMap =
            view.findViewById<View>(R.id.btn__select_map) as Button
        selectMap.setOnClickListener(this)
        mWpvDownloadProgress =
            view.findViewById<View>(R.id.wpv__download_progress) as WheelProgressView
        mWpvDownloadProgress!!.setOnClickListener(this)
        mTvCountry = view.findViewById<View>(R.id.tv__country_name) as TextView
        mTvActiveCountry =
            view.findViewById<View>(R.id.tv__active_country_name) as TextView
        mTvProgress = view.findViewById<View>(R.id.downloader_progress) as TextView
    }

    private fun updateViews() {
        if (!isAdded || MapManager.nativeGetDownloadedCount() > 0) return
        val downloading = MapManager.nativeIsDownloading()
        UiUtils.showIf(downloading, mLlActiveDownload!!)
        UiUtils.showIf(!downloading, mLlSelectDownload!!)
        if (!downloading) {
            val hasLocation = mCurrentCountry != null
            UiUtils.showIf(hasLocation, mLlWithLocation!!)
            UiUtils.showIf(!hasLocation, mLlNoLocation!!)
            refreshDownloadButton()
            if (hasLocation) mTvCountry!!.text = mCurrentCountry!!.name
            if (mDownloadingCountry != null) {
                mDownloadingCountry!!.progress = 0
                updateProgress()
            }
            return
        }
        mTvActiveCountry!!.text = mDownloadingCountry!!.name
        updateProgress()
    }

    private fun updateProgress() {
        val text = String.format(
            Locale.US,
            "%1\$s %2\$d%%",
            getString(R.string.downloader_downloading),
            mDownloadingCountry!!.progress
        )
        mTvProgress!!.text = text
        mWpvDownloadProgress!!.progress = mDownloadingCountry!!.progress
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn__download_map -> MapManager.warn3gAndDownload(
                activity,
                mCurrentCountry!!.id,
                Runnable  { mDownloadingCountry = mCurrentCountry }
            )
            R.id.btn__select_map -> mwmActivity.replaceFragment(
                DownloaderFragment::class.java,
                null,
                null
            )
            R.id.wpv__download_progress -> {
                MapManager.nativeCancel(mDownloadingCountry!!.id)
                Statistics.INSTANCE.trackEvent(
                    EventName.DOWNLOADER_CANCEL,
                    Statistics.params().add(
                        Statistics.EventParam.FROM,
                        "search"
                    )
                )
            }
        }
    }
}