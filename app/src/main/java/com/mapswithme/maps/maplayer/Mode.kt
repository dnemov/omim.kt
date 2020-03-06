package com.mapswithme.maps.maplayer

import android.content.Context

import com.mapswithme.maps.maplayer.subway.SubwayManager
import com.mapswithme.maps.maplayer.traffic.TrafficManager

enum class Mode {
    TRAFFIC {
        override fun isEnabled(context: Context): Boolean {
            return (!SubwayManager.Companion.from(context).isEnabled
                    && TrafficManager.INSTANCE.isEnabled)
        }

        override fun setEnabled(
            context: Context,
            isEnabled: Boolean
        ) {
            TrafficManager.INSTANCE.isEnabled = isEnabled
        }

        override fun toggle(context: Context) {
            TrafficManager.INSTANCE.toggle()
            SubwayManager.Companion.from(context).isEnabled = false
        }
    },
    SUBWAY {
        override fun isEnabled(context: Context): Boolean {
            return SubwayManager.Companion.from(context).isEnabled
        }

        override fun setEnabled(
            context: Context,
            isEnabled: Boolean
        ) {
            SubwayManager.Companion.from(context).isEnabled = isEnabled
        }

        override fun toggle(context: Context) {
            SubwayManager.Companion.from(context).toggle()
            TrafficManager.INSTANCE.isEnabled = false
        }
    };

    abstract fun isEnabled(context: Context): Boolean
    abstract fun setEnabled(
        context: Context,
        isEnabled: Boolean
    )

    abstract fun toggle(context: Context)
}