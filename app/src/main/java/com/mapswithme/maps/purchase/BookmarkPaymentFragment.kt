package com.mapswithme.maps.purchase

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.android.billingclient.api.SkuDetails
import com.bumptech.glide.Glide
import com.mapswithme.maps.Framework
import com.mapswithme.maps.PrivateVariables
import com.mapswithme.maps.PurchaseOperationObservable
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmFragment
import com.mapswithme.maps.bookmarks.data.PaymentData
import com.mapswithme.maps.dialog.AlertDialogCallback
import com.mapswithme.util.Utils
import com.mapswithme.util.log.LoggerFactory
import com.mapswithme.util.statistics.Statistics
import com.mapswithme.util.statistics.Statistics.EventName

class BookmarkPaymentFragment : BaseMwmFragment(), AlertDialogCallback,
    PurchaseStateActivator<BookmarkPaymentState> {
    private var mPurchaseController: PurchaseController<PurchaseCallback>? = null
    private lateinit var mPurchaseCallback: BookmarkPurchaseCallback
    private lateinit var mPaymentData: PaymentData
    private var mProductDetails: ProductDetails? = null
    private var mSubsProductDetails: ProductDetails? = null
    private var mValidationResult = false
    private var mState = BookmarkPaymentState.NONE
    private var mSubsProductDetailsLoadingManager: BillingManager<PlayStoreBillingCallback>? = null
    private val mSubsProductDetailsCallback =
        SubsProductDetailsCallback()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args =
            arguments ?: throw IllegalStateException("Args must be provided for payment fragment!")
        val paymentData: PaymentData =
            args.getParcelable(ARG_PAYMENT_DATA)
                ?: throw IllegalStateException("Payment data must be provided for payment fragment!")
        mPaymentData = paymentData
        mPurchaseCallback = BookmarkPurchaseCallback(mPaymentData.serverId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mPurchaseController = PurchaseFactory.createBookmarkPurchaseController(
            requireContext(),
            mPaymentData.productId,
            mPaymentData.serverId
        )
        if (savedInstanceState != null) mPurchaseController!!.onRestore(savedInstanceState)
        mPurchaseController!!.initialize(requireActivity())
        mSubsProductDetailsLoadingManager = PurchaseFactory.createSubscriptionBillingManager()
        mSubsProductDetailsLoadingManager!!.initialize(requireActivity())
        mSubsProductDetailsLoadingManager!!.addCallback(mSubsProductDetailsCallback)
        mSubsProductDetailsCallback.attach(this)
        val root =
            inflater.inflate(R.layout.fragment_bookmark_payment, container, false)
        val subscriptionButton =
            root.findViewById<View>(R.id.buy_subs_btn)
        subscriptionButton.setOnClickListener { v: View? -> onBuySubscriptionClicked() }
        val buyInappBtn = root.findViewById<TextView>(R.id.buy_inapp_btn)
        buyInappBtn.setOnClickListener { v: View? -> onBuyInappClicked() }
        return root
    }

    private fun onBuySubscriptionClicked() {
        val type: SubscriptionType =
            SubscriptionType.Companion.getTypeByBookmarksGroup(
                mPaymentData.group
            )
        if (type == SubscriptionType.BOOKMARKS_SIGHTS) {
            BookmarksSightsSubscriptionActivity.Companion.startForResult(
                this,
                PurchaseUtils.REQ_CODE_PAY_SUBSCRIPTION,
                Statistics.ParamValue.CARD
            )
            return
        }
        BookmarksAllSubscriptionActivity.Companion.startForResult(
            this,
            PurchaseUtils.REQ_CODE_PAY_SUBSCRIPTION,
            Statistics.ParamValue.CARD
        )
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return
        if (requestCode == PurchaseUtils.REQ_CODE_PAY_SUBSCRIPTION) {
            val intent = Intent()
            intent.putExtra(PurchaseUtils.EXTRA_IS_SUBSCRIPTION, true)
            requireActivity().setResult(Activity.RESULT_OK, intent)
            requireActivity().finish()
        }
    }

    private fun onBuyInappClicked() {
        Statistics.INSTANCE.trackPurchasePreviewSelect(
            mPaymentData.serverId,
            mPaymentData.productId
        )
        Statistics.INSTANCE.trackPurchaseEvent(
            EventName.INAPP_PURCHASE_PREVIEW_PAY,
            mPaymentData.serverId,
            Statistics.STATISTICS_CHANNEL_REALTIME
        )
        startPurchaseTransaction()
    }

    override fun onBackPressed(): Boolean {
        if (mState === BookmarkPaymentState.VALIDATION) {
            Toast.makeText(
                requireContext(),
                R.string.purchase_please_wait_toast,
                Toast.LENGTH_SHORT
            )
                .show()
            return true
        }
        Statistics.INSTANCE.trackPurchaseEvent(
            EventName.INAPP_PURCHASE_PREVIEW_CANCEL,
            mPaymentData.serverId
        )
        return super.onBackPressed()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) Statistics.INSTANCE.trackPurchasePreviewShow(
            mPaymentData.serverId,
            PrivateVariables.bookmarksVendor(),
            mPaymentData.productId
        )
        LOGGER.d(
            TAG,
            "onViewCreated savedInstanceState = $savedInstanceState"
        )
        setInitialPaymentData()
        loadImage()
        if (savedInstanceState != null) {
            mProductDetails =
                savedInstanceState.getParcelable(EXTRA_PRODUCT_DETAILS)
            if (mProductDetails != null) updateProductDetails()
            mSubsProductDetails =
                savedInstanceState.getParcelable(EXTRA_SUBS_PRODUCT_DETAILS)
            if (mSubsProductDetails != null) updateSubsProductDetails()
            mValidationResult =
                savedInstanceState.getBoolean(EXTRA_VALIDATION_RESULT)
            val savedState =
                BookmarkPaymentState.values()[savedInstanceState.getInt(EXTRA_CURRENT_STATE)]
            activateState(savedState)
            return
        }
        activateState(BookmarkPaymentState.PRODUCT_DETAILS_LOADING)
        mPurchaseController?.queryProductDetails()
        val subsProductIds: List<String?> =
            listOf(PrivateVariables.bookmarksSubscriptionMonthlyProductId())
        mSubsProductDetailsLoadingManager?.queryProductDetails(subsProductIds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mPurchaseController?.destroy()
        mSubsProductDetailsLoadingManager?.removeCallback(mSubsProductDetailsCallback)
        mSubsProductDetailsCallback.detach()
        mSubsProductDetailsLoadingManager?.destroy()
    }

    private fun startPurchaseTransaction() {
        activateState(BookmarkPaymentState.TRANSACTION_STARTING)
        Framework.nativeStartPurchaseTransaction(
            mPaymentData.serverId,
            PrivateVariables.bookmarksVendor()
        )
    }

    fun launchBillingFlow() {
        mPurchaseController?.launchPurchaseFlow(mPaymentData.productId)
        activateState(BookmarkPaymentState.PAYMENT_IN_PROGRESS)
    }

    override fun onStart() {
        super.onStart()
        val observable =
            PurchaseOperationObservable.from(requireContext())
        observable.addTransactionObserver(mPurchaseCallback)
        mPurchaseController?.addCallback(mPurchaseCallback)
        mPurchaseCallback.attach(this)
    }

    override fun onStop() {
        super.onStop()
        val observable =
            PurchaseOperationObservable.from(requireContext())
        observable.removeTransactionObserver(mPurchaseCallback)
        mPurchaseController?.removeCallback()
        mPurchaseCallback.detach()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        LOGGER.d(
            TAG,
            "onSaveInstanceState"
        )
        outState.putInt(EXTRA_CURRENT_STATE, mState.ordinal)
        outState.putParcelable(
            EXTRA_PRODUCT_DETAILS,
            mProductDetails
        )
        outState.putParcelable(
            EXTRA_SUBS_PRODUCT_DETAILS,
            mSubsProductDetails
        )
        mPurchaseController?.onSave(outState)
    }

    override fun activateState(state: BookmarkPaymentState?) {
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

    private fun loadImage() {
        if (TextUtils.isEmpty(mPaymentData.imgUrl)) return
        val imageView =
            viewOrThrow.findViewById<ImageView>(R.id.image)
        Glide.with(imageView.context)
            .load(mPaymentData.imgUrl)
            .centerCrop()
            .into(imageView)
    }

    private fun setInitialPaymentData() {
        val name = viewOrThrow.findViewById<TextView>(R.id.product_catalog_name)
        name.text = mPaymentData.name
        val author = viewOrThrow.findViewById<TextView>(R.id.author_name)
        author.text = mPaymentData.authorName
    }

    fun handleProductDetails(details: List<SkuDetails?>) {
        if (details.isEmpty()) return
        val skuDetails = details[0]!!
        mProductDetails = PurchaseUtils.toProductDetails(skuDetails)
    }

    fun handleSubsProductDetails(details: List<SkuDetails?>) {
        if (details.isEmpty()) return
        val skuDetails = details[0]!!
        mSubsProductDetails = PurchaseUtils.toProductDetails(skuDetails)
    }

    fun handleValidationResult(validationResult: Boolean) {
        mValidationResult = validationResult
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
            PurchaseUtils.REQ_CODE_PRODUCT_DETAILS_FAILURE -> requireActivity().finish()
            PurchaseUtils.REQ_CODE_START_TRANSACTION_FAILURE, PurchaseUtils.REQ_CODE_PAYMENT_FAILURE -> activateState(
                BookmarkPaymentState.PRODUCT_DETAILS_LOADED
            )
        }
    }

    fun updateProductDetails() {
        if (mProductDetails == null) throw AssertionError("Product details must be obtained at this moment!")
        val buyButton = viewOrThrow.findViewById<TextView>(R.id.buy_inapp_btn)
        val price = Utils.formatCurrencyString(
            mProductDetails!!.price,
            mProductDetails!!.currencyCode
        )
        buyButton.text = getString(R.string.buy_btn, price)
        val storeName = viewOrThrow.findViewById<TextView>(R.id.product_store_name)
        storeName.text = mProductDetails!!.title
    }

    fun updateSubsProductDetails() {
        if (mSubsProductDetails == null) throw AssertionError("Subs product details must be obtained at this moment!")
        val formattedPrice = Utils.formatCurrencyString(
            mSubsProductDetails!!.price,
            mSubsProductDetails!!.currencyCode
        )
        val subsButton = viewOrThrow.findViewById<TextView>(R.id.buy_subs_btn)
        subsButton.text = getString(R.string.buy_btn_for_subscription_version_2, formattedPrice)
    }

    fun finishValidation() {
        if (mValidationResult) requireActivity().setResult(Activity.RESULT_OK)
        requireActivity().finish()
    }

    companion object {
        const val ARG_PAYMENT_DATA = "arg_payment_data"
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.BILLING)
        private val TAG = BookmarkPaymentFragment::class.java.simpleName
        private const val EXTRA_CURRENT_STATE = "extra_current_state"
        private const val EXTRA_PRODUCT_DETAILS = "extra_product_details"
        private const val EXTRA_SUBS_PRODUCT_DETAILS = "extra_subs_product_details"
        private const val EXTRA_VALIDATION_RESULT = "extra_validation_result"
    }
}