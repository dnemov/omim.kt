package com.mapswithme.maps.analytics

import android.app.Activity
import android.app.Application
import android.util.Log
import com.flurry.android.FlurryAgent
import com.mapswithme.maps.BuildConfig
import com.mapswithme.maps.PrivateVariables

internal class FlurryEventLogger(application: Application) :
    DefaultEventLogger(application) {
    override fun initialize() {
        FlurryAgent.setVersionName(BuildConfig.VERSION_NAME)
        FlurryAgent.Builder()
            .withLogEnabled(true)
            .withLogLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
            .withCaptureUncaughtExceptions(false)
            .build(application, PrivateVariables.flurryKey())
    }

    override fun logEvent(
        event: String,
        params: Map<String?, String?>
    ) {
        super.logEvent(event, params)
        FlurryAgent.logEvent(event, params)
    }

    override fun startActivity(context: Activity) {
        super.startActivity(context)
        FlurryAgent.onStartSession(context.applicationContext)
    }

    override fun stopActivity(context: Activity) {
        super.stopActivity(context)
        FlurryAgent.onEndSession(context.applicationContext)
    }
}