package com.mapswithme.maps.purchase

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.CallSuper
import com.android.billingclient.api.SkuDetails
import com.mapswithme.maps.Framework.AuthTokenType
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseAuthFragment

import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.bookmarks.data.BookmarkManager.BookmarksCatalogPingListener
import com.mapswithme.maps.dialog.AlertDialog
import com.mapswithme.maps.dialog.AlertDialogCallback
import com.mapswithme.maps.purchase.PurchaseUtils.toProductDetails
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.NetworkPolicy
import com.mapswithme.util.log.LoggerFactory
import com.mapswithme.util.statistics.Statistics
import java.util.*

abstract class AbstractBookmarkSubscriptionFragment : BaseAuthFragment(),
    PurchaseStateActivator<BookmarkSubscriptionPaymentState?>, SubscriptionUiChangeListener,
    AlertDialogCallback {
    private var mPingingResult = false
    private var mValidationResult = false
    private val mPingCallback = PingCallback()
    private lateinit var mPurchaseCallback: BookmarkSubscriptionCallback
    private var mState = BookmarkSubscriptionPaymentState.NONE
    private lateinit var mProductDetails: MutableList<ProductDetails>
    private lateinit var mPurchaseController: PurchaseController<PurchaseCallback>
    private lateinit var mDelegate: SubscriptionFragmentDelegate
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mPurchaseCallback = BookmarkSubscriptionCallback(subscriptionType)
        mPurchaseController = createPurchaseController()
        if (savedInstanceState != null) mPurchaseController.onRestore(savedInstanceState)
        mPurchaseController.initialize(requireActivity())
        mPingCallback.attach(this)
        BookmarkManager.INSTANCE.addCatalogPingListener(mPingCallback)
        Statistics.INSTANCE.trackPurchasePreviewShow(
            subscriptionType.serverId,
            subscriptionType.vendor,
            subscriptionType.yearlyProductId,
            extraFrom
        )
        val root =
            onSubscriptionCreateView(inflater, container, savedInstanceState)
        mDelegate = createFragmentDelegate(this)
        if (root != null) mDelegate.onCreateView(root)
        return root
    }

    private val extraFrom: String?
        private get() = if (arguments == null) null else arguments!!.getString(
            EXTRA_FROM,
            null
        )

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState != null) {
            val savedState =
                BookmarkSubscriptionPaymentState.values()[savedInstanceState.getInt(
                    EXTRA_CURRENT_STATE
                )]
            val productDetails =
                savedInstanceState.getParcelableArray(EXTRA_PRODUCT_DETAILS) as Array<ProductDetails>?
            if (productDetails != null) mProductDetails = productDetails.toMutableList()
            activateState(savedState)
            return
        }
        activateState(BookmarkSubscriptionPaymentState.CHECK_NETWORK_CONNECTION)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mPingCallback.detach()
        BookmarkManager.INSTANCE.removeCatalogPingListener(mPingCallback)
        mDelegate.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        mPurchaseController.addCallback(mPurchaseCallback)
        mPurchaseCallback.attach(this)
    }

    override fun onStop() {
        super.onStop()
        mPurchaseController.removeCallback()
        mPurchaseCallback.detach()
    }

    private fun queryProductDetails() {
        mPurchaseController.queryProductDetails()
    }

    private fun launchPurchaseFlow(productId: String) {
        mPurchaseController.launchPurchaseFlow(productId)
    }

    private fun handleProductDetails(details: List<SkuDetails>) {
        for (sku in details) {
            val period =
                PurchaseUtils.Period.valueOf(sku.subscriptionPeriod)
            mProductDetails.add(period.ordinal, toProductDetails(sku))
        }
    }

    fun getProductDetailsForPeriod(period: PurchaseUtils.Period): ProductDetails {
        if (mProductDetails == null) throw AssertionError("Product details must be exist at this moment!")
        return mProductDetails!![period.ordinal]
    }

    val productDetails: List<ProductDetails>?
        get() = if (mProductDetails == null) null else Collections.synchronizedList(
            mProductDetails
        )

    override fun onAuthorizationFinish(success: Boolean) {
        hideProgress()
        if (!success) {
            Toast.makeText(
                requireContext(),
                R.string.profile_authorization_error,
                Toast.LENGTH_LONG
            )
                .show()
            return
        }
        launchPurchaseFlow()
    }

    override fun onAuthorizationStart() {
        showProgress()
    }

    override val progressMessageId: Int
        get() = R.string.please_wait

    override fun onSocialAuthenticationCancel(@AuthTokenType type: Int) {
        LOGGER.i(
            TAG,
            "Social authentication cancelled,  auth type = $type"
        )
    }

    override fun onSocialAuthenticationError(@AuthTokenType type: Int, error: String?) {
        LOGGER.w(
            TAG,
            "Social authentication error = $error,  auth type = $type"
        )
    }


    override fun activateState(state: BookmarkSubscriptionPaymentState?) {
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

    private fun handlePingingResult(result: Boolean) {
        mPingingResult = result
    }

    private fun finishPinging() {
        if (!mPingingResult) {
            PurchaseUtils.showPingFailureDialog(this)
            return
        }
        authorize()
    }

    override fun onCheckNetworkConnection() {
        if (ConnectionState.isConnected) NetworkPolicy.checkNetworkPolicy(
            requireFragmentManager(),
            object : NetworkPolicy.NetworkPolicyListener {
                override fun onResult(policy: NetworkPolicy) {
                    onNetworkPolicyResult(policy)
                }
            },
            true
        ) else PurchaseUtils.showNoConnectionDialog(this)
    }

    override fun onProductDetailsFailure() {
        PurchaseUtils.showProductDetailsFailureDialog(this, javaClass.simpleName)
    }

    override fun onPaymentFailure() {
        PurchaseUtils.showPaymentFailureDialog(this, javaClass.simpleName)
    }

    private fun onNetworkPolicyResult(policy: NetworkPolicy) {
        if (policy.canUseNetwork()) onNetworkCheckPassed() else requireActivity().finish()
    }

    private fun onNetworkCheckPassed() {
        activateState(BookmarkSubscriptionPaymentState.PRODUCT_DETAILS_LOADING)
    }

    override fun onReset() {
        mDelegate.onReset()
    }

    override fun onPriceSelection() {
        mDelegate.onPriceSelection()
    }

    @CallSuper
    override fun onProductDetailsLoading() {
        queryProductDetails()
        mDelegate.onProductDetailsLoading()
    }

    override fun onPinging() {
        showButtonProgress()
    }

    @CallSuper
    override fun onPingFinish() {
        finishPinging()
        hideButtonProgress()
    }

    @CallSuper
    override fun onValidationFinish() {
        finishValidation()
        hideButtonProgress()
    }

    private fun finishValidation() {
        if (mValidationResult) requireActivity().setResult(Activity.RESULT_OK)
        requireActivity().finish()
    }

    private fun handleValidationResult(result: Boolean) {
        mValidationResult = result
    }

    override fun onAlertDialogPositiveClick(requestCode: Int, which: Int) {
        if (requestCode == PurchaseUtils.REQ_CODE_NO_NETWORK_CONNECTION_DIALOG) {
            dismissOutdatedNoNetworkDialog()
            activateState(BookmarkSubscriptionPaymentState.NONE)
            activateState(BookmarkSubscriptionPaymentState.CHECK_NETWORK_CONNECTION)
        }
    }

    private fun dismissOutdatedNoNetworkDialog() {
        val strategy =
            AlertDialog.FragManagerStrategyType.ACTIVITY_FRAGMENT_MANAGER.value
        val manager = strategy.resolve(this)
        val outdatedInstance =
            manager.findFragmentByTag(PurchaseUtils.NO_NETWORK_CONNECTION_DIALOG_TAG)
                ?: return
        manager.beginTransaction().remove(outdatedInstance).commitAllowingStateLoss()
        manager.executePendingTransactions()
    }

    override fun onAlertDialogNegativeClick(requestCode: Int, which: Int) {
        if (requestCode == PurchaseUtils.REQ_CODE_NO_NETWORK_CONNECTION_DIALOG) requireActivity().finish()
    }

    override fun onAlertDialogCancel(requestCode: Int) {
        if (requestCode == PurchaseUtils.REQ_CODE_NO_NETWORK_CONNECTION_DIALOG) requireActivity().finish()
    }

    abstract fun createPurchaseController(): PurchaseController<PurchaseCallback>
    abstract fun createFragmentDelegate(
        fragment: AbstractBookmarkSubscriptionFragment
    ): SubscriptionFragmentDelegate

    abstract fun onSubscriptionCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View?

    abstract val subscriptionType: SubscriptionType
    override fun onValidating() {
        showButtonProgress()
    }

    private fun showButtonProgress() {
        mDelegate.showButtonProgress()
    }

    private fun hideButtonProgress() {
        mDelegate.hideButtonProgress()
    }

    override fun onBackPressed(): Boolean {
        Statistics.INSTANCE.trackPurchaseEvent(
            Statistics.EventName.INAPP_PURCHASE_PREVIEW_CANCEL,
            subscriptionType.serverId
        )
        return super.onBackPressed()
    }

    fun pingBookmarkCatalog() {
        BookmarkManager.INSTANCE.pingBookmarkCatalog()
        activateState(BookmarkSubscriptionPaymentState.PINGING)
    }

    private val selectedPeriod: PurchaseUtils.Period
        private get() = mDelegate.selectedPeriod

    fun trackPayEvent() {
        Statistics.INSTANCE.trackPurchaseEvent(
            Statistics.EventName.INAPP_PURCHASE_PREVIEW_PAY,
            subscriptionType.serverId,
            Statistics.STATISTICS_CHANNEL_REALTIME
        )
    }

    private fun launchPurchaseFlow() {
        val details = getProductDetailsForPeriod(selectedPeriod)
        launchPurchaseFlow(details.productId)
    }

    fun calculateYearlySaving(): Int {
        val pricePerMonth = getProductDetailsForPeriod(PurchaseUtils.Period.P1M).price
        val pricePerYear = getProductDetailsForPeriod(PurchaseUtils.Period.P1Y).price
        return (100 * (1 - pricePerYear / (pricePerMonth * PurchaseUtils.MONTHS_IN_YEAR))).toInt()
    }

    fun trackYearlyProductSelected() {
        Statistics.INSTANCE.trackPurchasePreviewSelect(
            subscriptionType.serverId,
            subscriptionType.yearlyProductId
        )
    }

    fun trackMonthlyProductSelected() {
        Statistics.INSTANCE.trackPurchasePreviewSelect(
            subscriptionType.serverId,
            subscriptionType.monthlyProductId
        )
    }

    private class PingCallback :
        StatefulPurchaseCallback<BookmarkSubscriptionPaymentState?, AbstractBookmarkSubscriptionFragment?>(),
        BookmarksCatalogPingListener {
        private var mPendingPingingResult: Boolean? = null
        override fun onPingFinished(isServiceAvailable: Boolean) {
            LOGGER.i(
                TAG,
                "Ping finished, isServiceAvailable: $isServiceAvailable"
            )
            if (uiObject == null) mPendingPingingResult =
                isServiceAvailable else uiObject!!.handlePingingResult(isServiceAvailable)
            activateStateSafely(BookmarkSubscriptionPaymentState.PINGING_FINISH)
        }

        override fun onAttach(fragment: AbstractBookmarkSubscriptionFragment?) {
            if (mPendingPingingResult != null) {
                fragment?.handlePingingResult(mPendingPingingResult!!)
                mPendingPingingResult = null
            }
        }
    }

    private class BookmarkSubscriptionCallback(private val mType: SubscriptionType) :
        StatefulPurchaseCallback<BookmarkSubscriptionPaymentState?, AbstractBookmarkSubscriptionFragment?>(),
        PurchaseCallback {
        private var mPendingDetails: List<SkuDetails>? = null
        private var mPendingValidationResult: Boolean? = null

        override fun onProductDetailsLoaded(details: List<SkuDetails>?) {
            if (details != null && PurchaseUtils.hasIncorrectSkuDetails(details)) {
                activateStateSafely(BookmarkSubscriptionPaymentState.PRODUCT_DETAILS_FAILURE)
                return
            }
            if (uiObject == null) mPendingDetails =
                details?.toList() else uiObject!!.handleProductDetails(
                details!!
            )
            activateStateSafely(BookmarkSubscriptionPaymentState.PRICE_SELECTION)
        }

        override fun onPaymentFailure(error: Int) {
            Statistics.INSTANCE.trackPurchaseStoreError(
                mType.serverId,
                error
            )
            activateStateSafely(BookmarkSubscriptionPaymentState.PAYMENT_FAILURE)
        }

        override fun onProductDetailsFailure() {
            activateStateSafely(BookmarkSubscriptionPaymentState.PRODUCT_DETAILS_FAILURE)
        }

        override fun onStoreConnectionFailed() {
            activateStateSafely(BookmarkSubscriptionPaymentState.PRODUCT_DETAILS_FAILURE)
        }

        override fun onValidationStarted() {
            Statistics.INSTANCE.trackPurchaseEvent(
                Statistics.EventName.INAPP_PURCHASE_STORE_SUCCESS,
                mType.serverId
            )
            activateStateSafely(BookmarkSubscriptionPaymentState.VALIDATION)
        }

        override fun onValidationFinish(success: Boolean) {
            if (uiObject == null) mPendingValidationResult =
                success else uiObject!!.handleValidationResult(success)
            activateStateSafely(BookmarkSubscriptionPaymentState.VALIDATION_FINISH)
        }

        override fun onAttach(uiObject: AbstractBookmarkSubscriptionFragment?) {
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

    companion object {
        const val EXTRA_FROM = "extra_from"
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.BILLING)
        private val TAG =
            AbstractBookmarkSubscriptionFragment::class.java.simpleName
        private const val EXTRA_CURRENT_STATE = "extra_current_state"
        private const val EXTRA_PRODUCT_DETAILS = "extra_product_details"
    }
}