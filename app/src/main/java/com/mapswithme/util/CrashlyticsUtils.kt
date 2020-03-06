package com.mapswithme.util

import com.crashlytics.android.Crashlytics
import com.mapswithme.maps.MwmApplication
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language

object CrashlyticsUtils {
    fun logException(exception: Throwable) {
        if (!checkCrashlytics()) return
        Crashlytics.logException(exception)
    }

    fun log(priority: Int, tag: String, msg: String) {
        if (!checkCrashlytics()) return
        Crashlytics.log(priority, tag, msg)
    }

    private fun checkCrashlytics(): Boolean {
        val app: MwmApplication = MwmApplication.get()
        if (!app.mediator.isCrashlyticsEnabled) return false
        if (!app.mediator.isCrashlyticsInitialized) app.mediator.initCrashlytics()
        return true
    }
}