package com.mapswithme.maps.gallery

import android.view.ViewGroup
import com.mapswithme.maps.gallery.Constants.TYPE_MORE
import com.mapswithme.maps.gallery.Constants.TYPE_PRODUCT

abstract class RegularAdapterStrategy<T : RegularAdapterStrategy.Item> @JvmOverloads constructor(
    items: MutableList<T>,
    moreItem: T?,
    listener: ItemSelectedListener<T>?,
    maxItems: Int = MAX_ITEMS_BY_DEFAULT
) : AdapterStrategy<Holders.BaseViewHolder<T>, T>(listener as ItemSelectedListener<Items.Item>) {
    override fun createViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holders.BaseViewHolder<T> {
        return when (viewType) {
            TYPE_PRODUCT -> createProductViewHolder(parent, viewType)
            TYPE_MORE -> createMoreProductsViewHolder(parent, viewType)
            else -> throw UnsupportedOperationException(
                "This strategy doesn't support specified view type: "
                        + viewType
            )
        }
    }

    override fun onBindViewHolder(
        holder: Holders.BaseViewHolder<T>?,
        position: Int
    ) {
        holder!!.bind(mItems[position])
    }

    override fun getItemViewType(position: Int): Int {
        return mItems[position]!!.type
    }

    protected abstract fun createProductViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holders.BaseViewHolder<T>

    protected abstract fun createMoreProductsViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holders.BaseViewHolder<T>

    open class Item(
        @field:Constants.ViewType @param:Constants.ViewType val type: Int, title: String,
        subtitle: String?, url: String?
    ) : Items.Item(title, url, subtitle)

    companion object {
        private const val MAX_ITEMS_BY_DEFAULT = 5
    }

    init {
        val showMoreItem = moreItem != null && items.size >= maxItems
        val size = if (showMoreItem) maxItems else items.size
        for (i in 0 until size) {
            val product = items[i]
            mItems.add(product)
        }
        if (showMoreItem) mItems.add(moreItem!!)
    }
}