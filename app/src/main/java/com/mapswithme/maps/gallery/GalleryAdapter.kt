package com.mapswithme.maps.gallery

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class GalleryAdapter<VH : Holders.BaseViewHolder<I>?, I : Items.Item?>(
    private val mStrategy: AdapterStrategy<VH, I>
) : RecyclerView.Adapter<VH>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return mStrategy.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        mStrategy.onBindViewHolder(holder, position)
    }

    override fun getItemCount(): Int {
        return mStrategy.itemCount
    }

    override fun getItemViewType(position: Int): Int {
        return mStrategy.getItemViewType(position)
    }

}