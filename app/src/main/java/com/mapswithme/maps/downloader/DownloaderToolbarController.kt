package com.mapswithme.maps.downloader

import android.app.Activity
import android.content.Intent
import android.text.TextUtils
import android.view.View
import com.mapswithme.maps.R
import com.mapswithme.maps.widget.SearchToolbarController
import com.mapswithme.util.statistics.Statistics
import com.mapswithme.util.statistics.Statistics.EventName

internal class DownloaderToolbarController(
    root: View,
    activity: Activity?,
    private val mFragment: DownloaderFragment
) : SearchToolbarController(root, activity) {
    override fun onUpClick() {
        activity?.onBackPressed()
    }

    override fun onTextChanged(query: String) {
        if (!mFragment.isAdded || !mFragment.shouldShowSearch()) return
        if (TextUtils.isEmpty(query)) mFragment.cancelSearch() else mFragment.startSearch()
    }

    fun update() {
        val showSearch = mFragment.shouldShowSearch()
        val title: String =
            if (showSearch) "" else mFragment.adapter?.currentRootName ?: ""
        showControls(showSearch)
        setTitle(title)
    }

    override val voiceInputPrompt: Int
        get() = R.string.downloader_search_field_hint

    override fun startVoiceRecognition(intent: Intent?, code: Int) {
        mFragment.startActivityForResult(intent, code)
    }

    override fun onQueryClick(query: String) {
        super.onQueryClick(query)
        val isDownloadNewMapsMode = mFragment.adapter!!.canGoUpwards()
        val params = Statistics
            .params().add(
                Statistics.EventParam.SCREEN,
                if (isDownloadNewMapsMode) Statistics.ParamValue.DOWNLOAD else Statistics.ParamValue.PLUS
            )
        Statistics.INSTANCE.trackEvent(
            EventName.DOWNLOADER_SEARCH_CLICK,
            params
        )
    }

    override fun supportsVoiceSearch(): Boolean {
        return true
    }

    init {
        setHint(R.string.downloader_search_field_hint)
    }
}