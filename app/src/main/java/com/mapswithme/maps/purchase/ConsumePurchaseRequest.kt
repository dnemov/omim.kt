package com.mapswithme.maps.purchase

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponse

class ConsumePurchaseRequest internal constructor(
    client: BillingClient,
    productType: String,
    callback: PlayStoreBillingCallback?,
    private val mPurchaseToken: String
) : PlayStoreBillingRequest<PlayStoreBillingCallback?>(client, productType, callback) {
    override fun execute() {
        client.consumeAsync(
            mPurchaseToken
        ) { responseCode: Int, purchaseToken: String ->
            onConsumeResponseInternal(
                responseCode,
                purchaseToken
            )
        }
    }

    private fun onConsumeResponseInternal(
        @BillingResponse responseCode: Int,
        purchaseToken: String
    ) {
        PlayStoreBillingManager.Companion.LOGGER.i(
            PlayStoreBillingManager.Companion.TAG,
            "Consumption response: $responseCode"
        )
        if (responseCode == BillingResponse.OK) {
            if (callback != null) callback.onConsumptionSuccess()
            return
        }
        if (callback != null) callback.onConsumptionFailure()
    }

}