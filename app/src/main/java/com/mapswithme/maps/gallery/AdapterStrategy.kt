package com.mapswithme.maps.gallery

import android.view.ViewGroup
import com.facebook.internal.Mutable
import java.util.*

abstract class AdapterStrategy<VH : Holders.BaseViewHolder<I>?, I : Items.Item?> internal constructor(
    protected val listener: ItemSelectedListener<Items.Item>?
) {
    @JvmField
    protected val mItems: MutableList<I> = ArrayList()
    abstract fun createViewHolder(parent: ViewGroup, viewType: Int): VH
    abstract fun onBindViewHolder(
        holder: Holders.BaseViewHolder<I>?,
        position: Int
    )

    abstract fun getItemViewType(position: Int): Int
    val itemCount: Int
        get() = mItems.size

}