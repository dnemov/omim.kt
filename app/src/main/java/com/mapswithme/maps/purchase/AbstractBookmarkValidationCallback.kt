package com.mapswithme.maps.purchase


import com.mapswithme.util.log.LoggerFactory
import com.mapswithme.util.statistics.Statistics

internal abstract class AbstractBookmarkValidationCallback(private val mServerId: String?) :
    ValidationCallback {
    override fun onValidate(
        purchaseData: String,
        status: ValidationStatus
    ) {
        LOGGER.i(
            TAG,
            "Validation status of 'paid bookmark': $status"
        )
        if (status == ValidationStatus.VERIFIED) {
            Statistics.INSTANCE.trackPurchaseEvent(
                Statistics.EventName.INAPP_PURCHASE_VALIDATION_SUCCESS,
                mServerId!!
            )
            consumePurchase(purchaseData)
            return
        }
        // We consume purchase in 'NOT_VERIFIED' case to allow user enter in bookmark catalog again.
        if (status == ValidationStatus.NOT_VERIFIED) consumePurchase(purchaseData)
        Statistics.INSTANCE.trackPurchaseValidationError(
            mServerId!!,
            status
        )
        onValidationError(status)
    }

    abstract fun onValidationError(status: ValidationStatus)
    abstract fun consumePurchase(purchaseData: String)

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.BILLING)
        private val TAG =
            AbstractBookmarkValidationCallback::class.java.simpleName
    }

}