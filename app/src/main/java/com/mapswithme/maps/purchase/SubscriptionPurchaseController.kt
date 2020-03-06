package com.mapswithme.maps.purchase

import android.app.Activity
import android.text.TextUtils
import com.android.billingclient.api.Purchase
import com.mapswithme.maps.Framework
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.log.LoggerFactory
import com.mapswithme.util.statistics.Statistics
import com.mapswithme.util.statistics.Statistics.EventName

internal class SubscriptionPurchaseController(
    validator: PurchaseValidator<ValidationCallback>,
    billingManager: BillingManager<PlayStoreBillingCallback>,
    private val mType: SubscriptionType,
    vararg productIds: String
) : AbstractPurchaseController<ValidationCallback, PlayStoreBillingCallback, PurchaseCallback>(
    validator,
    billingManager,
    *productIds
) {
    private val mValidationCallback: ValidationCallback =
        ValidationCallbackImpl()
    private val mBillingCallback: PlayStoreBillingCallback =
        PlayStoreBillingCallbackImpl()

    override fun onInitialize(activity: Activity) {
        validator.addCallback(mValidationCallback)
        billingManager.addCallback(mBillingCallback)
    }

    override fun onDestroy() {
        validator.removeCallback()
        billingManager.removeCallback(mBillingCallback)
    }

    private inner class ValidationCallbackImpl : ValidationCallback {
        override fun onValidate(
            purchaseData: String,
            status: ValidationStatus
        ) {
            LOGGER.i(
                TAG,
                "Validation status of '$mType': $status"
            )
            if (status == ValidationStatus.VERIFIED) Statistics.INSTANCE.trackPurchaseEvent(
                EventName.INAPP_PURCHASE_VALIDATION_SUCCESS,
                mType.serverId
            ) else Statistics.INSTANCE.trackPurchaseValidationError(
                mType.serverId,
                status
            )
            val shouldActivateSubscription = status != ValidationStatus.NOT_VERIFIED
            val hasActiveSubscription =
                Framework.nativeHasActiveSubscription(mType.ordinal)
            if (!hasActiveSubscription && shouldActivateSubscription) {
                LOGGER.i(
                    TAG,
                    "'$mType' subscription activated"
                )
                Statistics.INSTANCE.trackPurchaseProductDelivered(
                    mType.serverId,
                    mType.vendor
                )
            } else if (hasActiveSubscription && !shouldActivateSubscription) {
                LOGGER.i(
                    TAG,
                    "'$mType' subscription deactivated"
                )
            }
            Framework.nativeSetActiveSubscription(mType.ordinal, shouldActivateSubscription)
            if (uiCallback != null) uiCallback!!.onValidationFinish(shouldActivateSubscription)
        }
    }

    private inner class PlayStoreBillingCallbackImpl :
        AbstractPlayStoreBillingCallback() {
        override fun validate(purchaseData: String) {
            validator.validate(mType.serverId, mType.vendor, purchaseData)
        }

        override fun onPurchasesLoaded(purchases: List<Purchase>) {
            var purchaseData: String? = null
            var productId: String? = null
            val target = findTargetPurchase(purchases)
            if (target != null) {
                purchaseData = target.originalJson
                productId = target.sku
            }
            if (TextUtils.isEmpty(purchaseData)) {
                LOGGER.i(
                    TAG,
                    "Existing purchase data for '$mType' not found"
                )
                if (Framework.nativeHasActiveSubscription(mType.ordinal)) {
                    LOGGER.i(
                        TAG,
                        "'$mType' subscription deactivated"
                    )
                    Framework.nativeSetActiveSubscription(mType.ordinal, false)
                }
                return
            }
            if (!ConnectionState.isWifiConnected) {
                LOGGER.i(
                    TAG,
                    "Validation postponed, connection not WI-FI."
                )
                return
            }
            LOGGER.i(
                TAG,
                "Validating existing purchase data for '$productId'..."
            )
            validator.validate(mType.serverId, mType.vendor, purchaseData!!)
        }
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.BILLING)
        private val TAG = SubscriptionPurchaseController::class.java.simpleName
    }

}