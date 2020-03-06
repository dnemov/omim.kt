package com.mapswithme.maps.purchase

import com.android.billingclient.api.BillingClient

internal class QueryExistingPurchases(
    client: BillingClient, productType: String,
    callback: PlayStoreBillingCallback?
) : PlayStoreBillingRequest<PlayStoreBillingCallback?>(client, productType, callback) {
    override fun execute() {
        val purchasesResult = client.queryPurchases(productType) ?: return
        val purchases = purchasesResult.purchasesList ?: return
        if (callback != null) callback.onPurchasesLoaded(purchases)
    }
}