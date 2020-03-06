package com.mapswithme.util

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.text.TextUtils
import android.view.Surface
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language
import com.mapswithme.util.log.Logger
import com.mapswithme.util.log.LoggerFactory

object LocationUtils {
    private val LOGGER: Logger = LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.LOCATION)
    private val TAG = LocationUtils::class.java.simpleName
    /**
     * Correct compass angles due to display orientation.
     */
    fun correctCompassAngle(displayOrientation: Int, angle: Double): Double {
        var angle = angle
        var correction = 0.0
        when (displayOrientation) {
            Surface.ROTATION_90 -> correction = Math.PI / 2.0
            Surface.ROTATION_180 -> correction = Math.PI
            Surface.ROTATION_270 -> correction = 3.0 * Math.PI / 2.0
        }
        // negative values (like -1.0) should remain negative (indicates that no direction available)
        if (angle >= 0.0) angle = correctAngle(angle, correction)
        return angle
    }

    @JvmStatic
    fun correctAngle(angle: Double, correction: Double): Double {
        var res = angle + correction
        val twoPI = 2.0 * Math.PI
        res %= twoPI
        // normalize angle into [0, 2PI]
        if (res < 0.0) res += twoPI
        return res
    }

    fun isExpired(
        l: Location,
        millis: Long,
        expirationMillis: Long
    ): Boolean {
        val timeDiff: Long
        timeDiff =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) (SystemClock.elapsedRealtimeNanos() - l.elapsedRealtimeNanos) / 1000000 else System.currentTimeMillis() - millis
        return timeDiff > expirationMillis
    }

    fun getDiff(
        lastLocation: Location,
        newLocation: Location
    ): Double {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) (newLocation.elapsedRealtimeNanos - lastLocation.elapsedRealtimeNanos) * 1.0E-9 else {
            var time = newLocation.time
            var lastTime = lastLocation.time
            if (!isSameLocationProvider(
                    newLocation.provider,
                    lastLocation.provider
                )
            ) { // Do compare current and previous system times in case when
// we have incorrect time settings on a device.
                time = System.currentTimeMillis()
                lastTime = LocationHelper.INSTANCE.savedLocationTime
            }
            (time - lastTime) * 1.0E-3
        }
    }

    private fun isSameLocationProvider(p1: String?, p2: String): Boolean {
        return p1 != null && p1 == p2
    }

    @SuppressLint("InlinedApi")
    fun areLocationServicesTurnedOn(): Boolean {
        val resolver: ContentResolver = MwmApplication.get().getContentResolver()
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) !TextUtils.isEmpty(
                Settings.Secure.getString(
                    resolver,
                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED
                )
            ) else Settings.Secure.getInt(
                resolver,
                Settings.Secure.LOCATION_MODE
            ) != Settings.Secure.LOCATION_MODE_OFF
        } catch (e: SettingNotFoundException) {
            e.printStackTrace()
            false
        }
    }

    private fun logAvailableProviders() {
        val locMngr =
            MwmApplication.get().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = locMngr.getProviders(true)
        val sb: StringBuilder
        if (!providers.isEmpty()) {
            sb = StringBuilder("Available location providers:")
            for (provider in providers) sb.append(" ").append(provider)
        } else {
            sb = StringBuilder("There are no enabled location providers!")
        }
        LOGGER.i(
            TAG,
            sb.toString()
        )
    }

    /**
     *
     * Use [.checkProvidersAvailability] instead.
     */
    @Deprecated("")
    fun checkProvidersAvailability(): Boolean {
        return checkProvidersAvailability(MwmApplication.get())
    }

    fun checkProvidersAvailability(application: Application): Boolean {
        val locationManager =
            application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (locationManager == null) {
            LOGGER.e(
                TAG,
                "This device doesn't support the location service."
            )
            return false
        }
        val networkEnabled =
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        val gpsEnabled =
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        logAvailableProviders()
        return networkEnabled || gpsEnabled
    }
}