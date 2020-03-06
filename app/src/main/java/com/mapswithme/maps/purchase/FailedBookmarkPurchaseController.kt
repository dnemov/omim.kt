package com.mapswithme.maps.purchase

import android.app.Activity
import android.os.Bundle
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.mapswithme.maps.PrivateVariables
import com.mapswithme.util.log.LoggerFactory

class FailedBookmarkPurchaseController internal constructor(
    private val mValidator: PurchaseValidator<ValidationCallback>,
    private val mBillingManager: BillingManager<PlayStoreBillingCallback>
) : PurchaseController<FailedPurchaseChecker> {
    private var mCallback: FailedPurchaseChecker? = null
    private val mValidationCallback: ValidationCallback =
        ValidationCallbackImpl(null)
    private val mBillingCallback: PlayStoreBillingCallback =
        PlayStoreBillingCallbackImpl()

    override fun initialize(activity: Activity) {
        mBillingManager.initialize(activity)
        mValidator.addCallback(mValidationCallback)
        mBillingManager.addCallback(mBillingCallback)
    }

    override fun destroy() {
        mBillingManager.destroy()
        mValidator.removeCallback()
        mBillingManager.removeCallback(mBillingCallback)
    }

    override val isPurchaseSupported: Boolean
        get() {
            throw UnsupportedOperationException(
                "This purchase controller doesn't respond for " +
                        "purchase supporting"
            )
        }

    override fun launchPurchaseFlow(productId: String) {
        throw UnsupportedOperationException(
            "This purchase controller doesn't support " +
                    "purchase flow"
        )
    }

    override fun queryProductDetails() {
        throw UnsupportedOperationException(
            "This purchase controller doesn't support " +
                    "querying purchase details"
        )
    }

    override fun validateExistingPurchases() {
        mBillingManager.queryExistingPurchases()
    }

    override fun addCallback(callback: FailedPurchaseChecker) {
        mCallback = callback
    }

    override fun removeCallback() {
        mCallback = null
    }

    override fun onSave(outState: Bundle?) {
        mValidator.onSave(outState)
    }

    override fun onRestore(inState: Bundle?) {
        mValidator.onRestore(inState)
    }

    private inner class ValidationCallbackImpl internal constructor(serverId: String?) :
        AbstractBookmarkValidationCallback(serverId) {
        override fun onValidationError(status: ValidationStatus) {
            if (status == ValidationStatus.AUTH_ERROR) {
                if (mCallback != null) mCallback!!.onAuthorizationRequired()
                return
            }
            if (mCallback != null) mCallback!!.onFailedPurchaseDetected(true)
        }

        override fun consumePurchase(purchaseData: String) {
            LOGGER.i(
                TAG,
                "Failed bookmark purchase consuming..."
            )
            mBillingManager.consumePurchase(PurchaseUtils.parseToken(purchaseData))
        }
    }

    private inner class PlayStoreBillingCallbackImpl : PlayStoreBillingCallback {
        override fun onProductDetailsLoaded(details: List<SkuDetails>) { // Do nothing by default.
        }

        override fun onPurchaseSuccessful(purchases: List<Purchase>) { // Do nothing by default.
        }

        override fun onPurchaseFailure(error: Int) { // Do nothing by default.
        }

        override fun onProductDetailsFailure() { // Do nothing by default.
        }

        override fun onStoreConnectionFailed() {
            if (mCallback != null) mCallback!!.onStoreConnectionFailed()
        }

        override fun onPurchasesLoaded(purchases: List<Purchase>) {
            if (purchases.isEmpty()) {
                LOGGER.i(
                    TAG,
                    "Non-consumed bookmark purchases not found"
                )
                if (mCallback != null) mCallback!!.onFailedPurchaseDetected(false)
                return
            }
            if (purchases.size > 1) {
                if (mCallback != null) mCallback!!.onFailedPurchaseDetected(true)
                return
            }
            val target = purchases[0]
            LOGGER.i(
                TAG,
                "Validating failed purchase data for '" + target!!.sku
                        + " " + target.orderId + "'..."
            )
            mValidator.validate(null, PrivateVariables.bookmarksVendor(), target.originalJson)
        }

        override fun onConsumptionSuccess() {
            LOGGER.i(
                TAG,
                "Failed bookmark purchase consumed"
            )
            if (mCallback != null) mCallback!!.onFailedPurchaseDetected(false)
        }

        override fun onConsumptionFailure() {
            LOGGER.w(
                TAG,
                "Failed bookmark purchase not consumed"
            )
            if (mCallback != null) mCallback!!.onFailedPurchaseDetected(true)
        }
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.BILLING)
        private val TAG =
            FailedBookmarkPurchaseController::class.java.simpleName
    }

}