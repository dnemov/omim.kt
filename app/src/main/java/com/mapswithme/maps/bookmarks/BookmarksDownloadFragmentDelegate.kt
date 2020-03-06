package com.mapswithme.maps.bookmarks

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.mapswithme.maps.R
import com.mapswithme.maps.auth.Authorizer
import com.mapswithme.maps.auth.TargetFragmentCallback
import com.mapswithme.maps.base.Detachable
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.bookmarks.data.BookmarkManager.BookmarksInvalidCategoriesListener
import com.mapswithme.maps.bookmarks.data.PaymentData
import com.mapswithme.maps.dialog.AlertDialog
import com.mapswithme.maps.dialog.ConfirmationDialogFactory
import com.mapswithme.maps.dialog.ProgressDialogFragment
import com.mapswithme.maps.purchase.BookmarkPaymentActivity
import com.mapswithme.maps.purchase.PurchaseUtils
import com.mapswithme.util.log.LoggerFactory

internal class BookmarksDownloadFragmentDelegate(private val mFragment: Fragment) :
    Authorizer.Callback, BookmarkDownloadCallback, TargetFragmentCallback {
    private lateinit var mAuthorizer: Authorizer
    private lateinit var mDownloadController: BookmarkDownloadController
    private var mAuthCompletionRunnable: Runnable? = null
    private val mInvalidCategoriesListener: InvalidCategoriesListener
    fun onCreate(savedInstanceState: Bundle?) {
        mAuthorizer = Authorizer(mFragment)
        val application = mFragment.requireActivity().application
        mDownloadController = DefaultBookmarkDownloadController(
            application,
            CatalogListenerDecorator(mFragment)
        )
        if (savedInstanceState != null) mDownloadController.onRestore(savedInstanceState)
    }

    fun onStart() {
        mAuthorizer.attach(this)
        mDownloadController.attach(this)
        mInvalidCategoriesListener.attach(mFragment)
    }

    fun onResume() {
        LOGGER.i(
            TAG,
            "Check invalid bookmark categories..."
        )
        BookmarkManager.INSTANCE.checkInvalidCategories()
    }

    fun onPause() { // Do nothing.
    }

    fun onStop() {
        mAuthorizer.detach()
        mDownloadController.detach()
        mInvalidCategoriesListener.detach()
    }

    fun onCreateView(savedInstanceState: Bundle?) {
        BookmarkManager.INSTANCE.addInvalidCategoriesListener(mInvalidCategoriesListener)
    }

    fun onDestroyView() {
        BookmarkManager.INSTANCE.removeInvalidCategoriesListener(mInvalidCategoriesListener)
    }

    fun onSaveInstanceState(outState: Bundle) {
        mDownloadController.onSave(outState)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) return
        when (requestCode) {
            PurchaseUtils.REQ_CODE_PAY_CONTINUE_SUBSCRIPTION -> BookmarkManager.INSTANCE.resetInvalidCategories()
            PurchaseUtils.REQ_CODE_PAY_BOOKMARK -> mDownloadController.retryDownloadBookmark()
        }
    }

    private fun showAuthorizationProgress() {
        val message = mFragment.getString(R.string.please_wait)
        val dialog =
            ProgressDialogFragment.newInstance(message, false, true)
        mFragment.requireActivity().supportFragmentManager
            .beginTransaction()
            .add(dialog, dialog.javaClass.canonicalName)
            .commitAllowingStateLoss()
    }

    private fun hideAuthorizationProgress() {
        val fm =
            mFragment.requireActivity().supportFragmentManager
        val tag = ProgressDialogFragment::class.java.canonicalName
        val frag =
            fm.findFragmentByTag(tag) as DialogFragment?
        frag?.dismissAllowingStateLoss()
    }

    override fun onAuthorizationFinish(success: Boolean) {
        hideAuthorizationProgress()
        if (!success) {
            Toast.makeText(
                mFragment.context, R.string.profile_authorization_error,
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (mAuthCompletionRunnable != null) mAuthCompletionRunnable!!.run()
    }

    override fun onAuthorizationStart() {
        showAuthorizationProgress()
    }

    override fun onSocialAuthenticationCancel(type: Int) { // Do nothing by default.
    }

    override fun onSocialAuthenticationError(
        type: Int,
        error: String?
    ) { // Do nothing by default.
    }

    override fun onAuthorizationRequired() {
        authorize(Runnable { retryBookmarkDownload() })
    }

    override fun onPaymentRequired(data: PaymentData) {
        BookmarkPaymentActivity.startForResult(mFragment, data, PurchaseUtils.REQ_CODE_PAY_BOOKMARK)
    }

    override fun onTargetFragmentResult(resultCode: Int, data: Intent?) {
        mAuthorizer.onTargetFragmentResult(resultCode, data)
    }

    override val isTargetAdded: Boolean
        get() = mFragment.isAdded

    fun downloadBookmark(url: String): Boolean {
        return mDownloadController.downloadBookmark(url)
    }

    private fun retryBookmarkDownload() {
        mDownloadController.retryDownloadBookmark()
    }

    fun authorize(completionRunnable: Runnable) {
        mAuthCompletionRunnable = completionRunnable
        mAuthorizer.authorize()
    }

    private class InvalidCategoriesListener internal constructor(fragment: Fragment) :
        BookmarksInvalidCategoriesListener, Detachable<Fragment?> {
        private var mFrag: Fragment?
        private var mPendingInvalidCategoriesResult: Boolean? = null
        override fun onCheckInvalidCategories(hasInvalidCategories: Boolean) {
            LOGGER.i(
                TAG,
                "Has invalid categories: $hasInvalidCategories"
            )
            if (mFrag == null) {
                mPendingInvalidCategoriesResult = hasInvalidCategories
                return
            }
            if (!hasInvalidCategories) return
            showInvalidBookmarksDialog()
        }

        private fun showInvalidBookmarksDialog() {
            if (mFrag == null) return
            val dialog =
                AlertDialog.Builder()
                    .setTitleId(R.string.renewal_screen_title)
                    .setMessageId(R.string.renewal_screen_message)
                    .setPositiveBtnId(R.string.renewal_screen_button_restore)
                    .setNegativeBtnId(R.string.renewal_screen_button_cancel)
                    .setReqCode(PurchaseUtils.REQ_CODE_CHECK_INVALID_SUBS_DIALOG)
                    .setImageResId(R.drawable.ic_error_red)
                    .setFragManagerStrategyType(AlertDialog.FragManagerStrategyType.ACTIVITY_FRAGMENT_MANAGER)
                    .setDialogViewStrategyType(AlertDialog.DialogViewStrategyType.CONFIRMATION_DIALOG)
                    .setDialogFactory(ConfirmationDialogFactory())
                    .setNegativeBtnTextColor(R.color.rating_horrible)
                    .build()
            dialog.isCancelable = false
            dialog.setTargetFragment(mFrag, PurchaseUtils.REQ_CODE_CHECK_INVALID_SUBS_DIALOG)
            dialog.show(mFrag!!, PurchaseUtils.DIALOG_TAG_CHECK_INVALID_SUBS)
        }

        override fun attach(`object`: Fragment?) {
            mFrag = `object`
            if (java.lang.Boolean.TRUE == mPendingInvalidCategoriesResult) {
                showInvalidBookmarksDialog()
                mPendingInvalidCategoriesResult = null
            }
        }

        override fun detach() {
            mFrag = null
        }

        init {
            mFrag = fragment
        }
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.BILLING)
        private val TAG =
            BookmarksDownloadFragmentDelegate::class.java.simpleName
    }

    init {
        mInvalidCategoriesListener = InvalidCategoriesListener(mFragment)
    }
}