package com.mapswithme.maps.purchase

import android.app.Activity
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.mapswithme.maps.PrivateVariables
import com.mapswithme.maps.purchase.AbstractPurchaseController
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.log.LoggerFactory
import com.mapswithme.util.statistics.Statistics

internal class BookmarkPurchaseController(
    validator: PurchaseValidator<ValidationCallback>,
    billingManager: BillingManager<PlayStoreBillingCallback>,
    productId: String?, private val mServerId: String?
) : AbstractPurchaseController<ValidationCallback, PlayStoreBillingCallback, PurchaseCallback>(
    validator,
    billingManager,
    productId
) {
    private val mBillingCallback: PlayStoreBillingCallback =
        PlayStoreBillingCallbackImpl()
    private val mValidationCallback: ValidationCallback
    override fun onInitialize(activity: Activity) {
        validator.addCallback(mValidationCallback)
        billingManager.addCallback(mBillingCallback)
    }

    override fun onDestroy() {
        validator.removeCallback()
        billingManager.removeCallback(mBillingCallback)
    }

    private inner class ValidationCallbackImpl internal constructor(serverId: String?) :
        AbstractBookmarkValidationCallback(serverId) {
        override fun onValidationError(status: ValidationStatus) {
            if (uiCallback != null) uiCallback!!.onValidationFinish(false)
        }

        override fun consumePurchase(purchaseData: String) {
            LOGGER.i(
                TAG,
                "Bookmark purchase consuming..."
            )
            billingManager.consumePurchase(PurchaseUtils.parseToken(purchaseData))
        }
    }

    private inner class PlayStoreBillingCallbackImpl :
        AbstractPlayStoreBillingCallback() {
        override fun onPurchaseFailure(error: Int) {
            super.onPurchaseFailure(error)
            Statistics.INSTANCE.trackPurchaseStoreError(
                mServerId!!,
                error
            )
        }

        override fun onProductDetailsLoaded(details: List<SkuDetails>) {
            if (uiCallback != null) uiCallback!!.onProductDetailsLoaded(details)
        }

        override fun validate(purchaseData: String) {
            validator.validate(mServerId, PrivateVariables.bookmarksVendor(), purchaseData)
        }

        override fun onPurchasesLoaded(purchases: List<Purchase>) {
            if (!ConnectionState.isWifiConnected) {
                LOGGER.i(
                    TAG,
                    "Validation postponed, connection not WI-FI."
                )
                return
            }
            if (purchases.isEmpty()) {
                LOGGER.i(
                    TAG,
                    "Non-consumed bookmark purchases not found"
                )
                return
            }
            for (target in purchases) {
                LOGGER.i(
                    TAG,
                    "Validating existing purchase data for '" + target!!.sku
                            + " " + target.orderId + "'..."
                )
                validator.validate(
                    mServerId,
                    PrivateVariables.bookmarksVendor(),
                    target.originalJson
                )
            }
        }

        override fun onConsumptionSuccess() {
            LOGGER.i(
                TAG,
                "Bookmark purchase consumed"
            )
            if (uiCallback != null) uiCallback!!.onValidationFinish(true)
        }

        override fun onConsumptionFailure() {
            LOGGER.w(
                TAG,
                "Bookmark purchase not consumed"
            )
            if (uiCallback != null) uiCallback!!.onValidationFinish(false)
        }
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.BILLING)
        private val TAG = AbstractPurchaseController::class.java.simpleName
    }

    init {
        mValidationCallback =
            ValidationCallbackImpl(mServerId)
    }
}