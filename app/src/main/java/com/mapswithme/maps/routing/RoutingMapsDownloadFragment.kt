package com.mapswithme.maps.routing

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.maps.downloader.CountryItem
import com.mapswithme.maps.downloader.MapManager.StorageCallback
import com.mapswithme.maps.downloader.MapManager.StorageCallbackData
import com.mapswithme.maps.downloader.MapManager.nativeCancel
import com.mapswithme.maps.downloader.MapManager.nativeDownload
import com.mapswithme.maps.downloader.MapManager.nativeGetOverallProgress
import com.mapswithme.maps.downloader.MapManager.nativeSubscribe
import com.mapswithme.maps.downloader.MapManager.nativeUnsubscribe
import com.mapswithme.maps.widget.WheelProgressView
import com.mapswithme.util.UiUtils
import java.util.*

class RoutingMapsDownloadFragment : BaseRoutingErrorDialogFragment() {
    private var mItemsFrame: ViewGroup? = null
    private val mMaps: MutableSet<String?> =
        HashSet()
    private var mSubscribeSlot = 0
    override fun beforeDialogCreated(builder: AlertDialog.Builder?) {
        super.beforeDialogCreated(builder)
        builder!!.setTitle(R.string.downloading)
        val mapsList = mutableListOf<String>()
        for (i in mMissingMaps.indices) {
            val item = mMissingMaps[i]
            mMaps.add(item.id)
            mapsList.add(i, item.id!!)
        }
        mMapsArray = mapsList.toTypedArray()
        for (map in mMaps) nativeDownload(map)
    }

    private fun setupFrame(frame: View): View {
        UiUtils.hide(frame.findViewById(R.id.tv__message))
        mItemsFrame = frame.findViewById<View>(R.id.items_frame) as ViewGroup
        return frame
    }

    override fun buildSingleMapView(map: CountryItem): View {
        val res = setupFrame(super.buildSingleMapView(map))
        bindGroup(res)
        return res
    }

    override fun buildMultipleMapView(): View? {
        return setupFrame(super.buildMultipleMapView()!!)
    }

    private val wheel: WheelProgressView?
        private get() {
            if (mItemsFrame == null) return null
            val frame = mItemsFrame!!.getChildAt(0) ?: return null
            val res =
                frame.findViewById<View>(R.id.wheel_progress) as WheelProgressView
            return if (res != null && UiUtils.isVisible(res)) res else null
        }

    private fun updateWheel(wheel: WheelProgressView) {
        val progress = nativeGetOverallProgress(mMapsArray)
        if (progress == 0) wheel.isPending = true else {
            wheel.isPending = false
            wheel.progress = progress
        }
    }

    override fun bindGroup(view: View?) {
        val wheel =
            view!!.findViewById<View>(R.id.wheel_progress) as WheelProgressView
        UiUtils.show(wheel)
        updateWheel(wheel)
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (mCancelled) for (item in mMaps) nativeCancel(item)
        super.onDismiss(dialog)
    }

    override fun onStart() {
        super.onStart()
        isCancelable = false
        mSubscribeSlot = nativeSubscribe(object : StorageCallback {
            private fun update() {
                val wheel = wheel
                wheel?.let { updateWheel(it) }
            }

            override fun onStatusChanged(data: List<StorageCallbackData>) {
                for (item in data) if (mMaps.contains(item.countryId)) {
                    if (item.newStatus == CountryItem.STATUS_DONE) {
                        mMaps.remove(item.countryId)
                        if (mMaps.isEmpty()) {
                            mCancelled = false
                            RoutingController.Companion.get().checkAndBuildRoute()
                            dismissAllowingStateLoss()
                            return
                        }
                    }
                    update()
                    return
                }
            }

            override fun onProgress(
                countryId: String,
                localSize: Long,
                remoteSize: Long
            ) {
                if (mMaps.contains(countryId)) update()
            }

            init {
                update()
            }
        })
    }

    override fun onStop() {
        super.onStop()
        if (mSubscribeSlot != 0) {
            nativeUnsubscribe(mSubscribeSlot)
            mSubscribeSlot = 0
        }
    }

    companion object {
        fun create(missingMaps: Array<String>?): RoutingMapsDownloadFragment {
            val args = Bundle()
            args.putStringArray(
                EXTRA_MISSING_MAPS,
                missingMaps
            )
            val res = Fragment.instantiate(
                MwmApplication.get(),
                RoutingMapsDownloadFragment::class.java.name
            ) as RoutingMapsDownloadFragment
            res.arguments = args
            return res
        }
    }
}