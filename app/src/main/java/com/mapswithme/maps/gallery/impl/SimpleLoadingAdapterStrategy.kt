package com.mapswithme.maps.gallery.impl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mapswithme.maps.R
import com.mapswithme.maps.gallery.Holders.SimpleViewHolder
import com.mapswithme.maps.gallery.ItemSelectedListener
import com.mapswithme.maps.gallery.Items
import com.mapswithme.maps.gallery.SimpleSingleItemAdapterStrategy

open class SimpleLoadingAdapterStrategy(
    listener: ItemSelectedListener<Items.Item>?,
    url: String?
) : SimpleSingleItemAdapterStrategy<SimpleViewHolder>(listener, url) {
    internal constructor(listener: ItemSelectedListener<Items.Item>?) : this(
        listener,
        null
    ) {
    }

    override val title: Int
        get() = R.string.discovery_button_other_loading_message

    override fun inflateView(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): View {
        return inflater.inflate(R.layout.item_discovery_simple_loading, parent, false)
    }

    override fun createViewHolder(itemView: View): SimpleViewHolder {
        return SimpleViewHolder(itemView, mItems, listener)
    }
}