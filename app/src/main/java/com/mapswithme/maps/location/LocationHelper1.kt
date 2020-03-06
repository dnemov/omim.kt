package com.mapswithme.maps.location

import android.app.Activity
import android.location.Location
import androidx.annotation.UiThread
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.mapswithme.maps.Framework
import com.mapswithme.maps.Framework.RouterType
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.bookmarks.data.FeatureId
import com.mapswithme.maps.bookmarks.data.MapObject
import com.mapswithme.maps.bookmarks.data.MapObject.Companion.createMapObject
import com.mapswithme.maps.location.LocationState.LocationPendingTimeoutListener
import com.mapswithme.maps.location.LocationState.ModeChangeListener
import com.mapswithme.maps.routing.RoutingController
import com.mapswithme.util.*
import com.mapswithme.util.log.LoggerFactory

enum class LocationHelper {
    INSTANCE;

    private val mOnTransition =
        TransitionListener()
    private val mCoreLocationListener: LocationListener =
        object : LocationListener {
            override fun onLocationUpdated(location: Location) { // If we are still in the first run mode, i.e. user is staying on the first run screens,
// not on the map, we mustn't post location update to the core. Only this preserving allows us
// to play nice zoom animation once a user will leave first screens and will see a map.
                if (mInFirstRun) {
                    mLogger.d(
                        TAG,
                        "Location update is obtained and must be ignored, " +
                                "because the app is in a first run mode"
                    )
                    return
                }
                nativeLocationUpdated(
                    location.time,
                    location.latitude,
                    location.longitude,
                    location.accuracy,
                    location.altitude,
                    location.speed,
                    location.bearing
                )
                if (mUiCallback != null) mUiCallback!!.onLocationUpdated(location)
            }

            override fun onCompassUpdated(
                time: Long,
                magneticNorth: Double,
                trueNorth: Double,
                accuracy: Double
            ) {
                if (compassData == null) compassData = CompassData()
                compassData!!.update(magneticNorth, trueNorth)
                if (mUiCallback != null) mUiCallback!!.onCompassUpdated(compassData!!)
            }

            override fun onLocationError(errorCode: Int) {
                mLogger.d(
                    TAG,
                    "onLocationError errorCode = $errorCode",
                    Throwable()
                )
                nativeOnLocationError(errorCode)
                mLogger.d(
                    TAG,
                    "nativeOnLocationError errorCode = " + errorCode +
                            ", current state = " + LocationState.nameOf(myPositionMode)
                )
                if (mUiCallback == null) return
                mUiCallback!!.onLocationError()
            }

