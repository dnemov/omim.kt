package com.mapswithme.maps.bookmarks

import androidx.fragment.app.Fragment
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.dialog.AlertDialogCallback
import com.mapswithme.maps.purchase.BookmarkSubscriptionActivity
import com.mapswithme.maps.purchase.PurchaseUtils
import com.mapswithme.util.log.LoggerFactory
import com.mapswithme.util.statistics.Statistics

internal class InvalidSubscriptionAlertDialogCallback(private val mFragment: Fragment) :
    AlertDialogCallback {
    override fun onAlertDialogPositiveClick(requestCode: Int, which: Int) {
        BookmarkSubscriptionActivity.startForResult(
            mFragment, PurchaseUtils.REQ_CODE_PAY_CONTINUE_SUBSCRIPTION,
            Statistics.ParamValue.POPUP
        )
    }

    override fun onAlertDialogNegativeClick(requestCode: Int, which: Int) {
        val logger =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.BILLING)
        val tag = InvalidSubscriptionAlertDialogCallback::class.java.simpleName
        logger.i(tag, "Delete invalid categories, user didn't continue subscription...")
        BookmarkManager.INSTANCE.deleteInvalidCategories()
    }

    override fun onAlertDialogCancel(requestCode: Int) { // Invalid subs dialog is not cancellable, so do nothing here.
    }

}