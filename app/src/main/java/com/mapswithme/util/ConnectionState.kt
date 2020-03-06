package com.mapswithme.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import android.telephony.TelephonyManager
import com.mapswithme.maps.MwmApplication.Companion.get
import com.mapswithme.util.ConnectionState.Type.NONE


object ConnectionState {
    private var connectivityManager: ConnectivityManager? = null
    // values should correspond to ones from enum class EConnectionType (in platform/platform.hpp)
    private const val CONNECTION_NONE: Byte = 0
    private const val CONNECTION_WIFI: Byte = 1
    private const val CONNECTION_WWAN: Byte = 2
    /**
     * Use the [.isNetworkConnected] method instead.
     */
    private fun isNetworkConnected(networkType: Int): Boolean {

        if (connectivityManager == null) {
            val context = get()
            updateConnectivityManager(context)
        }

        if (connectivityManager != null) {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val capabilities: NetworkCapabilities? =
                    connectivityManager!!.getNetworkCapabilities(connectivityManager!!.activeNetwork)

                capabilities != null && capabilities.hasTransport(networkType)
            } else {
                val info = connectivityManager!!.activeNetworkInfo
                val type: Int? = when (networkType) {
                    NetworkCapabilities.TRANSPORT_CELLULAR -> ConnectivityManager.TYPE_MOBILE
                    NetworkCapabilities.TRANSPORT_WIFI -> ConnectivityManager.TYPE_WIFI
                    else -> null

                }
                info != null &&  type!=null && info.type == type && info.isConnected
            }
        } else {
            return false
        }
    }

    private fun isNetworkConnected(
        context: Context,
        networkType: Int
    ): Boolean {
        updateConnectivityManager(context)
        return isNetworkConnected(networkType)
    }

    private fun updateConnectivityManager(context: Context) {
        connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    val isMobileConnected: Boolean
        get() = isNetworkConnected(NetworkCapabilities.TRANSPORT_CELLULAR)

    val isWifiConnected: Boolean
        get() = isNetworkConnected(NetworkCapabilities.TRANSPORT_WIFI)

    @JvmStatic
    val isConnected: Boolean
        get() = isNetworkConnected(NetworkCapabilities.TRANSPORT_WIFI) || isNetworkConnected(
            NetworkCapabilities.TRANSPORT_CELLULAR
        )

    fun isConnectionFast(info: NetworkInfo?): Boolean {
        if (info == null || !info.isConnected) return false
        val type = info.type
        val subtype = info.subtype
        if (type == ConnectivityManager.TYPE_WIFI) return true
        return if (type == ConnectivityManager.TYPE_MOBILE) {
            when (subtype) {
                TelephonyManager.NETWORK_TYPE_IDEN, TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_UNKNOWN -> false
                TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_LTE -> true
                else -> true
            }
        } else false
    }

    val isInRoaming: Boolean
        get() {
            val info = connectivityManager?.activeNetworkInfo
            return info != null && info.isRoaming
        }

    // Called from JNI.
    @JvmStatic
    val connectionState: Byte
        get() = requestCurrentType().nativeRepresentation

    /**
     * Use the [.requestCurrentType] method instead.
     */
    @Deprecated("")
    fun requestCurrentType(): Type {
        for (each in Type.values()) {
            if (isNetworkConnected(each.platformRepresentation)) return each
        }
        return NONE
    }

    fun requestCurrentType(context: Context): Type {
        for (each in Type.values()) {
            if (isNetworkConnected(
                    context,
                    each.platformRepresentation
                )
            ) return each
        }
        return NONE
    }

    enum class Type(
        val nativeRepresentation: Byte,
        val platformRepresentation: Int
    ) {
        NONE(CONNECTION_NONE, -1), WIFI(
            CONNECTION_WIFI,
            NetworkCapabilities.TRANSPORT_WIFI
        ),
        WWAN(CONNECTION_WWAN, NetworkCapabilities.TRANSPORT_CELLULAR);

    }
}