package com.mapswithme.maps.gallery

import android.view.LayoutInflater
import android.view.ViewGroup
import com.mapswithme.maps.MwmApplication

abstract class SimpleSingleItemAdapterStrategy<T : Holders.BaseViewHolder<Items.Item>> protected constructor(
    listener: ItemSelectedListener<Items.Item>?,
    url: String? = null
) : SingleItemAdapterStrategy<T>(url, listener) {
    override fun buildItem(url: String?) {
        val res = MwmApplication.get().resources
        mItems.add(Items.Item(res.getString(title), null, null))
    }

    override fun createViewHolder(parent: ViewGroup, viewType: Int): T {
        val itemView =
            inflateView(LayoutInflater.from(parent.context), parent)
        return createViewHolder(itemView)
    }

    protected override val subtitle: Int
        protected get() {
            throw UnsupportedOperationException("Subtitle is not supported by this strategy!")
        }

    protected override val labelForDetailsView: Int
        protected get() {
            throw UnsupportedOperationException("Details button is not supported by this strategy!")
        }
}