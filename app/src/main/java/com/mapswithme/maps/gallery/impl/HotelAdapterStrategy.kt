package com.mapswithme.maps.gallery.impl

import android.view.LayoutInflater
import android.view.ViewGroup
import com.mapswithme.maps.R
import com.mapswithme.maps.gallery.Holders
import com.mapswithme.maps.gallery.Holders.HotelViewHolder
import com.mapswithme.maps.gallery.Holders.SearchMoreHolder
import com.mapswithme.maps.gallery.ItemSelectedListener
import com.mapswithme.maps.gallery.Items.MoreSearchItem
import com.mapswithme.maps.gallery.Items.SearchItem
import com.mapswithme.maps.gallery.RegularAdapterStrategy
import com.mapswithme.maps.gallery.impl.SearchBasedAdapterStrategy
import com.mapswithme.maps.search.SearchResult

class HotelAdapterStrategy private constructor(
    items: MutableList<SearchItem>,
    moreItem: SearchItem?,
    listener: ItemSelectedListener<SearchItem>?
) : RegularAdapterStrategy<SearchItem>(items, moreItem, listener) {
    internal constructor(
        results: Array<SearchResult>,
        listener: ItemSelectedListener<SearchItem>?
    ) : this(
        SearchBasedAdapterStrategy.convertItems(results).toMutableList(),
        MoreSearchItem(),
        listener
    ) {
    }

    override fun createProductViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holders.BaseViewHolder<SearchItem> {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_discovery_hotel_product, parent, false)
        return HotelViewHolder(view, mItems, listener as ItemSelectedListener<SearchItem>)
    }

    override fun createMoreProductsViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holders.BaseViewHolder<SearchItem> {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_search_more, parent,
            false
        )
        return SearchMoreHolder(view, mItems, listener as ItemSelectedListener<SearchItem>)
    }
}