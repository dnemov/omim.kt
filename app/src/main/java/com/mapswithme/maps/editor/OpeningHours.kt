package com.mapswithme.maps.editor

import androidx.annotation.IntRange
import com.mapswithme.maps.editor.data.Timespan
import com.mapswithme.maps.editor.data.Timetable

object OpeningHours {
    @JvmStatic private external fun nativeInit()
    @JvmStatic external fun nativeGetDefaultTimetables(): Array<Timetable>
    @JvmStatic external fun nativeGetComplementTimetable(timetableSet: Array<Timetable>?): Timetable
    @JvmStatic external fun nativeAddTimetable(timetableSet: Array<Timetable>?): Array<Timetable>
    @JvmStatic external fun nativeRemoveTimetable(
        timetableSet: Array<Timetable>?,
        timetableIndex: Int
    ): Array<Timetable>

    @JvmStatic external fun nativeSetIsFullday(timetable: Timetable?, isFullday: Boolean): Timetable
    @JvmStatic external fun nativeAddWorkingDay(
        timetables: Array<Timetable>?,
        timetableIndex: Int, @IntRange(
            from = 1,
            to = 7
        ) day: Int
    ): Array<Timetable>

    @JvmStatic external fun nativeRemoveWorkingDay(
        timetables: Array<Timetable>?,
        timetableIndex: Int, @IntRange(
            from = 1,
            to = 7
        ) day: Int
    ): Array<Timetable>

    @JvmStatic external fun nativeSetOpeningTime(timetable: Timetable?, openingTime: Timespan?): Timetable
    @JvmStatic external fun nativeAddClosedSpan(timetable: Timetable?, closedSpan: Timespan?): Timetable
    @JvmStatic external fun nativeRemoveClosedSpan(timetable: Timetable?, spanIndex: Int): Timetable

    @JvmStatic external fun nativeTimetablesFromString(source: String?): Array<Timetable>?
    @JvmStatic external fun nativeTimetablesToString(timetables: Array<Timetable>): String
    /**
     * Sometimes timetables cannot be parsed with [.nativeTimetablesFromString] (hence can't be displayed in UI),
     * but still are valid OSM timetables.
     * @return true if timetable string is valid OSM timetable.
     */
    @JvmStatic external fun nativeIsTimetableStringValid(source: String?): Boolean

    init {
        nativeInit()
    }
}