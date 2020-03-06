package com.mapswithme.maps.routing

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.maps.downloader.CountryItem
import com.mapswithme.maps.downloader.MapManager.warnOn3g
import com.mapswithme.maps.routing.ResultCodesHelper.getDialogTitleSubtitle
import com.mapswithme.maps.routing.ResultCodesHelper.isDownloadable
import com.mapswithme.maps.routing.ResultCodesHelper.isMoreMapsNeeded
import com.mapswithme.util.UiUtils

class RoutingErrorDialogFragment : BaseRoutingErrorDialogFragment() {
    private var mResultCode = 0
    private var mMessage: String? = null
    private var mNeedMoreMaps = false
    override fun beforeDialogCreated(builder: AlertDialog.Builder?) {
        super.beforeDialogCreated(builder)
        val titleMessage =
            getDialogTitleSubtitle(mResultCode, mMissingMaps.size)
        builder!!.setTitle(titleMessage.first)
        mMessage = titleMessage.second
        if (isDownloadable(
                mResultCode,
                mMissingMaps.size
            )
        ) builder.setPositiveButton(R.string.download, null)
        mNeedMoreMaps = isMoreMapsNeeded(mResultCode)
        if (mNeedMoreMaps) builder.setNegativeButton(R.string.later, null)
    }

    private fun addMessage(frame: View): View {
        UiUtils.setTextAndHideIfEmpty(
            frame.findViewById<View>(R.id.tv__message) as TextView,
            mMessage
        )
        return frame
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (mNeedMoreMaps && mCancelled) mCancelled = false
        super.onDismiss(dialog)
    }

    override fun buildSingleMapView(map: CountryItem): View {
        return addMessage(super.buildSingleMapView(map))
    }

    override fun buildMultipleMapView(): View? {
        return addMessage(super.buildMultipleMapView()!!)
    }

    private fun startDownload() {
        if (mMissingMaps.isEmpty()) {
            dismiss()
            return
        }
        var size: Long = 0
        for (country in mMissingMaps) {
            if (country.status != CountryItem.STATUS_PROGRESS &&
                country.status != CountryItem.STATUS_APPLYING
            ) {
                size += country.totalSize
            }
        }
        warnOn3g(activity!!, size, Runnable {
            val downloader: RoutingMapsDownloadFragment =
                RoutingMapsDownloadFragment.Companion.create(mMapsArray)
            downloader.show(
                activity!!.supportFragmentManager,
                downloader.javaClass.simpleName
            )
            mCancelled = false
            dismiss()
        })
    }

    override fun onStart() {
        super.onStart()
        val dlg =
            dialog as AlertDialog?
        val button =
            dlg!!.getButton(AlertDialog.BUTTON_POSITIVE) ?: return
        button.setOnClickListener { startDownload() }
    }

    override fun onCancel(dialog: DialogInterface) {
        mCancelled = true
        super.onCancel(dialog)
    }

    override fun parseArguments() {
        super.parseArguments()
        mResultCode = arguments!!.getInt(EXTRA_RESULT_CODE)
    }

    companion object {
        private const val EXTRA_RESULT_CODE = "RouterResultCode"
        @JvmStatic
        fun create(
            resultCode: Int,
            missingMaps: Array<String>?
        ): RoutingErrorDialogFragment {
            val args = Bundle()
            args.putInt(EXTRA_RESULT_CODE, resultCode)
            args.putStringArray(
                EXTRA_MISSING_MAPS,
                missingMaps
            )
            val res = Fragment.instantiate(
                MwmApplication.get(),
                RoutingErrorDialogFragment::class.java.name
            ) as RoutingErrorDialogFragment
            res.arguments = args
            return res
        }
    }
}