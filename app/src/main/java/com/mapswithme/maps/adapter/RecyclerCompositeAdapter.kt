package com.mapswithme.maps.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class RecyclerCompositeAdapter @SafeVarargs constructor(
    private val mIndexConverter: AdapterPositionConverter,
    vararg adapters: RecyclerView.Adapter<out RecyclerView.ViewHolder>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val mAdapters: MutableList<RecyclerView.Adapter<out RecyclerView.ViewHolder>> =
        ArrayList()

    init {
        mAdapters.addAll(mutableListOf(*adapters))
    }

    override fun getItemCount(): Int {
        var total = 0
        for (each in mAdapters) {
            total += each.itemCount
        }
        return total
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        absViewType: Int
    ): RecyclerView.ViewHolder {
        val indexAndViewType =
            mIndexConverter.toRelativeViewTypeAndAdapterIndex(absViewType)
        val adapterIndex = indexAndViewType.index
        val relViewType = indexAndViewType.relativeViewType
        val adapter = mAdapters[adapterIndex]
        return adapter.onCreateViewHolder(parent, relViewType)
    }

    override fun getItemViewType(position: Int): Int {
        val indexAndPosition =
            mIndexConverter.toRelativePositionAndAdapterIndex(position)
        val adapterIndex = indexAndPosition.index
        val adapter = mAdapters[adapterIndex]
        val relViewType = adapter.getItemViewType(indexAndPosition.relativePosition)
        return mIndexConverter.toAbsoluteViewType(relViewType, adapterIndex)
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val indexAndPosition =
            mIndexConverter.toRelativePositionAndAdapterIndex(position)
        val adapterIndex = indexAndPosition.index
        val adapter = mAdapters[adapterIndex]
        val relPosition = indexAndPosition.relativePosition
        bindViewHolder(adapter, holder, relPosition)
    }

    private fun <Holder : RecyclerView.ViewHolder?> bindViewHolder(
        adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>,
        holder: Holder,
        position: Int
    ) {
        (adapter as RecyclerView.Adapter<Holder>).onBindViewHolder(holder, position)
    }

    abstract class AbstractAdapterPositionConverter : AdapterPositionConverter {
        override fun toRelativePositionAndAdapterIndex(absPosition: Int): AdapterIndexAndPosition {
            return indexAndPositionItems[absPosition]
        }

        override fun toRelativeViewTypeAndAdapterIndex(absViewType: Int): AdapterIndexAndViewType {
            return indexAndViewTypeItems[absViewType]
        }

        override fun toAbsoluteViewType(relViewType: Int, adapterIndex: Int): Int {
            val indexAndViewType: AdapterIndexAndViewType =
                AdapterIndexAndViewTypeImpl(adapterIndex, relViewType)
            val items = indexAndViewTypeItems
            val indexOf = items.indexOf(indexAndViewType)
            require(indexOf >= 0) {
                "Item " + indexAndViewType + " not found in list : " +
                        Arrays.toString(items.toTypedArray())
            }
            return indexOf
        }

        protected abstract val indexAndViewTypeItems: List<AdapterIndexAndViewType>
        protected abstract val indexAndPositionItems: List<AdapterIndexAndPosition>
    }
}