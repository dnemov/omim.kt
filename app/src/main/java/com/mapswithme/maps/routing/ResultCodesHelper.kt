package com.mapswithme.maps.routing

import android.util.Pair
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.maps.location.LocationHelper
import java.util.*

internal object ResultCodesHelper {
    // Codes correspond to native routing::RouterResultCode in routing/routing_callbacks.hpp
    const val NO_ERROR = 0
    const val CANCELLED = 1
    const val NO_POSITION = 2
    private const val INCONSISTENT_MWM_ROUTE = 3
    private const val ROUTING_FILE_NOT_EXIST = 4
    private const val START_POINT_NOT_FOUND = 5
    private const val END_POINT_NOT_FOUND = 6
    private const val DIFFERENT_MWM = 7
    private const val ROUTE_NOT_FOUND = 8
    private const val NEED_MORE_MAPS = 9
    private const val INTERNAL_ERROR = 10
    private const val FILE_TOO_OLD = 11
    private const val INTERMEDIATE_POINT_NOT_FOUND = 12
    private const val TRANSIT_ROUTE_NOT_FOUND_NO_NETWORK = 13
    private const val TRANSIT_ROUTE_NOT_FOUND_TOO_LONG_PEDESTRIAN = 14
    private const val ROUTE_NOT_FOUND_REDRESS_ROUTE_ERROR = 15
    const val HAS_WARNINGS = 16
    @kotlin.jvm.JvmStatic
    fun getDialogTitleSubtitle(
        errorCode: Int,
        missingCount: Int
    ): Pair<String, String> {
        val resources = MwmApplication.get().resources
        var titleRes = 0
        val messages: MutableList<String> =
            ArrayList()
        when (errorCode) {
            NO_POSITION -> if (!LocationHelper.INSTANCE.isActive) {
                titleRes = R.string.dialog_routing_location_turn_on
                messages.add(resources.getString(R.string.dialog_routing_location_unknown_turn_on))
            } else {
                titleRes = R.string.dialog_routing_check_gps
                messages.add(resources.getString(R.string.dialog_routing_error_location_not_found))
                messages.add(resources.getString(R.string.dialog_routing_location_turn_wifi))
            }
            INCONSISTENT_MWM_ROUTE, ROUTING_FILE_NOT_EXIST -> {
                titleRes = R.string.routing_download_maps_along
                messages.add(resources.getString(R.string.routing_requires_all_map))
            }
            START_POINT_NOT_FOUND -> {
                titleRes = R.string.dialog_routing_change_start
                messages.add(resources.getString(R.string.dialog_routing_start_not_determined))
                messages.add(resources.getString(R.string.dialog_routing_select_closer_start))
            }
            END_POINT_NOT_FOUND -> {
                titleRes = R.string.dialog_routing_change_end
                messages.add(resources.getString(R.string.dialog_routing_end_not_determined))
                messages.add(resources.getString(R.string.dialog_routing_select_closer_end))
            }
            INTERMEDIATE_POINT_NOT_FOUND -> {
                titleRes = R.string.dialog_routing_change_intermediate
                messages.add(resources.getString(R.string.dialog_routing_intermediate_not_determined))
            }
            DIFFERENT_MWM -> messages.add(resources.getString(R.string.routing_failed_cross_mwm_building))
            FILE_TOO_OLD -> {
                titleRes = R.string.downloader_update_maps
                messages.add(resources.getString(R.string.downloader_mwm_migration_dialog))
            }
            TRANSIT_ROUTE_NOT_FOUND_NO_NETWORK -> messages.add(
                resources.getString(
                    R.string.transit_not_found
                )
            )
            TRANSIT_ROUTE_NOT_FOUND_TOO_LONG_PEDESTRIAN -> messages.add(
                resources.getString(
                    R.string.dialog_pedestrian_route_is_long
                )
            )
            ROUTE_NOT_FOUND, ROUTE_NOT_FOUND_REDRESS_ROUTE_ERROR -> if (missingCount == 0) {
                titleRes = R.string.dialog_routing_unable_locate_route
                messages.add(resources.getString(R.string.dialog_routing_cant_build_route))
                messages.add(resources.getString(R.string.dialog_routing_change_start_or_end))
            } else {
                titleRes = R.string.routing_download_maps_along
                messages.add(resources.getString(R.string.routing_requires_all_map))
            }
            INTERNAL_ERROR -> {
                titleRes = R.string.dialog_routing_system_error
                messages.add(resources.getString(R.string.dialog_routing_application_error))
                messages.add(resources.getString(R.string.dialog_routing_try_again))
            }
            NEED_MORE_MAPS -> {
                titleRes = R.string.dialog_routing_download_and_build_cross_route
                messages.add(resources.getString(R.string.dialog_routing_download_cross_route))
            }
        }
        val builder = StringBuilder()
        for (messagePart in messages) {
            if (builder.length > 0) builder.append("\n\n")
            builder.append(messagePart)
        }
        return Pair(
            if (titleRes == 0) "" else resources.getString(
                titleRes
            ), builder.toString()
        )
    }

    @kotlin.jvm.JvmStatic
    fun isDownloadable(resultCode: Int, missingCount: Int): Boolean {
        if (missingCount <= 0) return false
        when (resultCode) {
            INCONSISTENT_MWM_ROUTE, ROUTING_FILE_NOT_EXIST, NEED_MORE_MAPS, ROUTE_NOT_FOUND, FILE_TOO_OLD -> return true
        }
        return false
    }

    @kotlin.jvm.JvmStatic
    fun isMoreMapsNeeded(resultCode: Int): Boolean {
        return resultCode == NEED_MORE_MAPS
    }
}