package com.mapswithme.maps.gallery.impl

import android.view.LayoutInflater
import android.view.ViewGroup
import com.mapswithme.maps.R
import com.mapswithme.maps.discovery.LocalExpert
import com.mapswithme.maps.gallery.*
import com.mapswithme.maps.gallery.Holders.GenericMoreHolder
import com.mapswithme.maps.gallery.Holders.LocalExpertViewHolder
import com.mapswithme.maps.gallery.Items.LocalExpertItem
import com.mapswithme.maps.gallery.Items.LocalExpertMoreItem
import java.util.*

class LocalExpertsAdapterStrategy internal constructor(
    experts: Array<LocalExpert>, moreUrl: String?,
    listener: ItemSelectedListener<LocalExpertItem>?
) : RegularAdapterStrategy<LocalExpertItem>(
    convertItems(
        experts
    ), LocalExpertMoreItem(moreUrl), listener
) {
    override fun createProductViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holders.BaseViewHolder<LocalExpertItem> {
        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_discovery_expert, parent,
                false
            )
        return LocalExpertViewHolder(view, mItems, listener as ItemSelectedListener<LocalExpertItem>)
    }

    override fun createMoreProductsViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holders.BaseViewHolder<LocalExpertItem> {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_viator_more, parent,
            false
        )
        return GenericMoreHolder<LocalExpertItem>(view, mItems, listener as ItemSelectedListener<Items.LocalExpertItem>)
    }

    companion object {
        private fun convertItems(items: Array<LocalExpert>): MutableList<LocalExpertItem> {
            val viewItems: MutableList<LocalExpertItem> =
                ArrayList()
            for (expert in items) {
                viewItems.add(
                    LocalExpertItem(
                        Constants.TYPE_PRODUCT,
                        expert.name,
                        expert.pageUrl,
                        expert.photoUrl,
                        expert.price,
                        expert.currency,
                        expert.rating
                    )
                )
            }
            return viewItems
        }
    }
}