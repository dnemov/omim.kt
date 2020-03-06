package com.mapswithme.maps.purchase

import android.app.Activity
import android.os.Bundle
import com.android.billingclient.api.BillingClient.BillingResponse
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails

import com.mapswithme.util.log.LoggerFactory
import java.util.*

internal abstract class AbstractPurchaseController<V, B, UiCallback : PurchaseCallback>(
    val validator: PurchaseValidator<V>,
    val billingManager: BillingManager<B>,
    vararg productIds: String?
) : PurchaseController<UiCallback> {
    var uiCallback: UiCallback? = null
        private set
    private val mProductIds: List<String>?
    override fun initialize(activity: Activity) {
        billingManager.initialize(activity)
        onInitialize(activity)
    }

    override fun destroy() {
        billingManager.destroy()
        onDestroy()
    }

    override fun addCallback(callback: UiCallback) {
        uiCallback = callback
    }

    override fun removeCallback() {
        uiCallback = null
    }

    override val isPurchaseSupported: Boolean
        get() = billingManager.isBillingSupported

    override fun launchPurchaseFlow(productId: String) {
        billingManager.launchBillingFlowForProduct(productId)
    }

    override fun queryProductDetails() {
        checkNotNull(mProductIds) { "Product ids must be non-null!" }
        billingManager.queryProductDetails(mProductIds)
    }

    override fun validateExistingPurchases() {
        billingManager.queryExistingPurchases()
    }

    fun findTargetPurchase(purchases: List<Purchase?>): Purchase? {
        if (mProductIds == null) return null
        for (purchase in purchases) {
            if (mProductIds.contains(purchase!!.sku)) return purchase
        }
        return null
    }

    abstract fun onInitialize(activity: Activity)
    abstract fun onDestroy()
    override fun onSave(outState: Bundle?) {
        validator.onSave(outState)
    }

    override fun onRestore(inState: Bundle?) {
        validator.onRestore(inState)
    }

    internal inner abstract class AbstractPlayStoreBillingCallback :
        PlayStoreBillingCallback {
        override fun onPurchaseSuccessful(purchases: List<Purchase>) {
            val target = findTargetPurchase(purchases) ?: return
            LOGGER.i(
                TAG,
                "Validating purchase '" + target.sku + " " + target.orderId
                        + "' on backend server..."
            )
            validate(target.originalJson)
            if (uiCallback != null) uiCallback?.onValidationStarted()
        }

        override fun onProductDetailsLoaded(details: List<SkuDetails>) {
            if (uiCallback != null) uiCallback!!.onProductDetailsLoaded(details)
        }

        override fun onPurchaseFailure(@BillingResponse error: Int) {
            if (uiCallback != null) uiCallback!!.onPaymentFailure(error)
        }

        override fun onProductDetailsFailure() {
            if (uiCallback != null) uiCallback!!.onProductDetailsFailure()
        }

        override fun onStoreConnectionFailed() {
            if (uiCallback != null) uiCallback!!.onStoreConnectionFailed()
        }

        override fun onConsumptionSuccess() { // Do nothing by default.
        }

        override fun onConsumptionFailure() { // Do nothing by default.
        }

        abstract fun validate(purchaseData: String)
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.BILLING)
        private val TAG = AbstractPurchaseController::class.java.simpleName
    }

    init {
        mProductIds = if (productIds != null) Collections.unmodifiableList(
            Arrays.asList<String>(*productIds)
        ) else null
    }
}