package com.mapswithme.maps.gallery.impl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mapswithme.maps.R
import com.mapswithme.maps.gallery.ItemSelectedListener
import com.mapswithme.maps.gallery.Items

class LocalExpertsLoadingAdapterStrategy internal constructor(listener: ItemSelectedListener<Items.Item>?) :
    SimpleLoadingAdapterStrategy(listener) {
    override fun inflateView(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): View {
        return inflater.inflate(R.layout.item_discovery_expert_loading, parent, false)
    }
}