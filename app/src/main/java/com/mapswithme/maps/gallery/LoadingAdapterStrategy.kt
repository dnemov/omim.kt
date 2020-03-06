package com.mapswithme.maps.gallery

import android.view.View
import com.mapswithme.maps.gallery.Holders.LoadingViewHolder

abstract class LoadingAdapterStrategy protected constructor(
    url: String?,
    listener: ItemSelectedListener<Items.Item>?
) : SingleItemAdapterStrategy<LoadingViewHolder>(url, listener) {
    override fun createViewHolder(itemView: View): LoadingViewHolder {
        return LoadingViewHolder(itemView, mItems, listener)
    }
}