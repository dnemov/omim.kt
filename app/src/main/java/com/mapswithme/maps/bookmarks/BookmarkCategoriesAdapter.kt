package com.mapswithme.maps.bookmarks

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.maps.adapter.OnItemClickListener
import com.mapswithme.maps.bookmarks.Holders.CategoryViewHolder
import com.mapswithme.maps.bookmarks.Holders.GeneralViewHolder
import com.mapswithme.maps.bookmarks.Holders.HeaderViewHolder.HeaderAction
import com.mapswithme.maps.bookmarks.data.BookmarkCategory
import com.mapswithme.maps.bookmarks.data.BookmarkCategory.AccessRules
import com.mapswithme.maps.bookmarks.data.BookmarkCategory.Author.Companion.getRepresentation
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.util.UiUtils

class BookmarkCategoriesAdapter internal constructor(
    context: Context,
    private val mType: BookmarkCategory.Type,
    categories: List<BookmarkCategory>
) : BaseBookmarkCategoryAdapter<RecyclerView.ViewHolder?>(
    context.applicationContext,
    categories
) {
    private val mResProvider: BookmarkCategoriesPageResProvider
    private var mLongClickListener: OnItemLongClickListener<BookmarkCategory>? =
        null
    private var mClickListener: OnItemClickListener<BookmarkCategory>? =
        null
    private var mCategoryListCallback: CategoryListCallback? = null
    private val mMassOperationAction = MassOperationAction()
    fun setOnClickListener(listener: OnItemClickListener<BookmarkCategory>?) {
        mClickListener = listener
    }

    fun setOnLongClickListener(listener: OnItemLongClickListener<BookmarkCategory>?) {
        mLongClickListener = listener
    }

    fun setCategoryListCallback(listener: CategoryListCallback?) {
        mCategoryListCallback = listener
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        if (viewType == TYPE_ACTION_HEADER) {
            val header =
                inflater.inflate(R.layout.item_bookmark_group_list_header, parent, false)
            return Holders.HeaderViewHolder(header)
        }
        if (viewType == TYPE_ACTION_FOOTER) {
            val item =
                inflater.inflate(R.layout.item_bookmark_create_group, parent, false)
            item.setOnClickListener(FooterClickListener())
            return GeneralViewHolder(item)
        }
        val view =
            inflater.inflate(R.layout.item_bookmark_category, parent, false)
        val holder = CategoryViewHolder(view)
        view.setOnClickListener(CategoryItemClickListener(holder))
        view.setOnLongClickListener(LongClickListener(holder))
        return holder
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val type = getItemViewType(position)
        if (type == TYPE_ACTION_FOOTER) {
            bindFooterHolder(holder)
            return
        }
        if (type == TYPE_ACTION_HEADER) {
            bindHeaderHolder(holder)
            return
        }
        bindCategoryHolder(holder, position)
    }

    private fun bindFooterHolder(holder: RecyclerView.ViewHolder) {
        val generalViewHolder = holder as GeneralViewHolder
        generalViewHolder.image.setImageResource(mResProvider.footerImage)
        generalViewHolder.text.setText(mResProvider.footerText)
    }

    private fun bindHeaderHolder(holder: RecyclerView.ViewHolder) {
        val headerViewHolder =
            holder as Holders.HeaderViewHolder
        headerViewHolder.setAction(
            mMassOperationAction,
            mResProvider,
            BookmarkManager.INSTANCE.areAllCategoriesInvisible(mType)
        )
        headerViewHolder.text.setText(mResProvider.headerText)
    }

    private fun bindCategoryHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val category = getCategoryByPosition(toCategoryPosition(position))
        val categoryHolder = holder as CategoryViewHolder
        categoryHolder.setCategory(category)
        categoryHolder.setName(category.name)
        bindSize(categoryHolder, category)
        bindAuthor(categoryHolder, category)
        bindAccessRules(category, categoryHolder)
        categoryHolder.setVisibilityState(category.isVisible)
        val listener = ToggleVisibilityClickListener(categoryHolder)
        categoryHolder.setVisibilityListener(listener)
        categoryHolder.setMoreListener(View.OnClickListener {
            onMoreOperationClicked(
                category
            )
        })
    }

    private fun bindAccessRules(
        category: BookmarkCategory,
        categoryHolder: CategoryViewHolder
    ) {
        val rules = category.accessRules
        categoryHolder.mAccessRuleImage.setImageResource(rules.drawableResId)
        val representation =
            (categoryHolder.itemView.resources.getString(rules.nameResId)
                    + UiUtils.PHRASE_SEPARATOR)
        categoryHolder.mAccessRule.text = representation
        UiUtils.hideIf(
            rules === AccessRules.ACCESS_RULES_P2P
                    || rules === AccessRules.ACCESS_RULES_PAID
        )
    }

    private fun onMoreOperationClicked(category: BookmarkCategory) {
        if (mCategoryListCallback != null) mCategoryListCallback!!.onMoreOperationClick(category)
    }

    private fun bindSize(
        categoryHolder: CategoryViewHolder,
        category: BookmarkCategory
    ) {
        val template = category.pluralsCountTemplate
        categoryHolder.setSize(template.plurals, template.count)
    }

    private fun bindAuthor(
        categoryHolder: CategoryViewHolder,
        category: BookmarkCategory
    ) {
        val author = category.author
        val authorName: CharSequence? =
            author?.let { getAuthorRepresentation(it, context) }
        categoryHolder.authorName.text = authorName
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) return TYPE_ACTION_HEADER
        return if (position == itemCount - 1 && mType.factory.hasAdapterFooter()) TYPE_ACTION_FOOTER else TYPE_CATEGORY_ITEM
    }

    private fun toCategoryPosition(adapterPosition: Int): Int {
        val type = getItemViewType(adapterPosition)
        if (type != TYPE_CATEGORY_ITEM) throw AssertionError(
            "An element at specified position is not category!"
        )
        // The header "Hide All" is located at first index, so subtraction is needed.
        return adapterPosition - 1
    }

    override fun getItemCount(): Int {
        val count = super.getItemCount()
        return if (count > 0) count + 1 /* header */ + (if (mType.factory.hasAdapterFooter()) 1 else 0) else 0
    }

    val factory: BookmarksPageFactory
        get() = mType.factory

    private inner class LongClickListener internal constructor(private val mHolder: CategoryViewHolder) :
        OnLongClickListener {
        override fun onLongClick(view: View): Boolean {
            if (mLongClickListener != null) {
                mLongClickListener!!.onItemLongClick(view, mHolder.entity)
            }
            return true
        }

    }

    private inner class MassOperationAction : HeaderAction {
        override fun onHideAll() {
            BookmarkManager.INSTANCE.setAllCategoriesVisibility(false, mType)
            notifyDataSetChanged()
        }

        override fun onShowAll() {
            BookmarkManager.INSTANCE.setAllCategoriesVisibility(true, mType)
            notifyDataSetChanged()
        }
    }

    private inner class CategoryItemClickListener internal constructor(private val mHolder: CategoryViewHolder) :
        View.OnClickListener {
        override fun onClick(v: View) {
            if (mClickListener != null) mClickListener!!.onItemClick(v, mHolder.entity)
        }

    }

    private inner class FooterClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            if (mCategoryListCallback != null) mCategoryListCallback!!.onFooterClick()
        }
    }

    private inner class ToggleVisibilityClickListener internal constructor(private val mHolder: CategoryViewHolder) :
        View.OnClickListener {
        override fun onClick(v: View) {
            BookmarkManager.INSTANCE.toggleCategoryVisibility(mHolder.entity.id)
            notifyItemChanged(mHolder.adapterPosition)
            notifyItemChanged(HEADER_POSITION)
        }

    }

    companion object {
        private const val TYPE_CATEGORY_ITEM = 0
        private const val TYPE_ACTION_FOOTER = 1
        private const val TYPE_ACTION_HEADER = 2
        private const val HEADER_POSITION = 0
        private fun getAuthorRepresentation(
            author: BookmarkCategory.Author,
            context: Context
        ): String {
            return UiUtils.PHRASE_SEPARATOR + getRepresentation(
                context,
                author
            )
        }
    }

    init {
        mResProvider = mType.factory.resProvider
    }
}