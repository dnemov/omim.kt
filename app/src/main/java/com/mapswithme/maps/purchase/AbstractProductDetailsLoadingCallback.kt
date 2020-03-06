package com.mapswithme.maps.purchase

import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails

abstract class AbstractProductDetailsLoadingCallback : PlayStoreBillingCallback {
    abstract override fun onProductDetailsLoaded(details: List<SkuDetails>)
    abstract override fun onProductDetailsFailure()
    override fun onPurchaseSuccessful(purchases: List<Purchase>) { // Do nothing.
    }

    override fun onPurchaseFailure(error: Int) { // Do nothing.
    }

    override fun onStoreConnectionFailed() { // Do nothing.
    }

    override fun onPurchasesLoaded(purchases: List<Purchase>) { // Do nothing.
    }

    override fun onConsumptionSuccess() { // Do nothing.
    }

    override fun onConsumptionFailure() { // Do nothing.
    }
}