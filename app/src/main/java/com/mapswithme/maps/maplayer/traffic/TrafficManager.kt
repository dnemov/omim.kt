package com.mapswithme.maps.maplayer.traffic

import androidx.annotation.MainThread

import com.mapswithme.maps.maplayer.traffic.TrafficState.StateChangeListener
import com.mapswithme.util.log.LoggerFactory
import java.util.*

@MainThread
enum class TrafficManager {
    INSTANCE;

    private val mLogger =
        LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.TRAFFIC)
    private val mStateChangeListener: StateChangeListener = TrafficStateListener()
    private var mState = TrafficState.DISABLED
    private val mCallbacks: MutableList<TrafficCallback> =
        ArrayList()
    private var mInitialized = false
    fun initialize() {
        mLogger.d(
            TAG,
            "Initialization of traffic manager and setting the listener for traffic state changes"
        )
        TrafficState.Companion.nativeSetListener(mStateChangeListener)
        mInitialized = true
    }

    fun toggle() {
        checkInitialization()
        if (isEnabled) disable() else enable()
    }

    private fun enable() {
        mLogger.d(TAG, "Enable traffic")
        TrafficState.Companion.nativeEnable()
    }

    private fun disable() {
        checkInitialization()
        mLogger.d(TAG, "Disable traffic")
        TrafficState.Companion.nativeDisable()
    }

    var isEnabled: Boolean
        get() {
            checkInitialization()
            return TrafficState.Companion.nativeIsEnabled()
        }
        set(enabled) {
            checkInitialization()
            if (isEnabled == enabled) return
            if (enabled) enable() else disable()
        }

    fun attach(callback: TrafficCallback) {
        checkInitialization()
        check(!mCallbacks.contains(callback)) {
            ("A callback '" + callback
                    + "' is already attached. Check that the 'detachAll' method was called.")
        }
        mLogger.d(TAG, "Attach callback '$callback'")
        mCallbacks.add(callback)
        postPendingState()
    }

    private fun postPendingState() {
        mStateChangeListener.onTrafficStateChanged(mState.ordinal)
    }

    fun detachAll() {
        checkInitialization()
        if (mCallbacks.isEmpty()) {
            mLogger.w(
                TAG,
                "There are no attached callbacks. Invoke the 'detachAll' method " +
                        "only when it's really needed!",
                Throwable()
            )
            return
        }
        for (callback in mCallbacks) mLogger.d(
            TAG,
            "Detach callback '$callback'"
        )
        mCallbacks.clear()
    }

    private fun checkInitialization() {
        if (!mInitialized) throw AssertionError("Traffic manager is not initialized!")
    }

    private inner class TrafficStateListener : StateChangeListener {
        @MainThread
        override fun onTrafficStateChanged(index: Int) {
            val newTrafficState =
                TrafficState.values()[index]
            mLogger.d(
                TAG, "onTrafficStateChanged current state = " + mState
                        + " new value = " + newTrafficState
            )
            if (mState === newTrafficState) return
            mState = newTrafficState
            mState.activate(mCallbacks)
        }
    }

    interface TrafficCallback {
        fun onEnabled()
        fun onDisabled()
        fun onWaitingData()
        fun onOutdated()
        fun onNetworkError()
        fun onNoData()
        fun onExpiredData()
        fun onExpiredApp()
    }

    companion object {
        private val TAG = TrafficManager::class.java.simpleName
    }
}