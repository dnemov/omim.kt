package com.mapswithme.maps.maplayer.subway

import android.content.Context
import android.widget.Toast
import com.mapswithme.maps.R

import com.mapswithme.util.statistics.Statistics

internal enum class TransitSchemeState {
    DISABLED, ENABLED {
        override fun activate(context: Context) {
            Statistics.INSTANCE.trackSubwayEvent(Statistics.ParamValue.SUCCESS)
        }
    },
    NO_DATA {
        override fun activate(context: Context) {
            Toast.makeText(context, R.string.subway_data_unavailable, Toast.LENGTH_SHORT).show()
            Statistics.INSTANCE.trackSubwayEvent(Statistics.ParamValue.UNAVAILABLE)
        }
    };

    open fun activate(context: Context) { /* Do nothing by default */
    }
}