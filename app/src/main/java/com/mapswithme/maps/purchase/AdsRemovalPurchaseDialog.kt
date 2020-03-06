package com.mapswithme.maps.purchase

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.android.billingclient.api.BillingClient.BillingResponse
import com.android.billingclient.api.SkuDetails
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmDialogFragment

import com.mapswithme.maps.dialog.AlertDialogCallback
import com.mapswithme.util.Graphics
import com.mapswithme.util.UiUtils
import com.mapswithme.util.Utils
import com.mapswithme.util.log.LoggerFactory
import com.mapswithme.util.statistics.Statistics
import java.util.*

class AdsRemovalPurchaseDialog : BaseMwmDialogFragment(), AlertDialogCallback,
    PurchaseStateActivator<AdsRemovalPaymentState?> {
    private var mActivationResult = false
    private var mProductDetails: MutableList<ProductDetails>? = null
    private var mState = AdsRemovalPaymentState.NONE
    private var mControllerProvider: AdsRemovalPurchaseControllerProvider? = null
    private val mPurchaseCallback = AdsRemovalPurchaseCallback()
    private val mActivationCallbacks: MutableList<AdsRemovalActivationCallback?> =
        ArrayList()
    private lateinit var mYearlyButton: View
    private lateinit var mMonthlyButton: TextView
    private lateinit var mWeeklyButton: TextView
    private lateinit var mOptionsButton: CompoundButton
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LOGGER.d(
            TAG,
            "onCreate savedInstanceState = $savedInstanceState"
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        LOGGER.d(
            TAG,
            "onAttach"
        )
        if (context is AdsRemovalPurchaseControllerProvider) mControllerProvider =
            context
        if (parentFragment is AdsRemovalPurchaseControllerProvider) mControllerProvider =
            parentFragment as AdsRemovalPurchaseControllerProvider?
        if (context is AdsRemovalActivationCallback) mActivationCallbacks.add(context as AdsRemovalActivationCallback)
        if (parentFragment is AdsRemovalActivationCallback) mActivationCallbacks.add(
            parentFragment as AdsRemovalActivationCallback?
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        LOGGER.d(
            TAG,
            "onCreateView savedInstanceState = " + savedInstanceState + "this " + this
        )
        val view =
            inflater.inflate(R.layout.fragment_ads_removal_purchase_dialog, container, false)
        val payButtonContainer =
            view.findViewById<View>(R.id.pay_button_container)
        mYearlyButton = payButtonContainer.findViewById(R.id.yearly_button)
        mYearlyButton.setOnClickListener { v: View? -> onYearlyProductClicked() }
        mMonthlyButton = payButtonContainer.findViewById(R.id.monthly_button)
        mMonthlyButton.setOnClickListener { v: View? -> onMonthlyProductClicked() }
        mWeeklyButton = payButtonContainer.findViewById(R.id.weekly_button)
        mWeeklyButton.setOnClickListener { v: View? -> onWeeklyProductClicked() }
        mOptionsButton = payButtonContainer.findViewById(R.id.options)
        mOptionsButton.setCompoundDrawablesWithIntrinsicBounds(null, null, optionsToggle, null)
        mOptionsButton.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean -> onOptionsClicked() }
        view.findViewById<View>(R.id.explanation)
            .setOnClickListener { v: View? -> onExplanationClick() }
        return view
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        LOGGER.d(
            TAG,
            "onViewCreated savedInstanceState = $savedInstanceState"
        )
        if (savedInstanceState != null) {
            val savedState =
                AdsRemovalPaymentState.values()[savedInstanceState.getInt(EXTRA_CURRENT_STATE)]
            val productDetails =
                savedInstanceState.getParcelableArray(EXTRA_PRODUCT_DETAILS) as Array<ProductDetails>?
            if (productDetails != null) mProductDetails = productDetails.toMutableList()
            mActivationResult =
                savedInstanceState.getBoolean(EXTRA_ACTIVATION_RESULT)
            activateState(savedState)
        } else {
            activateState(AdsRemovalPaymentState.LOADING)
        }
    }

    private val optionsToggle: Drawable?
        get() = Graphics.tint(
            context as Context,
            if (mOptionsButton.isChecked) R.drawable.ic_expand_less else R.drawable.ic_expand_more,
            android.R.attr.textColorSecondary
        )

    override fun onStart() {
        super.onStart()
        controllerOrThrow.addCallback(mPurchaseCallback)
        mPurchaseCallback.attach(this)
    }

    override fun onStop() {
        super.onStop()
        controllerOrThrow.removeCallback()
        mPurchaseCallback.detach()
    }

    fun queryPurchaseDetails() {
        controllerOrThrow.queryProductDetails()
    }

    fun onYearlyProductClicked() {
        launchPurchaseFlowForPeriod(PurchaseUtils.Period.P1Y)
    }

    fun onMonthlyProductClicked() {
        launchPurchaseFlowForPeriod(PurchaseUtils.Period.P1M)
    }

    fun onWeeklyProductClicked() {
        launchPurchaseFlowForPeriod(PurchaseUtils.Period.P1W)
    }

    fun onOptionsClicked() {
        mOptionsButton.setCompoundDrawablesWithIntrinsicBounds(null, null, optionsToggle, null)
        val payContainer =
            viewOrThrow.findViewById<View>(R.id.pay_button_container)
        UiUtils.showIf(
            mOptionsButton.isChecked, payContainer, R.id.monthly_button,
            R.id.weekly_button
        )
    }

    private fun launchPurchaseFlowForPeriod(period: PurchaseUtils.Period) {
        val details = getProductDetailsForPeriod(period)
        controllerOrThrow.launchPurchaseFlow(details.productId)
        val purchaseId =
            SubscriptionType.ADS_REMOVAL.serverId
        Statistics.INSTANCE.trackPurchasePreviewSelect(
            purchaseId,
            details.productId
        )
        Statistics.INSTANCE.trackPurchaseEvent(
            Statistics.EventName.INAPP_PURCHASE_PREVIEW_PAY,
            purchaseId, Statistics.STATISTICS_CHANNEL_REALTIME
        )
    }

    fun onExplanationClick() {
        activateState(AdsRemovalPaymentState.EXPLANATION)
    }

    override fun activateState(state: AdsRemovalPaymentState?) {
        if (state === mState) return
        LOGGER.i(
            TAG,
            "Activate state: $state"
        )
        state?.let {
            mState = it
            mState.activate(this)
        }

    }

    private val controllerOrThrow: PurchaseController<PurchaseCallback>
        get() {
            checkNotNull(mControllerProvider) { "Controller provider must be non-null at this point!" }
            return mControllerProvider!!.adsRemovalPurchaseController
                ?: throw IllegalStateException("Controller must be non-null at this point!")
        }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        LOGGER.d(
            TAG,
            "onSaveInstanceState"
        )
        outState.putInt(EXTRA_CURRENT_STATE, mState.ordinal)
        outState.putParcelableArray(
            EXTRA_PRODUCT_DETAILS,
            mProductDetails!!.toTypedArray()
        )
        outState.putBoolean(
            EXTRA_ACTIVATION_RESULT,
            mActivationResult
        )
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        Statistics.INSTANCE.trackPurchaseEvent(
            Statistics.EventName.INAPP_PURCHASE_PREVIEW_CANCEL,
            SubscriptionType.ADS_REMOVAL.serverId
        )
    }

    override fun onDetach() {
        LOGGER.d(
            TAG,
            "onDetach"
        )
        mControllerProvider = null
        mActivationCallbacks.clear()
        super.onDetach()
    }

    fun finishValidation() {
        if (mActivationResult) {
            for (callback in mActivationCallbacks) callback!!.onAdsRemovalActivation()
        }
        dismissAllowingStateLoss()
    }

    fun updatePaymentButtons() {
        updateYearlyButton()
        updateMonthlyButton()
        updateWeeklyButton()
    }

    private fun updateYearlyButton() {
        val details = getProductDetailsForPeriod(PurchaseUtils.Period.P1Y)
        val price =
            Utils.formatCurrencyString(details.price, details.currencyCode)
        val priceView = mYearlyButton.findViewById<TextView>(R.id.price)
        priceView.text = getString(R.string.paybtn_title, price)
        val savingView = mYearlyButton.findViewById<TextView>(R.id.saving)
        val saving = Utils.formatCurrencyString(
            calculateYearlySaving(),
            details.currencyCode
        )
        savingView.text = getString(R.string.paybtn_subtitle, saving)
    }

    @SuppressLint("StringFormatMatches")
    private fun updateMonthlyButton() {
        val details = getProductDetailsForPeriod(PurchaseUtils.Period.P1M)
        val price =
            Utils.formatCurrencyString(details.price, details.currencyCode)
        val saving = Utils.formatCurrencyString(
            calculateMonthlySaving(),
            details.currencyCode
        )
        mMonthlyButton.text = getString(R.string.options_dropdown_item1, price, saving)
    }

    private fun updateWeeklyButton() {
        val details = getProductDetailsForPeriod(PurchaseUtils.Period.P1W)
        val price =
            Utils.formatCurrencyString(details.price, details.currencyCode)
        mWeeklyButton.text = getString(R.string.options_dropdown_item2, price)
    }

    private fun getProductDetailsForPeriod(period: PurchaseUtils.Period): ProductDetails {
        if (mProductDetails == null) throw AssertionError("Product details must be exist at this moment!")
        return mProductDetails!![period.ordinal]
    }

    private fun calculateYearlySaving(): Float {
        val pricePerWeek = getProductDetailsForPeriod(PurchaseUtils.Period.P1W).price
        val pricePerYear = getProductDetailsForPeriod(PurchaseUtils.Period.P1Y).price
        return pricePerWeek * PurchaseUtils.WEEKS_IN_YEAR - pricePerYear
    }

    private fun calculateMonthlySaving(): Float {
        val pricePerWeek = getProductDetailsForPeriod(PurchaseUtils.Period.P1W).price
        val pricePerMonth = getProductDetailsForPeriod(PurchaseUtils.Period.P1M).price
        return pricePerWeek * PurchaseUtils.WEEKS_IN_YEAR - pricePerMonth * PurchaseUtils.MONTHS_IN_YEAR
    }

    override fun onAlertDialogPositiveClick(requestCode: Int, which: Int) {
        handleErrorDialogEvent(requestCode)
    }

    override fun onAlertDialogNegativeClick(
        requestCode: Int,
        which: Int
    ) { // Do nothing by default.
    }

    override fun onAlertDialogCancel(requestCode: Int) {
        handleErrorDialogEvent(requestCode)
    }

    private fun handleErrorDialogEvent(requestCode: Int) {
        when (requestCode) {
            PurchaseUtils.REQ_CODE_PRODUCT_DETAILS_FAILURE, PurchaseUtils.REQ_CODE_VALIDATION_SERVER_ERROR -> dismissAllowingStateLoss()
            PurchaseUtils.REQ_CODE_PAYMENT_FAILURE -> activateState(AdsRemovalPaymentState.PRICE_SELECTION)
        }
    }

    private fun handleProductDetails(details: List<SkuDetails?>) {
        mProductDetails = mutableListOf<ProductDetails>()
        for (sku in details) {
            val period =
                PurchaseUtils.Period.valueOf(sku!!.subscriptionPeriod)
            mProductDetails!!.add(period.ordinal, PurchaseUtils.toProductDetails(sku))
        }
    }

    private fun handleActivationResult(result: Boolean) {
        mActivationResult = result
    }

    private class AdsRemovalPurchaseCallback :
        StatefulPurchaseCallback<AdsRemovalPaymentState?, AdsRemovalPurchaseDialog?>(),
        PurchaseCallback {
        private var mPendingDetails: List<SkuDetails>? = null
        private var mPendingActivationResult: Boolean? = null
        override fun onProductDetailsLoaded(details: List<SkuDetails>?) {
            if (PurchaseUtils.hasIncorrectSkuDetails(details!!)) {
                activateStateSafely(AdsRemovalPaymentState.PRODUCT_DETAILS_FAILURE)
                return
            }
            if (uiObject == null) mPendingDetails =
                Collections.unmodifiableList(details) else uiObject!!.handleProductDetails(
                details
            )
            activateStateSafely(AdsRemovalPaymentState.PRICE_SELECTION)
        }

        override fun onPaymentFailure(@BillingResponse error: Int) {
            Statistics.INSTANCE.trackPurchaseStoreError(
                SubscriptionType.ADS_REMOVAL.serverId,
                error
            )
            activateStateSafely(AdsRemovalPaymentState.PAYMENT_FAILURE)
        }

        override fun onProductDetailsFailure() {
            activateStateSafely(AdsRemovalPaymentState.PRODUCT_DETAILS_FAILURE)
        }

        override fun onStoreConnectionFailed() {
            activateStateSafely(AdsRemovalPaymentState.PRODUCT_DETAILS_FAILURE)
        }

        override fun onValidationStarted() {
            Statistics.INSTANCE.trackPurchaseEvent(
                Statistics.EventName.INAPP_PURCHASE_STORE_SUCCESS,
                SubscriptionType.ADS_REMOVAL.serverId
            )
            activateStateSafely(AdsRemovalPaymentState.VALIDATION)
        }

        override fun onValidationFinish(success: Boolean) {
            if (uiObject == null) mPendingActivationResult =
                success else uiObject!!.handleActivationResult(success)
            activateStateSafely(AdsRemovalPaymentState.VALIDATION_FINISH)
        }

        override fun onAttach(uiObject: AdsRemovalPurchaseDialog?) {
            if (mPendingDetails != null) {
                uiObject?.handleProductDetails(mPendingDetails!!)
                mPendingDetails = null
            }
            if (mPendingActivationResult != null) {
                uiObject?.handleActivationResult(mPendingActivationResult!!)
                mPendingActivationResult = null
            }
        }
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.BILLING)
        private val TAG = AdsRemovalPurchaseDialog::class.java.simpleName
        private const val EXTRA_CURRENT_STATE = "extra_current_state"
        private const val EXTRA_PRODUCT_DETAILS = "extra_product_details"
        private const val EXTRA_ACTIVATION_RESULT = "extra_activation_result"
        fun show(context: FragmentActivity) {
            show(context.supportFragmentManager)
        }

        fun show(parent: Fragment) {
            show(parent.childFragmentManager)
        }

        private fun show(manager: FragmentManager) {
            val fragment: DialogFragment = AdsRemovalPurchaseDialog()
            fragment.show(manager, AdsRemovalPurchaseDialog::class.java.name)
        }
    }
}