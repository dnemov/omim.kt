package com.mapswithme.maps.discovery

import androidx.annotation.MainThread
import com.mapswithme.util.NetworkPolicy
import com.mapswithme.util.concurrency.UiThread

internal class Locals private constructor() {
    private var mListener: LocalsListener? = null
    fun setLocalsListener(listener: LocalsListener?) {
        mListener = listener
    }



    // Called from JNI.
    @MainThread
    fun onLocalsReceived(experts: Array<LocalExpert?>) {
        if (!UiThread.isUiThread) throw AssertionError("Must be called from UI thread!")
        if (mListener != null) mListener!!.onLocalsReceived(experts)
    }

    // Called from JNI.
    @MainThread
    fun onLocalsErrorReceived(error: LocalsError) {
        if (!UiThread.isUiThread) throw AssertionError("Must be called from UI thread!")
        if (mListener != null) mListener!!.onLocalsErrorReceived(error)
    }

    interface LocalsListener {
        fun onLocalsReceived(experts: Array<LocalExpert?>)
        fun onLocalsErrorReceived(error: LocalsError)
    }

    companion object {
        val INSTANCE = Locals()

        @JvmStatic external fun nativeRequestLocals(
            policy: NetworkPolicy,
            lat: Double, lon: Double
        )
    }
}