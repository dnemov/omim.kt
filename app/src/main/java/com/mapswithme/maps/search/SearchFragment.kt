package com.mapswithme.maps.search

import android.app.Activity
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.viewpager.widget.ViewPager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.tabs.TabLayout
import com.mapswithme.maps.Framework
import com.mapswithme.maps.MwmActivity
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmFragment
import com.mapswithme.maps.base.OnBackPressListener
import com.mapswithme.maps.bookmarks.data.FeatureId
import com.mapswithme.maps.bookmarks.data.MapObject
import com.mapswithme.maps.bookmarks.data.MapObject.Companion.createMapObject
import com.mapswithme.maps.downloader.CountrySuggestFragment
import com.mapswithme.maps.downloader.MapManager.nativeGetDownloadedCount
import com.mapswithme.maps.downloader.MapManager.nativeIsDownloading
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.location.LocationListener
import com.mapswithme.maps.routing.RoutingController.Companion.get
import com.mapswithme.maps.search.CategoriesAdapter.CategoriesUiListener
import com.mapswithme.maps.search.FilterActivity.Companion.startForResult
import com.mapswithme.maps.search.HiddenCommand.BaseHiddenCommand
import com.mapswithme.maps.search.SearchFilterController.DefaultFilterListener
import com.mapswithme.maps.search.SearchFragment
import com.mapswithme.maps.widget.PlaceholderView
import com.mapswithme.maps.widget.SearchToolbarController
import com.mapswithme.util.Language
import com.mapswithme.util.SharedPropertiesUtils
import com.mapswithme.util.UiUtils
import com.mapswithme.util.Utils
import com.mapswithme.util.statistics.Statistics
import java.util.*

