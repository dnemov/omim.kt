package com.mapswithme.maps.gallery.impl

import android.view.LayoutInflater
import android.view.ViewGroup
import com.mapswithme.maps.R
import com.mapswithme.maps.gallery.Holders
import com.mapswithme.maps.gallery.Holders.SearchMoreHolder
import com.mapswithme.maps.gallery.Holders.SearchViewHolder
import com.mapswithme.maps.gallery.ItemSelectedListener
import com.mapswithme.maps.gallery.Items.SearchItem
import com.mapswithme.maps.gallery.RegularAdapterStrategy
import com.mapswithme.maps.search.SearchResult
import java.util.*

internal class SearchBasedAdapterStrategy private constructor(
    items: MutableList<SearchItem>,
    moreItem: SearchItem?,
    listener: ItemSelectedListener<SearchItem>?
) : RegularAdapterStrategy<SearchItem>(items, moreItem, listener) {
    constructor(
        results: Array<SearchResult>, moreItem: SearchItem?,
        listener: ItemSelectedListener<SearchItem>?
    ) : this(convertItems(results), moreItem, listener) {
    }

    override fun createProductViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holders.BaseViewHolder<SearchItem> {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_discovery_search, parent, false)
        return SearchViewHolder(view, mItems, listener as ItemSelectedListener<SearchItem>)
    }

    override fun createMoreProductsViewHolder(
        parent: ViewGroup, viewType: Int
    ): Holders.BaseViewHolder<SearchItem> {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_search_more, parent,
            false
        )
        return SearchMoreHolder(view, mItems, listener as ItemSelectedListener<SearchItem>)
    }

    companion object {
        fun convertItems(results: Array<SearchResult>): MutableList<SearchItem> {
            val viewItems: MutableList<SearchItem> =
                ArrayList()
            for (result in results) viewItems.add(
                SearchItem(result)
            )
            return viewItems
        }
    }
}