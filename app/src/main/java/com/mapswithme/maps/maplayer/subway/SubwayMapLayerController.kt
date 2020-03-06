package com.mapswithme.maps.maplayer.subway

import android.view.View
import android.view.ViewGroup.MarginLayoutParams

import com.mapswithme.maps.maplayer.MapLayerController
import com.mapswithme.util.UiUtils

class SubwayMapLayerController(private val mSubwayBtn: View) : MapLayerController {
    override fun turnOn() {
        mSubwayBtn.isSelected = true
    }

    override fun turnOff() {
        mSubwayBtn.isSelected = false
    }

    override fun show() {
        UiUtils.show(mSubwayBtn)
    }

    override fun showImmediately() {
        mSubwayBtn.visibility = View.VISIBLE
    }

    override fun hide() {
        UiUtils.hide(mSubwayBtn)
    }

    override fun hideImmediately() {
        mSubwayBtn.visibility = View.GONE
    }

    override fun adjust(offsetX: Int, offsetY: Int) {
        val params = mSubwayBtn.layoutParams as MarginLayoutParams
        params.setMargins(offsetX, offsetY, 0, 0)
        mSubwayBtn.layoutParams = params
    }

    override fun attachCore() { /* Do nothing by default */
    }

    override fun detachCore() { /* Do nothing by default */
    }

    init {
        UiUtils.addStatusBarOffset(mSubwayBtn)
    }
}