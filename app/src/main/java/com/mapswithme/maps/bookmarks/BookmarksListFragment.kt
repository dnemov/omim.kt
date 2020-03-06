package com.mapswithme.maps.bookmarks

import android.app.Activity
import android.content.Intent
import android.location.Location
import android.os.Bundle
import androidx.annotation.CallSuper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup

import com.cocosw.bottomsheet.BottomSheet
import com.crashlytics.android.Crashlytics
import com.mapswithme.maps.MwmActivity
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmRecyclerFragment
import com.mapswithme.maps.bookmarks.data.BookmarkCategory
import com.mapswithme.maps.bookmarks.data.BookmarkInfo
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.bookmarks.data.BookmarkSharingResult
import com.mapswithme.maps.bookmarks.data.CategoryDataSource
import com.mapswithme.maps.bookmarks.data.SortedBlock
import com.mapswithme.maps.bookmarks.data.Track
import com.mapswithme.maps.intent.Factory
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.search.NativeBookmarkSearchListener
import com.mapswithme.maps.search.SearchEngine
import com.mapswithme.maps.ugc.routes.BaseUgcRouteActivity
import com.mapswithme.maps.ugc.routes.UgcRouteEditSettingsActivity
import com.mapswithme.maps.ugc.routes.UgcRouteSharingOptionsActivity
import com.mapswithme.maps.widget.SearchToolbarController
import com.mapswithme.maps.widget.placepage.EditBookmarkFragment
import com.mapswithme.maps.widget.placepage.Sponsored
import com.mapswithme.maps.widget.recycler.ItemDecoratorFactory
import com.mapswithme.maps.widget.recycler.RecyclerClickListener
import com.mapswithme.maps.widget.recycler.RecyclerLongClickListener
import com.mapswithme.util.BottomSheetHelper
import com.mapswithme.util.UiUtils
import com.mapswithme.util.sharing.ShareOption
import com.mapswithme.util.sharing.SharingHelper
import com.mapswithme.util.statistics.Statistics

