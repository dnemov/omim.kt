package com.mapswithme.maps.settings

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Switch
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmToolbarFragment
import com.mapswithme.maps.routing.RoutingOptions.activeRoadTypes
import com.mapswithme.maps.routing.RoutingOptions.addOption
import com.mapswithme.maps.routing.RoutingOptions.hasOption
import com.mapswithme.maps.routing.RoutingOptions.removeOption
import com.mapswithme.util.statistics.Statistics
import java.util.*

class DrivingOptionsFragment : BaseMwmToolbarFragment() {
    private var mRoadTypes: Set<RoadType> = emptySet()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root =
            inflater.inflate(R.layout.fragment_driving_options, container, false)
        val componentDescent =
            if (arguments == null) Statistics.EventParam.ROUTE else arguments!!.getString(
                Statistics.EventParam.FROM,
                Statistics.EventParam.ROUTE
            )
        initViews(root, componentDescent)
        mRoadTypes =
            if (savedInstanceState != null && savedInstanceState.containsKey(BUNDLE_ROAD_TYPES)) makeRouteTypes(
                savedInstanceState
            ) else activeRoadTypes
        return root
    }

    private fun makeRouteTypes(bundle: Bundle): Set<RoadType> {
        val result: MutableSet<RoadType> = HashSet()
        val items: List<Int> = bundle.getIntegerArrayList(BUNDLE_ROAD_TYPES)!!
        for (each in items) {
            result.add(RoadType.values()[each])
        }
        return result
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val savedRoadTypes = ArrayList<Int>()
        for (each in mRoadTypes) {
            savedRoadTypes.add(each.ordinal)
        }
        outState.putIntegerArrayList(
            BUNDLE_ROAD_TYPES,
            savedRoadTypes
        )
    }

    private fun areSettingsNotChanged(): Boolean {
        val lastActiveRoadTypes = activeRoadTypes
        return mRoadTypes == lastActiveRoadTypes
    }

    override fun onBackPressed(): Boolean {
        requireActivity().setResult(if (areSettingsNotChanged()) Activity.RESULT_CANCELED else Activity.RESULT_OK)
        return super.onBackPressed()
    }

    private fun initViews(root: View, componentDescent: String) {
        val tollsBtn =
            root.findViewById<Switch>(R.id.avoid_tolls_btn)
        tollsBtn.isChecked = hasOption(RoadType.Toll)
        val tollBtnListener: CompoundButton.OnCheckedChangeListener =
            ToggleRoutingOptionListener(RoadType.Toll, componentDescent)
        tollsBtn.setOnCheckedChangeListener(tollBtnListener)
        val motorwaysBtn =
            root.findViewById<Switch>(R.id.avoid_motorways_btn)
        motorwaysBtn.isChecked = hasOption(RoadType.Motorway)
        val motorwayBtnListener: CompoundButton.OnCheckedChangeListener =
            ToggleRoutingOptionListener(RoadType.Motorway, componentDescent)
        motorwaysBtn.setOnCheckedChangeListener(motorwayBtnListener)
        val ferriesBtn =
            root.findViewById<Switch>(R.id.avoid_ferries_btn)
        ferriesBtn.isChecked = hasOption(RoadType.Ferry)
        val ferryBtnListener: CompoundButton.OnCheckedChangeListener =
            ToggleRoutingOptionListener(RoadType.Ferry, componentDescent)
        ferriesBtn.setOnCheckedChangeListener(ferryBtnListener)
        val dirtyRoadsBtn =
            root.findViewById<Switch>(R.id.avoid_dirty_roads_btn)
        dirtyRoadsBtn.isChecked = hasOption(RoadType.Dirty)
        val dirtyBtnListener: CompoundButton.OnCheckedChangeListener =
            ToggleRoutingOptionListener(RoadType.Dirty, componentDescent)
        dirtyRoadsBtn.setOnCheckedChangeListener(dirtyBtnListener)
    }

    private class ToggleRoutingOptionListener(
        private val mRoadType: RoadType,
        private val mComponentDescent: String
    ) : CompoundButton.OnCheckedChangeListener {
        override fun onCheckedChanged(
            buttonView: CompoundButton,
            isChecked: Boolean
        ) {
            if (isChecked) addOption(mRoadType) else removeOption(
                mRoadType
            )
            Statistics.INSTANCE.trackSettingsDrivingOptionsChangeEvent(
                mComponentDescent
            )
        }

    }

    companion object {
        const val BUNDLE_ROAD_TYPES = "road_types"
    }
}