class SearchFragment : BaseMwmFragment(), OnBackPressListener, NativeSearchListener,
    SearchToolbarController.Container, CategoriesUiListener, HotelsFilterHolder,
    NativeBookingFilterListener {
    private var mLastQueryTimestamp: Long = 0
    private val mHiddenCommands: MutableList<HiddenCommand> =
        ArrayList()

    private class LastPosition {
        var lat = 0.0
        var lon = 0.0
        var valid = false
        operator fun set(lat: Double, lon: Double) {
            this.lat = lat
            this.lon = lon
            valid = true
        }
    }

    private inner class ToolbarController(root: View) :
        SearchToolbarController(root, this@SearchFragment.activity) {
        override fun useExtendedToolbar(): Boolean {
            return false
        }

        override fun onTextChanged(query: String) {
            if (!isAdded) return
            if (TextUtils.isEmpty(query)) {
                mSearchAdapter.clear()
                stopSearch()
                return
            }
            if (tryRecognizeHiddenCommand(query)) {
                mSearchAdapter.clear()
                stopSearch()
                closeSearch()
                return
            }
            runSearch()
        }

        override fun onStartSearchClick(): Boolean {
            if (!get().isWaitingPoiPick) showAllResultsOnMap()
            return true
        }

        override val voiceInputPrompt: Int
            get() = R.string.search_map


        override fun startVoiceRecognition(intent: Intent?, code: Int) {
            startActivityForResult(intent, code)
        }

        override fun supportsVoiceSearch(): Boolean {
            return true
        }

        override fun onUpClick() {
            if (!onBackPressed()) super.onUpClick()
        }

        override fun clear() {
            super.clear()
            if (mFilterController != null) mFilterController!!.resetFilter()
        }
    }

    private var mTabFrame: View? = null
    private var mResultsFrame: View? = null
    private var mResultsPlaceholder: PlaceholderView? = null
    private var mResults: RecyclerView? = null
    private var mAppBarLayout: AppBarLayout? = null
    private var mToolbarLayout: CollapsingToolbarLayout? = null
    private var mFilterController: SearchFilterController? = null
    private var mToolbarController: SearchToolbarController? = null
    private lateinit var mSearchAdapter: SearchAdapter
    private val mAttachedRecyclers: MutableList<RecyclerView?> =
        ArrayList()
    private val mRecyclerListener: RecyclerView.OnScrollListener =
        object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(
                recyclerView: RecyclerView,
                newState: Int
            ) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) mToolbarController!!.deactivate()
            }
        }
    private val mLastPosition = LastPosition()
    private var mSearchRunning = false
    private var mInitialQuery: String? = null
    private var mInitialLocale: String? = null
    private var mInitialSearchOnMap = false
    private var mInitialHotelsFilter: HotelsFilter? = null
    private var mInitialFilterParams: BookingFilterParams? = null
    private val mLocationListener: LocationListener =
        object : LocationListener.Simple() {
            override fun onLocationUpdated(location: Location) {
                mLastPosition[location.latitude] = location.longitude
                if (!TextUtils.isEmpty(query)) mSearchAdapter.notifyDataSetChanged()
            }
        }
    private val mOffsetListener = OnOffsetChangedListener { appBarLayout, verticalOffset ->
        if (mFilterController == null) return@OnOffsetChangedListener
        val show =
            Math.abs(verticalOffset) != appBarLayout.totalScrollRange
        mFilterController!!.showDivider(show)
    }

    override val hotelsFilter: HotelsFilter?
        get() = if (mFilterController == null) null else mFilterController?.filter

    override val filterParams: BookingFilterParams?
        get() = if (mFilterController == null) null else mFilterController?.bookingFilterParams

    private fun showDownloadSuggest() {
        val fm = childFragmentManager
        val fragmentName = CountrySuggestFragment::class.java.name
        var fragment = fm.findFragmentByTag(fragmentName)
        if (fragment == null || fragment.isDetached || fragment.isRemoving) {
            fragment = instantiate(activity!!, fragmentName, null)
            fm.beginTransaction()
                .add(R.id.download_suggest_frame, fragment, fragmentName)
                .commit()
        }
    }

    private fun hideDownloadSuggest() {
        if (!isAdded) return
        val manager = childFragmentManager
        val fragment = manager.findFragmentByTag(
            CountrySuggestFragment::class.java.name
        )
        if (fragment != null && !fragment.isDetached && !fragment.isRemoving) manager.beginTransaction()
            .remove(fragment)
            .commitAllowingStateLoss()
    }

    private fun updateFrames() {
        val hasQuery = mToolbarController!!.hasQuery()
        UiUtils.showIf(hasQuery, mResultsFrame)
        val lp =
            mToolbarLayout!!.layoutParams as AppBarLayout.LayoutParams
        lp.scrollFlags = if (hasQuery) AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                else 0
        mToolbarLayout!!.layoutParams = lp
        if (mFilterController != null) mFilterController!!.show(
            hasQuery && mSearchAdapter.itemCount != 0,
            mSearchAdapter.showPopulateButton()
        )
        if (hasQuery) hideDownloadSuggest() else if (doShowDownloadSuggest()) showDownloadSuggest() else hideDownloadSuggest()
    }

    private fun updateResultsPlaceholder() {
        val show = (!mSearchRunning
                && mSearchAdapter.itemCount == 0 && mToolbarController!!.hasQuery())
        UiUtils.showIf(show, mResultsPlaceholder)
        if (mFilterController != null) mFilterController!!.showPopulateButton(mSearchAdapter.showPopulateButton() && !isTabletSearch)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    @CallSuper
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        mSearchAdapter = SearchAdapter(this)
        readArguments()
        val root = view as ViewGroup
        mAppBarLayout = root.findViewById(R.id.app_bar)
        mToolbarLayout = mAppBarLayout?.findViewById(R.id.collapsing_toolbar)
        mTabFrame = root.findViewById(R.id.tab_frame)
        val pager: ViewPager = mTabFrame!!.findViewById(R.id.pages)
        mToolbarController = ToolbarController(view)
        val tabLayout: TabLayout = root.findViewById(R.id.tabs)
        val tabAdapter =
            TabAdapter(childFragmentManager, pager, tabLayout)
        mResultsFrame = root.findViewById(R.id.results_frame)
        mResults = mResultsFrame?.findViewById(R.id.recycler)
        setRecyclerScrollListener(mResults)
        mResultsPlaceholder = mResultsFrame?.findViewById(R.id.placeholder)
        mResultsPlaceholder?.setContent(
            R.drawable.img_search_nothing_found_light,
            R.string.search_not_found, R.string.search_not_found_query
        )
        mFilterController =
            SearchFilterController(root.findViewById(R.id.filter_frame),
                object : DefaultFilterListener() {
                    override fun onShowOnMapClick() {
                        showAllResultsOnMap()
                    }

                    override fun onFilterClick() {
                        var filter: HotelsFilter? = null
                        var params: BookingFilterParams? = null
                        if (mFilterController != null) {
                            filter = mFilterController?.filter
                            params = mFilterController?.bookingFilterParams
                        }
                        startForResult(
                            this@SearchFragment, filter, params,
                            FilterActivity.REQ_CODE_FILTER
                        )
                    }

                    override fun onFilterClear() {
                        runSearch()
                    }
                })
        if (savedInstanceState != null) mFilterController!!.onRestoreState(savedInstanceState)
        if (mInitialHotelsFilter != null || mInitialFilterParams != null) mFilterController!!.setFilterAndParams(
            mInitialHotelsFilter,
            mInitialFilterParams
        )
        mFilterController!!.updateFilterButtonVisibility(mInitialFilterParams != null)
        mSearchAdapter.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onChanged() {
                updateResultsPlaceholder()
            }
        })
        mResults?.setLayoutManager(LinearLayoutManager(view.getContext()))
        mResults?.setAdapter(mSearchAdapter)
        updateFrames()
        updateResultsPlaceholder()
        if (mInitialQuery != null) {
            query = mInitialQuery
        }
        mToolbarController?.activate()
        SearchEngine.INSTANCE.addListener(this)
        if (SearchRecents.size == 0) pager.currentItem = TabAdapter.Tab.CATEGORIES.ordinal
        tabAdapter.setTabSelectedListener(object : TabAdapter.OnTabSelectedListener{
            override fun onTabSelected(tab: TabAdapter.Tab) {
                Statistics.INSTANCE.trackSearchTabSelected(tab.name)
                mToolbarController?.deactivate()
            }

        })
        if (mInitialSearchOnMap) showAllResultsOnMap()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (mFilterController != null) mFilterController!!.onSaveState(outState)
    }

    override fun onResume() {
        super.onResume()
        LocationHelper.INSTANCE.addListener(mLocationListener, true)
        SearchEngine.INSTANCE.addHotelListener(this)
        mAppBarLayout!!.addOnOffsetChangedListener(mOffsetListener)
    }

    override fun onPause() {
        LocationHelper.INSTANCE.removeListener(mLocationListener)
        SearchEngine.INSTANCE.removeHotelListener(this)
        super.onPause()
        mAppBarLayout!!.removeOnOffsetChangedListener(mOffsetListener)
    }

    override fun onDestroy() {
        for (v in mAttachedRecyclers) v!!.removeOnScrollListener(mRecyclerListener)
        mAttachedRecyclers.clear()
        SearchEngine.INSTANCE.removeListener(this)
        super.onDestroy()
    }

    var query: String?
        get() = mToolbarController!!.query
        set(text) {
            mToolbarController!!.query = text.orEmpty()
        }

    private fun readArguments() {
        val arguments = arguments ?: return
        mInitialQuery = arguments.getString(SearchActivity.Companion.EXTRA_QUERY)
        mInitialLocale = arguments.getString(SearchActivity.Companion.EXTRA_LOCALE)
        mInitialSearchOnMap = arguments.getBoolean(SearchActivity.Companion.EXTRA_SEARCH_ON_MAP)
        mInitialHotelsFilter = arguments.getParcelable(FilterActivity.EXTRA_FILTER)
        mInitialFilterParams = arguments.getParcelable(FilterActivity.EXTRA_FILTER_PARAMS)
    }

    private fun tryRecognizeHiddenCommand(query: String): Boolean {
        for (command in hiddenCommands) {
            if (command.execute(query)) return true
        }
        return false
    }

    private val hiddenCommands: List<HiddenCommand>
        private get() {
            if (mHiddenCommands.isEmpty()) {
                mHiddenCommands.addAll(
                    Arrays.asList(
                        BadStorageCommand("?emulateBadStorage"),
                        JavaCrashCommand("?emulateJavaCrash"),
                        NativeCrashCommand("?emulateNativeCrash")
                    )
                )
            }
            return mHiddenCommands
        }

    private fun processSelected(result: SearchResult) {
        if (get().isWaitingPoiPick) {
            val description =
                result.description
            val subtitle =
                if (description != null) Utils.getLocalizedFeatureType(
                    context!!,
                    description.featureType
                ) else ""
            val title = if (TextUtils.isEmpty(result.name)) subtitle else ""
            val point = createMapObject(
                FeatureId.EMPTY, MapObject.SEARCH,
                title, subtitle, result.lat, result.lon
            )
            get().onPoiSelected(point)
        }
        if (mFilterController != null) mFilterController!!.resetFilter()
        mToolbarController!!.deactivate()
        if (activity is SearchActivity) Utils.navigateToParent(activity)
    }

    fun showSingleResultOnMap(
        result: SearchResult,
        resultIndex: Int
    ) {
        val query = query!!
        SearchRecents.add(query)
        SearchEngine.INSTANCE.cancel()
        if (!get().isWaitingPoiPick) SearchEngine.INSTANCE.showResult(resultIndex)
        processSelected(result)
        Statistics.INSTANCE.trackEvent(Statistics.EventName.SEARCH_ITEM_CLICKED)
    }

    fun showAllResultsOnMap() { // The previous search should be cancelled before the new one is started, since previous search
// results are no longer needed.
        SearchEngine.INSTANCE.cancel()
        val query = query!!
        SearchRecents.add(query)
        mLastQueryTimestamp = System.nanoTime()
        var hotelsFilter: HotelsFilter? = null
        var bookingFilterParams: BookingFilterParams? = null
        if (mFilterController != null) {
            hotelsFilter = mFilterController?.filter
            bookingFilterParams = mFilterController?.bookingFilterParams
        }
        SearchEngine.INSTANCE.searchInteractive(
            query,
            if (!TextUtils.isEmpty(mInitialLocale)) mInitialLocale!! else Language.keyboardLocale,
            mLastQueryTimestamp,
            false /* isMapAndTable */,
            hotelsFilter,
            bookingFilterParams
        )
        SearchEngine.INSTANCE.query = query
        Utils.navigateToParent(activity)
        Statistics.INSTANCE.trackEvent(Statistics.EventName.SEARCH_ON_MAP_CLICKED)
    }

    private fun onSearchEnd() {
        if (mSearchRunning && isAdded) updateSearchView()
    }

    private fun updateSearchView() {
        mSearchRunning = false
        mToolbarController!!.showProgress(false)
        updateFrames()
        updateResultsPlaceholder()
    }

    private fun stopSearch() {
        SearchEngine.INSTANCE.cancel()
        updateSearchView()
    }

    // TODO @yunitsky Implement more elegant solution.
    private val isTabletSearch: Boolean
        get() =// TODO @yunitsky Implement more elegant solution.
            activity is MwmActivity

    private fun runSearch() { // The previous search should be cancelled before the new one is started, since previous search
// results are no longer needed.
        SearchEngine.INSTANCE.cancel()
        var hotelsFilter: HotelsFilter? = null
        var bookingFilterParams: BookingFilterParams? = null
        if (mFilterController != null) {
            hotelsFilter = mFilterController?.filter
            bookingFilterParams = mFilterController?.bookingFilterParams
        }
        mLastQueryTimestamp = System.nanoTime()
        if (isTabletSearch) {
            SearchEngine.INSTANCE.searchInteractive(
                query!!, mLastQueryTimestamp, true /* isMapAndTable */,
                hotelsFilter, bookingFilterParams
            )
        } else {
            if (!SearchEngine.INSTANCE.search(
                    query!!, mLastQueryTimestamp, mLastPosition.valid,
                    mLastPosition.lat, mLastPosition.lon,
                    hotelsFilter, bookingFilterParams
                )
            ) {
                return
            }
        }
        mSearchRunning = true
        mToolbarController!!.showProgress(true)
        updateFrames()
    }

    override fun onResultsUpdate(results: Array<SearchResult>?, timestamp: Long, isHotel: Boolean) {
        if (!isAdded || !mToolbarController!!.hasQuery()) return
        refreshSearchResults(isHotel, results)
    }

    override fun onResultsEnd(timestamp: Long) {
        onSearchEnd()
    }

    override fun onFilterHotels(
        @BookingFilter.Type type: Int, hotels: Array<FeatureId>?
    ) {
        if (hotels == null) return
        mSearchAdapter.setFilteredHotels(type, hotels)
    }

    override fun onSearchCategorySelected(category: String) {
        mToolbarController!!.query = category.orEmpty()
    }

    override fun onPromoCategorySelected(promo: PromoCategory) { // Do nothing by default.
    }

    override fun onAdsRemovalSelected() { // Do nothing by default.
    }

    private fun refreshSearchResults(
        isHotel: Boolean,
        results: Array<SearchResult>?
    ) {
        mSearchRunning = true
        updateFrames()
        mSearchAdapter.refreshData(results)
        mToolbarController!!.showProgress(true)
        updateFilterButton(isHotel)
    }

    private fun updateFilterButton(isHotel: Boolean) {
        if (mFilterController != null) {
            mFilterController!!.updateFilterButtonVisibility(isHotel)
            if (!isHotel) mFilterController!!.setFilterAndParams(null, null)
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        mToolbarController!!.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return
        when (requestCode) {
            FilterActivity.REQ_CODE_FILTER -> {
                data?.let {
                    if (mFilterController == null) return
                    mFilterController!!.setFilterAndParams(
                        data.getParcelableExtra(FilterActivity.EXTRA_FILTER),
                        data.getParcelableExtra(FilterActivity.EXTRA_FILTER_PARAMS)
                    )
                    runSearch()
                }
            }
        }
    }

    override fun onBackPressed(): Boolean {
        if (mToolbarController!!.hasQuery()) {
            mToolbarController!!.clear()
            return true
        }
        val isSearchActivity = activity is SearchActivity
        mToolbarController!!.deactivate()
        if (get().isWaitingPoiPick) {
            get().onPoiSelected(null)
            if (isSearchActivity) closeSearch()
            return !isSearchActivity
        }
        if (isSearchActivity) closeSearch()
        return isSearchActivity
    }

    private fun closeSearch() {
        activity!!.finish()
        activity!!.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    fun setRecyclerScrollListener(recycler: RecyclerView?) {
        recycler!!.addOnScrollListener(mRecyclerListener)
        mAttachedRecyclers.add(recycler)
    }

    override val controller: SearchToolbarController
        get() = mToolbarController!!

    private class BadStorageCommand(command: String) :
        BaseHiddenCommand(command) {
        override fun executeInternal() {
            SharedPropertiesUtils.setShouldShowEmulateBadStorageSetting(true)
        }
    }

    private class JavaCrashCommand(command: String) :
        BaseHiddenCommand(command) {
        override fun executeInternal() {
            throw RuntimeException("Diagnostic java crash!")
        }
    }

    private class NativeCrashCommand(command: String) :
        BaseHiddenCommand(command) {
        override fun executeInternal() {
            Framework.nativeMakeCrash()
        }
    }

    companion object {
        private fun doShowDownloadSuggest(): Boolean {
            return nativeGetDownloadedCount() == 0 && !nativeIsDownloading()
        }
    }
}