package com.mapswithme.maps.purchase

import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import com.mapswithme.maps.Framework
import com.mapswithme.maps.PurchaseOperationObservable
import com.mapswithme.util.log.LoggerFactory

internal class DefaultPurchaseValidator(private val mOperationObservable: PurchaseOperationObservable) :
    PurchaseValidator<ValidationCallback>, CoreValidationObserver {
    private var mCallback: ValidationCallback? = null
    private var mValidatedOrderId: String? = null
    override fun validate(
        serverId: String?, vendor: String,
        purchaseData: String
    ) {
        val orderId = PurchaseUtils.parseOrderId(purchaseData)
        mValidatedOrderId = orderId
        mOperationObservable.addValidationObserver(orderId, this)
        val encodedPurchaseData = Base64.encodeToString(
            purchaseData.toByteArray(),
            Base64.DEFAULT
        )
        Framework.nativeValidatePurchase(
            serverId ?: "",
            vendor,
            encodedPurchaseData
        )
    }

    override fun addCallback(callback: ValidationCallback) {
        mCallback = callback
        if (!TextUtils.isEmpty(mValidatedOrderId)) mOperationObservable.addValidationObserver(
            mValidatedOrderId!!,
            this
        )
    }

    override fun removeCallback() {
        mCallback = null
        if (!TextUtils.isEmpty(mValidatedOrderId)) mOperationObservable.removeValidationObserver(
            mValidatedOrderId!!
        )
    }

    override fun onSave(outState: Bundle?) {
        outState?.putString(
            EXTRA_VALIDATED_ORDER_ID,
            mValidatedOrderId
        )
    }

    override fun onRestore(inState: Bundle?) {
        mValidatedOrderId =
            inState?.getString(EXTRA_VALIDATED_ORDER_ID)
    }

    override fun onValidatePurchase(
        status: ValidationStatus, serverId: String,
        vendorId: String, purchaseData: String
    ) {
        LOGGER.i(
            TAG,
            "Validation code: $status"
        )
        val orderId = PurchaseUtils.parseOrderId(purchaseData)
        mOperationObservable.removeValidationObserver(orderId)
        mValidatedOrderId = null
        if (mCallback != null) mCallback!!.onValidate(purchaseData, status)
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.BILLING)
        private val TAG = DefaultPurchaseValidator::class.java.simpleName
        private const val EXTRA_VALIDATED_ORDER_ID = "extra_validated_order_id"
    }

}