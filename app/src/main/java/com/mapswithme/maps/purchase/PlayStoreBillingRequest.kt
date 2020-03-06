package com.mapswithme.maps.purchase

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.SkuType

abstract class PlayStoreBillingRequest<T> @JvmOverloads constructor(
    val client: BillingClient, @field:SkuType @get:SkuType val productType: String,
    val callback: T? = null
) : BillingRequest