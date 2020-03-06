package com.mapswithme.maps.purchase

import com.android.billingclient.api.BillingClient.BillingResponse
import com.android.billingclient.api.SkuDetails

interface PurchaseCallback {
    fun onProductDetailsLoaded(details: List<SkuDetails>?)
    fun onPaymentFailure(@BillingResponse error: Int)
    fun onProductDetailsFailure()
    fun onStoreConnectionFailed()
    fun onValidationStarted()
    fun onValidationFinish(success: Boolean)
}