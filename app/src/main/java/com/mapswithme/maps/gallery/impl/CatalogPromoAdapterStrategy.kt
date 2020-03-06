package com.mapswithme.maps.gallery.impl

import android.view.LayoutInflater
import android.view.ViewGroup
import com.facebook.internal.Mutable
import com.mapswithme.maps.R
import com.mapswithme.maps.gallery.Holders
import com.mapswithme.maps.gallery.Holders.CatalogPromoHolder
import com.mapswithme.maps.gallery.Holders.GenericMoreHolder
import com.mapswithme.maps.gallery.ItemSelectedListener
import com.mapswithme.maps.gallery.RegularAdapterStrategy
import com.mapswithme.maps.promo.PromoEntity


internal class CatalogPromoAdapterStrategy(
    items: MutableList<PromoEntity>, moreItem: PromoEntity?,
    listener: ItemSelectedListener<PromoEntity>?
) : RegularAdapterStrategy<PromoEntity>(
    items,
    moreItem,
    listener,
    MAX_ITEMS
) {

    @Suppress("UNCHECKED_CAST")
    override fun createProductViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holders.BaseViewHolder<PromoEntity> {
        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.catalog_promo_item_card, parent,
                false
            )
        return CatalogPromoHolder(view, mItems, listener as ItemSelectedListener<PromoEntity>?)
    }

    @Suppress("UNCHECKED_CAST")
    override fun createMoreProductsViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holders.BaseViewHolder<PromoEntity> {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_search_more, parent, false)
        return GenericMoreHolder<PromoEntity>(view, mItems, listener as ItemSelectedListener<PromoEntity>?)
    }

    companion object {
        private const val MAX_ITEMS = 3
    }
}