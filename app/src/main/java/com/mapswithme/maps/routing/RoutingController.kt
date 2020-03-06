package com.mapswithme.maps.routing

import android.content.Context
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import androidx.annotation.DimenRes
import androidx.annotation.IntRange
import androidx.annotation.MainThread
import androidx.appcompat.app.AlertDialog
import androidx.core.util.Pair
import androidx.fragment.app.FragmentActivity
import com.mapswithme.maps.Framework
import com.mapswithme.maps.Framework.ROUTER_TYPE_TAXI
import com.mapswithme.maps.Framework.ROUTER_TYPE_TRANSIT
import com.mapswithme.maps.Framework.ROUTER_TYPE_VEHICLE
import com.mapswithme.maps.Framework.ROUTE_REBUILD_AFTER_POINTS_LOADING
import com.mapswithme.maps.Framework.nativeAddRoutePoint
import com.mapswithme.maps.Framework.nativeApplyRoutePointsTransaction
import com.mapswithme.maps.Framework.nativeBuildRoute
import com.mapswithme.maps.Framework.nativeCancelRoutePointsTransaction
import com.mapswithme.maps.Framework.nativeCloseRouting
import com.mapswithme.maps.Framework.nativeCouldAddIntermediatePoint
import com.mapswithme.maps.Framework.nativeDeleteSavedRoutePoints
import com.mapswithme.maps.Framework.nativeDisableFollowing
import com.mapswithme.maps.Framework.nativeFollowRoute
import com.mapswithme.maps.Framework.nativeFormatLatLon
import com.mapswithme.maps.Framework.nativeGetBestRouter
import com.mapswithme.maps.Framework.nativeGetLastUsedRouter
import com.mapswithme.maps.Framework.nativeGetRouteFollowingInfo
import com.mapswithme.maps.Framework.nativeGetRoutePoints
import com.mapswithme.maps.Framework.nativeGetTransitRouteInfo
import com.mapswithme.maps.Framework.nativeHasSavedRoutePoints
import com.mapswithme.maps.Framework.nativeInvalidRoutePointsTransactionId
import com.mapswithme.maps.Framework.nativeLoadRoutePoints
import com.mapswithme.maps.Framework.nativeOpenRoutePointsTransaction
import com.mapswithme.maps.Framework.nativeRemoveIntermediateRoutePoints
import com.mapswithme.maps.Framework.nativeRemoveRoute
import com.mapswithme.maps.Framework.nativeRemoveRoutePoint
import com.mapswithme.maps.Framework.nativeSaveRoutePoints
import com.mapswithme.maps.Framework.nativeSetRouteProgressListener
import com.mapswithme.maps.Framework.nativeSetRouter
import com.mapswithme.maps.Framework.nativeSetRoutingListener
import com.mapswithme.maps.Framework.nativeSetRoutingLoadPointsListener
import com.mapswithme.maps.Framework.nativeSetRoutingRecommendationListener
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.maps.bookmarks.data.FeatureId
import com.mapswithme.maps.bookmarks.data.MapObject
import com.mapswithme.maps.bookmarks.data.MapObject.Companion.createMapObject
import com.mapswithme.maps.bookmarks.data.MapObject.Companion.isOfType
import com.mapswithme.maps.bookmarks.data.MapObject.Companion.same
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.routing.ResultCodesHelper.isMoreMapsNeeded
import com.mapswithme.maps.routing.RoutePointInfo.RouteMarkType
import com.mapswithme.maps.taxi.TaxiInfo
import com.mapswithme.maps.taxi.TaxiInfoError
import com.mapswithme.maps.taxi.TaxiManager
import com.mapswithme.maps.taxi.TaxiManager.TaxiListener
import com.mapswithme.util.*
import com.mapswithme.util.concurrency.UiThread
import com.mapswithme.util.log.LoggerFactory
import com.mapswithme.util.statistics.AlohaHelper
import com.mapswithme.util.statistics.Statistics
import com.mapswithme.util.statistics.Statistics.EventName
import java.util.*
import java.util.concurrent.TimeUnit

@androidx.annotation.UiThread
class RoutingController : TaxiListener {
    private enum class State {
        NONE, PREPARE, NAVIGATION
    }

    enum class BuildState {
        NONE, BUILDING, BUILT, ERROR
    }

    interface Container {
        val activity: FragmentActivity?
        fun showSearch()
        fun showRoutePlan(
            show: Boolean,
            completionListener: Runnable?
        )

