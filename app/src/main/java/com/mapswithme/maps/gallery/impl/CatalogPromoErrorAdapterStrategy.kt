package com.mapswithme.maps.gallery.impl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mapswithme.maps.R
import com.mapswithme.maps.gallery.Holders.CatalogErrorHolder
import com.mapswithme.maps.gallery.Holders.SimpleViewHolder
import com.mapswithme.maps.gallery.ItemSelectedListener
import com.mapswithme.maps.gallery.Items

internal class CatalogPromoErrorAdapterStrategy(listener: ItemSelectedListener<Items.Item>?) :
    SimpleErrorAdapterStrategy(listener) {
    override fun inflateView(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): View {
        return inflater.inflate(R.layout.catalog_promo_placeholder_card, parent, false)
    }

    override fun createViewHolder(itemView: View): SimpleViewHolder {
        return CatalogErrorHolder(itemView, mItems, listener)
    }
}