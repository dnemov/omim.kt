package com.mapswithme.maps.analytics

import android.app.Activity
import android.app.Application
import java.util.*

internal class EventLoggerAggregator(application: Application) :
    ContextDependentEventLogger(application) {
    private val mLoggers: MutableMap<Class<out EventLogger>?, EventLogger>
    override fun initialize() {
        for ((_, value) in mLoggers) {
            value.initialize()
        }
    }

    override fun sendTags(
        tag: String,
        params: Array<String>?
    ) {
        for ((_, value) in mLoggers) {
            value.sendTags(tag, params)
        }
    }

    override fun logEvent(
        event: String,
        params: Map<String?, String?>
    ) {
        for ((_, value) in mLoggers) {
            value.logEvent(event, params)
        }
    }

    override fun startActivity(context: Activity) {
        for ((_, value) in mLoggers) {
            value.startActivity(context)
        }
    }

    override fun stopActivity(context: Activity) {
        for ((_, value) in mLoggers) {
            value.stopActivity(context)
        }
    }

    init {
        mLoggers =
            HashMap()
        mLoggers[MyTrackerEventLogger::class.java] = MyTrackerEventLogger(application)
        mLoggers[FlurryEventLogger::class.java] = FlurryEventLogger(application)
    }
}