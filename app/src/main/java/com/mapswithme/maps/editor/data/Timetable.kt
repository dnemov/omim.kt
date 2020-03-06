package com.mapswithme.maps.editor.data

import androidx.annotation.IntRange

class Timetable(
    val workingTimespan: Timespan,
    val closedTimespans: Array<Timespan>,
    val isFullday: Boolean,
    val weekdays: IntArray
) {
    fun containsWeekday(@IntRange(from = 1, to = 7) day: Int): Boolean {
        for (workingDay in weekdays) {
            if (workingDay == day) return true
        }
        return false
    }

    val isFullWeek: Boolean
        get() = weekdays.size == 7

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("Working timespan : ").append(workingTimespan).append("\n")
            .append("Closed timespans : ")
        for (timespan in closedTimespans) stringBuilder.append(timespan).append("   ")
        stringBuilder.append("\n")
        stringBuilder.append("Fullday : ").append(isFullday).append("\n")
            .append("Weekdays : ")
        for (i in weekdays) stringBuilder.append(i)
        return stringBuilder.toString()
    }

}