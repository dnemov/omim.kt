package com.mapswithme.maps.taxi

import com.mapswithme.util.Utils

internal class LocaleDependentFormatPriceStrategy : FormatPriceStrategy {
    override fun format(product: TaxiInfo.Product): String {
        return Utils.formatCurrencyString(product.price, product.currency)
    }
}