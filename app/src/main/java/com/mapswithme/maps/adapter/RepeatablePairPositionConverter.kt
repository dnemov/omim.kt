package com.mapswithme.maps.adapter

import android.util.Pair
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.adapter.RecyclerCompositeAdapter.AbstractAdapterPositionConverter
import java.util.*

class RepeatablePairPositionConverter(
    first: RecyclerView.Adapter<out RecyclerView.ViewHolder?>,
    second: RecyclerView.Adapter<out RecyclerView.ViewHolder?>
) : AbstractAdapterPositionConverter() {
    override val indexAndPositionItems: List<AdapterIndexAndPosition>
    override val indexAndViewTypeItems: List<AdapterIndexAndViewType>

    companion object {
        private const val FIRST_ADAPTER_INDEX = 0
        private const val SECOND_ADAPTER_INDEX = 1
        private fun mixDataSet(
            first: RecyclerView.Adapter<out RecyclerView.ViewHolder?>,
            second: RecyclerView.Adapter<out RecyclerView.ViewHolder?>
        ): Pair<List<AdapterIndexAndPosition>, List<AdapterIndexAndViewType>> {
            val indexAndPositions: MutableList<AdapterIndexAndPosition> =
                ArrayList()
            val indexAndViewTypes: MutableList<AdapterIndexAndViewType> =
                ArrayList()
            val secondAdapterCount = second.itemCount
            val firstAdapterCount = first.itemCount
            require(secondAdapterCount == firstAdapterCount) { "firstAdapterCount different from secondAdapterCount" }
            for (i in 0 until secondAdapterCount) {
                indexAndPositions.add(
                    AdapterIndexAndPositionImpl(
                        FIRST_ADAPTER_INDEX,
                        i
                    )
                )
                indexAndPositions.add(
                    AdapterIndexAndPositionImpl(
                        SECOND_ADAPTER_INDEX,
                        i
                    )
                )
                val viewTypeFirst: AdapterIndexAndViewType = AdapterIndexAndViewTypeImpl(
                    FIRST_ADAPTER_INDEX,
                    first.getItemViewType(i)
                )
                val viewTypeSecond: AdapterIndexAndViewType = AdapterIndexAndViewTypeImpl(
                    SECOND_ADAPTER_INDEX,
                    second.getItemViewType(i)
                )
                if (!indexAndViewTypes.contains(viewTypeFirst)) indexAndViewTypes.add(viewTypeFirst)
                if (!indexAndViewTypes.contains(viewTypeSecond)) indexAndViewTypes.add(
                    viewTypeSecond
                )
            }
            return Pair(
                indexAndPositions,
                indexAndViewTypes
            )
        }
    }

    init {
        val pair =
            mixDataSet(
                first,
                second
            )
        indexAndPositionItems = pair.first
        indexAndViewTypeItems = pair.second
    }
}