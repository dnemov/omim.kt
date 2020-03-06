package com.mapswithme.maps.adapter

internal class AdapterIndexAndPositionImpl(
    override val index: Int,
    override val relativePosition: Int
) :
    AdapterIndexAndPosition {

    override fun toString(): String {
        val sb = StringBuilder("AdapterIndexAndPositionImpl{")
        sb.append("mIndex=").append(index)
        sb.append(", mRelativePosition=").append(relativePosition)
        sb.append('}')
        return sb.toString()
    }

}