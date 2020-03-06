package com.mapswithme.maps.widget.placepage

data class HotelPriceInfo(
    val id: String, val price: String, val currency: String,
    val discount: Int, private val mSmartDeal: Boolean
) {

    fun hasSmartDeal(): Boolean {
        return mSmartDeal
    }
}