package com.mapswithme.maps.downloader

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.Framework
import com.mapswithme.maps.MwmActivity
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.maps.downloader.DownloaderAdapter.ViewHolderWrapper
import com.mapswithme.maps.downloader.MapManager.StorageCallback
import com.mapswithme.maps.downloader.MapManager.StorageCallbackData
import com.mapswithme.maps.downloader.MapManager.nativeCancel
import com.mapswithme.maps.intent.Factory.ShowCountryTask
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.routing.RoutingController
import com.mapswithme.util.BottomSheetHelper
import com.mapswithme.util.StringUtils
import com.mapswithme.util.ThemeUtils
import com.mapswithme.util.UiUtils
import com.mapswithme.util.statistics.MytargetHelper
import com.mapswithme.util.statistics.Statistics
import com.mapswithme.util.statistics.Statistics.EventName
import com.my.target.nativeads.banners.NativeAppwallBanner
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import java.util.*

class DownloaderAdapter(fragment: DownloaderFragment) :
    RecyclerView.Adapter<ViewHolderWrapper>(),
    StickyRecyclerHeadersAdapter<DownloaderAdapter.HeaderViewHolder> {
    val mRecycler: RecyclerView
    private lateinit var mActivity: Activity
    private val mFragment: DownloaderFragment
    private val mHeadersDecoration: StickyRecyclerHeadersDecoration
    var isMyMapsMode = true
        private set
    private var mAdsLoaded = false
    private var mAdsLoading = false
    private var mShowAds = false
    var isSearchResultsMode = false
        private set
    private var mSearchQuery: String? = null
    private val mItems: MutableList<CountryItem> =
        ArrayList()
    private val mCountryIndex: MutableMap<String?, CountryItem> =
        HashMap() // Country.id -> Country
    private val mAds: MutableList<NativeAppwallBanner> =
        ArrayList()
    private val mHeaders = SparseArray<String>()
    private val mPath =
        Stack<PathEntry>() // Holds navigation history. The last element is the current level.
    private var mNearMeCount = 0
    private var mListenerSlot = 0
    private var mMytargetHelper: MytargetHelper? = null

    init {
        mActivity = fragment.activity!!
        mFragment = fragment
        mRecycler = mFragment.recyclerView
        mHeadersDecoration = StickyRecyclerHeadersDecoration(this)
        mRecycler.addItemDecoration(mHeadersDecoration)
    }

    private enum class MenuItem(@field:DrawableRes @param:DrawableRes val icon: Int, @field:StringRes @param:StringRes val title: Int) {
        DOWNLOAD(R.drawable.ic_download, R.string.downloader_download_map) {
            override operator fun invoke(
                item: CountryItem?,
                adapter: DownloaderAdapter
            ) {
                MapManager.warn3gAndDownload(
                    adapter.mActivity,
                    item!!.id,
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
                                .add(
                                    "scenario",
                                    if (item.isExpandable) "download_group" else "download"
                                )
                        )
                    }
                )
            }
        },
        DELETE(R.drawable.ic_delete, R.string.delete) {
            private fun deleteNode(item: CountryItem?) {
                MapManager.nativeCancel(item!!.id)
                MapManager.nativeDelete(item.id)
                OnmapDownloader.setAutodownloadLocked(true)
                Statistics.INSTANCE.trackEvent(
                    EventName.DOWNLOADER_ACTION,
                    Statistics.params().add(
                        Statistics.EventParam.ACTION,
                        "delete"
                    )
                        .add(
                            Statistics.EventParam.FROM,
                            "downloader"
                        )
                        .add("scenario", if (item.isExpandable) "delete_group" else "delete")
                )
            }

            override operator fun invoke(
                item: CountryItem?,
                adapter: DownloaderAdapter
            ) {
                if (RoutingController.get().isNavigating) {
                    AlertDialog.Builder(adapter.mActivity!!)
                        .setTitle(R.string.downloader_delete_map)
                        .setMessage(R.string.downloader_delete_map_while_routing_dialog)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                    return
                }
                if (!MapManager.nativeHasUnsavedEditorChanges(item!!.id)) {
                    deleteNode(item, adapter)
                    return
                }
                AlertDialog.Builder(adapter.mActivity!!)
                    .setTitle(R.string.downloader_delete_map)
                    .setMessage(R.string.downloader_delete_map_dialog)
                    .setNegativeButton(android.R.string.no, null)
                    .setPositiveButton(
                        android.R.string.yes
                    ) { dialog, which -> deleteNode(item, adapter) }.show()
            }

            private fun deleteNode(item: CountryItem?, adapter: DownloaderAdapter) {
                if (adapter.mActivity is MwmActivity) {
                    (adapter.mActivity as MwmActivity?)!!.closePlacePage()
                }
                deleteNode(item)
            }
        },
        CANCEL(R.drawable.ic_cancel, R.string.cancel) {
            override operator fun invoke(
                item: CountryItem?,
                adapter: DownloaderAdapter
            ) {
                MapManager.nativeCancel(item!!.id)
                Statistics.INSTANCE.trackEvent(
                    EventName.DOWNLOADER_CANCEL,
                    Statistics.params().add(
                        Statistics.EventParam.FROM,
                        "downloader"
                    )
                )
            }
        },
        EXPLORE(R.drawable.ic_explore, R.string.zoom_to_country) {
            override operator fun invoke(
                item: CountryItem?,
                adapter: DownloaderAdapter
            ) {
                val intent = Intent(adapter.mActivity, MwmActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                intent.putExtra(MwmActivity.EXTRA_TASK, ShowCountryTask(item!!.id!!))
                adapter.mActivity.startActivity(intent)
                if (adapter.mActivity !is MwmActivity) adapter.mActivity.finish()
                Statistics.INSTANCE.trackEvent(
                    EventName.DOWNLOADER_ACTION,
                    Statistics.params().add(
                        Statistics.EventParam.ACTION,
                        "explore"
                    )
                        .add(
                            Statistics.EventParam.FROM,
                            "downloader"
                        )
                )
            }
        },
        UPDATE(R.drawable.ic_update, R.string.downloader_update_map) {
            override operator fun invoke(
                item: CountryItem?,
                adapter: DownloaderAdapter
            ) {
                item!!.update()
                if (item.status != CountryItem.STATUS_UPDATABLE) return
                MapManager.warnOn3gUpdate(adapter.mActivity, item.id, Runnable {
                    MapManager.nativeUpdate(item.id)
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
                            .add(
                                "scenario",
                                if (item.isExpandable) "update_group" else "update"
                            )
                    )
                })
            }
        };

        abstract operator fun invoke(item: CountryItem?, adapter: DownloaderAdapter)

    }

    private class PathEntry(
        val item: CountryItem?,
        val myMapsMode: Boolean,
        val topPosition: Int,
        val topOffset: Int
    ) {
        override fun toString(): String {
            return item!!.id + " (" + item.name + "), " +
                    "myMapsMode: " + myMapsMode +
                    ", topPosition: " + topPosition +
                    ", topOffset: " + topOffset
        }

    }

    private val mStorageCallback: StorageCallback = object : StorageCallback {
        private fun updateItem(countryId: String?) {
            val ci = mCountryIndex[countryId] ?: return

            ci.update()
            val lm =
                mRecycler.layoutManager as LinearLayoutManager?
            val first = lm!!.findFirstVisibleItemPosition()
            val last = lm.findLastVisibleItemPosition()
            if (first == RecyclerView.NO_POSITION || last == RecyclerView.NO_POSITION) return
            for (i in first..last) {
                val vh =
                    mRecycler.findViewHolderForAdapterPosition(i) as ViewHolderWrapper?
                if (vh != null && vh.mKind == TYPE_COUNTRY && (vh.mHolder.mItem as CountryItem).id == countryId) vh.mHolder.rebind()
            }
        }

        override fun onStatusChanged(data: List<StorageCallbackData>) {
            for (item in data) {
                if (item.isLeafNode && item.newStatus == CountryItem.STATUS_FAILED) {
                    MapManager.showError(mActivity, item, null)
                    break
                }
            }
            if (isSearchResultsMode) {
                for (item in data) updateItem(item.countryId)
            } else {
                refreshData()
            }
        }

        override fun onProgress(
            countryId: String,
            localSize: Long,
            remoteSize: Long
        ) {
            updateItem(countryId)
        }
    }

    fun createViewHolderFrame(parent: ViewGroup, kind: Int): View {
        return inflate(
            parent,
            if (kind == TYPE_ADVERTISMENT) R.layout.downloader_item_ad else R.layout.downloader_item
        )
    }

    @Suppress("UNCHECKED_CAST")
    inner class ViewHolderWrapper(parent: ViewGroup, val mKind: Int) :
        RecyclerView.ViewHolder(createViewHolderFrame(parent, mKind)) {
        val mHolder = if (mKind == TYPE_ADVERTISMENT) AdViewHolder(
            itemView
        ) else ItemViewHolder(itemView)

        fun bind(position: Int) {

            var mPosition = position

            val kind: Int = this@DownloaderAdapter.getItemViewType(mPosition)
            if (kind == TYPE_ADVERTISMENT) {
                (mHolder as AdViewHolder).bind(mAds[mPosition - mNearMeCount])
                return
            }
            if (mPosition > mNearMeCount) {
                mPosition -= adsCount
            }
            (mHolder as ItemViewHolder).bind(mItems[mPosition])
        }

    }

    inner abstract class BaseInnerViewHolder<T> {
        var mItem: T? = null

        @Suppress("UNCHECKED_CAST")
        internal open fun bind(item: T) {
            mItem = item
        }

        fun rebind() {
            bind(mItem!!)
        }
    }

    private inner class ItemViewHolder internal constructor(frame: View) :
        BaseInnerViewHolder<CountryItem>() {
        private val mStatusIcon: DownloaderStatusIcon?
        private val mName: TextView
        private val mSubtitle: TextView
        private val mFoundName: TextView
        private val mSize: TextView
        private fun processClick(clickOnStatus: Boolean) {
            when (mItem!!.status) {
                CountryItem.STATUS_DONE, CountryItem.STATUS_PROGRESS, CountryItem.STATUS_APPLYING, CountryItem.STATUS_ENQUEUED -> processLongClick()
                CountryItem.STATUS_DOWNLOADABLE, CountryItem.STATUS_PARTLY -> if (clickOnStatus) MenuItem.DOWNLOAD.invoke(
                    mItem,
                    this@DownloaderAdapter
                ) else processLongClick()
                CountryItem.STATUS_FAILED -> {
                    val listener =
                        RetryFailedDownloadConfirmationListener(mActivity!!.application)
                    MapManager.warn3gAndRetry(mActivity, mItem!!.id, listener)
                }
                CountryItem.STATUS_UPDATABLE -> MapManager.warnOn3gUpdate(
                    mActivity,
                    mItem!!.id,
                    Runnable {
                        MapManager.nativeUpdate(mItem!!.id)
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
                                .add(
                                    "scenario",
                                    if (mItem!!.isExpandable) "update_group" else "update"
                                )
                        )
                    }
                )
                else -> throw IllegalArgumentException("Inappropriate item status: " + mItem!!.status)
            }
        }

        private fun processLongClick() {
            val items: MutableList<MenuItem> =
                ArrayList()
            when (mItem!!.status) {
                CountryItem.STATUS_DOWNLOADABLE -> items.add(MenuItem.DOWNLOAD)
                CountryItem.STATUS_UPDATABLE -> {
                    items.add(MenuItem.UPDATE)
                    if (!mItem!!.isExpandable) items.add(MenuItem.EXPLORE)
                    items.add(MenuItem.DELETE)
                }
                CountryItem.STATUS_DONE -> {
                    if (!mItem!!.isExpandable) items.add(MenuItem.EXPLORE)
                    items.add(MenuItem.DELETE)
                }
                CountryItem.STATUS_FAILED -> {
                    items.add(MenuItem.CANCEL)
                    if (mItem!!.present) {
                        items.add(MenuItem.DELETE)
                        items.add(MenuItem.EXPLORE)
                    }
                }
                CountryItem.STATUS_PROGRESS, CountryItem.STATUS_APPLYING, CountryItem.STATUS_ENQUEUED -> {
                    items.add(MenuItem.CANCEL)
                    if (mItem!!.present) items.add(MenuItem.EXPLORE)
                }
                CountryItem.STATUS_PARTLY -> {
                    items.add(MenuItem.DOWNLOAD)
                    items.add(MenuItem.DELETE)
                }
            }
            if (items.isEmpty()) return
            val bs =
                BottomSheetHelper.create(mActivity, mItem!!.name)
            for (item in items) bs.sheet(
                item.ordinal,
                item.icon,
                item.title
            )
            val bottomSheet =
                bs.listener { item ->
                    MenuItem.values()[item.itemId].invoke(mItem, this@DownloaderAdapter)
                    false
                }.build()
            BottomSheetHelper.tint(bottomSheet)
            bottomSheet.show()
        }

        override fun bind(item: CountryItem) {
            mItem = item
            if (isSearchResultsMode) {
                mName.maxLines = 1
                mName.text = mItem!!.name
                val found = mItem!!.searchResultName!!.toLowerCase()
                val builder = SpannableStringBuilder(mItem!!.searchResultName)
                val start = found.indexOf(mSearchQuery!!)
                val end = start + mSearchQuery!!.length
                if (start > -1) builder.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                mFoundName.text = builder
                if (!mItem!!.isExpandable) UiUtils.setTextAndHideIfEmpty(
                    mSubtitle,
                    mItem!!.topmostParentName ?: ""
                )
            } else {
                mName.maxLines = 2
                mName.text = mItem!!.name
                if (!mItem!!.isExpandable) UiUtils.setTextAndHideIfEmpty(
                    mSubtitle,
                    mItem!!.description ?: ""
                )
            }
            if (mItem!!.isExpandable) {
                UiUtils.setTextAndHideIfEmpty(
                    mSubtitle, String.format(
                        "%s: %s", mActivity!!.getString(R.string.downloader_status_maps),
                        mActivity.getString(
                            R.string.downloader_of, mItem!!.childCount,
                            mItem!!.totalChildCount
                        )
                    )
                )
            }
            UiUtils.showIf(isSearchResultsMode, mFoundName)
            val size: Long
            size =
                if (mItem!!.status == CountryItem.STATUS_ENQUEUED || mItem!!.status == CountryItem.STATUS_PROGRESS || mItem!!.status == CountryItem.STATUS_APPLYING
                ) {
                    mItem!!.enqueuedSize
                } else {
                    if (!isSearchResultsMode && isMyMapsMode) mItem!!.size else mItem!!.totalSize
                }
            mSize.text = StringUtils.getFileSizeString(size)
            mStatusIcon!!.update(mItem)
        }

        init {
            mStatusIcon = object :
                DownloaderStatusIcon(frame.findViewById(R.id.downloader_status_frame)) {
                override fun selectIcon(country: CountryItem?): Int {
                    return if (country!!.status == CountryItem.STATUS_DOWNLOADABLE || country.status == CountryItem.STATUS_PARTLY) {
                        if (country.isExpandable) if (isMyMapsMode) R.attr.status_folder_done else R.attr.status_folder else R.attr.status_downloadable
                    } else super.selectIcon(country)
                }

                override fun updateIcon(country: CountryItem?) {
                    super.updateIcon(country)
                    mIcon.isFocusable =
                        country!!.isExpandable && country.status != CountryItem.STATUS_DONE
                }
            }.setOnIconClickListener(View.OnClickListener { processClick(true) })
                .setOnCancelClickListener(
                    View.OnClickListener {
                        nativeCancel(mItem!!.id)
                        Statistics.INSTANCE.trackEvent(
                            EventName.DOWNLOADER_CANCEL,
                            Statistics.params().add(
                                Statistics.EventParam.FROM,
                                "downloader"
                            )
                        )
                    })


            mName = frame.findViewById<View>(R.id.name) as TextView
            mSubtitle = frame.findViewById<View>(R.id.subtitle) as TextView
            mFoundName = frame.findViewById<View>(R.id.found_name) as TextView
            mSize = frame.findViewById<View>(R.id.size) as TextView
            frame.setOnClickListener {
                if (mItem!!.isExpandable) goDeeper(
                    mItem,
                    true
                ) else processClick(false)
            }
            frame.setOnLongClickListener {
                processLongClick()
                true
            }
        }
    }

    inner class HeaderViewHolder(frame: View) :
        RecyclerView.ViewHolder(frame) {
        private val mTitle: TextView
        fun bind(position: Int) {
            var position = position
            if (position >= mNearMeCount && position < mNearMeCount + adsCount) {
                mTitle.text = HEADER_ADVERTISMENT_TITLE
            } else {
                if (position > mNearMeCount) position -= adsCount
                val ci = mItems[position]
                mTitle.text = mHeaders[ci.headerId]
            }
        }

        init {
            mTitle = frame.findViewById<View>(R.id.title) as TextView
        }
    }

    private inner class AdViewHolder internal constructor(frame: View) :
        BaseInnerViewHolder<NativeAppwallBanner>() {
        private val mIcon: ImageView
        private val mTitle: TextView
        private val mSubtitle: TextView
        private var mData: NativeAppwallBanner? = null
        private val mClickListener =
            View.OnClickListener {
                if (mData != null) if (mMytargetHelper != null) mMytargetHelper!!.onBannerClick(
                    mData!!
                )
            }

        override fun bind(item: NativeAppwallBanner) {
            mData = item
            super.bind(item)
            mIcon.setImageBitmap(item.icon!!.bitmap)
            mTitle.text = item.title
            mSubtitle.text = item.description
        }

        init {
            mIcon =
                frame.findViewById<View>(R.id.downloader_ad_icon) as ImageView
            mTitle = frame.findViewById<View>(R.id.downloader_ad_title) as TextView
            mSubtitle =
                frame.findViewById<View>(R.id.downloader_ad_subtitle) as TextView
            frame.setOnClickListener(mClickListener)
        }
    }

    private fun collectHeaders() {
        mNearMeCount = 0
        mHeaders.clear()
        if (isSearchResultsMode) return
        var headerId = 0
        var prev = -1
        for (ci in mItems) {
            when (ci.category) {
                CountryItem.CATEGORY_NEAR_ME -> {
                    if (ci.category != prev) {
                        headerId = CountryItem.CATEGORY_NEAR_ME
                        mHeaders.put(
                            headerId,
                            MwmApplication.get().getString(R.string.downloader_near_me_subtitle)
                        )
                        prev = ci.category
                    }
                    mNearMeCount++
                }
                CountryItem.CATEGORY_DOWNLOADED -> if (ci.category != prev) {
                    headerId = CountryItem.CATEGORY_DOWNLOADED
                    mHeaders.put(
                        headerId,
                        MwmApplication.get().getString(R.string.downloader_downloaded_subtitle)
                    )
                    prev = ci.category
                }
                else -> {
                    val prevHeader = headerId
                    headerId =
                        CountryItem.CATEGORY_AVAILABLE * HEADER_ADS_OFFSET + ci.name!![0].toInt()
                    if (headerId != prevHeader) mHeaders.put(
                        headerId,
                        ci.name!!.substring(0, 1).toUpperCase()
                    )
                    prev = ci.category
                }
            }
            ci.headerId = headerId
        }
    }

    fun refreshData() {
        mItems.clear()
        val parent = currentRootId
        var hasLocation = false
        var lat = 0.0
        var lon = 0.0
        if (!isMyMapsMode && CountryItem.isRoot(parent)) {
            val loc = LocationHelper.INSTANCE.savedLocation
            hasLocation = loc != null
            if (hasLocation) {
                lat = loc!!.latitude
                lon = loc.longitude
            }
        }
        MapManager.nativeListItems(parent, lat, lon, hasLocation, isMyMapsMode, mItems)
        processData()
        loadAds()
    }

    fun setSearchResultsMode(
        results: Collection<CountryItem>,
        query: String
    ) {
        isSearchResultsMode = true
        mSearchQuery = query.toLowerCase()
        mItems.clear()
        mItems.addAll(results)
        processData()
    }

    fun clearAdsAndCancelMyTarget() {
        if (mAds.isEmpty()) return
        if (mMytargetHelper != null) mMytargetHelper!!.cancel()
        clearAdsInternal()
        mAdsLoaded = false
    }

    fun resetSearchResultsMode() {
        isSearchResultsMode = false
        mSearchQuery = null
        refreshData()
    }

    private fun processData() {
        if (!isSearchResultsMode) Collections.sort(mItems)
        collectHeaders()
        mCountryIndex.clear()
        for (ci in mItems) mCountryIndex[ci.id] = ci
        if (mItems.isEmpty()) mFragment.setupPlaceholder()
        mFragment.showPlaceholder(mItems.isEmpty())
        mHeadersDecoration.invalidateHeaders()
        notifyDataSetChanged()
    }

    private fun inflate(parent: ViewGroup, @LayoutRes layoutId: Int): View {
        return LayoutInflater.from(mActivity).inflate(layoutId, parent, false)
    }

    override fun getItemViewType(position: Int): Int {
        if (position < mNearMeCount) return TYPE_COUNTRY
        return if (position < mNearMeCount + adsCount) TYPE_ADVERTISMENT else TYPE_COUNTRY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderWrapper {
        return ViewHolderWrapper(parent, viewType)
    }

    override fun onBindViewHolder(holder: ViewHolderWrapper, position: Int) {
        holder.bind(position)
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup): HeaderViewHolder {
        return HeaderViewHolder(
            inflate(
                parent,
                R.layout.downloader_item_header
            )
        )
    }

    override fun onBindHeaderViewHolder(
        holder: HeaderViewHolder,
        position: Int
    ) {
        holder.bind(position)
    }

    override fun getHeaderId(position: Int): Long {
        var position = position
        if (position >= mNearMeCount) {
            if (position < mNearMeCount + adsCount) return HEADER_ADVERTISMENT_ID.toLong()
            position -= adsCount
        }
        return mItems[position].headerId.toLong()
    }

    private val adsCount: Int
        private get() = if (mShowAds) mAds.size else 0

    override fun getItemCount(): Int {
        return mItems.size + adsCount
    }

    private fun goDeeper(child: CountryItem?, refresh: Boolean) {
        val lm =
            mRecycler.layoutManager as LinearLayoutManager?
        // Save scroll positions (top item + item`s offset) for current hierarchy level
        var position = lm!!.findFirstVisibleItemPosition()
        val offset: Int
        if (position > -1) offset = lm.findViewByPosition(position)!!.top else {
            position = 0
            offset = 0
        }
        val wasEmpty = mPath.isEmpty()
        mPath.push(
            PathEntry(
                child,
                isMyMapsMode,
                position,
                offset
            )
        )
        isMyMapsMode = isMyMapsMode and (!isSearchResultsMode || child!!.childCount > 0)
        if (wasEmpty) mFragment.clearSearchQuery()
        lm.scrollToPosition(0)
        if (!refresh) return
        if (isSearchResultsMode) resetSearchResultsMode() else refreshData()
        mFragment.update()
    }

    fun canGoUpwards(): Boolean {
        return !mPath.isEmpty()
    }

    fun goUpwards(): Boolean {
        if (!canGoUpwards()) return false
        val entry = mPath.pop()
        isMyMapsMode = entry.myMapsMode
        refreshData()
        val lm =
            mRecycler.layoutManager as LinearLayoutManager?
        lm!!.scrollToPositionWithOffset(entry.topPosition, entry.topOffset)
        mFragment.update()
        return true
    }

    fun setAvailableMapsMode() {
        goDeeper(currentRootItem, false)
        isMyMapsMode = false
        refreshData()
    }

    private val currentRootItem: CountryItem?
        private get() = if (canGoUpwards()) mPath.peek().item else CountryItem.fill(
            CountryItem.rootId
        )

    val currentRootId: String
        get() = if (canGoUpwards()) currentRootItem!!.id!! else CountryItem.rootId!!

    val currentRootName: String?
        get() = if (canGoUpwards()) currentRootItem!!.name else null

    /**
     * Loads banner if:
     *
     *  * There is not active ads removal subscription
     *  * Not in `my maps` mode;
     *  * Currently at root level;
     *  * Day mode is active;
     *  * There is at least one map downloaded.
     *
     */
    private fun loadAds() {
        mShowAds = false
        if (Framework.nativeHasActiveSubscription(Framework.SUBSCRIPTION_TYPE_REMOVE_ADS)) return
        if (mAdsLoading) return
        if (isMyMapsMode || !CountryItem.isRoot(currentRootId)) return
        if (!ThemeUtils.isDefaultTheme) return
        if (MapManager.nativeGetDownloadedCount() < 1) return
        mShowAds = true
        if (mAdsLoaded) {
            mHeadersDecoration.invalidateHeaders()
            notifyItemRangeInserted(mNearMeCount, mAds.size)
            return
        }
        mAdsLoading = true
        if (mMytargetHelper == null) initMytargetHelper()
    }

    private fun handleBannersShow(ads: List<NativeAppwallBanner>) {
        if (mMytargetHelper != null) mMytargetHelper!!.handleBannersShow(ads)
    }

    private fun initMytargetHelper() {
        mMytargetHelper = MytargetHelper(object : MytargetHelper.Listener<Void?> {
            private fun onNoAdsInternal() {
                mAdsLoading = false
                mAdsLoaded = true
                clearAdsInternal()
            }

            override fun onNoAds() {
                onNoAdsInternal()
            }

            override fun onDataReady(data: Void?) {
                mMytargetHelper!!.loadShowcase(object :
                    MytargetHelper.Listener<List<NativeAppwallBanner>> {
                    override fun onNoAds() {
                        onNoAdsInternal()
                    }

                    override fun onDataReady(banners: List<NativeAppwallBanner>?) {
                        mAdsLoading = false
                        mAdsLoaded = true
                        mAds.clear()
                        if (banners != null) {
                            for (banner in banners) if (!banner.isAppInstalled) mAds.add(
                                banner
                            )
                            handleBannersShow(banners)
                        }
                        mHeadersDecoration.invalidateHeaders()
                        notifyDataSetChanged()
                    }
                }, mActivity)
            }
        })
    }

    private fun clearAdsInternal() {
        val oldSize = mAds.size
        mAds.clear()
        if (oldSize > 0) {
            mHeadersDecoration.invalidateHeaders()
            notifyItemRangeRemoved(mNearMeCount, oldSize)
        }
    }

    fun attach() {
        mListenerSlot = MapManager.nativeSubscribe(mStorageCallback)
    }

    fun detach() {
        MapManager.nativeUnsubscribe(mListenerSlot)
        if (mMytargetHelper != null) mMytargetHelper!!.cancel()
        mAdsLoading = false
    }

    companion object {
        private const val HEADER_ADVERTISMENT_TITLE = "MY.COM"
        private val HEADER_ADVERTISMENT_ID: Int = CountryItem.CATEGORY__LAST + 1
        private const val HEADER_ADS_OFFSET = 10
        private const val TYPE_COUNTRY = 0
        private const val TYPE_ADVERTISMENT = 1
    }
}