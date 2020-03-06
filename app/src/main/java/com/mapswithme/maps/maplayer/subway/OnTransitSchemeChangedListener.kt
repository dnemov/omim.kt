package com.mapswithme.maps.maplayer.subway

import android.app.Application
import androidx.annotation.MainThread

import com.mapswithme.maps.content.AbstractContextualListener

internal interface OnTransitSchemeChangedListener {
    @MainThread
    fun onTransitStateChanged(type: Int)

    class Default(context: Application) :
        AbstractContextualListener(context), OnTransitSchemeChangedListener {
        override fun onTransitStateChanged(index: Int) {
            val app = context
            val state =
                TransitSchemeState.values()[index]
            state.activate(app)
        }
    }
}