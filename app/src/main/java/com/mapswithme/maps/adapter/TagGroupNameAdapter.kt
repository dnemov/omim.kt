package com.mapswithme.maps.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.maps.adapter.TagGroupNameAdapter.TagGroupNameHolder
import com.mapswithme.maps.bookmarks.data.CatalogTagsGroup

class TagGroupNameAdapter(private val mTagsGroups: List<CatalogTagsGroup>) :
    RecyclerView.Adapter<TagGroupNameHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagGroupNameHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.tags_category, parent, false)
        return TagGroupNameHolder(itemView)
    }

    override fun onBindViewHolder(holder: TagGroupNameHolder, position: Int) {
        val item = mTagsGroups[position]
        holder.mText.text = item.localizedName
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return mTagsGroups.size
    }

    class TagGroupNameHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val mText: TextView

        init {
            mText = itemView.findViewById(R.id.text)
        }
    }

    init {
        setHasStableIds(true)
    }
}