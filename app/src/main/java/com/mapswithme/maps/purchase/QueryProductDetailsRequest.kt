package com.mapswithme.maps.purchase

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponse
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import java.util.*

internal class QueryProductDetailsRequest(
    client: BillingClient, productType: String,
    callback: PlayStoreBillingCallback?,
    productIds: List<String?>
) : PlayStoreBillingRequest<PlayStoreBillingCallback?>(client, productType, callback) {
    private val mProductIds: List<String?>
    override fun execute() {
        val builder = SkuDetailsParams.newBuilder()
            .setSkusList(mProductIds)
            .setType(productType)
        client.querySkuDetailsAsync(
            builder.build()
        ) { responseCode: Int, skuDetails: List<SkuDetails>? ->
            onSkuDetailsResponseInternal(
                responseCode,
                skuDetails
            )
        }
    }

    private fun onSkuDetailsResponseInternal(
        @BillingResponse responseCode: Int,
        skuDetails: List<SkuDetails>?
    ) {
        PlayStoreBillingManager.Companion.LOGGER.i(
            PlayStoreBillingManager.Companion.TAG,
            "Purchase details response code: " + responseCode
                    + ". Type: " + productType
        )
        if (responseCode != BillingResponse.OK) {
            PlayStoreBillingManager.Companion.LOGGER.w(
                PlayStoreBillingManager.Companion.TAG,
                "Unsuccessful request"
            )
            if (callback != null) callback.onProductDetailsFailure()
            return
        }
        if (skuDetails == null || skuDetails.isEmpty()) {
            PlayStoreBillingManager.Companion.LOGGER.w(
                PlayStoreBillingManager.Companion.TAG,
                "Purchase details not found"
            )
            if (callback != null) callback.onProductDetailsFailure()
            return
        }
        PlayStoreBillingManager.Companion.LOGGER.i(
            PlayStoreBillingManager.Companion.TAG,
            "Purchase details obtained: $skuDetails"
        )
        if (callback != null) callback.onProductDetailsLoaded(skuDetails)
    }

    init {
        mProductIds = Collections.unmodifiableList(productIds)
    }
}