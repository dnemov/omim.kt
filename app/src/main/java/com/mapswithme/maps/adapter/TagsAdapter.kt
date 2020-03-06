package com.mapswithme.maps.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.maps.adapter.TagsAdapter.TagViewHolder
import com.mapswithme.maps.adapter.TagsCompositeAdapter.SelectionPolicy
import com.mapswithme.maps.bookmarks.data.CatalogTag
import com.mapswithme.maps.bookmarks.data.CatalogTagsGroup
import com.mapswithme.maps.ugc.routes.TagsResFactory
import java.util.*

class TagsAdapter internal constructor(
    listener: OnItemClickListener<TagViewHolder>, state: SelectionState,
    tags: List<CatalogTag>,
    selectionPolicy: SelectionPolicy
) : RecyclerView.Adapter<TagViewHolder>() {
    private val mState: SelectionState
    private val mListener: OnItemClickListener<TagViewHolder>
    private val mTags: List<CatalogTag>
    private val mSelectionPolicy: SelectionPolicy
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.tag_item, parent, false)
        return TagViewHolder(itemView, mListener)
    }

    override fun getItemId(position: Int): Long {
        return mTags[position].hashCode().toLong()
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tag = mTags[position]
        val isTagSelected = mState.contains(tag)
        holder.itemView.isSelected = isTagSelected
        holder.mTag = tag
        val context = holder.itemView.context
        holder.mText.text = tag.localizedName
        val isEnabled = mSelectionPolicy.isTagsSelectionAllowed || isTagSelected
        holder.itemView.isEnabled = isEnabled
        val selector =
            TagsResFactory.makeSelector(context, tag.color)
        holder.itemView.setBackgroundDrawable(selector)
        val color = TagsResFactory.makeColor(context, tag.color)
        holder.mText.setTextColor(color)
        holder.mText.isSelected = isTagSelected
        holder.mText.isEnabled = isEnabled
    }

    override fun getItemCount(): Int {
        return mTags.size
    }

    val selectedTags: Collection<CatalogTag>
        get() = Collections.unmodifiableCollection(mState.mTags)

    class SelectionState private constructor() {
        val mTags: MutableList<CatalogTag> =
            ArrayList()

        fun addAll(vararg tags: CatalogTag) {
            mTags.addAll(Arrays.asList(*tags))
        }

        private fun addAll(tags: List<CatalogTag>) {
            mTags.addAll(tags)
        }

        fun removeAll(vararg tags: CatalogTag): Boolean {
            return mTags.removeAll(Arrays.asList(*tags))
        }

        private fun remove(tags: List<CatalogTag>): Boolean {
            return mTags.removeAll(tags)
        }

        operator fun contains(tag: CatalogTag): Boolean {
            return mTags.contains(tag)
        }

        companion object {
            fun empty(): SelectionState {
                return SelectionState()
            }

            @JvmStatic
            fun from(savedTags: MutableList<CatalogTag>, src: CatalogTagsGroup): TagsAdapter.SelectionState {

                val state = empty()
                for (each in savedTags) {
                    if (src.tags.contains(each)) state.addAll(each)
                }
                return state
            }
        }
    }

    class TagViewHolder internal constructor(
        itemView: View,
        listener: OnItemClickListener<TagViewHolder>
    ) : RecyclerView.ViewHolder(itemView) {
        val mText: TextView
        private val mListener: OnItemClickListener<TagViewHolder>
        var mTag: CatalogTag? = null
        val entity: CatalogTag
            get() = mTag!!

        init {
            mText = itemView.findViewById(R.id.text)
            mListener = listener
            itemView.setOnClickListener { v: View? ->
                mListener.onItemClick(
                    v!!,
                    this
                )
            }
        }
    }

    private inner class ClickListenerWrapper internal constructor(private val mListener: OnItemClickListener<TagViewHolder>) :
        OnItemClickListener<TagViewHolder> {

        override fun onItemClick(v: View, item: TagViewHolder) {
            if (mState.contains(item.entity)) mState.removeAll(item.entity) else mState.addAll(
                item.entity
            )
            mListener.onItemClick(v, item)
        }

    }

    init {
        mListener = ClickListenerWrapper(listener)
        mState = state
        mTags = tags
        mSelectionPolicy = selectionPolicy
        setHasStableIds(true)
    }
}