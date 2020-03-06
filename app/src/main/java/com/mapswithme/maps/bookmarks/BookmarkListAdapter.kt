package com.mapswithme.maps.bookmarks

import android.content.res.Resources
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.mapswithme.maps.R
import com.mapswithme.maps.bookmarks.data.BookmarkCategory
import com.mapswithme.maps.bookmarks.data.BookmarkInfo
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.bookmarks.data.SortedBlock
import com.mapswithme.maps.content.DataSource
import com.mapswithme.maps.widget.recycler.RecyclerClickListener
import com.mapswithme.maps.widget.recycler.RecyclerLongClickListener

import java.util.ArrayList
import java.util.Arrays

class BookmarkListAdapter internal constructor(private val mDataSource: DataSource<BookmarkCategory>) :
    RecyclerView.Adapter<Holders.BaseBookmarkHolder>() {
    private var mSearchResults: MutableList<Long>? = null
    private var mSortedResults: MutableList<SortedBlock>? = null

    private lateinit var mSectionsDataSource: SectionsDataSource

    private var mMoreListener: RecyclerClickListener? = null
    private var mClickListener: RecyclerClickListener? = null
    private var mLongClickListener: RecyclerLongClickListener? = null

    internal val isSearchResults: Boolean
        get() = mSearchResults != null

    class SectionPosition(val sectionIndex: Int, val itemIndex: Int) {

        val isTitlePosition: Boolean
            get() = sectionIndex != INVALID_POSITION && itemIndex == INVALID_POSITION

        val isItemPosition: Boolean
            get() = sectionIndex != INVALID_POSITION && itemIndex != INVALID_POSITION

        companion object {
            val INVALID_POSITION = -1
        }
    }

    abstract class SectionsDataSource internal constructor(private val mDataSource: DataSource<BookmarkCategory>) {

        val category: BookmarkCategory
            get() = mDataSource.data

        abstract val sectionsCount: Int

        internal fun hasDescription(): Boolean {
            return !mDataSource.data.annotation.isEmpty() || !mDataSource.data.description.isEmpty()
        }

        abstract fun isEditable(sectionIndex: Int): Boolean
        abstract fun hasTitle(sectionIndex: Int): Boolean
        abstract fun getTitle(sectionIndex: Int, rs: Resources): String?
        abstract fun getItemsCount(sectionIndex: Int): Int
        abstract fun getItemsType(sectionIndex: Int): Int
        abstract fun getBookmarkId(pos: SectionPosition): Long
        abstract fun getTrackId(pos: SectionPosition): Long
        abstract fun onDelete(pos: SectionPosition)
    }

    private class CategorySectionsDataSource internal constructor(dataSource: DataSource<BookmarkCategory>) :
        SectionsDataSource(dataSource) {
        override var sectionsCount: Int = 0
            private set
        private var mDescriptionSectionIndex: Int = 0
        private var mBookmarksSectionIndex: Int = 0
        private var mTracksSectionIndex: Int = 0

        init {
            calculateSections()
        }

        private fun calculateSections() {
            mDescriptionSectionIndex = SectionPosition.INVALID_POSITION
            mBookmarksSectionIndex = SectionPosition.INVALID_POSITION
            mTracksSectionIndex = SectionPosition.INVALID_POSITION

            sectionsCount = 0
            if (hasDescription())
                mDescriptionSectionIndex = sectionsCount++
            if (category.tracksCount > 0)
                mTracksSectionIndex = sectionsCount++
            if (category.bookmarksCount > 0)
                mBookmarksSectionIndex = sectionsCount++
        }

        override fun isEditable(sectionIndex: Int): Boolean {
            return sectionIndex != mDescriptionSectionIndex && !category.isFromCatalog
        }

        override fun hasTitle(sectionIndex: Int): Boolean {
            return true
        }

        override fun getTitle(sectionIndex: Int, rs: Resources): String? {
            if (sectionIndex == mDescriptionSectionIndex)
                return rs.getString(R.string.description)
            return if (sectionIndex == mTracksSectionIndex) rs.getString(R.string.tracks_title) else rs.getString(R.string.bookmarks)
        }

        override fun getItemsCount(sectionIndex: Int): Int {
            if (sectionIndex == mDescriptionSectionIndex)
                return 1
            if (sectionIndex == mTracksSectionIndex)
                return category.tracksCount
            return if (sectionIndex == mBookmarksSectionIndex) category.bookmarksCount else 0
        }

        override fun getItemsType(sectionIndex: Int): Int {
            if (sectionIndex == mDescriptionSectionIndex)
                return TYPE_DESC
            if (sectionIndex == mTracksSectionIndex)
                return TYPE_TRACK
            if (sectionIndex == mBookmarksSectionIndex)
                return TYPE_BOOKMARK
            throw AssertionError("Invalid section index: $sectionIndex")
        }

        override fun onDelete(pos: SectionPosition) {
            calculateSections()
        }

        override fun getBookmarkId(pos: SectionPosition): Long {
            return BookmarkManager.INSTANCE.getBookmarkIdByPosition(
                category.id,
                pos.itemIndex
            )
        }

        override fun getTrackId(pos: SectionPosition): Long {
            return BookmarkManager.INSTANCE.getTrackIdByPosition(
                category.id,
                pos.itemIndex
            )
        }
    }

    private class SearchResultsSectionsDataSource internal constructor(
        dataSource: DataSource<BookmarkCategory>,
        private val mSearchResults: MutableList<Long>
    ) : SectionsDataSource(dataSource) {

        override val sectionsCount: Int
            get() = 1

        override fun isEditable(sectionIndex: Int): Boolean {
            return true
        }

        override fun hasTitle(sectionIndex: Int): Boolean {
            return false
        }

        override fun getTitle(sectionIndex: Int, rs: Resources): String? {
            return null
        }

        override fun getItemsCount(sectionIndex: Int): Int {
            return mSearchResults.size
        }

        override fun getItemsType(sectionIndex: Int): Int {
            return TYPE_BOOKMARK
        }

        override fun onDelete(pos: SectionPosition) {
            mSearchResults.removeAt(pos.itemIndex)
        }

        override fun getBookmarkId(pos: SectionPosition): Long {
            return mSearchResults[pos.itemIndex]
        }

        override fun getTrackId(pos: SectionPosition): Long {
            throw AssertionError("Tracks unsupported in search results.")
        }
    }

    private class SortedSectionsDataSource internal constructor(
        dataSource: DataSource<BookmarkCategory>,
        private val mSortedBlocks: MutableList<SortedBlock>
    ) : SectionsDataSource(dataSource) {

        override val sectionsCount: Int
            get() = mSortedBlocks.size + if (hasDescription()) 1 else 0

        private fun isDescriptionSection(sectionIndex: Int): Boolean {
            return hasDescription() && sectionIndex == 0
        }

        private fun getSortedBlock(sectionIndex: Int): SortedBlock {
            require(!isDescriptionSection(sectionIndex)) { "Invalid section index for sorted block." }
            val blockIndex = sectionIndex - if (hasDescription()) 1 else 0
            return mSortedBlocks[blockIndex]
        }

        override fun isEditable(sectionIndex: Int): Boolean {
            return !isDescriptionSection(sectionIndex)
        }

        override fun hasTitle(sectionIndex: Int): Boolean {
            return true
        }

        override fun getTitle(sectionIndex: Int, rs: Resources): String? {
            return if (isDescriptionSection(sectionIndex)) rs.getString(R.string.description) else getSortedBlock(
                sectionIndex
            ).name
        }

        override fun getItemsCount(sectionIndex: Int): Int {
            if (isDescriptionSection(sectionIndex))
                return 1
            val block = getSortedBlock(sectionIndex)
            return if (block.isBookmarksBlock) block.bookmarkIds.size else block.trackIds.size
        }

        override fun getItemsType(sectionIndex: Int): Int {
            if (isDescriptionSection(sectionIndex))
                return TYPE_DESC
            return if (getSortedBlock(sectionIndex).isBookmarksBlock) TYPE_BOOKMARK else TYPE_TRACK
        }

        override fun onDelete(pos: SectionPosition) {
            require(!isDescriptionSection(pos.sectionIndex)) { "Delete failed. Invalid section index." }

            val blockIndex = pos.sectionIndex - if (hasDescription()) 1 else 0
            val block = mSortedBlocks[blockIndex]
            if (block.isBookmarksBlock) {
                block.bookmarkIds.removeAt(pos.itemIndex)
                if (block.bookmarkIds.isEmpty())
                    mSortedBlocks.removeAt(blockIndex)
                return
            }

            block.trackIds.removeAt(pos.itemIndex)
            if (block.trackIds.isEmpty())
                mSortedBlocks.removeAt(blockIndex)
        }

        override fun getBookmarkId(pos: SectionPosition): Long {
            return getSortedBlock(pos.sectionIndex).bookmarkIds[pos.itemIndex]
        }

        override fun getTrackId(pos: SectionPosition): Long {
            return getSortedBlock(pos.sectionIndex).trackIds[pos.itemIndex]
        }
    }

    init {
        refreshSections()
    }

    private fun refreshSections() {
        if (mSearchResults != null)
            mSectionsDataSource = SearchResultsSectionsDataSource(mDataSource, mSearchResults!!)
        else if (mSortedResults != null)
            mSectionsDataSource = SortedSectionsDataSource(mDataSource, mSortedResults!!)
        else
            mSectionsDataSource = CategorySectionsDataSource(mDataSource)
    }

    private fun getSectionPosition(position: Int): SectionPosition {
        var startSectionRow = 0
        var hasTitle: Boolean
        val sectionsCount = mSectionsDataSource.sectionsCount
        for (i in 0 until sectionsCount) {
            hasTitle = mSectionsDataSource.hasTitle(i)
            val sectionRowsCount = mSectionsDataSource.getItemsCount(i) + if (hasTitle) 1 else 0
            if (startSectionRow == position && hasTitle)
                return SectionPosition(i, SectionPosition.INVALID_POSITION)
            if (startSectionRow + sectionRowsCount > position)
                return SectionPosition(i, position - startSectionRow - if (hasTitle) 1 else 0)
            startSectionRow += sectionRowsCount
        }
        return SectionPosition(SectionPosition.INVALID_POSITION, SectionPosition.INVALID_POSITION)
    }

    internal fun setSearchResults(searchResults: LongArray?) {
        if (searchResults != null) {
            mSearchResults = ArrayList(searchResults.size)
            for (id in searchResults)
                mSearchResults!!.add(id)
        } else {
            mSearchResults = null
        }
        refreshSections()
    }

    internal fun setSortedResults(sortedResults: Array<SortedBlock>?) {
        if (sortedResults != null)
            mSortedResults = ArrayList(Arrays.asList(*sortedResults))
        else
            mSortedResults = null
        refreshSections()
    }

    fun setOnClickListener(listener: RecyclerClickListener?) {
        mClickListener = listener
    }

    internal fun setOnLongClickListener(listener: RecyclerLongClickListener?) {
        mLongClickListener = listener
    }

    internal fun setMoreListener(listener: RecyclerClickListener?) {
        mMoreListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holders.BaseBookmarkHolder {
        val inflater = LayoutInflater.from(parent.context)
        var holder: Holders.BaseBookmarkHolder? = null
        when (viewType) {
            TYPE_TRACK -> {
                val trackHolder = Holders.TrackViewHolder(
                    inflater.inflate(
                        R.layout.item_track, parent,
                        false
                    )
                )
                trackHolder.setOnClickListener(mClickListener)
                holder = trackHolder
            }
            TYPE_BOOKMARK -> {
                val bookmarkHolder = Holders.BookmarkViewHolder(
                    inflater.inflate(
                        R.layout.item_bookmark, parent,
                        false
                    )
                )
                bookmarkHolder.setOnClickListener(mClickListener)
                bookmarkHolder.setOnLongClickListener(mLongClickListener)
                bookmarkHolder.setMoreListener(mMoreListener)
                holder = bookmarkHolder
            }
            TYPE_SECTION -> {
                val tv = inflater.inflate(R.layout.item_category_title, parent, false) as TextView
                holder = Holders.SectionViewHolder(tv)
            }
            TYPE_DESC -> {
                val desc = inflater.inflate(R.layout.item_category_description, parent, false)
                holder = Holders.DescriptionViewHolder(desc, mSectionsDataSource.category)
            }
        }

        if (holder == null)
            throw AssertionError("Unsupported view type: $viewType")

        return holder
    }

    override fun onBindViewHolder(holder: Holders.BaseBookmarkHolder, position: Int) {
        val sp = getSectionPosition(position)
        holder.bind(sp, mSectionsDataSource)
    }

    override fun getItemViewType(position: Int): Int {
        val sp = getSectionPosition(position)
        if (sp.isTitlePosition)
            return TYPE_SECTION
        if (sp.isItemPosition)
            return mSectionsDataSource.getItemsType(sp.sectionIndex)
        throw IllegalArgumentException("Position not found: $position")
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        var itemCount = 0
        val sectionsCount = mSectionsDataSource.sectionsCount
        for (i in 0 until sectionsCount) {
            val sectionItemsCount = mSectionsDataSource.getItemsCount(i)
            if (sectionItemsCount == 0)
                continue
            itemCount += sectionItemsCount
            if (mSectionsDataSource.hasTitle(i))
                ++itemCount
        }
        return itemCount
    }

    internal fun onDelete(position: Int) {
        val sp = getSectionPosition(position)
        mSectionsDataSource.onDelete(sp)
        // In case of the search results editing reset cached sorted blocks.
        if (isSearchResults)
            mSortedResults = null
    }

    fun getItem(position: Int): Any {
        if (getItemViewType(position) == TYPE_DESC)
            throw UnsupportedOperationException("Not supported here! Position = $position")

        val pos = getSectionPosition(position)
        if (getItemViewType(position) == TYPE_TRACK) {
            val trackId = mSectionsDataSource.getTrackId(pos)
            return BookmarkManager.INSTANCE.getTrack(trackId)
        } else {
            val bookmarkId = mSectionsDataSource.getBookmarkId(pos)
            return BookmarkManager.INSTANCE.getBookmarkInfo(bookmarkId)
                ?: throw RuntimeException("Bookmark no longer exists $bookmarkId")
        }
    }

    companion object {
        // view types
        internal val TYPE_TRACK = 0
        internal val TYPE_BOOKMARK = 1
        internal val TYPE_SECTION = 2
        internal val TYPE_DESC = 3
    }
}