        fun showNavigation(show: Boolean)
        fun showDownloader(openDownloaded: Boolean)
        fun updateMenu()
        fun onTaxiInfoReceived(info: TaxiInfo)
        fun onTaxiError(code: TaxiManager.ErrorCode)
        fun onNavigationCancelled()
        fun onNavigationStarted()
        fun onAddedStop()
        fun onRemovedStop()
        fun onBuiltRoute()
        fun onDrivingOptionsWarning()
        val isSubwayEnabled: Boolean
        fun onCommonBuildError(
            lastResultCode: Int,
            lastMissingMaps: Array<String>
        )

        fun onDrivingOptionsBuildError()
        /**
         * @param progress progress to be displayed.
         */
        fun updateBuildProgress(
            @IntRange(
                from = 0,
                to = 100
            ) progress: Int, @Framework.RouterType router: Int
        )

        fun onStartRouteBuilding()
    }

    private val mLogger =
        LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.ROUTING)
    private var mContainer: Container? = null
    private var mBuildState = BuildState.NONE
    private var mState =
        State.NONE
    @RouteMarkType
    private var mWaitingPoiPickType = NO_WAITING_POI_PICK
    private var mLastBuildProgress = 0
    @get:Framework.RouterType
    @Framework.RouterType
    var lastRouterType = 0
        private set
    private var mHasContainerSavedState = false
    private var mContainsCachedResult = false
    private var mLastResultCode = 0
    private var mLastMissingMaps: Array<String>? = null
    var cachedRoutingInfo: RoutingInfo? = null
        private set
    var cachedTransitInfo: TransitRouteInfo? = null
        private set
    var isTaxiRequestHandled = false
        private set
    private var mTaxiPlanning = false
    var isInternetConnected = false
        private set
    private var mInvalidRoutePointsTransactionId = 0
    private var mRemovingIntermediatePointsTransactionId = 0
    private val mRoutingListener = object : Framework.RoutingListener{
        override fun onRoutingEvent(resultCode: Int, missingMaps: Array<String>?) {
            mLogger.d(
                TAG,
                "onRoutingEvent(resultCode: $resultCode)"
            )
            mLastResultCode = resultCode
            mLastMissingMaps = missingMaps
            mContainsCachedResult = true
            if (mLastResultCode == ResultCodesHelper.NO_ERROR
                || isMoreMapsNeeded(mLastResultCode)
            ) {
                onBuiltRoute()
            } else if (mLastResultCode == ResultCodesHelper.HAS_WARNINGS) {
                onBuiltRoute()
                if (mContainer != null) mContainer!!.onDrivingOptionsWarning()
            }
            processRoutingEvent()
        }

    }

    private fun onBuiltRoute() {
        cachedRoutingInfo = nativeGetRouteFollowingInfo()
        if (lastRouterType == ROUTER_TYPE_TRANSIT) cachedTransitInfo =
            nativeGetTransitRouteInfo()
        buildState = BuildState.BUILT
        mLastBuildProgress = 100
        if (mContainer != null) mContainer!!.onBuiltRoute()
    }

    private val mRoutingProgressListener =
        object : Framework.RoutingProgressListener{
            override fun onRouteBuildingProgress(progress: Float) {
                mLastBuildProgress = progress.toInt()
                updateProgress()
            }
        }
    private val mRoutingRecommendationListener =
        object : Framework.RoutingRecommendationListener{

            override fun onRecommend(recommendation: Int) {
                UiThread.run {
                    if (recommendation == ROUTE_REBUILD_AFTER_POINTS_LOADING) setStartPoint(
                        LocationHelper.INSTANCE.myPosition
                    )
                }
            }

        }
    private val mRoutingLoadPointsListener =
        object : Framework.RoutingLoadPointsListener{
            override fun onRoutePointsLoaded(success: Boolean) {
                if (success) prepare(
                    startPoint,
                    endPoint
                )
            }
        }

    private fun processRoutingEvent() {
        if (!mContainsCachedResult || mContainer == null ||
            mHasContainerSavedState
        ) return
        mContainsCachedResult = false
        if (isDrivingOptionsBuildError) mContainer!!.onDrivingOptionsWarning()
        if (mLastResultCode == ResultCodesHelper.NO_ERROR || mLastResultCode == ResultCodesHelper.HAS_WARNINGS) {
            updatePlan()
            return
        }
        if (mLastResultCode == ResultCodesHelper.CANCELLED) {
            buildState = BuildState.NONE
            updatePlan()
            return
        }
        if (!isMoreMapsNeeded(mLastResultCode)) {
            buildState = BuildState.ERROR
            mLastBuildProgress = 0
            updateProgress()
        }
        if (isDrivingOptionsBuildError) mContainer!!.onDrivingOptionsBuildError() else mContainer!!.onCommonBuildError(
            mLastResultCode,
            mLastMissingMaps!!
        )
    }

    private val isDrivingOptionsBuildError: Boolean
        private get() = (!isMoreMapsNeeded(mLastResultCode) && isVehicleRouterType
                && RoutingOptions.hasAnyOptions())

    private fun setState(newState: State) {
        mLogger.d(
            TAG,
            "[S] State: $mState -> $newState, BuildState: $mBuildState"
        )
        mState = newState
        if (mContainer != null) mContainer!!.updateMenu()
    }

    private fun updateProgress() {
        if (isTaxiPlanning) return
        if (mContainer != null) mContainer!!.updateBuildProgress(mLastBuildProgress, lastRouterType)
    }

    private fun showRoutePlan() {
        if (mContainer != null) mContainer!!.showRoutePlan(true, Runnable { updatePlan() })
    }

    fun attach(container: Container) {
        mContainer = container
    }

    fun initialize() {
        lastRouterType = nativeGetLastUsedRouter()
        mInvalidRoutePointsTransactionId = nativeInvalidRoutePointsTransactionId()
        mRemovingIntermediatePointsTransactionId = mInvalidRoutePointsTransactionId
        nativeSetRoutingListener(mRoutingListener)
        nativeSetRouteProgressListener(mRoutingProgressListener)
        nativeSetRoutingRecommendationListener(mRoutingRecommendationListener)
        nativeSetRoutingLoadPointsListener(mRoutingLoadPointsListener)
        TaxiManager.INSTANCE.setTaxiListener(this)
    }

    fun detach() {
        mContainer = null
    }

    @MainThread
    fun restore() {
        mHasContainerSavedState = false
        if (isPlanning) showRoutePlan()
        if (mContainer != null) {
            if (isTaxiPlanning) mContainer!!.updateBuildProgress(0, lastRouterType)
            mContainer!!.showNavigation(isNavigating)
            mContainer!!.updateMenu()
        }
        processRoutingEvent()
    }

    fun onSaveState() {
        mHasContainerSavedState = true
    }

    private fun build() {
        nativeRemoveRoute()
        mLogger.d(TAG, "build")
        isTaxiRequestHandled = false
        mLastBuildProgress = 0
        isInternetConnected = ConnectionState.isConnected
        if (isTaxiRouterType) {
            if (!isInternetConnected) {
                completeTaxiRequest()
                return
            }
            val start = startPoint
            val end = endPoint
            if (start != null && end != null) requestTaxiInfo(start, end)
        }
        buildState = BuildState.BUILDING
        if (mContainer != null) mContainer!!.onStartRouteBuilding()
        updatePlan()
        Statistics.INSTANCE.trackRouteBuild(
            lastRouterType,
            startPoint,
            endPoint
        )
        org.alohalytics.Statistics.logEvent(
            AlohaHelper.ROUTING_BUILD, arrayOf(
                Statistics.EventParam.FROM,
                Statistics.getPointType(startPoint),
                Statistics.EventParam.TO,
                Statistics.getPointType(endPoint)
            )
        )
        nativeBuildRoute()
    }

    private fun completeTaxiRequest() {
        isTaxiRequestHandled = true
        if (mContainer != null) {
            mContainer!!.updateBuildProgress(100, lastRouterType)
            mContainer!!.updateMenu()
        }
    }

    private fun showDisclaimer(
        startPoint: MapObject?, endPoint: MapObject?,
        fromApi: Boolean
    ) {
        if (mContainer == null) return
        val builder = StringBuilder()
        for (resId in intArrayOf(
            R.string.dialog_routing_disclaimer_priority,
            R.string.dialog_routing_disclaimer_precision,
            R.string.dialog_routing_disclaimer_recommendations,
            R.string.dialog_routing_disclaimer_borders,
            R.string.dialog_routing_disclaimer_beware
        )) builder.append(MwmApplication.get().getString(resId)).append("\n\n")
        AlertDialog.Builder(mContainer!!.activity!!)
            .setTitle(R.string.dialog_routing_disclaimer_title)
            .setMessage(builder.toString())
            .setCancelable(false)
            .setNegativeButton(R.string.decline, null)
            .setPositiveButton(R.string.accept) { dlg, which ->
                Config.acceptRoutingDisclaimer()
                prepare(startPoint, endPoint, fromApi)
            }.show()
    }

    fun restoreRoute() {
        nativeLoadRoutePoints()
    }

    fun hasSavedRoute(): Boolean {
        return nativeHasSavedRoutePoints()
    }

    fun saveRoute() {
        if (isNavigating || isPlanning && isBuilt) nativeSaveRoutePoints()
    }

    fun deleteSavedRoute() {
        nativeDeleteSavedRoutePoints()
    }

    fun rebuildLastRoute() {
        setState(State.NONE)
        buildState = BuildState.NONE
        prepare(startPoint, endPoint, false)
    }

    @JvmOverloads
    fun prepare(
        canUseMyPositionAsStart: Boolean,
        endPoint: MapObject?,
        fromApi: Boolean = false
    ) {
        val startPoint =
            if (canUseMyPositionAsStart) LocationHelper.INSTANCE.myPosition else null
        prepare(startPoint, endPoint, fromApi)
    }

    fun prepare(
        canUseMyPositionAsStart: Boolean, endPoint: MapObject?,
        @Framework.RouterType type: Int, fromApi: Boolean
    ) {
        val startPoint =
            if (canUseMyPositionAsStart) LocationHelper.INSTANCE.myPosition else null
        prepare(startPoint, endPoint, type, fromApi)
    }

    @JvmOverloads
    fun prepare(
        startPoint: MapObject?,
        endPoint: MapObject?,
        fromApi: Boolean = false
    ) {
        mLogger.d(
            TAG,
            "prepare (" + if (endPoint == null) "route)" else "p2p)"
        )
        if (!Config.isRoutingDisclaimerAccepted()) {
            showDisclaimer(startPoint, endPoint, fromApi)
            return
        }
        initLastRouteType(startPoint, endPoint, fromApi)
        prepare(startPoint, endPoint, lastRouterType, fromApi)
    }

    private fun initLastRouteType(
        startPoint: MapObject?, endPoint: MapObject?,
        fromApi: Boolean
    ) {
        if (isSubwayEnabled && !fromApi) {
            lastRouterType = ROUTER_TYPE_TRANSIT
            return
        }
        if (startPoint != null && endPoint != null) lastRouterType = nativeGetBestRouter(
            startPoint.lat, startPoint.lon,
            endPoint.lat, endPoint.lon
        )
    }

    private val isSubwayEnabled: Boolean
        private get() = mContainer != null && mContainer!!.isSubwayEnabled

    @JvmOverloads
    fun prepare(
        startPoint: MapObject?, endPoint: MapObject?,
        @Framework.RouterType routerType: Int, fromApi: Boolean = false
    ) {
        cancel()
        setState(State.PREPARE)
        lastRouterType = routerType
        nativeSetRouter(lastRouterType)
        if (startPoint != null || endPoint != null) setPointsInternal(startPoint, endPoint)
        if (mContainer != null) mContainer!!.showRoutePlan(
            true,
            Runnable { if (startPoint == null || endPoint == null) updatePlan() else build() })
        if (startPoint != null) trackPointAdd(
            startPoint,
            RoutePointInfo.ROUTE_MARK_START,
            false,
            false,
            fromApi
        )
        if (endPoint != null) trackPointAdd(
            endPoint,
            RoutePointInfo.ROUTE_MARK_FINISH,
            false,
            false,
            fromApi
        )
    }

    fun start() {
        mLogger.d(TAG, "start")
        // This saving is needed just for situation when the user starts navigation
// and then app crashes. So, the previous route will be restored on the next app launch.
        saveRoute()
        val my = LocationHelper.INSTANCE.myPosition
        if (my == null || !isOfType(MapObject.MY_POSITION, startPoint)) {
            Statistics.INSTANCE.trackEvent(EventName.ROUTING_START_SUGGEST_REBUILD)
            AlohaHelper.logClick(AlohaHelper.ROUTING_START_SUGGEST_REBUILD)
            suggestRebuildRoute()
            return
        }
        setState(State.NAVIGATION)
        if (mContainer != null) {
            mContainer!!.showRoutePlan(false, null)
            mContainer!!.showNavigation(true)
            mContainer!!.onNavigationStarted()
        }
        nativeFollowRoute()
        LocationHelper.INSTANCE.restart()
    }

    fun addStop(mapObject: MapObject) {
        addRoutePoint(RoutePointInfo.ROUTE_MARK_INTERMEDIATE, mapObject)
        trackPointAdd(
            mapObject, RoutePointInfo.ROUTE_MARK_INTERMEDIATE, isPlanning, isNavigating,
            false
        )
        build()
        if (mContainer != null) mContainer!!.onAddedStop()
        backToPlaningStateIfNavigating()
    }

    fun removeStop(mapObject: MapObject) {
        val info = mapObject.routePointInfo
            ?: throw AssertionError("A stop point must have the route point info!")
        applyRemovingIntermediatePointsTransaction()
        nativeRemoveRoutePoint(info.markType, info.intermediateIndex)
        trackPointRemove(
            mapObject,
            info.markType,
            isPlanning,
            isNavigating,
            false
        )
        build()
        if (mContainer != null) mContainer!!.onRemovedStop()
        backToPlaningStateIfNavigating()
    }

    private fun backToPlaningStateIfNavigating() {
        if (!isNavigating) return
        setState(State.PREPARE)
        if (mContainer != null) {
            mContainer!!.showNavigation(false)
            mContainer!!.showRoutePlan(true, null)
            mContainer!!.updateMenu()
            mContainer!!.onNavigationCancelled()
        }
    }

    private fun removeIntermediatePoints() {
        nativeRemoveIntermediateRoutePoints()
    }

    private fun toMapObject(point: RouteMarkData): MapObject {
        return createMapObject(
            FeatureId.EMPTY, if (point.mIsMyPosition) MapObject.MY_POSITION else MapObject.POI,
            point.mTitle ?: "",
            point.mSubtitle ?: "", point.mLat, point.mLon
        )
    }

    val isStopPointAllowed: Boolean
        get() = nativeCouldAddIntermediatePoint() && !isTaxiRouterType

    fun isRoutePoint(mapObject: MapObject): Boolean {
        return mapObject.routePointInfo != null
    }

    private fun suggestRebuildRoute() {
        if (mContainer == null) return
        val builder =
            AlertDialog.Builder(mContainer!!.activity!!)
                .setMessage(R.string.p2p_reroute_from_current)
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, null)
        val titleView = View.inflate(
            mContainer!!.activity,
            R.layout.dialog_suggest_reroute_title,
            null
        ) as TextView
        titleView.setText(R.string.p2p_only_from_current)
        builder.setCustomTitle(titleView)
        if (isOfType(MapObject.MY_POSITION, endPoint)) {
            builder.setPositiveButton(R.string.ok) { dialog, which -> swapPoints() }
        } else {
            if (LocationHelper.INSTANCE.myPosition == null) builder.setMessage(null).setNegativeButton(
                null,
                null
            )
            builder.setPositiveButton(R.string.ok) { dialog, which -> setStartFromMyPosition() }
        }
        builder.show()
    }

    private fun updatePlan() {
        updateProgress()
    }

    private fun cancelInternal() {
        mLogger.d(TAG, "cancelInternal")
        mWaitingPoiPickType = NO_WAITING_POI_PICK
        isTaxiRequestHandled = false
        buildState = BuildState.NONE
        setState(State.NONE)
        applyRemovingIntermediatePointsTransaction()
        nativeDeleteSavedRoutePoints()
        nativeCloseRouting()
    }

    fun cancel(): Boolean {
        if (isPlanning) {
            mLogger.d(TAG, "cancel: planning")
            cancelInternal()
            if (mContainer != null) mContainer!!.showRoutePlan(false, null)
            return true
        }
        if (isNavigating) {
            mLogger.d(TAG, "cancel: navigating")
            cancelInternal()
            if (mContainer != null) {
                mContainer!!.showNavigation(false)
                mContainer!!.updateMenu()
            }
            if (mContainer != null) mContainer!!.onNavigationCancelled()
            return true
        }
        mLogger.d(TAG, "cancel: none")
        return false
    }

    val isPlanning: Boolean
        get() = mState == State.PREPARE

    val isTaxiPlanning: Boolean
        get() = isTaxiRouterType && mTaxiPlanning

    val isTaxiRouterType: Boolean
        get() = lastRouterType == ROUTER_TYPE_TAXI

    val isTransitType: Boolean
        get() = lastRouterType == ROUTER_TYPE_TRANSIT

    val isVehicleRouterType: Boolean
        get() = lastRouterType == ROUTER_TYPE_VEHICLE

    val isNavigating: Boolean
        get() = mState == State.NAVIGATION

    val isVehicleNavigation: Boolean
        get() = isNavigating && isVehicleRouterType

    val isBuilding: Boolean
        get() = mState == State.PREPARE && mBuildState == BuildState.BUILDING

    val isErrorEncountered: Boolean
        get() = mBuildState == BuildState.ERROR

    val isBuilt: Boolean
        get() = mBuildState == BuildState.BUILT

    fun waitForPoiPick(@RouteMarkType pointType: Int) {
        mWaitingPoiPickType = pointType
    }

    val isWaitingPoiPick: Boolean
        get() = mWaitingPoiPickType != NO_WAITING_POI_PICK

    var buildState: BuildState
        get() = mBuildState
        private set(newState) {
            mLogger.d(
                TAG,
                "[B] State: $mState, BuildState: $mBuildState -> $newState"
            )
            mBuildState = newState
            if (mBuildState == BuildState.BUILT && !isOfType(
                    MapObject.MY_POSITION,
                    startPoint
                )
            ) nativeDisableFollowing()
            if (mContainer != null) mContainer!!.updateMenu()
        }

    val startPoint: MapObject?
        get() = getStartOrEndPointByType(RoutePointInfo.ROUTE_MARK_START)

    val endPoint: MapObject?
        get() = getStartOrEndPointByType(RoutePointInfo.ROUTE_MARK_FINISH)

    private fun getStartOrEndPointByType(@RouteMarkType type: Int): MapObject? {
        val points = nativeGetRoutePoints()
        val size = points.size
        if (size == 0) return null
        if (size == 1) {
            val point = points[0]
            return if (point!!.mPointType == type) toMapObject(point) else null
        }
        if (type == RoutePointInfo.ROUTE_MARK_START) return toMapObject(points[0]!!)
        return if (type == RoutePointInfo.ROUTE_MARK_FINISH) toMapObject(points[size - 1]!!) else null
    }

    fun hasStartPoint(): Boolean {
        return startPoint != null
    }

    fun hasEndPoint(): Boolean {
        return endPoint != null
    }

    private fun setPointsInternal(startPoint: MapObject?, endPoint: MapObject?) {
        val hasStart = startPoint != null
        val hasEnd = endPoint != null
        val hasOnePointAtLeast = hasStart || hasEnd
        if (hasOnePointAtLeast) applyRemovingIntermediatePointsTransaction()
        if (hasStart) addRoutePoint(
            RoutePointInfo.ROUTE_MARK_START,
            startPoint!!
        )
        if (hasEnd) addRoutePoint(
            RoutePointInfo.ROUTE_MARK_FINISH,
            endPoint!!
        )
        if (hasOnePointAtLeast && mContainer != null) mContainer!!.updateMenu()
    }

    fun checkAndBuildRoute() {
        if (isWaitingPoiPick) showRoutePlan()
        if (startPoint != null && endPoint != null) build()
    }

    private fun setStartFromMyPosition(): Boolean {
        mLogger.d(TAG, "setStartFromMyPosition")
        val my = LocationHelper.INSTANCE.myPosition
        if (my == null) {
            mLogger.d(
                TAG,
                "setStartFromMyPosition: no my position - skip"
            )
            return false
        }
        return setStartPoint(my)
    }

    /**
     * Sets starting point.
     *
     *  * If `point` matches ending one and the starting point was set  swap points.
     *  * The same as the currently set starting point is skipped.
     *
     * Route starts to build if both points were set.
     *
     * @return `true` if the point was set.
     */
    fun setStartPoint(point: MapObject?): Boolean {
        mLogger.d(TAG, "setStartPoint")
        var startPoint = startPoint
        var endPoint = endPoint
        val isSamePoint = same(startPoint, point)
        if (point != null) {
            applyRemovingIntermediatePointsTransaction()
            addRoutePoint(RoutePointInfo.ROUTE_MARK_START, point)
            startPoint = startPoint
        }
        if (isSamePoint) {
            mLogger.d(
                TAG,
                "setStartPoint: skip the same starting point"
            )
            return false
        }
        if (point != null && point.sameAs(endPoint)) {
            if (startPoint == null) {
                mLogger.d(
                    TAG,
                    "setStartPoint: skip because starting point is empty"
                )
                return false
            }
            mLogger.d(TAG, "setStartPoint: swap with end point")
            endPoint = startPoint
        }
        startPoint = point
        setPointsInternal(startPoint, endPoint)
        checkAndBuildRoute()
        if (startPoint != null) trackPointAdd(
            startPoint, RoutePointInfo.ROUTE_MARK_START, isPlanning, isNavigating,
            false
        )
        return true
    }

    /**
     * Sets ending point.
     *
     *  * If `point` is the same as starting point  swap points if ending point is set, skip otherwise.
     *  * Set starting point to MyPosition if it was not set before.
     *
     * Route starts to build if both points were set.
     *
     * @return `true` if the point was set.
     */
    fun setEndPoint(point: MapObject?): Boolean {
        mLogger.d(TAG, "setEndPoint")
        var startPoint = startPoint
        var endPoint = endPoint
        val isSamePoint = same(endPoint, point)
        if (point != null) {
            applyRemovingIntermediatePointsTransaction()
            addRoutePoint(RoutePointInfo.ROUTE_MARK_FINISH, point)
            endPoint = endPoint
        }
        if (isSamePoint) {
            mLogger.d(TAG, "setEndPoint: skip the same end point")
            return false
        }
        if (point != null && point.sameAs(startPoint)) {
            if (endPoint == null) {
                mLogger.d(
                    TAG,
                    "setEndPoint: skip because end point is empty"
                )
                return false
            }
            mLogger.d(TAG, "setEndPoint: swap with starting point")
            startPoint = endPoint
        }
        endPoint = point
        if (endPoint != null) trackPointAdd(
            endPoint, RoutePointInfo.ROUTE_MARK_FINISH, isPlanning, isNavigating,
            false
        )
        setPointsInternal(startPoint, endPoint)
        checkAndBuildRoute()
        return true
    }

    private fun swapPoints() {
        mLogger.d(TAG, "swapPoints")
        var startPoint = startPoint
        var endPoint = endPoint
        val point = startPoint
        startPoint = endPoint
        endPoint = point
        Statistics.INSTANCE.trackEvent(EventName.ROUTING_SWAP_POINTS)
        AlohaHelper.logClick(AlohaHelper.ROUTING_SWAP_POINTS)
        setPointsInternal(startPoint, endPoint)
        checkAndBuildRoute()
        if (mContainer != null) mContainer!!.updateMenu()
    }

    fun setRouterType(@Framework.RouterType router: Int) {
        mLogger.d(
            TAG,
            "setRouterType: " + lastRouterType + " -> " + router
        )
        // Repeating tap on Taxi icon should trigger the route building always,
// because it may be "No internet connection, try later" case
        if (router == lastRouterType && !isTaxiRouterType) return
        lastRouterType = router
        nativeSetRouter(router)
        // Taxi routing does not support intermediate points.
        if (isTaxiRouterType) {
            openRemovingIntermediatePointsTransaction()
            removeIntermediatePoints()
        } else {
            cancelRemovingIntermediatePointsTransaction()
        }
        if (startPoint != null && endPoint != null) build()
    }

    private fun openRemovingIntermediatePointsTransaction() {
        if (mRemovingIntermediatePointsTransactionId == mInvalidRoutePointsTransactionId) mRemovingIntermediatePointsTransactionId =
            nativeOpenRoutePointsTransaction()
    }

    private fun cancelRemovingIntermediatePointsTransaction() {
        if (mRemovingIntermediatePointsTransactionId == mInvalidRoutePointsTransactionId) return
        nativeCancelRoutePointsTransaction(mRemovingIntermediatePointsTransactionId)
        mRemovingIntermediatePointsTransactionId = mInvalidRoutePointsTransactionId
    }

    private fun applyRemovingIntermediatePointsTransaction() { // We have to apply removing intermediate points transaction each time
// we add/remove route points in the taxi mode.
        if (mRemovingIntermediatePointsTransactionId == mInvalidRoutePointsTransactionId) return
        nativeApplyRoutePointsTransaction(mRemovingIntermediatePointsTransactionId)
        mRemovingIntermediatePointsTransactionId = mInvalidRoutePointsTransactionId
    }

    fun onPoiSelected(point: MapObject?) {
        if (!isWaitingPoiPick) return
        if (mWaitingPoiPickType != RoutePointInfo.ROUTE_MARK_FINISH
            && mWaitingPoiPickType != RoutePointInfo.ROUTE_MARK_START
        ) {
            throw AssertionError("Only start and finish points can be added through search!")
        }
        if (point != null) {
            if (mWaitingPoiPickType == RoutePointInfo.ROUTE_MARK_FINISH) setEndPoint(point) else setStartPoint(
                point
            )
        }
        if (mContainer != null) {
            mContainer!!.updateMenu()
            showRoutePlan()
        }
        mWaitingPoiPickType = NO_WAITING_POI_PICK
    }

    private fun requestTaxiInfo(startPoint: MapObject, endPoint: MapObject) {
        mTaxiPlanning = true
        TaxiManager.nativeRequestTaxiProducts(
            NetworkPolicy.newInstance(true /* canUse */),
            startPoint.lat, startPoint.lon,
            endPoint.lat, endPoint.lon
        )
        if (mContainer != null) mContainer!!.updateBuildProgress(0, lastRouterType)
    }

    override fun onTaxiProviderReceived(provider: TaxiInfo) {
        mTaxiPlanning = false
        mLogger.d(TAG, "onTaxiInfoReceived provider = $provider")
        if (isTaxiRouterType && mContainer != null) {
            mContainer!!.onTaxiInfoReceived(provider)
            completeTaxiRequest()
            Statistics.INSTANCE.trackTaxiEvent(
                EventName.ROUTING_TAXI_ROUTE_BUILT,
                provider.type.providerName
            )
        }
    }

    override fun onTaxiErrorReceived(error: TaxiInfoError) {
        mTaxiPlanning = false
        mLogger.e(TAG, "onTaxiError error = $error")
        if (isTaxiRouterType && mContainer != null) {
            mContainer!!.onTaxiError(error.code)
            completeTaxiRequest()
            Statistics.INSTANCE.trackTaxiError(error)
        }
    }

    override fun onNoTaxiProviders() {
        mTaxiPlanning = false
        mLogger.e(TAG, "onNoTaxiProviders")
        if (isTaxiRouterType && mContainer != null) {
            mContainer!!.onTaxiError(TaxiManager.ErrorCode.NoProviders)
            completeTaxiRequest()
            Statistics.INSTANCE.trackNoTaxiProvidersError()
        }
    }

    companion object {
        private val TAG = RoutingController::class.java.simpleName
        private const val NO_WAITING_POI_PICK = -1
        private val sInstance = RoutingController()
        @JvmStatic
        fun get(): RoutingController {
            return sInstance
        }

        private fun trackPointAdd(
            point: MapObject, @RouteMarkType type: Int,
            isPlanning: Boolean, isNavigating: Boolean, fromApi: Boolean
        ) {
            val isMyPosition = point.mapObjectType == MapObject.MY_POSITION
            Statistics.INSTANCE.trackRoutingPoint(
                EventName.ROUTING_POINT_ADD, type, isPlanning, isNavigating,
                isMyPosition, fromApi
            )
        }

        private fun trackPointRemove(
            point: MapObject, @RouteMarkType type: Int,
            isPlanning: Boolean, isNavigating: Boolean, fromApi: Boolean
        ) {
            val isMyPosition = point.mapObjectType == MapObject.MY_POSITION
            Statistics.INSTANCE.trackRoutingPoint(
                EventName.ROUTING_POINT_REMOVE, type, isPlanning, isNavigating,
                isMyPosition, fromApi
            )
        }

        private fun addRoutePoint(@RouteMarkType type: Int, point: MapObject) {
            val description =
                getDescriptionForPoint(point)
            nativeAddRoutePoint(
                description.first /* title */, description.second /* subtitle */,
                type, 0 /* intermediateIndex */,
                isOfType(MapObject.MY_POSITION, point),
                point.lat, point.lon
            )
        }

        private fun getDescriptionForPoint(point: MapObject): Pair<String, String> {
            val title: String
            var subtitle = ""
            if (!TextUtils.isEmpty(point.title)) {
                title = point.title
                subtitle = point.subtitle
            } else {
                title = if (!TextUtils.isEmpty(point.subtitle)) {
                    point.subtitle
                } else if (!TextUtils.isEmpty(point.address)) {
                    point.address
                } else {
                    nativeFormatLatLon(point.lat, point.lon, false /* useDmsFormat */)
                }
            }
            return Pair(title, subtitle)
        }

        @JvmStatic
        fun formatRoutingTime(
            context: Context,
            seconds: Int, @DimenRes unitsSize: Int
        ): CharSequence {
            val minutes =
                TimeUnit.SECONDS.toMinutes(seconds.toLong()) % 60
            val hours = TimeUnit.SECONDS.toHours(seconds.toLong())
            val min = context.getString(R.string.minute)
            val hour = context.getString(R.string.hour)
            @DimenRes val textSize = R.dimen.text_size_routing_number
            val displayedH = Utils.formatUnitsText(
                context,
                textSize,
                unitsSize,
                hours.toString(),
                hour
            )
            val displayedM = Utils.formatUnitsText(
                context,
                textSize,
                unitsSize,
                minutes.toString(),
                min
            )
            return if (hours == 0L) displayedM else TextUtils.concat(
                "$displayedH ",
                displayedM
            )
        }

        fun formatArrivalTime(seconds: Int): String {
            val current = Calendar.getInstance()
            current[Calendar.SECOND] = 0
            current.add(Calendar.SECOND, seconds)
            return StringUtils.formatUsingUsLocale(
                "%d:%02d",
                current[Calendar.HOUR_OF_DAY],
                current[Calendar.MINUTE]
            )
        }
    }
}