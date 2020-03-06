package com.mapswithme.maps.gallery.impl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mapswithme.maps.R
import com.mapswithme.maps.gallery.Holders.CrossPromoLoadingHolder
import com.mapswithme.maps.gallery.Holders.SimpleViewHolder
import com.mapswithme.maps.gallery.ItemSelectedListener
import com.mapswithme.maps.gallery.Items

internal class CatalogPromoLoadingAdapterStrategy(
    listener: ItemSelectedListener<Items.Item>?,
    url: String?
) : SimpleLoadingAdapterStrategy(listener, url) {
    override fun inflateView(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): View {
        return inflater.inflate(R.layout.catalog_promo_placeholder_card, parent, false)
    }

    override fun createViewHolder(itemView: View): SimpleViewHolder {
        return CrossPromoLoadingHolder(itemView, mItems, listener)
    }
}