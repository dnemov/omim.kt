package com.mapswithme.maps.routing

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.mapswithme.maps.Framework
import com.mapswithme.maps.Framework.RouterType
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.maps.routing.RoutingBottomMenuController.Companion.newInstance
import com.mapswithme.maps.routing.RoutingController.BuildState
import com.mapswithme.maps.routing.RoutingPlanInplaceController.RoutingPlanListener
import com.mapswithme.maps.settings.DrivingOptionsActivity
import com.mapswithme.maps.taxi.TaxiInfo
import com.mapswithme.maps.taxi.TaxiManager
import com.mapswithme.maps.widget.RoutingToolbarButton
import com.mapswithme.maps.widget.ToolbarController
import com.mapswithme.maps.widget.WheelProgressView
import com.mapswithme.util.UiUtils
import com.mapswithme.util.statistics.AlohaHelper
import com.mapswithme.util.statistics.Statistics
import com.mapswithme.util.statistics.Statistics.EventName

open class RoutingPlanController internal constructor(
    protected val frame: View, activity: Activity?,
    private val mRoutingPlanListener: RoutingPlanListener,
    listener: RoutingBottomMenuListener?
) : ToolbarController(frame, activity!!) {
    private val mRouterTypes: RadioGroup
    private val mProgressVehicle: WheelProgressView
    private val mProgressPedestrian: WheelProgressView
    private val mProgressTransit: WheelProgressView
    private val mProgressBicycle: WheelProgressView
    private val mProgressTaxi: WheelProgressView
    private val mRoutingBottomMenuController: RoutingBottomMenuController
    var mFrameHeight = 0
    private val drivingOptionsBtnContainer: View
    private val mDriverOptionsLayoutListener: View.OnLayoutChangeListener
    private val mDrivingOptionsImage: View
    private fun setupRouterButton(@IdRes buttonId: Int, @DrawableRes iconRes: Int, clickListener: View.OnClickListener): RadioButton {
        val listener =
            CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
                val button = buttonView as RoutingToolbarButton
                button.setIcon(iconRes)
                if (isChecked) button.activate() else button.deactivate()
            }
        val rb =
            mRouterTypes.findViewById<View>(buttonId) as RoutingToolbarButton
        listener.onCheckedChanged(rb, false)
        rb.setOnCheckedChangeListener(listener)
        rb.setOnClickListener(clickListener)
        return rb
    }

    private fun setupRouterButtons() {
        setupRouterButton(
            R.id.vehicle,
            R.drawable.ic_car,
            View.OnClickListener { v: View -> onVehicleModeSelected(v) }
        )
        setupRouterButton(
            R.id.pedestrian,
            R.drawable.ic_pedestrian,
            View.OnClickListener { v: View ->
                onPedestrianModeSelected(v)
            }
        )
        setupRouterButton(
            R.id.bicycle,
            R.drawable.ic_bike,
            View.OnClickListener { v: View -> onBicycleModeSelected(v) }
        )
        setupRouterButton(
            R.id.taxi,
            R.drawable.ic_taxi,
            View.OnClickListener { v: View -> onTaxiModeSelected(v) }
        )
        setupRouterButton(
            R.id.transit,
            R.drawable.ic_transit,
            View.OnClickListener { v: View? -> onTransitModeSelected() }
        )
    }

    private fun onTransitModeSelected() {
        AlohaHelper.logClick(AlohaHelper.ROUTING_TRANSIT_SET)
        Statistics.INSTANCE.trackEvent(EventName.ROUTING_TRANSIT_SET)
        RoutingController.Companion.get().setRouterType(Framework.ROUTER_TYPE_TRANSIT)
    }

    private fun onTaxiModeSelected(v: View) {
        AlohaHelper.logClick(AlohaHelper.ROUTING_TAXI_SET)
        Statistics.INSTANCE.trackEvent(EventName.ROUTING_TAXI_SET)
        RoutingController.Companion.get().setRouterType(Framework.ROUTER_TYPE_TAXI)
    }

    private fun onBicycleModeSelected(v: View) {
        AlohaHelper.logClick(AlohaHelper.ROUTING_BICYCLE_SET)
        Statistics.INSTANCE.trackEvent(EventName.ROUTING_BICYCLE_SET)
        RoutingController.Companion.get().setRouterType(Framework.ROUTER_TYPE_BICYCLE)
    }

    private fun onPedestrianModeSelected(v: View) {
        AlohaHelper.logClick(AlohaHelper.ROUTING_PEDESTRIAN_SET)
        Statistics.INSTANCE.trackEvent(EventName.ROUTING_PEDESTRIAN_SET)
        RoutingController.Companion.get().setRouterType(Framework.ROUTER_TYPE_PEDESTRIAN)
    }

    private fun onVehicleModeSelected(v: View) {
        AlohaHelper.logClick(AlohaHelper.ROUTING_VEHICLE_SET)
        Statistics.INSTANCE.trackEvent(EventName.ROUTING_VEHICLE_SET)
        RoutingController.Companion.get().setRouterType(Framework.ROUTER_TYPE_VEHICLE)
    }

    override fun onUpClick() {
        AlohaHelper.logClick(AlohaHelper.ROUTING_CANCEL)
        Statistics.INSTANCE.trackEvent(EventName.ROUTING_CANCEL)
        RoutingController.Companion.get().cancel()
    }

    fun checkFrameHeight(): Boolean {
        if (mFrameHeight > 0) return true
        mFrameHeight = frame.height
        return mFrameHeight > 0
    }

    private fun updateProgressLabels() {
        val buildState: BuildState = RoutingController.Companion.get().buildState
        val ready = buildState == BuildState.BUILT
        if (!ready) {
            mRoutingBottomMenuController.hideAltitudeChartAndRoutingDetails()
            return
        }
        if (isTransitType) {
            val info: TransitRouteInfo? = RoutingController.get().cachedTransitInfo
            if (info != null) mRoutingBottomMenuController.showTransitInfo(info)
            return
        }
        if (!isTaxiRouterType) {
            mRoutingBottomMenuController.setStartButton()
            mRoutingBottomMenuController.showAltitudeChartAndRoutingDetails()
        }
    }

    fun updateBuildProgress(progress: Int, @RouterType router: Int) {
        UiUtils.invisible(
            mProgressVehicle, mProgressPedestrian, mProgressTransit,
            mProgressBicycle, mProgressTaxi
        )
        val progressView: WheelProgressView
        progressView = if (router == Framework.ROUTER_TYPE_VEHICLE) {
            mRouterTypes.check(R.id.vehicle)
            mProgressVehicle
        } else if (router == Framework.ROUTER_TYPE_PEDESTRIAN) {
            mRouterTypes.check(R.id.pedestrian)
            mProgressPedestrian
        } else if (router == Framework.ROUTER_TYPE_TAXI) {
            mRouterTypes.check(R.id.taxi)
            mProgressTaxi
        } else if (router == Framework.ROUTER_TYPE_TRANSIT) {
            mRouterTypes.check(R.id.transit)
            mProgressTransit
        } else {
            mRouterTypes.check(R.id.bicycle)
            mProgressBicycle
        }
        val button = mRouterTypes
            .findViewById<View>(mRouterTypes.checkedRadioButtonId) as RoutingToolbarButton
        button.progress()
        updateProgressLabels()
        if (RoutingController.Companion.get().isTaxiRequestHandled) {
            if (!RoutingController.Companion.get().isInternetConnected) {
                showNoInternetError()
                return
            }
            button.complete()
            return
        }
        if (!RoutingController.Companion.get().isBuilding && !RoutingController.Companion.get().isTaxiPlanning) {
            button.complete()
            return
        }
        UiUtils.show(progressView)
        progressView.isPending = progress == 0
        if (progress != 0) progressView.progress = progress
    }

    private val isTaxiRouterType: Boolean
        private get() = RoutingController.Companion.get().isTaxiRouterType

    private val isTransitType: Boolean
        private get() = RoutingController.Companion.get().isTransitType

    fun showTaxiInfo(info: TaxiInfo) {
        mRoutingBottomMenuController.showTaxiInfo(info)
    }

    fun showTaxiError(code: TaxiManager.ErrorCode) {
        when (code) {
            TaxiManager.ErrorCode.NoProducts -> showError(R.string.taxi_not_found)
            TaxiManager.ErrorCode.RemoteError -> showError(R.string.dialog_taxi_error)
            TaxiManager.ErrorCode.NoProviders -> showError(R.string.taxi_no_providers)
            else -> throw AssertionError("Unsupported uber error: $code")
        }
    }

    private fun showNoInternetError() {
        @IdRes val checkedId = mRouterTypes.checkedRadioButtonId
        val rb =
            mRouterTypes.findViewById<View>(checkedId) as RoutingToolbarButton
        rb.error()
        showError(R.string.dialog_taxi_offline)
    }

    private fun showError(@StringRes message: Int) {
        mRoutingBottomMenuController.showError(message)
    }

    fun showStartButton(show: Boolean) {
        mRoutingBottomMenuController.showStartButton(show)
    }

    fun saveRoutingPanelState(outState: Bundle) {
        mRoutingBottomMenuController.saveRoutingPanelState(outState)
        outState.putBoolean(
            BUNDLE_HAS_DRIVING_OPTIONS_VIEW,
            UiUtils.isVisible(drivingOptionsBtnContainer)
        )
    }

    fun restoreRoutingPanelState(state: Bundle) {
        mRoutingBottomMenuController.restoreRoutingPanelState(state)
        val hasView =
            state.getBoolean(BUNDLE_HAS_DRIVING_OPTIONS_VIEW)
        if (hasView) showDrivingOptionView()
    }

    fun showAddStartFrame() {
        mRoutingBottomMenuController.showAddStartFrame()
    }

    fun showAddFinishFrame() {
        mRoutingBottomMenuController.showAddFinishFrame()
    }

    fun hideActionFrame() {
        mRoutingBottomMenuController.hideActionFrame()
    }

    fun showDrivingOptionView() {
        drivingOptionsBtnContainer.addOnLayoutChangeListener(mDriverOptionsLayoutListener)
        UiUtils.show(drivingOptionsBtnContainer)
        val hasAnyOptions = RoutingOptions.hasAnyOptions()
        UiUtils.showIf(hasAnyOptions, mDrivingOptionsImage)
        val title =
            drivingOptionsBtnContainer.findViewById<TextView>(R.id.driving_options_btn_title)
        title.setText(if (hasAnyOptions) R.string.change_driving_options_btn else R.string.define_to_avoid_btn)
    }

    fun hideDrivingOptionsView() {
        UiUtils.hide(drivingOptionsBtnContainer)
        mRoutingPlanListener.onRoutingPlanStartAnimate(UiUtils.isVisible(frame))
    }

    fun calcHeight(): Int {
        val frameHeight = frame.height
        if (frameHeight == 0) return 0
        val driverOptionsView = drivingOptionsBtnContainer
        val extraOppositeOffset =
            if (UiUtils.isVisible(driverOptionsView)) 0 else driverOptionsView.height
        return frameHeight - extraOppositeOffset
    }

    private inner class SelfTerminatedDrivingOptionsLayoutListener :
        View.OnLayoutChangeListener {
        override fun onLayoutChange(
            v: View, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int,
            oldTop: Int, oldRight: Int, oldBottom: Int
        ) {
            mRoutingPlanListener.onRoutingPlanStartAnimate(UiUtils.isVisible(frame))
            drivingOptionsBtnContainer.removeOnLayoutChangeListener(this)
        }
    }

    companion object {
        val ANIM_TOGGLE =
            MwmApplication.get().resources.getInteger(R.integer.anim_default)
        private const val BUNDLE_HAS_DRIVING_OPTIONS_VIEW = "has_driving_options_view"
    }

    init {
        mRouterTypes = toolbar.findViewById<View>(R.id.route_type) as RadioGroup
        setupRouterButtons()
        val progressFrame =
            toolbar.findViewById<View>(R.id.progress_frame)
        mProgressVehicle =
            progressFrame.findViewById<View>(R.id.progress_vehicle) as WheelProgressView
        mProgressPedestrian =
            progressFrame.findViewById<View>(R.id.progress_pedestrian) as WheelProgressView
        mProgressTransit =
            progressFrame.findViewById<View>(R.id.progress_transit) as WheelProgressView
        mProgressBicycle =
            progressFrame.findViewById<View>(R.id.progress_bicycle) as WheelProgressView
        mProgressTaxi =
            progressFrame.findViewById<View>(R.id.progress_taxi) as WheelProgressView
        mRoutingBottomMenuController =
            newInstance(activity!!, frame, listener)
        drivingOptionsBtnContainer =
            frame.findViewById(R.id.driving_options_btn_container)
        val btn =
            drivingOptionsBtnContainer.findViewById<View>(R.id.driving_options_btn)
        mDrivingOptionsImage = frame.findViewById(R.id.driving_options_btn_img)
        btn.setOnClickListener { v: View? ->
            DrivingOptionsActivity.start(
                activity
            )
        }
        mDriverOptionsLayoutListener = SelfTerminatedDrivingOptionsLayoutListener()
    }
}