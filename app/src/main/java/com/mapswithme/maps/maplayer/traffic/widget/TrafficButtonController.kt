package com.mapswithme.maps.maplayer.traffic.widget

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.widget.Toast
import com.mapswithme.maps.R

import com.mapswithme.maps.maplayer.MapLayerController
import com.mapswithme.maps.maplayer.traffic.TrafficManager
import com.mapswithme.maps.maplayer.traffic.TrafficManager.TrafficCallback

class TrafficButtonController(
    private val mButton: TrafficButton,
    private val mActivity: Activity
) : TrafficCallback, MapLayerController {
    private var mDialog: Dialog? = null
    override fun onEnabled() {
        turnOn()
    }

    override fun turnOn() {
        mButton.turnOn()
    }

    override fun hideImmediately() {
        mButton.hideImmediately()
    }

    override fun adjust(offsetX: Int, offsetY: Int) {
        mButton.setOffset(offsetX, offsetY)
    }

    override fun attachCore() {
        TrafficManager.INSTANCE.attach(this)
    }

    override fun detachCore() {
        destroy()
    }

    override fun onDisabled() {
        turnOff()
    }

    override fun turnOff() {
        mButton.turnOff()
    }

    override fun show() {
        mButton.show()
    }

    override fun showImmediately() {
        mButton.showImmediately()
    }

    override fun hide() {
        mButton.hide()
    }

    override fun onWaitingData() {
        mButton.startWaitingAnimation()
    }

    override fun onOutdated() {
        mButton.markAsOutdated()
    }

    override fun onNoData() {
        turnOn()
        Toast.makeText(mActivity, R.string.traffic_data_unavailable, Toast.LENGTH_SHORT).show()
    }

    override fun onNetworkError() {
        if (mDialog != null && mDialog!!.isShowing) return
        val builder = AlertDialog.Builder(mActivity)
            .setMessage(R.string.common_check_internet_connection_dialog)
            .setPositiveButton(android.R.string.ok) { dialog, which ->
                TrafficManager.INSTANCE.isEnabled = false
            }
            .setCancelable(true)
            .setOnCancelListener { TrafficManager.INSTANCE.isEnabled = false }
        mDialog = builder.show()
    }

    fun destroy() {
        if (mDialog != null && mDialog!!.isShowing) mDialog!!.cancel()
    }

    override fun onExpiredData() {
        turnOn()
        Toast.makeText(mActivity, R.string.traffic_update_maps_text, Toast.LENGTH_SHORT).show()
    }

    override fun onExpiredApp() {
        turnOn()
        Toast.makeText(mActivity, R.string.traffic_update_app, Toast.LENGTH_SHORT).show()
    }

}