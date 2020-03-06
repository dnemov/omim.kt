package com.mapswithme.maps.widget.placepage

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import com.mapswithme.maps.Framework
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmDialogFragment
import com.mapswithme.maps.bookmarks.data.MapObject
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.location.LocationListener
import com.mapswithme.maps.widget.ArrowView
import com.mapswithme.util.LocationUtils
import com.mapswithme.util.UiUtils
import com.mapswithme.util.statistics.AlohaHelper
import com.mapswithme.util.statistics.Statistics
import com.mapswithme.util.statistics.Statistics.EventName

class DirectionFragment : BaseMwmDialogFragment(),
    LocationListener {
    private var mAvDirection: ArrowView? = null
    private var mTvTitle: TextView? = null
    private var mTvSubtitle: TextView? = null
    private var mTvDistance: TextView? = null
    private var mMapObject: MapObject? = null
    override val customTheme: Int
        protected get() = R.style.MwmTheme_DialogFragment_Fullscreen_Translucent

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root =
            inflater.inflate(R.layout.fragment_direction, container, false)
        root.setOnTouchListener { v, event ->
            dismiss()
            Statistics.INSTANCE.trackEvent(EventName.PP_DIRECTION_ARROW_CLOSE)
            AlohaHelper.logClick(AlohaHelper.PP_DIRECTION_ARROW_CLOSE)
            false
        }
        initViews(root)
        if (savedInstanceState != null) setMapObject(
            savedInstanceState.getParcelable(
                EXTRA_MAP_OBJECT
            )
        )
        return root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(EXTRA_MAP_OBJECT, mMapObject)
        super.onSaveInstanceState(outState)
    }

    private fun initViews(root: View) {
        mAvDirection = root.findViewById<View>(R.id.av__direction) as ArrowView
        mTvTitle = root.findViewById<View>(R.id.tv__title) as TextView
        mTvSubtitle = root.findViewById<View>(R.id.tv__subtitle) as TextView
        mTvDistance = root.findViewById<View>(R.id.tv__straight_distance) as TextView
        UiUtils.waitLayout(mTvTitle!!, ViewTreeObserver.OnGlobalLayoutListener {
            val height = mTvTitle!!.height
            val lineHeight = mTvTitle!!.lineHeight
            mTvTitle!!.maxLines = height / lineHeight
        })
    }

    fun setMapObject(`object`: MapObject?) {
        mMapObject = `object`
        refreshViews()
    }

    private fun refreshViews() {
        if (mMapObject != null && isResumed) {
            mTvTitle!!.text = mMapObject!!.title
            mTvSubtitle!!.text = mMapObject!!.subtitle
        }
    }

    override fun onResume() {
        super.onResume()
        LocationHelper.INSTANCE.addListener(this, true)
        refreshViews()
    }

    override fun onPause() {
        super.onPause()
        LocationHelper.INSTANCE.removeListener(this)
    }

    override fun onLocationUpdated(location: Location) {
        if (mMapObject != null) {
            val distanceAndAzimuth =
                Framework.nativeGetDistanceAndAzimuthFromLatLon(
                    mMapObject!!.lat, mMapObject!!.lon,
                    location.latitude, location.longitude, 0.0
                )
            mTvDistance!!.text = distanceAndAzimuth?.distance
        }
    }

    override fun onCompassUpdated(
        time: Long,
        magneticNorth: Double,
        trueNorth: Double,
        accuracy: Double
    ) {
        var magneticNorth = magneticNorth
        var trueNorth = trueNorth
        val last = LocationHelper.INSTANCE.savedLocation
        if (last == null || mMapObject == null) return
        val rotation = activity!!.windowManager.defaultDisplay.rotation
        magneticNorth = LocationUtils.correctCompassAngle(rotation, magneticNorth)
        trueNorth = LocationUtils.correctCompassAngle(rotation, trueNorth)
        val north = if (trueNorth >= 0.0) trueNorth else magneticNorth
        val da = Framework.nativeGetDistanceAndAzimuthFromLatLon(
            mMapObject!!.lat, mMapObject!!.lon,
            last.latitude, last.longitude, north
        )
        if (da != null && da.azimuth >= 0) mAvDirection!!.setAzimuth(da.azimuth)
    }

    override fun onLocationError(errorCode: Int) {}

    companion object {
        private const val EXTRA_MAP_OBJECT = "MapObject"
    }
}