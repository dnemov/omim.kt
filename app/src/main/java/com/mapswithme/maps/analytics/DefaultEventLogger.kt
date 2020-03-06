package com.mapswithme.maps.analytics

import android.app.Activity
import android.app.Application

internal open class DefaultEventLogger(application: Application) :
    ContextDependentEventLogger(application) {
    override fun initialize() { /* Do nothing */
    }

    override fun sendTags(
        tag: String,
        params: Array<String>?
    ) { /* Do nothing */
    }

    override fun logEvent(
        event: String,
        params: Map<String?, String?>
    ) { /* Do nothing */
    }

    override fun startActivity(context: Activity) { /* Do nothing */
    }

    override fun stopActivity(context: Activity) { /* Do nothing */
    }
}