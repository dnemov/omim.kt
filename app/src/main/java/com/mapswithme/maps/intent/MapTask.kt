package com.mapswithme.maps.intent

import com.mapswithme.maps.MwmActivity
import com.mapswithme.util.statistics.StatisticValueConverter
import java.io.Serializable

interface MapTask : Serializable, StatisticValueConverter<String?> {
    fun run(target: MwmActivity): Boolean
}