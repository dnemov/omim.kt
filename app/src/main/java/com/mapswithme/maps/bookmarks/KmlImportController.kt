package com.mapswithme.maps.bookmarks

import android.app.Activity
import android.app.ProgressDialog
import android.content.DialogInterface
import com.mapswithme.maps.R
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.bookmarks.data.BookmarkManager.KmlConversionListener
import com.mapswithme.maps.dialog.DialogUtils

class KmlImportController internal constructor(
    private val mContext: Activity,
    private val mCallback: ImportKmlCallback?
) :
    KmlConversionListener {
    private var mProgressDialog: ProgressDialog? = null
    fun onStart() {
        BookmarkManager.INSTANCE.addKmlConversionListener(this)
    }

    fun onStop() {
        BookmarkManager.INSTANCE.removeKmlConversionListener(this)
    }

    fun importKml() {
        val count = BookmarkManager.INSTANCE.kmlFilesCountForConversion
        if (count == 0) return
        val clickListener =
            DialogInterface.OnClickListener { dialog: DialogInterface, which: Int ->
                BookmarkManager.INSTANCE.convertAllKmlFiles()
                dialog.dismiss()
                mProgressDialog =
                    DialogUtils.createModalProgressDialog(mContext, R.string.converting)
                mProgressDialog!!.show()
            }
        val msg = mContext.resources.getQuantityString(
            R.plurals.bookmarks_detect_message, count, count
        )
        DialogUtils.showAlertDialog(
            mContext, R.string.bookmarks_detect_title, msg,
            R.string.button_convert, clickListener, R.string.cancel
        )
    }

    override fun onFinishKmlConversion(success: Boolean) {
        if (mProgressDialog != null && mProgressDialog!!.isShowing) mProgressDialog!!.dismiss()
        if (success) {
            mCallback?.onFinishKmlImport()
            return
        }
        DialogUtils.showAlertDialog(
            mContext, R.string.bookmarks_convert_error_title,
            R.string.bookmarks_convert_error_message
        )
    }

    internal interface ImportKmlCallback {
        fun onFinishKmlImport()
    }

}