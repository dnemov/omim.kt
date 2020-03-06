package com.mapswithme.maps.purchase

import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import java.util.*

internal class SubsProductDetailsCallback :
    StatefulPurchaseCallback<BookmarkPaymentState, BookmarkPaymentFragment>(),
    PlayStoreBillingCallback {
    private var mPendingDetails: List<SkuDetails>? = null
    override fun onProductDetailsLoaded(details: List<SkuDetails>) {
        if (PurchaseUtils.hasIncorrectSkuDetails(details)) {
            activateStateSafely(BookmarkPaymentState.SUBS_PRODUCT_DETAILS_FAILURE)
            return
        }
        if (uiObject == null) mPendingDetails =
            Collections.unmodifiableList(details) else uiObject!!.handleSubsProductDetails(
            details
        )
        activateStateSafely(BookmarkPaymentState.SUBS_PRODUCT_DETAILS_LOADED)
    }

    public override fun onAttach(bookmarkPaymentFragment: BookmarkPaymentFragment) {
        if (mPendingDetails != null) {
            bookmarkPaymentFragment.handleProductDetails(mPendingDetails!!)
            mPendingDetails = null
        }
    }

    override fun onPurchaseSuccessful(purchases: List<Purchase>) { // Do nothing.
    }

    override fun onPurchaseFailure(error: Int) { // Do nothing.
    }

    override fun onProductDetailsFailure() {
        activateStateSafely(BookmarkPaymentState.SUBS_PRODUCT_DETAILS_FAILURE)
    }

    override fun onStoreConnectionFailed() { // Do nothing.
    }

    override fun onPurchasesLoaded(purchases: List<Purchase>) { // Do nothing.
    }

    override fun onConsumptionSuccess() {
        throw UnsupportedOperationException()
    }

    override fun onConsumptionFailure() {
        throw UnsupportedOperationException()
    }
}