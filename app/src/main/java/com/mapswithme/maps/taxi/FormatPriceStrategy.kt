package com.mapswithme.maps.taxi

interface FormatPriceStrategy {
    fun format(product: TaxiInfo.Product): String
}