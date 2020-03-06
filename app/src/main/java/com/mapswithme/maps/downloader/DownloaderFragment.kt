package com.mapswithme.maps.downloader

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmRecyclerFragment
import com.mapswithme.maps.base.OnBackPressListener
import com.mapswithme.maps.downloader.MapManager.StorageCallback
import com.mapswithme.maps.downloader.MapManager.StorageCallbackData
import com.mapswithme.maps.search.NativeMapSearchListener
import com.mapswithme.maps.search.SearchEngine
import com.mapswithme.maps.widget.PlaceholderView
import java.util.*

class DownloaderFragment : BaseMwmRecyclerFragment<DownloaderAdapter?>(),
    OnBackPressListener {
    private var mToolbarController: DownloaderToolbarController? = null
    private var mBottomPanel: BottomPanel? = null
    private var mAdapter: DownloaderAdapter? = null
    private var mCurrentSearch: Long = 0
    private var mSearchRunning = false
    private var mSubscriberSlot = 0
    private val mScrollListener: RecyclerView.OnScrollListener =
        object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(
                recyclerView: RecyclerView,
                newState: Int
            ) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) mToolbarController!!.deactivate()
            }
        }
    private val mSearchListener: NativeMapSearchListener = object : NativeMapSearchListener {
        override fun onMapSearchResults(
            results: Array<NativeMapSearchListener.Result>?,
            timestamp: Long,
            isLast: Boolean
        ) {
            if (!mSearchRunning || timestamp != mCurrentSearch) return
            val rs: MutableList<CountryItem> = ArrayList()
            for (result in results!!) {
                val item: CountryItem = CountryItem.Companion.fill(result!!.countryId)
                item.searchResultName = result.matchedString
                rs.add(item)
            }
            if (mAdapter != null) mAdapter!!.setSearchResultsMode(rs, mToolbarController!!.query)
            if (isLast) onSearchEnd()
        }
    }

    fun shouldShowSearch(): Boolean {
        return CountryItem.Companion.isRoot(currentRoot)
    }

    fun startSearch() {
        mSearchRunning = true
        mCurrentSearch = System.nanoTime()
        SearchEngine.searchMaps(mToolbarController!!.query, mCurrentSearch)
        mToolbarController!!.showProgress(true)
        if (mAdapter != null) mAdapter!!.clearAdsAndCancelMyTarget()
    }

    fun clearSearchQuery() {
        mToolbarController!!.clear()
    }

    fun cancelSearch() {
        if (mAdapter == null || !mAdapter!!.isSearchResultsMode) return
        mAdapter!!.resetSearchResultsMode()
        onSearchEnd()
    }

    private fun onSearchEnd() {
        mSearchRunning = false
        mToolbarController!!.showProgress(false)
        update()
    }

    fun update() {
        mToolbarController!!.update()
        mBottomPanel!!.update()
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity!!.window
            .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        mSubscriberSlot = MapManager.nativeSubscribe(object : StorageCallback {
            override fun onStatusChanged(data: List<StorageCallbackData>) {
                if (isAdded) update()
            }

            override fun onProgress(
                countryId: String,
                localSize: Long,
                remoteSize: Long
            ) {
            }
        })
        SearchEngine.INSTANCE.addMapListener(mSearchListener)
        recyclerView.addOnScrollListener(mScrollListener)
        if (mAdapter != null) {
            mAdapter!!.refreshData()
            mAdapter!!.attach()
        }
        mBottomPanel = BottomPanel(this, view)
        mToolbarController = DownloaderToolbarController(view, activity, this)
        update()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (mAdapter != null) mAdapter!!.detach()
        mAdapter = null
        if (mSubscriberSlot != 0) {
            MapManager.nativeUnsubscribe(mSubscriberSlot)
            mSubscriberSlot = 0
        }
        SearchEngine.INSTANCE.removeMapListener(mSearchListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        recyclerView.removeOnScrollListener(mScrollListener)
    }

    override fun onBackPressed(): Boolean {
        if (mToolbarController!!.hasQuery()) {
            mToolbarController!!.clear()
            return true
        }
        return mAdapter != null && mAdapter!!.goUpwards()
    }

    override val layoutRes: Int
        get() = R.layout.fragment_downloader

    override fun createAdapter(): DownloaderAdapter {
        if (mAdapter == null) mAdapter = DownloaderAdapter(this)
        return mAdapter!!
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        mToolbarController!!.onActivityResult(requestCode, resultCode, data)
    }

    val currentRoot: String
        get() = if (mAdapter != null) mAdapter!!.currentRootId else ""

    override fun setupPlaceholder(placeholder: PlaceholderView?) {
        if (placeholder == null) return
        if (mAdapter != null && mAdapter!!.isSearchResultsMode) placeholder.setContent(
            R.drawable.img_search_nothing_found_light,
            R.string.search_not_found, R.string.search_not_found_query
        ) else placeholder.setContent(
            R.drawable.img_search_no_maps,
            R.string.downloader_no_downloaded_maps_title,
            R.string.downloader_no_downloaded_maps_message
        )
    }
}