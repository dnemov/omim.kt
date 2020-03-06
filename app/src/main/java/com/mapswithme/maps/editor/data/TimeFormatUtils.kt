package com.mapswithme.maps.editor.data

import androidx.annotation.IntRange
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.util.Utils
import java.text.DateFormatSymbols
import java.util.*

object TimeFormatUtils {
    private lateinit var sShortWeekdays: Array<String>
    private var sCurrentLocale: Locale? = null
    private fun refreshWithCurrentLocale() {
        if (Locale.getDefault() != sCurrentLocale) {
            sCurrentLocale = Locale.getDefault()
            sShortWeekdays =
                DateFormatSymbols.getInstance().shortWeekdays
            for (i in sShortWeekdays.indices) {
                sShortWeekdays[i] =
                    Utils.capitalize(sShortWeekdays[i])
            }
        }
    }

    fun formatShortWeekday(
        @IntRange(
            from = 1,
            to = 7
        ) day: Int
    ): String {
        refreshWithCurrentLocale()
        return sShortWeekdays[day]
    }

    fun formatWeekdays(timetable: Timetable): String {
        refreshWithCurrentLocale()
        val weekdays = timetable.weekdays
        if (weekdays!!.size == 0) return ""
        val builder =
            StringBuilder(sShortWeekdays[weekdays[0]])
        var iteratingRange: Boolean
        var i = 1
        while (i < weekdays.size) {
            iteratingRange = weekdays[i] == weekdays[i - 1] + 1
            if (iteratingRange) {
                while (i < weekdays.size && weekdays[i] == weekdays[i - 1] + 1) i++
                builder.append("-").append(sShortWeekdays[weekdays[i - 1]])
                continue
            }
            if (i < weekdays.size) builder.append(", ").append(
                sShortWeekdays[weekdays[i]]
            )
            i++
        }
        return builder.toString()
    }

    @kotlin.jvm.JvmStatic
    fun formatTimetables(timetables: Array<Timetable>): String {
        val resources = MwmApplication.get().resources
        if (timetables[0]!!.isFullWeek) {
            return if (timetables[0]!!.isFullday) resources.getString(R.string.twentyfour_seven) else resources.getString(
                R.string.daily
            ) + " " + timetables[0]!!.workingTimespan
        }
        val builder = StringBuilder()
        for (tt in timetables) {
            val workingTime =
                if (tt.isFullday) resources.getString(R.string.editor_time_allday) else tt.workingTimespan.toString()
            builder.append(
                String.format(
                    Locale.getDefault(),
                    "%-21s",
                    formatWeekdays(tt)
                )
            ).append("   ")
                .append(workingTime)
                .append("\n")
        }
        return builder.toString()
    }
}