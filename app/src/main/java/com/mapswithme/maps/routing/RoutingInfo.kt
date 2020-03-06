package com.mapswithme.maps.routing

import android.location.Location
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.mapswithme.maps.R
import com.mapswithme.maps.bookmarks.data.DistanceAndAzimut
import com.mapswithme.maps.routing.RoutingInfo.CarDirection
import com.mapswithme.maps.routing.RoutingInfo.PedestrianTurnDirection

class RoutingInfo(
    // Target (end point of route).
    val distToTarget: String,
    val targetUnits: String,
    // Next turn.
    val distToTurn: String,
    val turnUnits: String,
    // Current street name.
    val currentStreet: String,
    // The next street name.
    val nextStreet: String,
    val completionPercent: Double,
    vehicleTurnOrdinal: Int,
    vehicleNextTurnOrdinal: Int,
    pedestrianTurnOrdinal: Int,
    pedestrianDirectionLat: Double,
    pedestrianDirectionLon: Double,
    exitNum: Int,
    val totalTimeInSeconds: Int,
    lanes: Array<SingleLaneInfo>?,
    speedLimitExceeded: Boolean,
    shouldPlayWarningSignal: Boolean
) {
    // For vehicle routing.
    val carDirection: CarDirection
    val nextCarDirection: CarDirection
    val exitNum: Int
    val lanes: Array<SingleLaneInfo>?
    // For pedestrian routing.
    val pedestrianTurnDirection: PedestrianTurnDirection
    val isSpeedLimitExceeded: Boolean
    private val shouldPlayWarningSignal: Boolean
    val pedestrianNextDirection: Location

    /**
     * IMPORTANT : Order of enum values MUST BE the same as native CarDirection enum.
     */
    enum class CarDirection(@param:DrawableRes private val mTurnRes: Int, @param:DrawableRes private val mNextTurnRes: Int) {
        NO_TURN(R.drawable.ic_turn_straight, 0), GO_STRAIGHT(
            R.drawable.ic_turn_straight,
            0
        ),
        TURN_RIGHT(
            R.drawable.ic_turn_right,
            R.drawable.ic_then_right
        ),
        TURN_SHARP_RIGHT(
            R.drawable.ic_turn_right_sharp,
            R.drawable.ic_then_right_sharp
        ),
        TURN_SLIGHT_RIGHT(
            R.drawable.ic_turn_right_slight,
            R.drawable.ic_then_right_slight
        ),
        TURN_LEFT(
            R.drawable.ic_turn_left,
            R.drawable.ic_then_left
        ),
        TURN_SHARP_LEFT(
            R.drawable.ic_turn_left_sharp,
            R.drawable.ic_then_left_sharp
        ),
        TURN_SLIGHT_LEFT(
            R.drawable.ic_turn_left_slight,
            R.drawable.ic_then_left_slight
        ),
        U_TURN_LEFT(
            R.drawable.ic_turn_uleft,
            R.drawable.ic_then_uleft
        ),
        U_TURN_RIGHT(
            R.drawable.ic_turn_uright,
            R.drawable.ic_then_uright
        ),
        ENTER_ROUND_ABOUT(
            R.drawable.ic_turn_round,
            R.drawable.ic_then_round
        ),
        LEAVE_ROUND_ABOUT(
            R.drawable.ic_turn_round,
            R.drawable.ic_then_round
        ),
        STAY_ON_ROUND_ABOUT(
            R.drawable.ic_turn_round,
            R.drawable.ic_then_round
        ),
        START_AT_THE_END_OF_STREET(0, 0), REACHED_YOUR_DESTINATION(
            R.drawable.ic_turn_finish,
            R.drawable.ic_then_finish
        ),
        EXIT_HIGHWAY_TO_LEFT(
            R.drawable.ic_exit_highway_to_left,
            R.drawable.ic_then_exit_highway_to_left
        ),
        EXIT_HIGHWAY_TO_RIGHT(
            R.drawable.ic_exit_highway_to_right,
            R.drawable.ic_then_exit_highway_to_right
        );

        fun setTurnDrawable(imageView: ImageView) {
            imageView.setImageResource(mTurnRes)
            imageView.rotation = 0.0f
        }

        fun setNextTurnDrawable(imageView: ImageView) {
            imageView.setImageResource(mNextTurnRes)
        }

        fun containsNextTurn(): Boolean {
            return mNextTurnRes != 0
        }

        companion object {
            fun isRoundAbout(turn: CarDirection): Boolean {
                return turn == ENTER_ROUND_ABOUT || turn == LEAVE_ROUND_ABOUT || turn == STAY_ON_ROUND_ABOUT
            }
        }

    }

    enum class PedestrianTurnDirection {
        NONE, UPSTAIRS, DOWNSTAIRS, LIFT_GATE, GATE, REACHED_YOUR_DESTINATION;

        companion object {
            fun setTurnDrawable(
                view: ImageView,
                distanceAndAzimut: DistanceAndAzimut
            ) {
                view.setImageResource(R.drawable.ic_turn_direction)
                view.rotation = Math.toDegrees(distanceAndAzimut.azimuth).toFloat()
            }
        }
    }

    /**
     * IMPORTANT : Order of enum values MUST BE the same
     * with native LaneWay enum (see routing/turns.hpp for details).
     * Information for every lane is composed of some number values below.
     * For example, a lane may have THROUGH and RIGHT values.
     */
    enum class LaneWay {
        NONE, REVERSE, SHARP_LEFT, LEFT, SLIGHT_LEFT, MERGE_TO_RIGHT, THROUGH, MERGE_TO_LEFT, SLIGHT_RIGHT, RIGHT, SHARP_RIGHT
    }

    fun shouldPlayWarningSignal(): Boolean {
        return shouldPlayWarningSignal
    }

    init {
        carDirection =
            CarDirection.values()[vehicleTurnOrdinal]
        nextCarDirection = CarDirection.values()[vehicleNextTurnOrdinal]
        this.lanes = lanes
        this.exitNum = exitNum
        pedestrianTurnDirection =
            PedestrianTurnDirection.values()[pedestrianTurnOrdinal]
        isSpeedLimitExceeded = speedLimitExceeded
        this.shouldPlayWarningSignal = shouldPlayWarningSignal
        pedestrianNextDirection = Location("")
        pedestrianNextDirection.latitude = pedestrianDirectionLat
        pedestrianNextDirection.longitude = pedestrianDirectionLon
    }
}