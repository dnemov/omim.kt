package com.mapswithme.maps.maplayer.traffic

import androidx.annotation.MainThread

import com.mapswithme.maps.maplayer.traffic.TrafficManager.TrafficCallback
import com.mapswithme.util.statistics.Statistics

internal enum class TrafficState {
    DISABLED {
        override fun activateInternal(callback: TrafficCallback) {
            callback.onDisabled()
        }
    },
    ENABLED(Statistics.ParamValue.SUCCESS) {
        override fun activateInternal(callback: TrafficCallback) {
            callback.onEnabled()
        }
    },
    WAITING_DATA {
        override fun activateInternal(callback: TrafficCallback) {
            callback.onWaitingData()
        }
    },
    OUTDATED {
        override fun activateInternal(callback: TrafficCallback) {
            callback.onOutdated()
        }
    },
    NO_DATA(Statistics.ParamValue.UNAVAILABLE) {
        override fun activateInternal(callback: TrafficCallback) {
            callback.onNoData()
        }
    },
    NETWORK_ERROR(Statistics.EventParam.ERROR) {
        override fun activateInternal(callback: TrafficCallback) {
            callback.onNetworkError()
        }
    },
    EXPIRED_DATA {
        override fun activateInternal(callback: TrafficCallback) {
            callback.onExpiredData()
        }
    },
    EXPIRED_APP {
        override fun activateInternal(callback: TrafficCallback) {
            callback.onExpiredApp()
        }
    };

    private val analyticsParamName: String

    constructor() {
        analyticsParamName = name
    }

    constructor(analyticsParamName: String) {
        this.analyticsParamName = analyticsParamName
    }

    fun activate(trafficCallbacks: List<TrafficCallback>) {
        for (callback in trafficCallbacks) {
            activateInternal(callback)
        }
        Statistics.INSTANCE.trackTrafficEvent(analyticsParamName)
    }

    protected abstract fun activateInternal(callback: TrafficCallback)
    internal interface StateChangeListener {
        // This method is called from JNI layer.
        @MainThread
        fun onTrafficStateChanged(state: Int)
    }

    companion object {
        @MainThread
        @JvmStatic external fun nativeSetListener(listener: StateChangeListener)

        @JvmStatic external fun nativeRemoveListener()
        @JvmStatic external fun nativeEnable()
        @JvmStatic external fun nativeDisable()
        @JvmStatic external fun nativeIsEnabled(): Boolean
    }
}