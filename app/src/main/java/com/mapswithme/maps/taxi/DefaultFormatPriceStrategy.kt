package com.mapswithme.maps.taxi

internal class DefaultFormatPriceStrategy : FormatPriceStrategy {
    override fun format(product: TaxiInfo.Product): String {
        return product.price
    }
}