class BookmarksListFragment : BaseMwmRecyclerFragment<BookmarkListAdapter>(), BookmarkManager.BookmarksSharingListener,
    BookmarkManager.BookmarksSortingListener, NativeBookmarkSearchListener,
    ChooseBookmarksSortingTypeFragment.ChooseSortingTypeListener {

    private lateinit var mToolbarController: SearchToolbarController

    private var mLastQueryTimestamp: Long = 0
    private var mLastSortTimestamp: Long = 0

    private lateinit var mCategoryDataSource: CategoryDataSource
    private var mSelectedPosition: Int = 0

    private var mSearchMode = false
    private var mNeedUpdateSorting = true

    private lateinit var mSearchContainer: ViewGroup
    private lateinit var mFabViewOnMap: FloatingActionButton

    private lateinit var mCatalogListener: BookmarkManager.BookmarksCatalogListener

    private val mRecyclerListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_DRAGGING)
                mToolbarController.deactivate()
        }
    }

    private val categoryOrThrow: BookmarkCategory
        get() {
            val args = arguments

            val category: BookmarkCategory? = args?.getParcelable(EXTRA_CATEGORY)
            require(
                !(args == null || category == null)
            ) { "Category not exist in bundle" }

            return category
        }

    private val availableSortingTypes: IntArray
        @BookmarkManager.SortingType
        get() {
            val catId = mCategoryDataSource.data.id
            val loc = LocationHelper.INSTANCE.savedLocation
            val hasMyPosition = loc != null
            return BookmarkManager.INSTANCE.getAvailableSortingTypes(catId, hasMyPosition)
        }

    private val lastSortingType: Int
        get() {
            val catId = mCategoryDataSource.data.id
            return if (BookmarkManager.INSTANCE.hasLastSortingType(catId)) BookmarkManager.INSTANCE.getLastSortingType(
                catId
            ) else -1
        }

    private val lastAvailableSortingType: Int
        get() {
            val currentType = lastSortingType
            @BookmarkManager.SortingType val types = availableSortingTypes
            for (type in types) {
                if (type == currentType)
                    return currentType
            }
            return -1
        }

    private val isEmpty: Boolean
        get() = !adapter!!.isSearchResults && adapter!!.itemCount == 0

    private val isEmptySearchResults: Boolean
        get() = adapter!!.isSearchResults && adapter!!.itemCount == 0

    private val isDownloadedCategory: Boolean
        get() {
            val category = mCategoryDataSource.data
            return category.type === BookmarkCategory.Type.DOWNLOADED
        }

    private val isSearchAllowed: Boolean
        get() {
            val category = mCategoryDataSource.data
            return BookmarkManager.INSTANCE.isSearchAllowed(category.id)
        }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Crashlytics.log("onCreate")

        val category = categoryOrThrow
        mCategoryDataSource = CategoryDataSource(category)
        mCatalogListener = CatalogListenerDecorator(this)
    }

    override fun createAdapter(): BookmarkListAdapter {
        return BookmarkListAdapter(mCategoryDataSource)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_bookmark_list, container, false)
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Crashlytics.log("onViewCreated")

        configureAdapter()
        configureFab(view)

        setHasOptionsMenu(true)

        val bar = (requireActivity() as AppCompatActivity).supportActionBar
        bar?.setTitle(mCategoryDataSource.data.name)

        val toolbar = (requireActivity() as AppCompatActivity).findViewById<ViewGroup>(R.id.toolbar)
        mSearchContainer = toolbar.findViewById(R.id.frame)
        mToolbarController = BookmarksToolbarController(toolbar, requireActivity(), this)
        mToolbarController.setHint(R.string.search_in_the_list)
        configureRecycler()
    }

    override fun onStart() {
        super.onStart()
        //Crashlytics.log("onStart")
        SearchEngine.INSTANCE.addBookmarkListener(this)
        BookmarkManager.INSTANCE.addSortingListener(this)
        BookmarkManager.INSTANCE.addSharingListener(this)
        BookmarkManager.INSTANCE.addCatalogListener(mCatalogListener)
    }

    override fun onResume() {
        super.onResume()
        //Crashlytics.log("onResume")
        val adapter = adapter
        adapter!!.notifyDataSetChanged()
        updateSorting()
        updateSearchVisibility()
        updateRecyclerVisibility()
    }

    override fun onPause() {
        super.onPause()
        //Crashlytics.log("onPause")
    }

    override fun onStop() {
        super.onStop()
        //Crashlytics.log("onStop")
        SearchEngine.INSTANCE.removeBookmarkListener(this)
        BookmarkManager.INSTANCE.removeSortingListener(this)
        BookmarkManager.INSTANCE.removeSharingListener(this)
        BookmarkManager.INSTANCE.removeCatalogListener(mCatalogListener)
    }

    private fun configureAdapter() {
        val adapter = adapter
        adapter!!.registerAdapterDataObserver(mCategoryDataSource)
        adapter.setOnClickListener(object : RecyclerClickListener {
            override fun onItemClick(v: View?, position: Int) {
                onItemClick(position)
            }

        })
        adapter.setOnLongClickListener(object : RecyclerLongClickListener {
            override fun onLongItemClick(v: View?, position: Int) {
                onItemMore(position.toInt())
            }

        })

        adapter.setMoreListener(object : RecyclerClickListener {
            override fun onItemClick(v: View?, position: Int) {
                onItemMore(position)
            }
        })
    }

    private fun configureFab(view: View) {
        mFabViewOnMap = view.findViewById(R.id.fabViewOnMap)
        mFabViewOnMap.setOnClickListener { v ->
            val i = makeMwmActivityIntent()
            i.putExtra(
                MwmActivity.EXTRA_TASK,
                Factory.ShowBookmarkCategoryTask(mCategoryDataSource.data.id)
            )
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(i)
        }
    }

    private fun configureRecycler() {
        val decor = ItemDecoratorFactory
            .createDefaultDecorator(requireContext(), LinearLayoutManager.VERTICAL)
        recyclerView.addItemDecoration(decor)
        recyclerView.addOnScrollListener(mRecyclerListener)
    }

    private fun updateRecyclerVisibility() {
        if (isEmptySearchResults) {
            requirePlaceholder()!!.setContent(
                R.drawable.img_search_nothing_found_light,
                R.string.search_not_found,
                R.string.search_not_found_query
            )
        } else if (isEmpty) {
            requirePlaceholder()!!.setContent(
                R.drawable.img_empty_bookmarks,
                R.string.bookmarks_empty_list_title,
                R.string.bookmarks_empty_list_message
            )
        }

        val isEmptyRecycler = isEmpty || isEmptySearchResults

        showPlaceholder(isEmptyRecycler)
        UiUtils.showIf(!isEmptyRecycler, recyclerView, mFabViewOnMap)
        requireActivity().invalidateOptionsMenu()
    }

    private fun updateSearchVisibility() {
        if (!isSearchAllowed || isEmpty) {
            UiUtils.hide(mSearchContainer)
        } else {
            UiUtils.showIf(mSearchMode, mSearchContainer)
            if (mSearchMode)
                mToolbarController.activate()
            else
                mToolbarController.deactivate()
        }
        requireActivity().invalidateOptionsMenu()
    }

    fun runSearch(query: String) {
        SearchEngine.INSTANCE.cancel()

        mLastQueryTimestamp = System.nanoTime()
        if (SearchEngine.INSTANCE.searchInBookmarks(
                query,
                mCategoryDataSource.data.id,
                mLastQueryTimestamp
            )
        ) {
            mToolbarController.showProgress(true)
        }
    }

    override fun onBookmarkSearchResultsUpdate(bookmarkIds: LongArray?, timestamp: Long) {
        if (!isAdded || !mToolbarController.hasQuery() || mLastQueryTimestamp != timestamp)
            return
        updateSearchResults(bookmarkIds)
    }

    override fun onBookmarkSearchResultsEnd(bookmarkIds: LongArray?, timestamp: Long) {
        if (!isAdded || !mToolbarController.hasQuery() || mLastQueryTimestamp != timestamp)
            return
        trackBookmarksSearch()
        mLastQueryTimestamp = 0
        mToolbarController.showProgress(false)
        updateSearchResults(bookmarkIds)
    }

    private fun updateSearchResults(bookmarkIds: LongArray?) {
        val adapter = adapter
        adapter!!.setSearchResults(bookmarkIds)
        adapter.notifyDataSetChanged()
        updateRecyclerVisibility()
    }

    fun cancelSearch() {
        mLastQueryTimestamp = 0
        SearchEngine.INSTANCE.cancel()
        mToolbarController.showProgress(false)
        updateSearchResults(null)
        updateSorting()
    }

    fun activateSearch() {
        mSearchMode = true
        BookmarkManager.INSTANCE.setNotificationsEnabled(true)
        BookmarkManager.INSTANCE.prepareForSearch(mCategoryDataSource.data.id)
        updateSearchVisibility()
    }

    fun deactivateSearch() {
        mSearchMode = false
        BookmarkManager.INSTANCE.setNotificationsEnabled(false)
        updateSearchVisibility()
    }

    override fun onBookmarksSortingCompleted(sortedBlocks: Array<SortedBlock>, timestamp: Long) {
        if (mLastSortTimestamp != timestamp)
            return
        mLastSortTimestamp = 0

        val adapter = adapter
        adapter!!.setSortedResults(sortedBlocks)
        adapter.notifyDataSetChanged()

        updateSortingProgressBar()
    }

    override fun onBookmarksSortingCancelled(timestamp: Long) {
        if (mLastSortTimestamp != timestamp)
            return
        mLastSortTimestamp = 0

        val adapter = adapter
        adapter!!.setSortedResults(null)
        adapter.notifyDataSetChanged()

        updateSortingProgressBar()
    }

    override fun onSort(@BookmarkManager.SortingType sortingType: Int) {
        mLastSortTimestamp = System.nanoTime()

        val loc = LocationHelper.INSTANCE.savedLocation
        val hasMyPosition = loc != null
        if (!hasMyPosition && sortingType == BookmarkManager.SORT_BY_DISTANCE)
            return

        val catId = mCategoryDataSource.data.id
        val lat = if (hasMyPosition) loc!!.latitude else 0.0
        val lon = if (hasMyPosition) loc!!.longitude else 0.0

        BookmarkManager.INSTANCE.setLastSortingType(catId, sortingType)
        BookmarkManager.INSTANCE.getSortedCategory(
            catId, sortingType, hasMyPosition, lat, lon,
            mLastSortTimestamp
        )

        updateSortingProgressBar()
    }

    override fun onResetSorting() {
        mLastSortTimestamp = 0
        val catId = mCategoryDataSource.data.id
        BookmarkManager.INSTANCE.resetLastSortingType(catId)

        val adapter = adapter
        adapter!!.setSortedResults(null)
        adapter.notifyDataSetChanged()
    }

    private fun updateSorting() {
        if (!mNeedUpdateSorting)
            return
        mNeedUpdateSorting = false

        // Do nothing in case of sorting has already started and we are waiting for results.
        if (mLastSortTimestamp != 0L)
            return

        val catId = mCategoryDataSource.data.id
        if (!BookmarkManager.INSTANCE.hasLastSortingType(catId))
            return

        val currentType = lastAvailableSortingType
        if (currentType >= 0)
            onSort(currentType)
    }

    private fun forceUpdateSorting() {
        mLastSortTimestamp = 0
        mNeedUpdateSorting = true
        updateSorting()
    }

    private fun resetSearchAndSort() {
        val adapter = adapter
        adapter!!.setSortedResults(null)
        adapter.setSearchResults(null)
        adapter.notifyDataSetChanged()

        if (mSearchMode) {
            cancelSearch()
            deactivateSearch()
        }
        forceUpdateSorting()
        updateRecyclerVisibility()
    }

    private fun updateSortingProgressBar() {
        requireActivity().invalidateOptionsMenu()
    }

    fun onItemClick(position: Int) {
        val intent = makeMwmActivityIntent()

        val adapter = adapter

        when (adapter!!.getItemViewType(position)) {
            BookmarkListAdapter.TYPE_SECTION, BookmarkListAdapter.TYPE_DESC -> return

            BookmarkListAdapter.TYPE_BOOKMARK -> onBookmarkClicked(position, intent, adapter)

            BookmarkListAdapter.TYPE_TRACK -> onTrackClicked(position, intent, adapter)
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    private fun makeMwmActivityIntent(): Intent {
        return Intent(requireActivity(), MwmActivity::class.java)
    }

    private fun onTrackClicked(position: Int, i: Intent, adapter: BookmarkListAdapter) {
        val track = adapter.getItem(position) as Track
        i.putExtra(
            MwmActivity.EXTRA_TASK,
            Factory.ShowTrackTask(track.categoryId, track.trackId)
        )
        if (BookmarkManager.INSTANCE.isGuide(mCategoryDataSource.data))
            Statistics.INSTANCE.trackGuideTrackSelect(mCategoryDataSource.data.serverId)
    }

    private fun onBookmarkClicked(
        position: Int, i: Intent,
        adapter: BookmarkListAdapter
    ) {
        if (adapter.isSearchResults)
            trackBookmarksSearchResultSelected()

        val bookmark = adapter.getItem(position) as BookmarkInfo
        i.putExtra(
            MwmActivity.EXTRA_TASK,
            Factory.ShowBookmarkTask(bookmark.categoryId, bookmark.bookmarkId)
        )

        if (BookmarkManager.INSTANCE.isGuide(mCategoryDataSource.data))
            Statistics.INSTANCE.trackGuideBookmarkSelect(mCategoryDataSource.data.serverId)
    }

    fun onItemMore(position: Int) {
        val adapter = adapter

        mSelectedPosition = position
        val type = adapter!!.getItemViewType(mSelectedPosition)

        when (type) {
            BookmarkListAdapter.TYPE_SECTION, BookmarkListAdapter.TYPE_DESC -> {
            }

            BookmarkListAdapter.TYPE_BOOKMARK -> {
                val bookmark = adapter.getItem(mSelectedPosition) as BookmarkInfo
                val menuResId = if (isDownloadedCategory)
                    R.menu.menu_bookmarks_catalog
                else
                    R.menu.menu_bookmarks
                val bs = BottomSheetHelper.create(requireActivity(), bookmark.name)
                    .sheet(menuResId)
                    .listener(MenuItem.OnMenuItemClickListener { this.onBookmarkMenuItemClicked(it) })
                    .build()
                BottomSheetHelper.tint(bs)
                bs.show()
            }

            BookmarkListAdapter.TYPE_TRACK -> {
                val track = adapter.getItem(mSelectedPosition) as Track
                val bottomSheet = BottomSheetHelper
                    .create(requireActivity(), track.name)
                    .sheet(Menu.NONE, R.drawable.ic_delete, R.string.delete)
                    .listener { menuItem -> onTrackMenuItemClicked(track.trackId) }
                    .build()

                BottomSheetHelper.tint(bottomSheet)
                bottomSheet.show()
            }
        }// Do nothing here?
    }

    private fun onTrackMenuItemClicked(trackId: Long): Boolean {
        BookmarkManager.INSTANCE.deleteTrack(trackId)
        adapter!!.notifyDataSetChanged()
        return false
    }

    fun onBookmarkMenuItemClicked(menuItem: MenuItem): Boolean {
        val adapter = adapter
        val item = adapter!!.getItem(mSelectedPosition) as BookmarkInfo
        when (menuItem.itemId) {
            R.id.share -> ShareOption.AnyShareOption.ANY.shareBookmarkObject(
                requireActivity(), item,
                Sponsored.nativeGetCurrent()
            )

            R.id.edit -> EditBookmarkFragment.editBookmark(
                item.categoryId, item.bookmarkId, requireActivity(), childFragmentManager,
                object : EditBookmarkFragment.EditBookmarkListener {
                    override fun onBookmarkSaved(bookmarkId: Long, movedFromCategory: Boolean) {
                        if (movedFromCategory)
                            resetSearchAndSort()
                        else
                            adapter.notifyDataSetChanged()
                    }

                })

            R.id.delete -> {
                adapter.onDelete(mSelectedPosition)
                BookmarkManager.INSTANCE.deleteBookmark(item.bookmarkId)
                adapter.notifyDataSetChanged()
                if (mSearchMode)
                    mNeedUpdateSorting = true
                updateSearchVisibility()
                updateRecyclerVisibility()
            }
        }
        return false
    }

    fun onListMoreMenuItemClick(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.sort -> {
                ChooseBookmarksSortingTypeFragment.chooseSortingType(
                    availableSortingTypes,
                    lastSortingType, requireActivity(), childFragmentManager
                )
                return false
            }

            R.id.sharing_options -> {
                openSharingOptionsScreen()
                trackBookmarkListSharingOptions()
                return false
            }

            R.id.share_category -> {
                val catId = mCategoryDataSource.data.id
                SharingHelper.INSTANCE.prepareBookmarkCategoryForSharing(requireActivity(), catId)
                return false
            }

            R.id.settings -> {
                val intent = Intent(requireContext(), UgcRouteEditSettingsActivity::class.java).putExtra(
                    BaseUgcRouteActivity.EXTRA_BOOKMARK_CATEGORY,
                    mCategoryDataSource.data
                )
                startActivityForResult(intent, UgcRouteEditSettingsActivity.REQUEST_CODE)
                return false
            }

            R.id.delete_category -> {
                requireActivity().setResult(Activity.RESULT_OK)
                requireActivity().finish()
                return false
            }
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.option_menu_bookmarks, menu)

        val itemSearch = menu.findItem(R.id.bookmarks_search)
        itemSearch.isVisible = isSearchAllowed && !isEmpty

        val itemMore = menu.findItem(R.id.bookmarks_more)
        itemMore.isVisible = !isEmpty
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        val visible = !mSearchMode && !isEmpty
        val itemSearch = menu.findItem(R.id.bookmarks_search)
        itemSearch.isVisible = isSearchAllowed && visible

        val itemMore = menu.findItem(R.id.bookmarks_more)
        itemMore.isVisible = visible

        if (mLastSortTimestamp != 0L)
            itemMore.setActionView(R.layout.toolbar_menu_progressbar)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.bookmarks_search) {
            activateSearch()
            return true
        }

        if (item.itemId == R.id.bookmarks_more) {
            val bs = BottomSheetHelper.create(
                requireActivity(),
                mCategoryDataSource.data.name
            )
                .sheet(R.menu.menu_bookmarks_list)
                .listener(MenuItem.OnMenuItemClickListener { this.onListMoreMenuItemClick(it) })
                .build()

            if (isDownloadedCategory) {
                bs.menu.findItem(R.id.sharing_options).isVisible = false
                bs.menu.findItem(R.id.share_category).isVisible = false
                bs.menu.findItem(R.id.settings).isVisible = false
            }

            @BookmarkManager.SortingType val types = availableSortingTypes
            bs.menu.findItem(R.id.sort).isVisible = types.size > 0

            BottomSheetHelper.tint(bs)
            bs.show()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onPreparedFileForSharing(result: BookmarkSharingResult) {
        SharingHelper.INSTANCE.onPreparedFileForSharing(requireActivity(), result)
    }

    private fun openSharingOptionsScreen() {
        UgcRouteSharingOptionsActivity.startForResult(requireActivity(), mCategoryDataSource.data)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        adapter!!.notifyDataSetChanged()
        val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
        actionBar!!.setTitle(mCategoryDataSource.data.name)
    }

    companion object {
        val TAG = BookmarksListFragment::class.java.simpleName
        val EXTRA_CATEGORY = "bookmark_category"

        private fun trackBookmarkListSharingOptions() {
            Statistics.INSTANCE.trackBookmarkListSharingOptions()
        }

        private fun trackBookmarksSearch() {
            Statistics.INSTANCE.trackBookmarksListSearch()
        }

        private fun trackBookmarksSearchResultSelected() {
            Statistics.INSTANCE.trackBookmarksListSearchResultSelected()
        }
    }
}
