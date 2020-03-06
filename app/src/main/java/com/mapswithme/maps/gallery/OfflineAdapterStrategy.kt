package com.mapswithme.maps.gallery

import android.view.View
import com.mapswithme.maps.R
import com.mapswithme.maps.gallery.Holders.OfflineViewHolder

abstract class OfflineAdapterStrategy protected constructor(
    url: String?,
    listener: ItemSelectedListener<Items.Item>?
) : SingleItemAdapterStrategy<OfflineViewHolder>(url, listener) {
    override fun createViewHolder(itemView: View): OfflineViewHolder {
        return OfflineViewHolder(itemView, mItems, listener)
    }

    protected override val labelForDetailsView: Int
        protected get() = R.string.details
}