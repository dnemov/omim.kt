package com.mapswithme.maps.purchase

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponse
import com.android.billingclient.api.BillingClient.SkuType
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.mapswithme.maps.purchase.PlayStoreBillingConnection.ConnectionListener
import com.mapswithme.util.log.LoggerFactory
import java.util.*

internal class PlayStoreBillingManager(@field:SkuType @param:SkuType private val mProductType: String) :
    BillingManager<PlayStoreBillingCallback>, PurchasesUpdatedListener, ConnectionListener {
    private var mActivity: Activity? = null
    private var mBillingClient: BillingClient? = null
    private var mCallback: PlayStoreBillingCallback? = null
    private lateinit var mConnection: BillingConnection
    private val mPendingRequests: MutableList<BillingRequest> =
        ArrayList()

    override fun initialize(context: Activity) {
        LOGGER.i(
            TAG,
            "Creating play store billing client..."
        )
        mActivity = context
        mBillingClient = BillingClient.newBuilder(mActivity!!).setListener(this).build()
        mConnection = PlayStoreBillingConnection(mBillingClient!!, this)
        mConnection.open()
    }

    override fun destroy() {
        mActivity = null
        mConnection.close()
        mPendingRequests.clear()
    }

    override fun queryProductDetails(productIds: List<String?>) {
        executeBillingRequest(
            QueryProductDetailsRequest(
                clientOrThrow, mProductType,
                mCallback, productIds
            )
        )
    }

    override fun queryExistingPurchases() {
        executeBillingRequest(QueryExistingPurchases(clientOrThrow, mProductType, mCallback))
    }

    override fun consumePurchase(purchaseToken: String) {
        executeBillingRequest(
            ConsumePurchaseRequest(
                clientOrThrow, mProductType, mCallback,
                purchaseToken
            )
        )
    }

    private fun executeBillingRequest(request: BillingRequest) {
        when (mConnection.state) {
            BillingConnection.State.CONNECTING -> mPendingRequests.add(
                request
            )
            BillingConnection.State.CONNECTED -> request.execute()
            BillingConnection.State.DISCONNECTED -> {
                mPendingRequests.add(request)
                mConnection.open()
            }
            BillingConnection.State.CLOSED -> throw IllegalStateException(
                "Billing service connection already closed, " +
                        "please initialize it again."
            )
        }
    }

    override fun launchBillingFlowForProduct(productId: String) {
        if (!isBillingSupported) {
            LOGGER.w(
                TAG,
                "Purchase type '$mProductType' is not supported by this device!"
            )
            return
        }
        executeBillingRequest(
            LaunchBillingFlowRequest(
                activityOrThrow, clientOrThrow,
                mProductType, productId
            )
        )
    }

    override val isBillingSupported: Boolean
        get() {
            if (SkuType.SUBS == mProductType) {
                @BillingResponse val result =
                    clientOrThrow.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
                return result != BillingResponse.FEATURE_NOT_SUPPORTED
            }
            return true
        }

    override fun addCallback(callback: PlayStoreBillingCallback) {
        mCallback = callback
    }

    override fun removeCallback(callback: PlayStoreBillingCallback) {
        mCallback = null
    }

    override fun onPurchasesUpdated(
        responseCode: Int,
        purchases: List<Purchase>?
    ) {
        if (responseCode == BillingResponse.USER_CANCELED) {
            LOGGER.i(
                TAG,
                "Billing cancelled by user."
            )
            return
        }
        if (responseCode == BillingResponse.ITEM_ALREADY_OWNED) {
            LOGGER.i(
                TAG,
                "Billing already done before."
            )
            return
        }
        if (responseCode != BillingResponse.OK || purchases == null || purchases.isEmpty()) {
            LOGGER.e(
                TAG,
                "Billing failed. Response code: $responseCode"
            )
            if (mCallback != null) mCallback!!.onPurchaseFailure(responseCode)
            return
        }
        LOGGER.i(
            TAG,
            "Purchase process successful. Count of purchases: " + purchases.size
        )
        if (mCallback != null) mCallback!!.onPurchaseSuccessful(purchases)
    }

    private val clientOrThrow: BillingClient
        get() {
            checkNotNull(mBillingClient) { "Manager must be initialized! Call 'initialize' method first." }
            return mBillingClient!!
        }

    private val activityOrThrow: Activity
        get() {
            checkNotNull(mActivity) { "Manager must be initialized! Call 'initialize' method first." }
            return mActivity!!
        }

    override fun onConnected() {
        for (request in mPendingRequests) request.execute()
        mPendingRequests.clear()
    }

    override fun onDisconnected() {
        LOGGER.w(
            TAG,
            "Play store connection failed."
        )
        if (mPendingRequests.isEmpty()) return
        mPendingRequests.clear()
        if (mCallback != null) mCallback!!.onStoreConnectionFailed()
    }

    companion object {
        val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.BILLING)
        val TAG = PlayStoreBillingManager::class.java.simpleName
    }

}