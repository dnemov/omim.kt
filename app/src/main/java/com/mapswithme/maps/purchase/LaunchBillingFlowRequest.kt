package com.mapswithme.maps.purchase

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams

internal class LaunchBillingFlowRequest(
    private val mActivity: Activity, client: BillingClient,
    productType: String, private val mProductId: String
) : PlayStoreBillingRequest<Any?>(client, productType) {
    override fun execute() {
        val params = BillingFlowParams.newBuilder()
            .setSku(mProductId)
            .setType(productType)
            .build()
        val responseCode = client.launchBillingFlow(mActivity, params)
        PlayStoreBillingManager.Companion.LOGGER.i(
            PlayStoreBillingManager.Companion.TAG,
            "Launch billing flow response: $responseCode"
        )
    }

}