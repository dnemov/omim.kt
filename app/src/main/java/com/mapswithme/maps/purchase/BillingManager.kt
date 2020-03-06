package com.mapswithme.maps.purchase

import android.app.Activity

/**
 * Manages a billing flow for the specific product.
 */
interface BillingManager<T> {
    /**
     * Initializes the current billing manager.
     */
    fun initialize(context: Activity)

    /**
     * Destroys the billing manager.
     */
    fun destroy()

    /**
     * Launches the billing flow for the product with the specified id.
     * Controls whole native part of billing process.
     *
     * @param productId identifier of the product which is going to be purchased.
     */
    fun launchBillingFlowForProduct(productId: String)

    /**
     * Indicates whether billing is supported for this device or not.
     */
    val isBillingSupported: Boolean

    /**
     * Queries existing purchases.
     */
    fun queryExistingPurchases()

    /**
     * Queries product details for specified products. They will be delivered to the caller
     * through callback [T].
     */
    fun queryProductDetails(productIds: List<String?>)

    /**
     * Consumes the purchase with the specified token. Result of consumption will be delivered
     * through callback [T].
     *
     * @param purchaseToken purchase token that is going to be consumed.
     */
    fun consumePurchase(purchaseToken: String)

    /**
     * Adds a billing callback.
     */
    fun addCallback(callback: T)

    /**
     * Removes the billing callback.
     */
    fun removeCallback(callback: T)
}