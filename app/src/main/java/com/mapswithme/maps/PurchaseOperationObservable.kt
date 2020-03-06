package com.mapswithme.maps

import android.content.Context
import android.util.Base64
import com.mapswithme.maps.Framework.PurchaseValidationListener
import com.mapswithme.maps.Framework.StartTransactionListener
import com.mapswithme.maps.Framework.nativeSetPurchaseValidationListener
import com.mapswithme.maps.Framework.nativeStartPurchaseTransactionListener
import com.mapswithme.maps.purchase.CoreStartTransactionObserver
import com.mapswithme.maps.purchase.CoreValidationObserver
import com.mapswithme.maps.purchase.PurchaseUtils.parseOrderId
import com.mapswithme.maps.purchase.ValidationStatus
import com.mapswithme.util.log.Logger
import com.mapswithme.util.log.LoggerFactory
import java.util.*
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.set

class PurchaseOperationObservable internal constructor() : PurchaseValidationListener,
    StartTransactionListener {
    private val mValidationObservers: MutableMap<String, CoreValidationObserver> =
        HashMap()
    private val mTransactionObservers: MutableList<CoreStartTransactionObserver> =
        ArrayList()
    private val mValidationPendingResults: MutableMap<String, PendingResult> =
        HashMap()
    private lateinit var mLogger: Logger
    fun initialize() {
        mLogger = LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.BILLING)
        mLogger.i(
            TAG,
            "Initializing purchase operation observable..."
        )
        nativeSetPurchaseValidationListener(this)
        nativeStartPurchaseTransactionListener(this)
    }

    override fun onValidatePurchase(
        code: Int, serverId: String, vendorId: String,
        encodedPurchaseData: String
    ) {
        val tokenBytes =
            Base64.decode(encodedPurchaseData, Base64.DEFAULT)
        val purchaseData = String(tokenBytes)
        val orderId = parseOrderId(purchaseData)
        val observer = mValidationObservers[orderId]
        val status = ValidationStatus.values()[code]
        if (observer == null) {
            val result =
                PendingResult(
                    status,
                    serverId,
                    vendorId,
                    purchaseData
                )
            mValidationPendingResults[orderId] = result
            return
        }
        observer.onValidatePurchase(status, serverId, vendorId, purchaseData)
    }

    override fun onStartTransaction(
        success: Boolean,
        serverId: String,
        vendorId: String
    ) {
        for (observer in mTransactionObservers) observer.onStartTransaction(
            success,
            serverId,
            vendorId
        )
    }

    fun addValidationObserver(
        orderId: String,
        observer: CoreValidationObserver
    ) {
        mLogger.d(
            TAG,
            "Add validation observer '$observer' for '$orderId'"
        )
        mValidationObservers[orderId] = observer
        val result =
            mValidationPendingResults.remove(orderId)
        if (result != null) {
            mLogger.d(
                TAG,
                "Post pending validation result to '" + observer + "' for '"
                        + orderId + "'"
            )
            observer.onValidatePurchase(
                result.status, result.serverId, result.vendorId,
                result.purchaseData
            )
        }
    }

    fun removeValidationObserver(orderId: String) {
        mLogger.d(
            TAG,
            "Remove validation observer for '$orderId'"
        )
        mValidationObservers.remove(orderId)
    }

    fun addTransactionObserver(observer: CoreStartTransactionObserver) {
        mLogger.d(
            TAG,
            "Add transaction observer '$observer'"
        )
        mTransactionObservers.add(observer)
    }

    fun removeTransactionObserver(observer: CoreStartTransactionObserver) {
        mLogger.d(
            TAG,
            "Remove transaction observer '$observer'"
        )
        mTransactionObservers.remove(observer)
    }

    private class PendingResult(
        val status: ValidationStatus, val serverId: String,
        val vendorId: String, val purchaseData: String
    )

    companion object {
        private val TAG = PurchaseOperationObservable::class.java.simpleName
        fun from(context: Context): PurchaseOperationObservable {
            val application = context.applicationContext as MwmApplication
            return application.purchaseOperationObservable
        }
    }
}