            override fun toString(): String {
                return "LocationHelper.mCoreLocationListener"
            }
        }
    private val mLogger =
        LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.LOCATION)
    private val mListeners =
        Listeners<LocationListener>()
    /**
     *
     * Obtains last known saved location. It depends on "My position" button mode and is erased on "No follow, no position" one.
     *
     * If you need the location regardless of the button's state, use [.getLastKnownLocation].
     * @return `null` if no location is saved or "My position" button is in "No follow, no position" mode.
     */
    var savedLocation: Location? = null
        private set
    private var mMyPosition: MapObject? = null
    var savedLocationTime: Long = 0
        private set
    private val mSensorHelper = SensorHelper()
    private var mLocationProvider: BaseLocationProvider? = null
    private var mUiCallback: UiCallback? = null
    var interval: Long = 0
        private set
    var compassData: CompassData? = null
        private set
    private var mInFirstRun = false
    private var isLocationUpdateStoppedByUser: Boolean = false
    private val mMyPositionModeListener = object : ModeChangeListener {
        override fun onMyPositionModeChanged(newMode: Int) {
            notifyMyPositionModeChanged(newMode)
            mLogger.d(
                TAG,
                "onMyPositionModeChanged mode = " + LocationState.nameOf(newMode)
            )
            if (mUiCallback == null) mLogger.d(
                TAG,
                "UI is not ready to listen my position changes, i.e. it's not attached yet."
            )
        }
    }
    private val mLocationPendingTimeoutListener =
        object : LocationPendingTimeoutListener {
            override fun onLocationPendingTimeout() {
                stop()
                if (LocationUtils.areLocationServicesTurnedOn()) notifyLocationNotFound()
            }
        }

    @UiThread
    fun initialize() {
        initProvider()
        LocationState.nativeSetListener(mMyPositionModeListener)
        LocationState.nativeSetLocationPendingTimeoutListener(mLocationPendingTimeoutListener)
        MwmApplication.backgroundTracker()?.addListener(mOnTransition)
    }

    private fun initProvider() {
        mLogger.d(TAG, "initProvider", Throwable())
        val application = MwmApplication.get()
        val containsGoogleServices =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(application) == ConnectionResult.SUCCESS
        val googleServicesTurnedInSettings =
            Config.useGoogleServices()
        if (containsGoogleServices && googleServicesTurnedInSettings) {
            mLogger.d(TAG, "Use fused provider.")
            mLocationProvider = GoogleFusedLocationProvider(FusedLocationFixChecker())
        } else {
            initNativeProvider()
        }
    }

    fun initNativeProvider() {
        mLogger.d(TAG, "Use native provider")
        mLocationProvider = AndroidNativeProvider(DefaultLocationFixChecker())
    }

    fun onLocationUpdated(location: Location) {
        savedLocation = location
        mMyPosition = null
        savedLocationTime = System.currentTimeMillis()
    }

    /**
     * @return MapObject.MY_POSITION, null if location is not yet determined or "My position" button is switched off.
     */
    val myPosition: MapObject?
        get() {
            if (!LocationState.isTurnedOn) {
                mMyPosition = null
                return null
            }
            if (savedLocation == null) return null
            if (mMyPosition == null) mMyPosition = createMapObject(
                FeatureId.EMPTY, MapObject.MY_POSITION, "", "",
                savedLocation!!.latitude, savedLocation!!.longitude
            )
            return mMyPosition
        }

    fun switchToNextMode() {
        mLogger.d(TAG, "switchToNextMode()")
        LocationState.nativeSwitchToNextMode()
    }

    /**
     * Indicates about whether a location provider is polling location updates right now or not.
     * @see BaseLocationProvider.isActive
     */
    val isActive: Boolean
        get() = mLocationProvider != null && mLocationProvider!!.isActive

    fun setStopLocationUpdateByUser(isStopped: Boolean) {
        mLogger.d(TAG, "Set stop location update by user: $isStopped")
        isLocationUpdateStoppedByUser = isStopped
    }

    fun notifyCompassUpdated(
        time: Long,
        magneticNorth: Double,
        trueNorth: Double,
        accuracy: Double
    ) {
        for (listener in mListeners) listener.onCompassUpdated(
            time,
            magneticNorth,
            trueNorth,
            accuracy
        )
        mListeners.finishIterate()
    }

    fun notifyLocationUpdated() {
        if (savedLocation == null) {
            mLogger.d(TAG, "No saved location - skip")
            return
        }
        for (listener in mListeners)
            listener.onLocationUpdated(savedLocation!!)
        mListeners.finishIterate()
        // TODO: consider to create callback mechanism to transfer 'ROUTE_IS_FINISHED' event from
// the core to the platform code (https://jira.mail.ru/browse/MAPSME-3675),
// because calling the native method 'nativeIsRouteFinished'
// too often can result in poor UI performance.
        if (RoutingController.get().isNavigating && Framework.nativeIsRouteFinished()) {
            mLogger.d(TAG, "End point is reached")
            restart()
            if (mUiCallback != null) mUiCallback!!.onRoutingFinish()
            RoutingController.get().cancel()
        }
    }

    private fun notifyLocationUpdated(listener: LocationListener) {
        mLogger.d(TAG, "notifyLocationUpdated(), listener: $listener")
        if (savedLocation == null) {
            mLogger.d(TAG, "No saved location - skip")
            return
        }
        listener.onLocationUpdated(savedLocation!!)
    }

    private fun notifyLocationError(errCode: Int) {
        mLogger.d(TAG, "notifyLocationError(): $errCode")
        for (listener in mListeners) listener.onLocationError(
            errCode
        )
        mListeners.finishIterate()
    }

    private fun notifyMyPositionModeChanged(newMode: Int) {
        mLogger.d(
            TAG,
            "notifyMyPositionModeChanged(): " + LocationState.nameOf(newMode),
            Throwable()
        )
        if (mUiCallback != null) mUiCallback!!.onMyPositionModeChanged(newMode)
    }

    private fun notifyLocationNotFound() {
        mLogger.d(TAG, "notifyLocationNotFound()")
        if (mUiCallback != null) mUiCallback!!.onLocationNotFound()
    }

    /**
     * Registers listener to obtain location updates.
     *
     * @param listener    listener to be registered.
     * @param forceUpdate instantly notify given listener about available location, if any.
     */
    @UiThread
    fun addListener(
        listener: LocationListener,
        forceUpdate: Boolean
    ) {
        mLogger.d(
            TAG,
            "addListener(): $listener, forceUpdate: $forceUpdate"
        )
        mLogger.d(TAG, " - listener count was: " + mListeners.getSize())
        mListeners.register(listener)
        if (forceUpdate) notifyLocationUpdated(listener)
    }

    /**
     * Registers listener to obtain location updates.
     *
     * @param listener listener to be registered.
     */
    @UiThread
    fun addListener(listener: LocationListener) {
        addListener(listener, false)
    }

    @UiThread
            /**
             * Removes given location listener.
             * @param listener listener to unregister.
             */
    fun removeListener(listener: LocationListener) {
        mLogger.d(TAG, "removeListener(), listener: $listener")
        mLogger.d(TAG, " - listener count was: " + mListeners.getSize())
        mListeners.unregister(listener)
    }

    fun startSensors() {
        mSensorHelper.start()
    }

    fun resetMagneticField(location: Location) {
        mSensorHelper.resetMagneticField(savedLocation, location)
    }

    private fun calcLocationUpdatesInterval() {
        mLogger.d(TAG, "calcLocationUpdatesInterval()")
        if (RoutingController.get().isNavigating) {
            mLogger.d(
                TAG,
                "calcLocationUpdatesInterval(), it's navigation mode"
            )
            @RouterType val router = Framework.nativeGetRouter()
            when (router) {
                Framework.ROUTER_TYPE_PEDESTRIAN -> interval =
                    INTERVAL_NAVIGATION_PEDESTRIAN_MS
                Framework.ROUTER_TYPE_VEHICLE -> interval =
                    INTERVAL_NAVIGATION_VEHICLE_MS
                Framework.ROUTER_TYPE_BICYCLE -> interval =
                    INTERVAL_NAVIGATION_BICYCLE_MS
                Framework.ROUTER_TYPE_TRANSIT ->  // TODO: what is the interval should be for transit type?
                    interval = INTERVAL_NAVIGATION_PEDESTRIAN_MS
                else -> throw IllegalArgumentException("Unsupported router type: $router")
            }
            return
        }
        val mode = myPositionMode
        when (mode) {
            LocationState.FOLLOW -> interval = INTERVAL_FOLLOW_MS
            LocationState.FOLLOW_AND_ROTATE -> interval =
                INTERVAL_FOLLOW_AND_ROTATE_MS
            else -> interval = INTERVAL_NOT_FOLLOW_MS
        }
    }

    /**
     * Stops the current provider. Then initialize the location provider again,
     * because location settings could be changed and a new location provider can be used,
     * such as Google fused provider. And we think that Google fused provider is preferable
     * for the most cases. And starts the initialized location provider.
     *
     * @see .start
     */
    fun restart() {
        mLogger.d(TAG, "restart()")
        checkProviderInitialization()
        stopInternal()
        initProvider()
        start()
    }

    /**
     * Adds the [.mCoreLocationListener] to listen location updates and notify UI.
     * Notifies about [.ERROR_DENIED] if there are no enabled location providers.
     * Calculates minimum time interval for location updates.
     * Starts polling location updates.
     */
    fun start() {
        if (isLocationUpdateStoppedByUser) {
            mLogger.d(
                TAG,
                "Location updates are stopped by the user manually, so skip provider start"
                        + " until the user starts it manually."
            )
            return
        }
        checkProviderInitialization()
        if (mLocationProvider!!.isActive) throw AssertionError(
            "Location provider '" + mLocationProvider
                    + "' must be stopped first"
        )
        addListener(mCoreLocationListener, true)
        if (!LocationUtils.checkProvidersAvailability()) { // No need to notify about an error in first run mode
            if (!mInFirstRun) notifyLocationError(ERROR_DENIED)
            return
        }
        val oldInterval = interval
        mLogger.d(TAG, "Old time interval (ms): $oldInterval")
        calcLocationUpdatesInterval()
        mLogger.d(TAG, "start(), params: " + interval)
        startInternal()
    }

    /**
     * Stops the polling location updates, i.e. removes the [.mCoreLocationListener] and stops
     * the current active provider.
     */
    private fun stop() {
        mLogger.d(TAG, "stop()")
        checkProviderInitialization()
        if (!mLocationProvider!!.isActive) {
            mLogger.i(
                TAG,
                "Provider '$mLocationProvider' is already stopped"
            )
            return
        }
        removeListener(mCoreLocationListener)
        stopInternal()
    }

    /**
     * Actually starts location polling.
     */
    private fun startInternal() {
        mLogger.d(
            TAG,
            "startInternal(), current provider is '" + mLocationProvider
                    + "' , my position mode = " + LocationState.nameOf(myPositionMode)
                    + ", mInFirstRun = " + mInFirstRun
        )
        if (!PermissionsUtils.isLocationGranted()) {
            mLogger.w(
                TAG,
                "Dynamic permission ACCESS_COARSE_LOCATION/ACCESS_FINE_LOCATION is not granted",
                Throwable()
            )
            return
        }
        checkProviderInitialization()
        mLocationProvider!!.start()
        mLogger.d(
            TAG,
            if (mLocationProvider!!.isActive) "SUCCESS" else "FAILURE"
        )
    }

    private fun checkProviderInitialization() {
        if (mLocationProvider == null) {
            val error = "A location provider must be initialized!"
            mLogger.e(TAG, error, Throwable())
            throw AssertionError(error)
        }
    }

    /**
     * Actually stops location polling.
     */
    private fun stopInternal() {
        mLogger.d(TAG, "stopInternal()")
        checkProviderInitialization()
        mLocationProvider!!.stop()
        mSensorHelper.stop()
    }

    /**
     * Attach UI to helper.
     */
    @UiThread
    fun attach(callback: UiCallback) {
        mLogger.d(TAG, "attach() callback = $callback")
        if (mUiCallback != null) {
            mLogger.d(TAG, " - already attached. Skip.")
            return
        }
        mUiCallback = callback
        Utils.keepScreenOn(true, mUiCallback!!.activity.window)
        mUiCallback!!.onMyPositionModeChanged(myPositionMode)
        if (compassData != null) mUiCallback!!.onCompassUpdated(compassData!!)
        checkProviderInitialization()
        if (mLocationProvider!!.isActive) {
            mLogger.d(
                TAG,
                "attach() provider '$mLocationProvider' is active, just add the listener"
            )
            addListener(mCoreLocationListener, true)
        } else {
            restart()
        }
    }

    /**
     * Detach UI from helper.
     */
    @UiThread
    fun detach(delayed: Boolean) {
        mLogger.d(TAG, "detach(), delayed: $delayed")
        if (mUiCallback == null) {
            mLogger.d(TAG, " - already detached. Skip.")
            return
        }
        Utils.keepScreenOn(false, mUiCallback!!.activity.window)
        mUiCallback = null
        stop()
    }

    @UiThread
    fun onEnteredIntoFirstRun() {
        mLogger.i(TAG, "onEnteredIntoFirstRun")
        mInFirstRun = true
    }

    @UiThread
    fun onExitFromFirstRun() {
        mLogger.i(TAG, "onExitFromFirstRun")
        if (!mInFirstRun) throw AssertionError("Must be called only after 'onEnteredIntoFirstRun' method!")
        mInFirstRun = false
        if (myPositionMode != LocationState.NOT_FOLLOW_NO_POSITION) throw AssertionError(
            "My position mode must be equal NOT_FOLLOW_NO_POSITION"
        )
        // If there is a location we need just to pass it to the listeners, so that
// my position state machine will be switched to the FOLLOW state.
        val location = savedLocation
        if (location != null) {
            notifyLocationUpdated()
            mLogger.d(
                TAG,
                "Current location is available, so play the nice zoom animation"
            )
            Framework.nativeRunFirstLaunchAnimation()
            return
        }
        checkProviderInitialization()
        // If the location hasn't been obtained yet we need to switch to the next mode and wait for locations.
// Otherwise, try to restart location updates polling.
// noinspection ConstantConditions
        if (mLocationProvider!!.isActive) switchToNextMode() else restart()
    }

    /**
     * Obtains last known location regardless of "My position" button state.
     * @return `null` on failure.
     */
    val lastKnownLocation: Location?
        get() = if (savedLocation != null) savedLocation else AndroidNativeProvider.Companion.findBestLocation()

    @get:LocationState.Value
    val myPositionMode: Int
        get() = LocationState.nativeGetMode()

    interface UiCallback {
        val activity: Activity
        fun onMyPositionModeChanged(newMode: Int)
        fun onLocationUpdated(location: Location)
        fun onCompassUpdated(compass: CompassData)
        fun onLocationError()
        fun onLocationNotFound()
        fun onRoutingFinish()
    }

    companion object {
        // These constants should correspond to values defined in platform/location.hpp
// Leave 0-value as no any error.
        const val ERROR_NOT_SUPPORTED = 1
        const val ERROR_DENIED = 2
        const val ERROR_GPS_OFF = 3
        const val ERROR_UNKNOWN = 0
        private const val INTERVAL_FOLLOW_AND_ROTATE_MS: Long = 3000
        private const val INTERVAL_FOLLOW_MS: Long = 1000
        private const val INTERVAL_NOT_FOLLOW_MS: Long = 3000
        private const val INTERVAL_NAVIGATION_VEHICLE_MS: Long = 500
        // TODO (trashkalmar): Correct value
        private const val INTERVAL_NAVIGATION_BICYCLE_MS: Long = 1000
        private const val INTERVAL_NAVIGATION_PEDESTRIAN_MS: Long = 1000
        private val TAG = LocationHelper::class.java.simpleName
        @JvmStatic private external fun nativeOnLocationError(errorCode: Int)
        @JvmStatic private external fun nativeLocationUpdated(
            time: Long, lat: Double, lon: Double, accuracy: Float,
            altitude: Double, speed: Float, bearing: Float
        )

        @JvmStatic external fun nativeUpdateCompassSensor(
            ind: Int,
            arr: FloatArray?
        ): FloatArray?
    }
}