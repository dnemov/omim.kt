package com.mapswithme.maps.purchase

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponse
import com.android.billingclient.api.BillingClientStateListener
import com.mapswithme.util.log.LoggerFactory

internal class PlayStoreBillingConnection(
    private val mBillingClient: BillingClient,
    private val mListener: ConnectionListener?
) : BillingConnection, BillingClientStateListener {
    override var state =
        BillingConnection.State.DISCONNECTED
        private set

    override fun open() {
        LOGGER.i(
            TAG,
            "Opening billing connection..."
        )
        state = BillingConnection.State.CONNECTING
        mBillingClient.startConnection(this)
    }

    override fun close() {
        LOGGER.i(
            TAG,
            "Closing billing connection..."
        )
        mBillingClient.endConnection()
        state = BillingConnection.State.CLOSED
    }

    override fun onBillingSetupFinished(responseCode: Int) {
        LOGGER.i(
            TAG,
            "Connection established to billing client. Response code: $responseCode"
        )
        if (responseCode == BillingResponse.OK) {
            state = BillingConnection.State.CONNECTED
            mListener?.onConnected()
            return
        }
        state = BillingConnection.State.DISCONNECTED
        mListener?.onDisconnected()
    }

    override fun onBillingServiceDisconnected() {
        LOGGER.i(
            TAG,
            "Billing client is disconnected."
        )
        state = BillingConnection.State.DISCONNECTED
    }

    internal interface ConnectionListener {
        fun onConnected()
        fun onDisconnected()
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.BILLING)
        private val TAG = PlayStoreBillingConnection::class.java.simpleName
    }

}