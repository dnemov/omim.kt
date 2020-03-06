package com.mapswithme.maps.routing

class SingleLaneInfo internal constructor(var mLane: ByteArray, var mIsActive: Boolean) {
    override fun toString(): String {
        val initialCapacity = 32
        val sb = StringBuilder(initialCapacity)
        sb.append("Is the lane active? ").append(mIsActive).append(". The lane directions IDs are")
        for (i in mLane) sb.append(" ").append(i.toInt())
        return sb.toString()
    }

}