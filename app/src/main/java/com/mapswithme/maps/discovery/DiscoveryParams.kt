package com.mapswithme.maps.discovery

import androidx.annotation.IntDef
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.util.*

/**
 * Represents discovery::ClientParams from core.
 */
class DiscoveryParams(
    val mCurrency: String?, val mLang: String?, val mItemsCount: Int,
    @param:ItemType vararg val mItemTypes: Int
) {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        ITEM_TYPE_ATTRACTIONS,
        ITEM_TYPE_CAFES,
        ITEM_TYPE_HOTELS,
        ITEM_TYPE_LOCAL_EXPERTS,
        ITEM_TYPE_PROMO
    )
    internal annotation class ItemType

    override fun toString(): String {
        return "DiscoveryParams{" +
                "mCurrency='" + mCurrency + '\'' +
                ", mLang='" + mLang + '\'' +
                ", mItemsCount=" + mItemsCount +
                ", mItemTypes=" + Arrays.toString(mItemTypes) +
                '}'
    }

    companion object {
        const val ITEM_TYPE_ATTRACTIONS = 0
        const val ITEM_TYPE_CAFES = 1
        const val ITEM_TYPE_HOTELS = 2
        const val ITEM_TYPE_LOCAL_EXPERTS = 3
        const val ITEM_TYPE_PROMO = 4
    }

}