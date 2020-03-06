package com.mapswithme.maps.scheduling

import com.mapswithme.maps.background.NotificationService
import com.mapswithme.maps.background.WorkerService
import com.mapswithme.maps.bookmarks.SystemDownloadCompletedService
import com.mapswithme.maps.geofence.GeofenceTransitionsIntentService
import com.mapswithme.maps.location.TrackRecorderWakeService
import com.mapswithme.util.Utils
import java.util.*

object JobIdMap {
    private val MAP: MutableMap<Class<*>, Int> =
        HashMap()
    private const val ID_BASIC = 1070
    private const val JOB_TYPE_SHIFTS = 12
    private fun calcIdentifier(count: Int): Int {
        return (count + 1 shl JOB_TYPE_SHIFTS) + ID_BASIC
    }

    fun getId(clazz: Class<*>): Int {
        return MAP[clazz]
            ?: throw IllegalArgumentException("Value not found for args : $clazz")
    }

    init {
        MAP[if (Utils.isLollipopOrLater) NativeJobService::class.java else FirebaseJobService::class.java] =
            calcIdentifier(MAP.size)
        MAP[NotificationService::class.java] = calcIdentifier(MAP.size)
        MAP[TrackRecorderWakeService::class.java] = calcIdentifier(MAP.size)
        MAP[SystemDownloadCompletedService::class.java] = calcIdentifier(MAP.size)
        MAP[WorkerService::class.java] = calcIdentifier(MAP.size)
        MAP[GeofenceTransitionsIntentService::class.java] = calcIdentifier(MAP.size)
    }
}