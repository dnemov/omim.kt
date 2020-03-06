package com.mapswithme.maps.adapter

interface AdapterPositionConverter {
    fun toRelativePositionAndAdapterIndex(absPosition: Int): AdapterIndexAndPosition
    fun toRelativeViewTypeAndAdapterIndex(absViewType: Int): AdapterIndexAndViewType
    fun toAbsoluteViewType(relViewType: Int, adapterIndex: Int): Int
}