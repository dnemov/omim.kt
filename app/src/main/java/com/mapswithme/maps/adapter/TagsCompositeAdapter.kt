package com.mapswithme.maps.adapter

import android.content.Context
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.facebook.internal.Mutable
import com.google.android.flexbox.FlexboxLayoutManager
import com.mapswithme.maps.R
import com.mapswithme.maps.adapter.TagsAdapter.SelectionState.Companion.from
import com.mapswithme.maps.adapter.TagsAdapter.TagViewHolder
import com.mapswithme.maps.adapter.TagsCompositeAdapter.TagsRecyclerHolder
import com.mapswithme.maps.bookmarks.data.CatalogTag
import com.mapswithme.maps.bookmarks.data.CatalogTagsGroup
import com.mapswithme.maps.widget.recycler.TagItemDecoration
import com.mapswithme.maps.widget.recycler.UgcRouteTagItemDecorator
import java.util.*

class TagsCompositeAdapter(
    private val mContext: Context,
    private val mCatalogTagsGroups: MutableList<CatalogTagsGroup>,
    savedState: MutableList<CatalogTag>,
    clickListener: OnItemClickListener<Pair<TagsAdapter, TagViewHolder>>,
    selectedTagsLimit: Int
) : RecyclerView.Adapter<TagsRecyclerHolder>() {
    private val mComponentHolders: List<ComponentHolder>
    private fun makeRecyclerComponents(
        context: Context,
        groups: MutableList<CatalogTagsGroup>,
        savedState: MutableList<CatalogTag>,
        externalListener: OnItemClickListener<Pair<TagsAdapter, TagViewHolder>>,
        selectedTagsLimit: Int
    ): List<ComponentHolder> {
        val result: MutableList<ComponentHolder> =
            ArrayList()
        val selectionPolicy = object : SelectionPolicy {
            override val isTagsSelectionAllowed: Boolean
                get() = selectedTags.size < selectedTagsLimit
        }
        for (i in groups.indices) {
            val each = groups[i]
            val state = from(savedState, each)
            val listener: OnItemClickListener<TagViewHolder> =
                TagsListClickListener(externalListener, i)
            val adapter = TagsAdapter(listener, state, each.tags, selectionPolicy)
            val res = context.resources
            val divider =
                res.getDrawable(R.drawable.divider_transparent_base)
            val decor: TagItemDecoration = UgcRouteTagItemDecorator(divider)
            val holder = ComponentHolder(adapter, decor)
            result.add(holder)
        }
        return result
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagsRecyclerHolder {
        val inflater = LayoutInflater.from(parent.context)
        return TagsRecyclerHolder(inflater.inflate(R.layout.tags_recycler, parent, false))
    }

    override fun onBindViewHolder(holder: TagsRecyclerHolder, position: Int) {
        val componentHolder = mComponentHolders[position]
        holder.mRecycler.layoutManager = FlexboxLayoutManager(mContext)
        holder.mRecycler.itemAnimator = null
        initDecor(holder, componentHolder)
        holder.mRecycler.adapter = componentHolder.mAdapter
    }

    private fun initDecor(
        holder: TagsRecyclerHolder,
        componentHolder: ComponentHolder
    ) {
        val decorationCount = holder.mRecycler.itemDecorationCount
        for (i in 0 until decorationCount) {
            holder.mRecycler.removeItemDecorationAt(i)
        }
        holder.mRecycler.addItemDecoration(componentHolder.mDecor)
    }

    override fun getItemCount(): Int {
        return mCatalogTagsGroups.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    val selectedTags: Collection<CatalogTag>
        get() {
            val tags: MutableList<CatalogTag> = ArrayList()
            for (each in mComponentHolders) {
                tags.addAll(each.mAdapter.selectedTags)
            }
            return Collections.unmodifiableList(tags)
        }

    fun hasSelectedItems(): Boolean {
        for (each in mComponentHolders) {
            if (!each.mAdapter.selectedTags.isEmpty()) return true
        }
        return false
    }

    fun getItem(index: Int): TagsAdapter {
        return mComponentHolders[index].mAdapter
    }

    class ComponentHolder internal constructor(
        val mAdapter: TagsAdapter,
        val mDecor: ItemDecoration
    )

    class TagsRecyclerHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val mRecycler: RecyclerView

        init {
            mRecycler = itemView.findViewById(R.id.recycler)
        }
    }

    private inner class TagsListClickListener internal constructor(
        private val mListener: OnItemClickListener<Pair<TagsAdapter, TagViewHolder>>,
        private val mIndex: Int
    ) : OnItemClickListener<TagViewHolder> {
        override fun onItemClick(v: View, item: TagViewHolder) {
            val components = mComponentHolders[mIndex]
            val pair =
                Pair(components.mAdapter, item)
            mListener.onItemClick(v, pair)
        }
    }

    interface SelectionPolicy {
        val isTagsSelectionAllowed: Boolean
    }

    init {
        mComponentHolders = makeRecyclerComponents(
            mContext, mCatalogTagsGroups, savedState, clickListener,
            selectedTagsLimit
        )
        setHasStableIds(true)
    }
}