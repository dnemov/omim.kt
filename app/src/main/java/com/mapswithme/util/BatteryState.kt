package com.mapswithme.util

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import com.mapswithme.maps.MwmApplication

object BatteryState {
    const val CHARGING_STATUS_UNKNOWN: Byte = 0
    const val CHARGING_STATUS_PLUGGED: Byte = 1
    const val CHARGING_STATUS_UNPLUGGED: Byte = 2
    // Because it's a sticky intent, you don't need to register a BroadcastReceiver
// by simply calling registerReceiver passing in null
    val state: State
        get() {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            // Because it's a sticky intent, you don't need to register a BroadcastReceiver
// by simply calling registerReceiver passing in null
            val batteryStatus: Intent = MwmApplication.get().registerReceiver(null, filter)
                ?: return State(
                    0,
                    CHARGING_STATUS_UNKNOWN.toInt()
                )
            return State(
                getLevel(
                    batteryStatus
                ), getChargingStatus(batteryStatus)
            )
        }

    @get:IntRange(from = 0, to = 100)
    val level: Int
        get() = state.level

    @ChargingStatus
    val chargingStatus: Int
        @JvmStatic
        get() = state.chargingStatus

    @IntRange(from = 0, to = 100)
    private fun getLevel(batteryStatus: Intent): Int {
        return batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
    }

    @ChargingStatus
    private fun getChargingStatus(batteryStatus: Intent): Int { // Extra for {@link android.content.Intent#ACTION_BATTERY_CHANGED}:
// integer indicating whether the device is plugged in to a power
// source; 0 means it is on battery, other constants are different
// types of power sources.
        val chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        if (chargePlug > 0) return CHARGING_STATUS_PLUGGED.toInt() else if (chargePlug < 0) return CHARGING_STATUS_UNKNOWN.toInt()
        return CHARGING_STATUS_UNPLUGGED.toInt()
    }

    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @IntDef(
        CHARGING_STATUS_UNKNOWN.toInt(),
        CHARGING_STATUS_PLUGGED.toInt(),
        CHARGING_STATUS_UNPLUGGED.toInt()
    )


    annotation class ChargingStatus

    class State(
        @field:IntRange(from = 0, to = 100) @get:IntRange(from = 0, to = 100)
        @param:IntRange(
            from = 0,
            to = 100
        ) val level: Int, @field:ChargingStatus @get:ChargingStatus
        @param:ChargingStatus val chargingStatus: Int
    )
}