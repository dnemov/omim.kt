package com.mapswithme.maps.taxi

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
class TaxiInfo private constructor(
    private var taxiType: Int,
    private val productsList: Array<Product>
) : Parcelable {

    val type: TaxiType
        get() = TaxiType.values()[taxiType]

    val products: List<Product>
        get() = productsList.toList()

    @Parcelize
    class Product private constructor(
        val productId: String, val name: String, val time: String,
        val price: String, val currency: String
    ) : Parcelable
}