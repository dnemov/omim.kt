package com.mapswithme.maps.maplayer.subway

import android.app.Application
import android.content.Context
import com.mapswithme.maps.Framework
import com.mapswithme.maps.MwmApplication


class SubwayManager(application: Application) {
    private val mSchemeChangedListener: OnTransitSchemeChangedListener

    var isEnabled: Boolean
        get() = Framework.nativeIsTransitSchemeEnabled()
        set(isEnabled) {
            if (isEnabled == isEnabled) return
            Framework.nativeSetTransitSchemeEnabled(isEnabled)
            Framework.nativeSaveSettingSchemeEnabled(isEnabled)
        }

    fun toggle() {
        isEnabled = !isEnabled
    }

    fun initialize() {
        registerListener()
    }

    private fun registerListener() {
        nativeAddListener(mSchemeChangedListener)
    }

    companion object {
        @kotlin.jvm.JvmStatic
        fun from(context: Context): SubwayManager {
            val app = context.applicationContext as MwmApplication
            return app.subwayManager
        }

        @JvmStatic private external fun nativeAddListener(listener: OnTransitSchemeChangedListener)
        @JvmStatic private external fun nativeRemoveListener(listener: OnTransitSchemeChangedListener)
    }

    init {
        mSchemeChangedListener =
            OnTransitSchemeChangedListener.Default(application)
    }
}