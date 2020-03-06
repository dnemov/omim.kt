package com.mapswithme.maps.purchase

import android.os.Bundle
import com.mapswithme.maps.base.Savable

/**
 * Represents a purchase validator. The main of purpose is to validate existing purchase and inform
 * the client code through typed callback [T].<br></br><br></br>
 * **Important note: ** one validator can serve only one purchase, i.e. logical link is
 * **one-to-one**. If you need to validate different purchases you have to create different
 * implementations of this interface.
 */
internal interface PurchaseValidator<T> : Savable<Bundle?> {
    /**
     * Validates the purchase with specified purchase data.
     *
     * @param serverId identifier of the purchase on the server.
     * @param vendor vendor of the purchase.
     * @param purchaseData token which describes the validated purchase.
     */
    fun validate(
        serverId: String?,
        vendor: String,
        purchaseData: String
    )

    /**
     * Adds validation observer.
     */
    fun addCallback(callback: T)

    /**
     * Removes validation observer.
     */
    fun removeCallback()
}