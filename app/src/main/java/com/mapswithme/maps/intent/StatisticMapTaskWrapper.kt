package com.mapswithme.maps.intent

import com.mapswithme.maps.MwmActivity
import com.mapswithme.maps.MwmApplication
import com.mapswithme.util.statistics.Statistics

class StatisticMapTaskWrapper private constructor(private val mMapTask: MapTask) : MapTask {
    override fun run(target: MwmActivity): Boolean {
        val success = mMapTask.run(target)
        val firstLaunch = MwmApplication.from(target).isFirstLaunch
        if (success) Statistics.INSTANCE.trackDeeplinkEvent(
            Statistics.EventName.DEEPLINK_CALL,
            mMapTask.toStatisticValue()!!, firstLaunch
        ) else Statistics.INSTANCE.trackDeeplinkEvent(
            Statistics.EventName.DEEPLINK_CALL_MISSED,
            toStatisticValue(), firstLaunch
        )
        return success
    }

    override fun toStatisticValue(): String {
        return mMapTask.toStatisticValue()!!
    }

    companion object {
        private const val serialVersionUID = 7604577952712453816L
        fun wrap(task: MapTask): MapTask {
            return StatisticMapTaskWrapper(task)
        }
    }

}