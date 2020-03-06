package com.mapswithme.maps.analytics

import android.app.Activity
import android.app.Application
import com.mapswithme.maps.BuildConfig
import com.mapswithme.maps.PrivateVariables
import com.mapswithme.util.PermissionsUtils
import com.my.tracker.MyTracker

internal class MyTrackerEventLogger(application: Application) :
    ContextDependentEventLogger(application) {
    override fun initialize() {
        initTracker()
    }

    override fun sendTags(
        tag: String,
        params: Array<String>?
    ) { /* Do nothing */
    }

    override fun logEvent(
        event: String,
        params: Map<String?, String?>
    ) {
        MyTracker.trackEvent(event, params)
    }

    override fun startActivity(context: Activity) {
        MyTracker.onStartActivity(context)
    }

    override fun stopActivity(context: Activity) {
        MyTracker.onStopActivity(context)
    }

    private fun initTracker() {
        MyTracker.setDebugMode(BuildConfig.DEBUG)
        MyTracker.createTracker(PrivateVariables.myTrackerKey(), application)
        val myParams = MyTracker.getTrackerParams()
        if (myParams != null) {
            myParams.setDefaultVendorAppPackage()
            val isLocationGranted = PermissionsUtils.isLocationGranted(application)
            myParams.isTrackingLocationEnabled = isLocationGranted
            myParams.isTrackingEnvironmentEnabled = isLocationGranted
        }
        MyTracker.initTracker()
    }
}