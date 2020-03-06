package com.mapswithme.maps.adapter

internal class AdapterIndexAndViewTypeImpl(
    override val index: Int,
    override val relativeViewType: Int
) :
    AdapterIndexAndViewType {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as AdapterIndexAndViewTypeImpl
        return if (index != that.index) false else relativeViewType == that.relativeViewType
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + relativeViewType
        return result
    }

    override fun toString(): String {
        val sb = StringBuilder("AdapterIndexAndViewTypeImpl{")
        sb.append("mIndex=").append(index)
        sb.append(", mViewType=").append(relativeViewType)
        sb.append('}')
        return sb.toString()
    }

}