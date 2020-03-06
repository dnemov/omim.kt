package com.mapswithme.maps.purchase

import com.android.billingclient.api.BillingClient.BillingResponse
import com.android.billingclient.api.SkuDetails
import com.mapswithme.maps.base.Detachable
import com.mapswithme.maps.purchase.BookmarkPaymentState
import com.mapswithme.util.statistics.Statistics
import java.util.*

internal class BookmarkPurchaseCallback(private val mServerId: String) :
    StatefulPurchaseCallback<BookmarkPaymentState, BookmarkPaymentFragment?>(), PurchaseCallback,
    Detachable<BookmarkPaymentFragment?>, CoreStartTransactionObserver {
    private var mPendingDetails: List<SkuDetails?>? = null
    private var mPendingValidationResult: Boolean? = null
    override fun onStartTransaction(
        success: Boolean,
        serverId: String,
        vendorId: String
    ) {
        if (!success) {
            activateStateSafely(BookmarkPaymentState.TRANSACTION_FAILURE)
            return
        }
        activateStateSafely(BookmarkPaymentState.TRANSACTION_STARTED)
    }

    override fun onProductDetailsLoaded(details: List<SkuDetails>?) {
        if (uiObject == null) mPendingDetails =
            Collections.unmodifiableList(details!!) else uiObject!!.handleProductDetails(
            details!!
        )
        activateStateSafely(BookmarkPaymentState.PRODUCT_DETAILS_LOADED)
    }

    override fun onPaymentFailure(@BillingResponse error: Int) {
        activateStateSafely(BookmarkPaymentState.PAYMENT_FAILURE)
    }

    override fun onProductDetailsFailure() {
        activateStateSafely(BookmarkPaymentState.PRODUCT_DETAILS_FAILURE)
    }

    override fun onStoreConnectionFailed() {
        activateStateSafely(BookmarkPaymentState.PRODUCT_DETAILS_FAILURE)
    }

    override fun onValidationStarted() {
        Statistics.INSTANCE.trackPurchaseEvent(
            Statistics.EventName.INAPP_PURCHASE_STORE_SUCCESS,
            mServerId
        )
        activateStateSafely(BookmarkPaymentState.VALIDATION)
    }

    override fun onValidationFinish(success: Boolean) {
        if (uiObject == null) mPendingValidationResult =
            success else uiObject!!.handleValidationResult(success)
        activateStateSafely(BookmarkPaymentState.VALIDATION_FINISH)
    }

    override fun onAttach(uiObject: BookmarkPaymentFragment?) {
        if (mPendingDetails != null) {
            uiObject?.handleProductDetails(mPendingDetails!!)
            mPendingDetails = null
        }
        if (mPendingValidationResult != null) {
            uiObject?.handleValidationResult(mPendingValidationResult!!)
            mPendingValidationResult = null
        }
    }

}