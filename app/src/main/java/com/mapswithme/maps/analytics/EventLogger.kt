package com.mapswithme.maps.analytics

import android.app.Activity

interface EventLogger {
    fun initialize()
    fun sendTags(tag: String, params: Array<String>?)
    fun logEvent(
        event: String,
        params: Map<String?, String?>
    )

    fun startActivity(context: Activity)
    fun stopActivity(context: Activity)
}