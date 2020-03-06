package com.mapswithme.maps.routing

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.TextView
import com.mapswithme.maps.Framework
import com.mapswithme.maps.MwmActivity
import com.mapswithme.maps.R
import com.mapswithme.maps.base.MediaPlayerWrapper.Companion.from
import com.mapswithme.maps.bookmarks.BookmarkCategoriesActivity.Companion.start
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.maplayer.traffic.TrafficManager
import com.mapswithme.maps.maplayer.traffic.TrafficManager.TrafficCallback
import com.mapswithme.maps.settings.SettingsActivity
import com.mapswithme.maps.sound.TtsPlayer
import com.mapswithme.maps.widget.FlatProgressView
import com.mapswithme.maps.widget.menu.BaseMenu
import com.mapswithme.maps.widget.menu.BaseMenu.ItemClickListener
import com.mapswithme.maps.widget.menu.NavMenu
import com.mapswithme.util.Graphics
import com.mapswithme.util.StringUtils
import com.mapswithme.util.UiUtils
import com.mapswithme.util.Utils
import com.mapswithme.util.statistics.Statistics
import com.mapswithme.util.statistics.Statistics.EventName
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NavigationController(activity: Activity) : TrafficCallback,
    View.OnClickListener {
    private val mFrame: View
    private val mBottomFrame: View
    private val mSearchButtonFrame: View
    val navMenu: NavMenu
    private val mNextTurnImage: ImageView
    private val mNextTurnDistance: TextView
    private val mCircleExit: TextView
    private val mNextNextTurnFrame: View
    private val mNextNextTurnImage: ImageView
    private val mStreetFrame: View
    private val mNextStreet: TextView
    private val mSpeedValue: TextView
    private val mSpeedUnits: TextView
    private val mTimeHourValue: TextView
    private val mTimeHourUnits: TextView
    private val mTimeMinuteValue: TextView
    private val mTimeMinuteUnits: TextView
    private val mDotTimeLeft: ImageView
    private val mDotTimeArrival: ImageView
    private val mDistanceValue: TextView
    private val mDistanceUnits: TextView
    private val mRouteProgress: FlatProgressView
    private val mSearchWheel: SearchWheel
    private val mSpeedViewContainer: View
    private val mOnboardingBtn: View
    private var mShowTimeLeft = true
    private var mNorth = 0.0
    private val mSpeedCamSignalCompletionListener: OnCompletionListener
    fun onResume() {
        navMenu.onResume(null)
        mSearchWheel.onResume()
    }

    fun performSearchClick(): Boolean {
        return mSearchWheel.performClick()
    }

    private fun createNavMenu(): NavMenu {
        return NavMenu(
            mBottomFrame,
            object : ItemClickListener<BaseMenu.Item> {
                override fun onItemClick(item: BaseMenu.Item) {
                    onMenuItemClicked(item)
                }
            }
        )
    }

    private fun onMenuItemClicked(item: BaseMenu.Item) {
        val parent = mFrame.context as MwmActivity
        when (item) {
            NavMenu.Item.STOP -> {
                navMenu.close(false)
                Statistics.INSTANCE.trackRoutingFinish(
                    true,
                    RoutingController.get().lastRouterType,
                    TrafficManager.INSTANCE.isEnabled
                )
                RoutingController.get().cancel()
            }
            NavMenu.Item.SETTINGS -> {
                Statistics.INSTANCE.trackEvent(EventName.ROUTING_SETTINGS)
                parent.closeMenu(Runnable {
                    parent.startActivity(
                        Intent(
                            parent,
                            SettingsActivity::class.java
                        )
                    )
                })
            }
            NavMenu.Item.TTS_VOLUME -> {
                TtsPlayer.isEnabled = !TtsPlayer.isEnabled
                navMenu.refreshTts()
            }
            NavMenu.Item.TRAFFIC -> {
                TrafficManager.INSTANCE.toggle()
                parent.onTrafficLayerSelected()
                navMenu.refreshTraffic()
            }
            NavMenu.Item.TOGGLE -> {
                navMenu.toggle(true)
                parent.refreshFade()
            }
        }
    }

    fun stop(parent: MwmActivity) {
        parent.refreshFade()
        mSearchWheel.reset()
    }

    private fun updateVehicle(info: RoutingInfo) {
        mNextTurnDistance.text = Utils.formatUnitsText(
            mFrame.context,
            R.dimen.text_size_nav_number,
            R.dimen.text_size_nav_dimension,
            info.distToTurn,
            info.turnUnits
        )
        info.carDirection.setTurnDrawable(mNextTurnImage)
        if (RoutingInfo.CarDirection.isRoundAbout(info.carDirection)) UiUtils.setTextAndShow(
            mCircleExit,
            info.exitNum.toString()
        ) else UiUtils.hide(mCircleExit)
        UiUtils.showIf(info.nextCarDirection.containsNextTurn(), mNextNextTurnFrame)
        if (info.nextCarDirection.containsNextTurn()) info.nextCarDirection.setNextTurnDrawable(
            mNextNextTurnImage
        )
    }

    private fun updatePedestrian(info: RoutingInfo) {
        val next = info.pedestrianNextDirection
        val location = LocationHelper.INSTANCE.savedLocation
        val da = Framework.nativeGetDistanceAndAzimuthFromLatLon(
            next.latitude, next.longitude,
            location!!.latitude, location.longitude,
            mNorth
        )
        val splitDistance = da?.distance?.split(" ")?.toTypedArray()
        mNextTurnDistance.text = Utils.formatUnitsText(
            mFrame.context,
            R.dimen.text_size_nav_number,
            R.dimen.text_size_nav_dimension,
            splitDistance!!.get(0),
            splitDistance[1]
        )
        if (info.pedestrianTurnDirection != null) RoutingInfo.PedestrianTurnDirection.setTurnDrawable(
            mNextTurnImage,
            da!!
        )
    }

    fun updateNorth(north: Double) {
        if (!RoutingController.get().isNavigating) return
        mNorth = north
        update(Framework.nativeGetRouteFollowingInfo())
    }

    fun update(info: RoutingInfo?) {
        if (info == null) return
        if (Framework.nativeGetRouter() == Framework.ROUTER_TYPE_PEDESTRIAN) updatePedestrian(info) else updateVehicle(
            info
        )
        updateStreetView(info)
        updateSpeedView(info)
        updateTime(info.totalTimeInSeconds)
        mDistanceValue.text = info.distToTarget
        mDistanceUnits.text = info.targetUnits
        mRouteProgress.progress = info.completionPercent.toInt()
        playbackSpeedCamWarning(info)
    }

    private fun updateStreetView(info: RoutingInfo) {
        val hasStreet = !TextUtils.isEmpty(info.nextStreet)
        UiUtils.showIf(hasStreet, mStreetFrame)
        if (!TextUtils.isEmpty(info.nextStreet)) mNextStreet.text = info.nextStreet
    }

    private fun updateSpeedView(info: RoutingInfo) {
        val last = LocationHelper.INSTANCE.lastKnownLocation ?: return
        val speedAndUnits =
            StringUtils.nativeFormatSpeedAndUnits(last.speed.toDouble())
        mSpeedUnits.text = speedAndUnits?.second
        mSpeedValue.text = speedAndUnits?.first
        mSpeedViewContainer.isActivated = info.isSpeedLimitExceeded
    }

    private fun playbackSpeedCamWarning(info: RoutingInfo) {
        if (!info.shouldPlayWarningSignal() || TtsPlayer.INSTANCE.isSpeaking) return
        val context = mBottomFrame.context
        val player = from(context)
        player.playback(R.raw.speed_cams_beep, mSpeedCamSignalCompletionListener)
    }

    private fun updateTime(seconds: Int) {
        if (mShowTimeLeft) updateTimeLeft(seconds) else updateTimeEstimate(seconds)
        mDotTimeLeft.isEnabled = mShowTimeLeft
        mDotTimeArrival.isEnabled = !mShowTimeLeft
    }

    private fun updateTimeLeft(seconds: Int) {
        val hours = TimeUnit.SECONDS.toHours(seconds.toLong())
        val minutes =
            TimeUnit.SECONDS.toMinutes(seconds.toLong()) % 60
        UiUtils.setTextAndShow(mTimeMinuteValue, minutes.toString())
        val min = mFrame.resources.getString(R.string.minute)
        UiUtils.setTextAndShow(mTimeMinuteUnits, min)
        if (hours == 0L) {
            UiUtils.hide(mTimeHourUnits, mTimeHourValue)
            return
        }
        UiUtils.setTextAndShow(mTimeHourValue, hours.toString())
        val hour = mFrame.resources.getString(R.string.hour)
        UiUtils.setTextAndShow(mTimeHourUnits, hour)
    }

    private fun updateTimeEstimate(seconds: Int) {
        val currentTime = Calendar.getInstance()
        currentTime.add(Calendar.SECOND, seconds)
        val timeFormat12: DateFormat =
            SimpleDateFormat("h:mm aa", Locale.getDefault())
        val timeFormat24: DateFormat =
            SimpleDateFormat("HH:mm", Locale.getDefault())
        val is24Format =
            android.text.format.DateFormat.is24HourFormat(mTimeMinuteValue.context)
        UiUtils.setTextAndShow(
            mTimeMinuteValue,
            if (is24Format) timeFormat24.format(currentTime.time) else timeFormat12.format(
                currentTime.time
            )
        )
        UiUtils.hide(mTimeHourUnits, mTimeHourValue, mTimeMinuteUnits)
    }

    private fun switchTimeFormat() {
        mShowTimeLeft = !mShowTimeLeft
        update(Framework.nativeGetRouteFollowingInfo())
    }

    fun showSearchButtons(show: Boolean) {
        UiUtils.showIf(show, mSearchButtonFrame)
    }

    fun adjustSearchButtons(width: Int) {
        val params = mSearchButtonFrame.layoutParams as MarginLayoutParams
        params.setMargins(width, params.topMargin, params.rightMargin, params.bottomMargin)
        mSearchButtonFrame.requestLayout()
    }

    fun updateSearchButtonsTranslation(translation: Float) {
        val offset = if (UiUtils.isVisible(mOnboardingBtn)) mOnboardingBtn.height else 0
        mSearchButtonFrame.translationY = translation + offset
    }

    fun fadeInSearchButtons() {
        UiUtils.show(mSearchButtonFrame)
    }

    fun fadeOutSearchButtons() {
        UiUtils.invisible(mSearchButtonFrame)
    }

    fun show(show: Boolean) {
        UiUtils.showIf(show, mFrame)
        UiUtils.showIf(show, mSearchButtonFrame)
        navMenu.show(show)
    }

    fun resetSearchWheel() {
        mSearchWheel.reset()
    }

    fun onSaveState(outState: Bundle) {
        outState.putBoolean(STATE_SHOW_TIME_LEFT, mShowTimeLeft)
        mSearchWheel.saveState(outState)
    }

    fun onRestoreState(savedInstanceState: Bundle) {
        mShowTimeLeft =
            savedInstanceState.getBoolean(STATE_SHOW_TIME_LEFT)
        mSearchWheel.restoreState(savedInstanceState)
    }

    override fun onEnabled() {
        navMenu.refreshTraffic()
    }

    override fun onDisabled() {
        navMenu.refreshTraffic()
    }

    override fun onWaitingData() { // no op
    }

    override fun onOutdated() { // no op
    }

    override fun onNoData() { // no op
    }

    override fun onNetworkError() { // no op
    }

    override fun onExpiredData() { // no op
    }

    override fun onExpiredApp() { // no op
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_bookmarks -> {
                start(mFrame.context)
                Statistics.INSTANCE.trackRoutingEvent(
                    EventName.ROUTING_BOOKMARKS_CLICK,
                    RoutingController.get().isPlanning
                )
            }
        }
    }

    fun destroy() {
        from(mBottomFrame.context).release()
    }

    private class CameraWarningSignalCompletionListener internal constructor(private val mApp: Application) :
        OnCompletionListener {
        override fun onCompletion(mp: MediaPlayer) {
            TtsPlayer.INSTANCE.playTurnNotifications(mApp)
        }

    }

    companion object {
        private const val STATE_SHOW_TIME_LEFT = "ShowTimeLeft"
    }

    init {
        mFrame = activity.findViewById(R.id.navigation_frame)
        mBottomFrame = mFrame.findViewById(R.id.nav_bottom_frame)
        mBottomFrame.setOnClickListener { switchTimeFormat() }
        navMenu = createNavMenu()
        navMenu.refresh()
        // Top frame
        val topFrame = mFrame.findViewById<View>(R.id.nav_top_frame)
        val turnFrame =
            topFrame.findViewById<View>(R.id.nav_next_turn_frame)
        mNextTurnImage =
            turnFrame.findViewById<View>(R.id.turn) as ImageView
        mNextTurnDistance = turnFrame.findViewById<View>(R.id.distance) as TextView
        mCircleExit = turnFrame.findViewById<View>(R.id.circle_exit) as TextView
        mNextNextTurnFrame = topFrame.findViewById(R.id.nav_next_next_turn_frame)
        mNextNextTurnImage =
            mNextNextTurnFrame.findViewById<View>(R.id.turn) as ImageView
        mStreetFrame = topFrame.findViewById(R.id.street_frame)
        mNextStreet = mStreetFrame.findViewById<View>(R.id.street) as TextView
        val shadow = topFrame.findViewById<View>(R.id.shadow_top)
        UiUtils.showIf(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP, shadow)
        UiUtils.extendViewWithStatusBar(mStreetFrame)
        UiUtils.extendViewMarginWithStatusBar(turnFrame)
        // Bottom frame
        mSpeedViewContainer =
            mBottomFrame.findViewById(R.id.speed_view_container)
        mSpeedValue = mBottomFrame.findViewById<View>(R.id.speed_value) as TextView
        mSpeedUnits = mBottomFrame.findViewById<View>(R.id.speed_dimen) as TextView
        mTimeHourValue =
            mBottomFrame.findViewById<View>(R.id.time_hour_value) as TextView
        mTimeHourUnits =
            mBottomFrame.findViewById<View>(R.id.time_hour_dimen) as TextView
        mTimeMinuteValue =
            mBottomFrame.findViewById<View>(R.id.time_minute_value) as TextView
        mTimeMinuteUnits =
            mBottomFrame.findViewById<View>(R.id.time_minute_dimen) as TextView
        mDotTimeArrival =
            mBottomFrame.findViewById<View>(R.id.dot_estimate) as ImageView
        mDotTimeLeft =
            mBottomFrame.findViewById<View>(R.id.dot_left) as ImageView
        mDistanceValue =
            mBottomFrame.findViewById<View>(R.id.distance_value) as TextView
        mDistanceUnits =
            mBottomFrame.findViewById<View>(R.id.distance_dimen) as TextView
        mRouteProgress =
            mBottomFrame.findViewById<View>(R.id.navigation_progress) as FlatProgressView
        mSearchButtonFrame = activity.findViewById(R.id.search_button_frame)
        mSearchWheel = SearchWheel(mSearchButtonFrame)
        mOnboardingBtn = activity.findViewById(R.id.onboarding_btn)
        val bookmarkButton =
            mSearchButtonFrame.findViewById<View>(R.id.btn_bookmarks) as ImageView
        bookmarkButton.setImageDrawable(
            Graphics.tint(
                bookmarkButton.context,
                R.drawable.ic_menu_bookmarks
            )
        )
        bookmarkButton.setOnClickListener(this)
        val app =
            bookmarkButton.context.applicationContext as Application
        mSpeedCamSignalCompletionListener = CameraWarningSignalCompletionListener(app)
    }
}