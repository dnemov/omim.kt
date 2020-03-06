package com.mapswithme.maps.purchase

import android.app.Activity
import android.os.Bundle
import com.mapswithme.maps.base.Savable

/**
 * Provides necessary purchase functionality to the UI. Controls whole platform-specific billing
 * process. This controller has to be used only within [.initialize] and [.destroy]
 * interval.
 */
interface PurchaseController<T> : Savable<Bundle?> {
    /**
     * Initializes the controller.
     * @param activity the activity which controller serves.
     */
    fun initialize(activity: Activity)

    /**
     * Destroys the controller.
     */
    fun destroy()

    /**
     * Indicates whether the purchase flow is supported by this device or not.
     */
    val isPurchaseSupported: Boolean

    /**
     * Launches the purchase flow for the specified product. The purchase results will be delivered
     * through [T] callback.
     *
     * @param productId id of the product which is going to be purchased.
     */
    fun launchPurchaseFlow(productId: String)

    /**
     * Queries product details. They will be delivered to the caller through callback [T].
     */
    fun queryProductDetails()

    /**
     * Validates existing purchase. A validation result will be delivered through callback [T].
     */
    fun validateExistingPurchases()

    fun addCallback(callback: T)
    fun removeCallback()
}