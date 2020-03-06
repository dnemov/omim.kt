package com.mapswithme.util

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import com.mapswithme.maps.MwmApplication
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language
import com.mapswithme.util.permissions.PermissionsResult
import java.util.*

object PermissionsUtils {
    private val PERMISSIONS = arrayOf(
        permission.WRITE_EXTERNAL_STORAGE,
        permission.ACCESS_COARSE_LOCATION,
        permission.ACCESS_FINE_LOCATION
    )
    private val LOCATION_PERMISSIONS = arrayOf(
        permission.ACCESS_COARSE_LOCATION,
        permission.ACCESS_FINE_LOCATION
    )

    fun computePermissionsResult(
        permissions: Array<String>,
        grantResults: IntArray
    ): PermissionsResult {
        val result: MutableMap<String, Boolean> =
            HashMap()
        for (i in permissions.indices) {
            result[permissions[i]] = grantResults[i] == PackageManager.PERMISSION_GRANTED
        }
        return getPermissionsResult(result)
    }

    fun isLocationGranted(context: Context): Boolean {
        return checkPermissions(context).isLocationGranted
    }

    /**
     *
     * Use [.isLocationGranted] instead.
     */
    @Deprecated("")
    fun isLocationGranted(): Boolean {
        return checkPermissions(MwmApplication.get())
            .isLocationGranted
    }

    fun isLocationExplanationNeeded(activity: Activity): Boolean {
        return (shouldShowRequestPermissionRationale(activity, permission.ACCESS_COARSE_LOCATION)
                || shouldShowRequestPermissionRationale(activity, permission.ACCESS_FINE_LOCATION))
    }

    fun isExternalStorageGranted(): Boolean {
        return checkPermissions(MwmApplication.get())
            .isExternalStorageGranted
    }

    private fun checkPermissions(context: Context): PermissionsResult {
        val appContext = context.applicationContext
        val result: MutableMap<String, Boolean> =
            HashMap()
        for (permission in PERMISSIONS) {
            result[permission] = (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                    || appContext.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED)
        }
        return getPermissionsResult(result)
    }

    private fun getPermissionsResult(result: Map<String, Boolean>): PermissionsResult {
        val externalStorageGranted =
            if (result.containsKey(permission.WRITE_EXTERNAL_STORAGE)) result[permission.WRITE_EXTERNAL_STORAGE]!! else false
        val locationGranted =
            ((if (result.containsKey(permission.ACCESS_COARSE_LOCATION)) result[permission.ACCESS_COARSE_LOCATION]!! else false)
                    || if (result.containsKey(permission.ACCESS_FINE_LOCATION)) result[permission.ACCESS_FINE_LOCATION]!! else false)
        return PermissionsResult(externalStorageGranted, locationGranted)
    }

    fun requestPermissions(activity: Activity, code: Int) {
        ActivityCompat.requestPermissions(
            activity,
            PERMISSIONS,
            code
        )
    }

    fun requestLocationPermission(activity: Activity, code: Int) {
        ActivityCompat.requestPermissions(
            activity,
            LOCATION_PERMISSIONS,
            code
        )
    }
}