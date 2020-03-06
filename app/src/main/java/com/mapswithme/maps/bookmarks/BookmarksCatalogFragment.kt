package com.mapswithme.maps.bookmarks

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.webkit.*
import android.widget.Toast
import com.android.billingclient.api.SkuDetails
import com.mapswithme.maps.Framework
import com.mapswithme.maps.PrivateVariables
import com.mapswithme.maps.R
import com.mapswithme.maps.auth.BaseWebViewMwmFragment
import com.mapswithme.maps.auth.TargetFragmentCallback
import com.mapswithme.maps.bookmarks.BookmarksCatalogFragment
import com.mapswithme.maps.bookmarks.BookmarksCatalogFragment.WebViewBookmarksCatalogClient
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.dialog.AlertDialog
import com.mapswithme.maps.dialog.AlertDialogCallback
import com.mapswithme.maps.dialog.ConfirmationDialogFactory
import com.mapswithme.maps.metrics.UserActionsLogger
import com.mapswithme.maps.purchase.*
import com.mapswithme.util.*
import com.mapswithme.util.log.LoggerFactory
import com.mapswithme.util.statistics.Statistics
import java.io.UnsupportedEncodingException
import java.lang.ref.WeakReference
import java.net.URLEncoder
import java.util.*

class BookmarksCatalogFragment : BaseWebViewMwmFragment(), TargetFragmentCallback,
    AlertDialogCallback {
    private lateinit var mWebViewClient: WebViewBookmarksCatalogClient
    private lateinit var mWebView: WebView
    private lateinit var mRetryBtn: View
    private lateinit var mProgressView: View
    private lateinit var mFailedPurchaseController: PurchaseController<FailedPurchaseChecker>
    private lateinit var mPurchaseChecker: FailedPurchaseChecker
    private lateinit var mProductDetailsLoadingManager: BillingManager<PlayStoreBillingCallback>
    private lateinit var mProductDetailsLoadingCallback: PlayStoreBillingCallback
    private lateinit var mDelegate: BookmarksDownloadFragmentDelegate
    private lateinit var mInvalidSubsDialogCallback: AlertDialogCallback
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mDelegate = BookmarksDownloadFragmentDelegate(this)
        mDelegate.onCreate(savedInstanceState)
        mInvalidSubsDialogCallback = InvalidSubscriptionAlertDialogCallback(this)
    }

    override fun onStart() {
        super.onStart()
        mDelegate.onStart()
        mFailedPurchaseController.addCallback(mPurchaseChecker)
        mFailedPurchaseController.validateExistingPurchases()
        mProductDetailsLoadingManager.addCallback(mProductDetailsLoadingCallback)
    }

    override fun onResume() {
        super.onResume()
        mDelegate.onResume()
    }

    override fun onPause() {
        super.onPause()
        mDelegate.onPause()
    }

    override fun onStop() {
        super.onStop()
        mDelegate.onStop()
        mFailedPurchaseController.removeCallback()
        mProductDetailsLoadingManager.removeCallback(mProductDetailsLoadingCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mDelegate.onDestroyView()
        mWebViewClient.clear()
        mFailedPurchaseController.destroy()
        mProductDetailsLoadingManager.destroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        mFailedPurchaseController =
            PurchaseFactory.createFailedBookmarkPurchaseController(requireContext())
        mFailedPurchaseController.initialize(requireActivity())
        mPurchaseChecker = FailedBookmarkPurchaseChecker()
        mProductDetailsLoadingManager = PurchaseFactory.createInAppBillingManager()
        mProductDetailsLoadingManager.initialize(requireActivity())
        mProductDetailsLoadingCallback = ProductDetailsLoadingCallback()
        val root =
            inflater.inflate(R.layout.fragment_bookmarks_catalog, container, false)
        mWebView = root.findViewById(webViewResId)
        mRetryBtn = root.findViewById(R.id.retry_btn)
        mProgressView = root.findViewById(R.id.progress)
        initWebView()
        mRetryBtn.setOnClickListener { v: View? -> onRetryClick() }
        mDelegate.onCreateView(savedInstanceState)
        return root
    }

    private fun onRetryClick() {
        mWebViewClient.retry()
        UiUtils.hide(mRetryBtn, mWebView)
        UiUtils.show(mProgressView)
        mFailedPurchaseController.validateExistingPurchases()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        mWebViewClient = WebViewBookmarksCatalogClient(this)
        mWebView.webViewClient = mWebViewClient
        val webSettings = mWebView.settings
        webSettings.javaScriptEnabled = true
        webSettings.userAgentString = Framework.nativeGetUserAgent()
        if (Utils.isLollipopOrLater) webSettings.mixedContentMode =
            WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
    }

    private val catalogUrlOrThrow: String
        private get() {
            val args = arguments
            var result =
                args?.getString(EXTRA_BOOKMARKS_CATALOG_URL)
            if (result == null) {
                result = requireActivity().intent
                    .getStringExtra(EXTRA_BOOKMARKS_CATALOG_URL)
            }
            requireNotNull(result) { "Catalog url not found in bundle" }
            return result
        }

    private fun downloadBookmark(url: String): Boolean {
        return mDelegate.downloadBookmark(url)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mDelegate.onSaveInstanceState(outState)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        mDelegate.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return
        if (requestCode == PurchaseUtils.REQ_CODE_PAY_SUBSCRIPTION) {
            showSubscriptionSuccessDialog()
            return
        }
        if (requestCode == PurchaseUtils.REQ_CODE_PAY_BOOKMARK && data != null && data.getBooleanExtra(
                PurchaseUtils.EXTRA_IS_SUBSCRIPTION,
                false
            )
        ) {
            mWebView.reload()
        }
    }

    private fun showSubscriptionSuccessDialog() {
        val dialog =
            AlertDialog.Builder()
                .setTitleId(R.string.subscription_success_dialog_title)
                .setMessageId(R.string.subscription_success_dialog_message)
                .setPositiveBtnId(R.string.subscription_error_button)
                .setReqCode(PurchaseUtils.REQ_CODE_BMK_SUBS_SUCCESS_DIALOG)
                .setFragManagerStrategyType(AlertDialog.FragManagerStrategyType.ACTIVITY_FRAGMENT_MANAGER)
                .setDialogViewStrategyType(AlertDialog.DialogViewStrategyType.CONFIRMATION_DIALOG)
                .setDialogFactory(ConfirmationDialogFactory())
                .build()
        dialog.setTargetFragment(this, PurchaseUtils.REQ_CODE_BMK_SUBS_SUCCESS_DIALOG)
        dialog.show(this, PurchaseUtils.DIALOG_TAG_BMK_SUBSCRIPTION_SUCCESS)
    }

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater
    ) {
        inflater.inflate(R.menu.menu_bookmark_catalog, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.close) requireActivity().finish()
        return super.onOptionsItemSelected(item)
    }

    override fun onTargetFragmentResult(resultCode: Int, data: Intent?) {
        mDelegate.onTargetFragmentResult(resultCode, data)
    }

    override val isTargetAdded: Boolean
        get() = mDelegate.isTargetAdded

    private fun loadCatalog(productDetailsBundle: String?) {
        val token = Framework.nativeGetAccessToken()
        val headers: MutableMap<String, String?> =
            HashMap()
        if (!TextUtils.isEmpty(token)) headers[HttpClient.HEADER_AUTHORIZATION] =
            HttpClient.HEADER_BEARER_PREFFIX + token
        if (!TextUtils.isEmpty(productDetailsBundle)) headers[HttpClient.HEADER_BUNDLE_TIERS] =
            productDetailsBundle
        for (header in BookmarkManager.INSTANCE.catalogHeaders) {
            if (!TextUtils.isEmpty(header.mValue)) headers[header.mKey] = header.mValue
        }
        mWebView.loadUrl(catalogUrlOrThrow, headers)
        UserActionsLogger.logBookmarksCatalogShownEvent()
    }

    override fun onAlertDialogPositiveClick(requestCode: Int, which: Int) {
        when (requestCode) {
            PurchaseUtils.REQ_CODE_CHECK_INVALID_SUBS_DIALOG -> mInvalidSubsDialogCallback.onAlertDialogPositiveClick(
                requestCode,
                which
            )
            PurchaseUtils.REQ_CODE_BMK_SUBS_SUCCESS_DIALOG -> onRetryClick()
        }
    }

    override fun onAlertDialogNegativeClick(requestCode: Int, which: Int) {
        if (requestCode == PurchaseUtils.REQ_CODE_CHECK_INVALID_SUBS_DIALOG) {
            mInvalidSubsDialogCallback.onAlertDialogNegativeClick(requestCode, which)
        }
    }

    override fun onAlertDialogCancel(requestCode: Int) {
        when (requestCode) {
            PurchaseUtils.REQ_CODE_CHECK_INVALID_SUBS_DIALOG -> mInvalidSubsDialogCallback.onAlertDialogCancel(
                requestCode
            )
            PurchaseUtils.REQ_CODE_BMK_SUBS_SUCCESS_DIALOG -> onRetryClick()
        }
    }

    private class WebViewBookmarksCatalogClient internal constructor(frag: BookmarksCatalogFragment) :
        WebViewClient() {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.BILLING)
        private val TAG = WebViewBookmarksCatalogClient::class.java.simpleName
        private val mReference: WeakReference<BookmarksCatalogFragment>
        private var mError: Any? = null
        override fun shouldOverrideUrlLoading(
            view: WebView,
            url: String
        ): Boolean {
            val fragment = mReference.get() ?: return false
            val result = fragment.downloadBookmark(url)
            val uri = Uri.parse(url)
            val pathSegments = uri.pathSegments
            for (each in pathSegments) {
                if (TextUtils.equals(
                        each,
                        SUBSCRIBE_PATH_SEGMENT
                    )
                ) {
                    val group = PurchaseUtils.getTargetBookmarkGroupFromUri(uri)
                    openSubscriptionScreen(
                        SubscriptionType.getTypeByBookmarksGroup(
                            group
                        )
                    )
                    return true
                }
            }
            return result
        }

        private fun openSubscriptionScreen(type: SubscriptionType) {
            val frag = mReference.get()
            if (frag == null || frag.activity == null) return
            if (type == SubscriptionType.BOOKMARKS_ALL) BookmarksAllSubscriptionActivity.startForResult(
                frag,
                PurchaseUtils.REQ_CODE_PAY_SUBSCRIPTION,
                Statistics.ParamValue.WEBVIEW
            ) else BookmarksSightsSubscriptionActivity.startForResult(
                frag,
                PurchaseUtils.REQ_CODE_PAY_SUBSCRIPTION,
                Statistics.ParamValue.WEBVIEW
            )
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            var frag: BookmarksCatalogFragment
            if (mReference.get().also { frag = it!! } == null || mError != null) {
                return
            }
            UiUtils.show(frag.mWebView)
            UiUtils.hide(frag.mProgressView, frag.mRetryBtn)
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            super.onReceivedError(view, request, error)
            val description =
                if (Utils.isMarshmallowOrLater) makeDescription(
                    error
                ) else null
            handleError(error, description)
        }

        override fun onReceivedSslError(
            view: WebView,
            handler: SslErrorHandler,
            error: SslError
        ) {
            super.onReceivedSslError(view, handler, error)
            handleError(error)
        }

        private fun handleError(
            error: Any,
            description: String? = null
        ) {
            mError = error
            var frag: BookmarksCatalogFragment
            if (mReference.get().also { frag = it!! } == null) return
            UiUtils.show(frag.mRetryBtn)
            UiUtils.hide(frag.mWebView, frag.mProgressView)
            if (ConnectionState.isConnected) {
                LOGGER.e(TAG, "Failed to load catalog: $mError, description: $description")
                Statistics.INSTANCE.trackDownloadCatalogError(Statistics.ParamValue.UNKNOWN)
                return
            }
            Statistics.INSTANCE.trackDownloadCatalogError(Statistics.ParamValue.NO_INTERNET)
            Toast.makeText(
                frag.context, R.string.common_check_internet_connection_dialog_title,
                Toast.LENGTH_SHORT
            ).show()
        }

        fun retry() {
            mError = null
        }

        fun clear() {
            mReference.clear()
        }

        companion object {
            private const val SUBSCRIBE_PATH_SEGMENT = "subscribe"
            @TargetApi(Build.VERSION_CODES.M)
            private fun makeDescription(error: WebResourceError): String {
                return error.errorCode.toString() + "  " + error.description
            }
        }

        init {
            mReference = WeakReference(frag)
        }
    }

    private inner class FailedBookmarkPurchaseChecker : FailedPurchaseChecker {
        override fun onFailedPurchaseDetected(isDetected: Boolean) {
            if (isDetected) {
                UiUtils.hide(mProgressView)
                UiUtils.show(mRetryBtn)
                val dialog =
                    AlertDialog.Builder()
                        .setTitleId(R.string.bookmarks_convert_error_title)
                        .setMessageId(R.string.failed_purchase_support_message)
                        .setPositiveBtnId(R.string.ok)
                        .build()
                dialog.show(
                    this@BookmarksCatalogFragment,
                    FAILED_PURCHASE_DIALOG_TAG
                )
                return
            }
            UiUtils.show(mProgressView)
            UiUtils.hide(mRetryBtn)
            mProductDetailsLoadingManager.queryProductDetails(
                Arrays.asList(
                    *PrivateVariables.bookmarkInAppIds()
                )
            )
        }

        override fun onAuthorizationRequired() {
            mDelegate.authorize(Runnable { mFailedPurchaseController.validateExistingPurchases() })
        }

        override fun onStoreConnectionFailed() {
            LOGGER.e(
                TAG,
                "Failed to check failed bookmarks due play store connection failure"
            )
            loadCatalog(null)
        }
    }

    private inner class ProductDetailsLoadingCallback :
        AbstractProductDetailsLoadingCallback() {
        override fun onProductDetailsLoaded(details: List<SkuDetails>) {
            if (details.isEmpty()) {
                LOGGER.i(
                    TAG,
                    "Product details not found."
                )
                loadCatalog(null)
                return
            }
            LOGGER.i(
                TAG,
                "Product details for web catalog loaded: $details"
            )
            loadCatalog(toDetailsBundle(details))
        }

        private fun toDetailsBundle(details: List<SkuDetails>): String? {
            val bundle = PurchaseUtils.toProductDetailsBundle(details)
            var encodedBundle: String? = null
            try {
                encodedBundle = URLEncoder.encode(bundle, "UTF-8")
            } catch (e: UnsupportedEncodingException) {
                val msg = "Failed to encode details bundle '$bundle': "
                LOGGER.e(
                    TAG,
                    msg,
                    e
                )
                CrashlyticsUtils.logException(RuntimeException(msg, e))
            }
            return encodedBundle
        }

        override fun onProductDetailsFailure() {
            LOGGER.e(
                TAG,
                "Failed to load product details for web catalog"
            )
            loadCatalog(null)
        }
    }

    companion object {
        const val EXTRA_BOOKMARKS_CATALOG_URL = "bookmarks_catalog_url"
        private const val FAILED_PURCHASE_DIALOG_TAG = "failed_purchase_dialog_tag"
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.BILLING)
        private val TAG = BookmarksCatalogFragment::class.java.simpleName
    }
}