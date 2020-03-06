package com.mapswithme.maps.location

import androidx.annotation.IntDef
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

object LocationState {
    // These values should correspond to location::EMyPositionMode enum (from platform/location.hpp)
    const val PENDING_POSITION = 0
    const val NOT_FOLLOW_NO_POSITION = 1
    const val NOT_FOLLOW = 2
    const val FOLLOW = 3
    const val FOLLOW_AND_ROTATE = 4
    @JvmStatic external fun nativeSwitchToNextMode()
    @Value
    @JvmStatic external fun nativeGetMode(): Int

    @JvmStatic external fun nativeSetListener(listener: ModeChangeListener?)
    @JvmStatic external fun nativeRemoveListener()
    @JvmStatic external fun nativeSetLocationPendingTimeoutListener(
        listener: LocationPendingTimeoutListener
    )

    @JvmStatic external fun nativeRemoveLocationPendingTimeoutListener()
    /**
     * Checks if location state on the map is active (so its not turned off or pending).
     */
    val isTurnedOn: Boolean
        get() = hasLocation(nativeGetMode())

    fun hasLocation(mode: Int): Boolean {
        return mode > NOT_FOLLOW_NO_POSITION
    }

    fun nameOf(@Value mode: Int): String {
        return when (mode) {
            PENDING_POSITION -> "PENDING_POSITION"
            NOT_FOLLOW_NO_POSITION -> "NOT_FOLLOW_NO_POSITION"
            NOT_FOLLOW -> "NOT_FOLLOW"
            FOLLOW -> "FOLLOW"
            FOLLOW_AND_ROTATE -> "FOLLOW_AND_ROTATE"
            else -> "Unknown: $mode"
        }
    }

    interface ModeChangeListener {
        fun onMyPositionModeChanged(newMode: Int)
    }

    interface LocationPendingTimeoutListener {
        fun onLocationPendingTimeout()
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        PENDING_POSITION,
        NOT_FOLLOW_NO_POSITION,
        NOT_FOLLOW,
        FOLLOW,
        FOLLOW_AND_ROTATE
    )
    internal annotation class Value
}