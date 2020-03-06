package com.mapswithme.maps.bookmarks

import android.text.Html
import android.text.Layout
import android.text.StaticLayout
import android.text.TextUtils
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.PluralsRes
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.maps.bookmarks.BookmarkListAdapter.SectionPosition
import com.mapswithme.maps.bookmarks.BookmarkListAdapter.SectionsDataSource
import com.mapswithme.maps.bookmarks.data.BookmarkCategory
import com.mapswithme.maps.bookmarks.data.BookmarkCategory.Author.Companion.getRepresentation
import com.mapswithme.maps.bookmarks.data.BookmarkInfo
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.widget.recycler.RecyclerClickListener
import com.mapswithme.maps.widget.recycler.RecyclerLongClickListener
import com.mapswithme.util.Graphics
import com.mapswithme.util.UiUtils

class Holders {
    internal class GeneralViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val text: TextView
        val image: ImageView

        init {
            image = itemView.findViewById(R.id.image)
            text = itemView.findViewById(R.id.text)
        }
    }

    internal class HeaderViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val button: TextView
        val text: TextView

        fun setAction(
            action: HeaderAction,
            resProvider: BookmarkCategoriesPageResProvider,
            showAll: Boolean
        ) {
            button.setText(if (showAll) resProvider.headerBtn.selectModeText else resProvider.headerBtn.unSelectModeText)
            button.setOnClickListener(ToggleShowAllClickListener(action, showAll))
        }

        interface HeaderAction {
            fun onHideAll()
            fun onShowAll()
        }

        private class ToggleShowAllClickListener internal constructor(
            private val mAction: HeaderAction,
            private val mShowAll: Boolean
        ) : View.OnClickListener {
            override fun onClick(view: View) {
                if (mShowAll) mAction.onShowAll() else mAction.onHideAll()
            }

        }

        init {
            button = itemView.findViewById(R.id.button)
            text = itemView.findViewById(R.id.text_message)
        }
    }

    internal class CategoryViewHolder(root: View) :
        RecyclerView.ViewHolder(root) {
        private val mName: TextView
        var mVisibilityMarker: CheckBox
        var mSize: TextView
        var mMore: View
        var authorName: TextView
        var mAccessRule: TextView
        var mAccessRuleImage: ImageView
        private var mEntity: BookmarkCategory? = null
        fun setVisibilityState(visible: Boolean) {
            mVisibilityMarker.isChecked = visible
        }

        fun setVisibilityListener(listener: View.OnClickListener?) {
            mVisibilityMarker.setOnClickListener(listener)
        }

        fun setMoreListener(listener: View.OnClickListener?) {
            mMore.setOnClickListener(listener)
        }

        fun setName(name: String) {
            mName.text = name
        }

        fun setSize(@PluralsRes phrase: Int, size: Int) {
            mSize.text = mSize.resources.getQuantityString(phrase, size, size)
        }

        fun setCategory(entity: BookmarkCategory) {
            mEntity = entity
        }

        val entity: BookmarkCategory
            get() = mEntity ?: throw AssertionError("BookmarkCategory is null")

        init {
            mName = root.findViewById(R.id.name)
            mVisibilityMarker = root.findViewById(R.id.checkbox)
            val left = root.resources.getDimensionPixelOffset(R.dimen.margin_half_plus)
            val right = root.resources.getDimensionPixelOffset(R.dimen.margin_base_plus)
            UiUtils.expandTouchAreaForView(mVisibilityMarker, 0, left, 0, right)
            mSize = root.findViewById(R.id.size)
            mMore = root.findViewById(R.id.more)
            authorName = root.findViewById(R.id.author_name)
            mAccessRule = root.findViewById(R.id.access_rule)
            mAccessRuleImage = root.findViewById(R.id.access_rule_img)
        }
    }

    abstract class BaseBookmarkHolder(private val mView: View) :
        RecyclerView.ViewHolder(mView) {
        abstract fun bind(
            position: SectionPosition,
            sectionsDataSource: SectionsDataSource
        )

        fun setOnClickListener(listener: RecyclerClickListener?) {
            mView.setOnClickListener { v: View? ->
                listener?.onItemClick(
                    v,
                    adapterPosition
                )
            }
        }

        fun setOnLongClickListener(listener: RecyclerLongClickListener?) {
            mView.setOnLongClickListener { v: View? ->
                listener?.onLongItemClick(v, adapterPosition)
                true
            }
        }

    }

    internal class BookmarkViewHolder(itemView: View) :
        BaseBookmarkHolder(itemView) {
        private val mIcon: ImageView
        private val mName: TextView
        private val mDistance: TextView
        private val mMore: View
        override fun bind(
            position: SectionPosition,
            sectionsDataSource: SectionsDataSource
        ) {
            val bookmarkId = sectionsDataSource.getBookmarkId(position)
            val bookmark = BookmarkInfo(
                sectionsDataSource.category.id,
                bookmarkId
            )
            mName.text = bookmark.name
            val loc = LocationHelper.INSTANCE.savedLocation
            val distanceValue = if (loc == null) "" else bookmark.getDistance(
                loc.latitude,
                loc.longitude, 0.0
            )
            var separator = ""
            if (!distanceValue.isEmpty() && !bookmark.featureType.isEmpty()) separator = " • "
            val subtitleValue = distanceValue + separator + bookmark.featureType
            mDistance.text = subtitleValue
            UiUtils.hideIf(TextUtils.isEmpty(subtitleValue), mDistance)
            mIcon.setImageResource(bookmark.icon.resId)
            val circle =
                Graphics.drawCircleAndImage(
                    bookmark.icon.argb(),
                    R.dimen.track_circle_size,
                    bookmark.icon.resId,
                    R.dimen.bookmark_icon_size,
                    mIcon.context.resources
                )
            mIcon.setImageDrawable(circle)
        }

        fun setMoreListener(listener: RecyclerClickListener?) {
            mMore.setOnClickListener { v: View? ->
                listener?.onItemClick(
                    v,
                    adapterPosition
                )
            }
        }

        init {
            mIcon = itemView.findViewById(R.id.iv__bookmark_color)
            mName = itemView.findViewById(R.id.tv__bookmark_name)
            mDistance = itemView.findViewById(R.id.tv__bookmark_distance)
            mMore = itemView.findViewById(R.id.more)
        }
    }

    internal class TrackViewHolder(itemView: View) :
        BaseBookmarkHolder(itemView) {
        private val mIcon: ImageView
        private val mName: TextView
        private val mDistance: TextView
        override fun bind(
            position: SectionPosition,
            sectionsDataSource: SectionsDataSource
        ) {
            val trackId = sectionsDataSource.getTrackId(position)
            val track =
                BookmarkManager.INSTANCE.getTrack(trackId)
            mName.text = track.name
            mDistance.text = StringBuilder().append(
                mDistance.context
                    .getString(R.string.length)
            )
                .append(" ")
                .append(track.lengthString)
                .toString()
            val circle =
                Graphics.drawCircle(
                    track.color, R.dimen.track_circle_size,
                    mIcon.context.resources
                )
            mIcon.setImageDrawable(circle)
        }

        init {
            mIcon = itemView.findViewById(R.id.iv__bookmark_color)
            mName = itemView.findViewById(R.id.tv__bookmark_name)
            mDistance = itemView.findViewById(R.id.tv__bookmark_distance)
        }
    }

    internal class SectionViewHolder(private val mView: TextView) :
        BaseBookmarkHolder(mView) {
        override fun bind(
            position: SectionPosition,
            sectionsDataSource: SectionsDataSource
        ) {
            mView.text = sectionsDataSource.getTitle(position.sectionIndex, mView.resources)
        }

    }

    internal class DescriptionViewHolder(
        itemView: View,
        category: BookmarkCategory
    ) : BaseBookmarkHolder(itemView) {
        private val mTitle: TextView
        private val mAuthor: TextView
        private val mDescText: TextView
        private val mMoreBtn: View
        private fun onMoreBtnClicked(
            v: View,
            category: BookmarkCategory
        ) {
            val lineCount =
                calcLineCount(mDescText, category.description)
            mDescText.maxLines = lineCount
            mDescText.text = Html.fromHtml(category.description)
            v.visibility = View.GONE
        }

        override fun bind(
            position: SectionPosition,
            sectionsDataSource: SectionsDataSource
        ) {
            mTitle.text = sectionsDataSource.category.name
            bindAuthor(sectionsDataSource.category)
            bindDescriptionIfEmpty(sectionsDataSource.category)
        }

        private fun bindDescriptionIfEmpty(category: BookmarkCategory) {
            if (TextUtils.isEmpty(mDescText.text)) {
                val desc =
                    if (TextUtils.isEmpty(category.annotation)) category.description else category.annotation
                val spannedDesc = Html.fromHtml(desc)
                mDescText.text = spannedDesc
            }
        }

        private fun bindAuthor(category: BookmarkCategory) {
            val author =
                category.author
            val authorName: CharSequence? =
                author?.let { getRepresentation(itemView.context, it) }
            mAuthor.text = authorName
        }

        companion object {
            const val SPACING_MULTIPLE = 1.0f
            const val SPACING_ADD = 0.0f
            private fun calcLineCount(textView: TextView, src: String): Int {
                val staticLayout = StaticLayout(
                    src,
                    textView.paint,
                    textView.width,
                    Layout.Alignment.ALIGN_NORMAL,
                    SPACING_MULTIPLE,
                    SPACING_ADD,
                    true
                )
                return staticLayout.lineCount
            }
        }

        init {
            mDescText = itemView.findViewById(R.id.text)
            mTitle = itemView.findViewById(R.id.title)
            mAuthor = itemView.findViewById(R.id.author)
            mMoreBtn = itemView.findViewById(R.id.more_btn)
            val isEmptyDesc = TextUtils.isEmpty(category.description)
            UiUtils.hideIf(isEmptyDesc, mMoreBtn)
            mMoreBtn.setOnClickListener { v: View ->
                onMoreBtnClicked(
                    v,
                    category
                )
            }
        }
    }
}