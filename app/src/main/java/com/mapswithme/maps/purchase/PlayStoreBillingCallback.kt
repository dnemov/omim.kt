package com.mapswithme.maps.purchase

import com.android.billingclient.api.BillingClient.BillingResponse
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails

interface PlayStoreBillingCallback {
    fun onProductDetailsLoaded(details: List<SkuDetails>)
    fun onPurchaseSuccessful(purchases: List<Purchase>)
    fun onPurchaseFailure(@BillingResponse error: Int)
    fun onProductDetailsFailure()
    fun onStoreConnectionFailed()
    fun onPurchasesLoaded(purchases: List<Purchase>)
    fun onConsumptionSuccess()
    fun onConsumptionFailure()
}