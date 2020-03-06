package com.mapswithme.maps.routing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mapswithme.maps.Framework.RouterType
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmFragment
import com.mapswithme.maps.base.OnBackPressListener
import com.mapswithme.maps.routing.RoutingPlanInplaceController.RoutingPlanListener
import com.mapswithme.maps.taxi.TaxiInfo
import com.mapswithme.maps.taxi.TaxiManager

class RoutingPlanFragment : BaseMwmFragment(), OnBackPressListener {
    private var mPlanController: RoutingPlanController? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val res = inflater.inflate(R.layout.fragment_routing, container, false)
        var listener: RoutingBottomMenuListener? = null
        if (activity is RoutingBottomMenuListener) listener =
            activity as RoutingBottomMenuListener?
        val planListener = requireActivity() as RoutingPlanListener
        mPlanController = RoutingPlanController(res, activity, planListener, listener)
        return res
    }

    fun updateBuildProgress(progress: Int, @RouterType router: Int) {
        mPlanController!!.updateBuildProgress(progress, router)
    }

    fun showTaxiInfo(info: TaxiInfo) {
        mPlanController!!.showTaxiInfo(info)
    }

    fun showTaxiError(code: TaxiManager.ErrorCode) {
        mPlanController!!.showTaxiError(code)
    }

    override fun onBackPressed(): Boolean {
        return RoutingController.Companion.get().cancel()
    }

    fun restoreRoutingPanelState(state: Bundle) {
        mPlanController!!.restoreRoutingPanelState(state)
    }

    fun saveRoutingPanelState(outState: Bundle) {
        mPlanController!!.saveRoutingPanelState(outState)
    }

    fun showAddStartFrame() {
        mPlanController!!.showAddStartFrame()
    }

    fun showAddFinishFrame() {
        mPlanController!!.showAddFinishFrame()
    }

    fun hideActionFrame() {
        mPlanController!!.hideActionFrame()
    }
}