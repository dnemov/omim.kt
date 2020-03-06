package com.mapswithme.maps.maplayer

import android.app.Activity
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.mapswithme.maps.maplayer.subway.SubwayMapLayerController
import com.mapswithme.maps.maplayer.traffic.widget.TrafficButton
import com.mapswithme.maps.maplayer.traffic.widget.TrafficButtonController
import com.mapswithme.maps.tips.Tutorial
import com.mapswithme.maps.tips.TutorialClickListener
import com.mapswithme.util.InputUtils
import java.util.*

class MapLayerCompositeController(
    traffic: TrafficButton, subway: View,
    activity: AppCompatActivity
) : MapLayerController {
    private val mActivity: AppCompatActivity
    private val mChildrenEntries: Collection<ControllerAndMode>
    private var mMasterEntry: ControllerAndMode
    fun toggleMode(mode: Mode) {
        toggleMode(mode, false)
    }

    private fun toggleMode(
        mode: Mode,
        animate: Boolean
    ) {
        setMasterController(mode)
        showMasterController(animate)
        val enabled = mode.isEnabled(mActivity)
        if (enabled) {
            turnOn()
        } else {
            turnOff()
            turnInitialMode()
        }
    }

    private fun turnInitialMode() {
        mMasterEntry.controller.hideImmediately()
        mMasterEntry = mChildrenEntries.iterator().next()
        mMasterEntry.controller.showImmediately()
    }

    fun applyLastActiveMode() {
        toggleMode(mMasterEntry.mode, true)
    }

    override fun attachCore() {
        for (each in mChildrenEntries) {
            each.controller.attachCore()
        }
    }

    override fun detachCore() {
        for (each in mChildrenEntries) {
            each.controller.detachCore()
        }
    }

    private fun setMasterController(mode: Mode) {
        for (each in mChildrenEntries) {
            if (each.mode === mode) {
                mMasterEntry = each
            } else {
                each.controller.hideImmediately()
                each.mode.setEnabled(mActivity, false)
            }
        }
    }

    private fun showMasterController(animate: Boolean) {
        if (animate) mMasterEntry.controller.show() else mMasterEntry.controller.showImmediately()
    }

    private val currentLayer: ControllerAndMode
        private get() {
            for (each in mChildrenEntries) {
                if (each.mode.isEnabled(mActivity)) return each
            }
            return mChildrenEntries.iterator().next()
        }

    override fun turnOn() {
        mMasterEntry.controller.turnOn()
        mMasterEntry.mode.setEnabled(mActivity, true)
    }

    override fun turnOff() {
        mMasterEntry.controller.turnOff()
        mMasterEntry.mode.setEnabled(mActivity, false)
    }

    override fun show() {
        mMasterEntry.controller.show()
    }

    override fun showImmediately() {
        mMasterEntry.controller.showImmediately()
    }

    override fun hide() {
        mMasterEntry.controller.hide()
    }

    override fun hideImmediately() {
        mMasterEntry.controller.hideImmediately()
    }

    override fun adjust(offsetX: Int, offsetY: Int) {
        for (controllerAndMode in mChildrenEntries) controllerAndMode.controller.adjust(
            offsetX,
            offsetY
        )
    }

    private fun showDialog() {
        ToggleMapLayerDialog.Companion.show(mActivity)
    }

    fun turnOn(mode: Mode) {
        val entry = findModeMapLayerController(mode)
        entry.mode.setEnabled(mActivity, true)
        entry.controller.turnOn()
        entry.controller.showImmediately()
    }

    fun turnOff(mode: Mode) {
        val entry = findModeMapLayerController(mode)
        entry.mode.setEnabled(mActivity, false)
        entry.controller.turnOff()
        entry.controller.hideImmediately()
        turnInitialMode()
    }

    private fun findModeMapLayerController(mode: Mode): ControllerAndMode {
        for (each in mChildrenEntries) {
            if (each.mode === mode) return each
        }
        throw IllegalArgumentException("Mode not found : $mode")
    }

    private class ControllerAndMode internal constructor(
        val mode: Mode,
        val controller: MapLayerController
    ) {
        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false
            val that = o as ControllerAndMode
            return mode === that.mode
        }

        override fun hashCode(): Int {
            return mode.hashCode()
        }

    }

    private inner class OpenBottomDialogClickListener internal constructor(
        activity: Activity,
        tutorial: Tutorial
    ) : TutorialClickListener(activity, tutorial) {
        override fun onProcessClick(view: View) {
            if (mMasterEntry.mode.isEnabled(mActivity)) {
                turnOff()
                toggleMode(currentLayer.mode)
            } else {
                InputUtils.hideKeyboard(mActivity.window.decorView)
                showDialog()
            }
        }
    }

    companion object {
        private fun createEntries(
            traffic: TrafficButton,
            subway: View,
            activity: AppCompatActivity,
            dialogClickListener: View.OnClickListener
        ): Collection<ControllerAndMode> {
            traffic.setOnclickListener(dialogClickListener)
            val trafficButtonController = TrafficButtonController(
                traffic,
                activity
            )
            subway.setOnClickListener(dialogClickListener)
            val subwayMapLayerController =
                SubwayMapLayerController(subway)
            val subwayEntry = ControllerAndMode(
                Mode.SUBWAY,
                subwayMapLayerController
            )
            val trafficEntry = ControllerAndMode(
                Mode.TRAFFIC,
                trafficButtonController
            )
            val entries: MutableSet<ControllerAndMode> =
                LinkedHashSet()
            entries.add(subwayEntry)
            entries.add(trafficEntry)
            return Collections.unmodifiableSet(entries)
        }
    }

    init {
        val listener: View.OnClickListener =
            OpenBottomDialogClickListener(activity, Tutorial.MAP_LAYERS)
        mActivity = activity
        mChildrenEntries =
            createEntries(traffic, subway, activity, listener)
        mMasterEntry = currentLayer
        toggleMode(mMasterEntry.mode)
    }
}