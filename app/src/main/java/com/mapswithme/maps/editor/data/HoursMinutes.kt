package com.mapswithme.maps.editor.data

import android.os.Parcelable
import android.text.format.DateFormat
import androidx.annotation.IntRange
import com.mapswithme.maps.MwmApplication
import kotlinx.android.parcel.Parcelize
import java.text.SimpleDateFormat
import java.util.*

@Parcelize
class HoursMinutes(
    @IntRange(
        from = 0,
        to = 23
    ) val hours: Long, @IntRange(from = 0, to = 59) val minutes: Long
) : Parcelable {
    private fun is24HourFormat(): Boolean {
        return DateFormat.is24HourFormat(MwmApplication.get())
    }

    override fun toString(): String {
        if (is24HourFormat()) return String.format(
            Locale.US,
            "%02d:%02d",
            hours,
            minutes
        )
        val calendar: Calendar = GregorianCalendar()
        calendar[Calendar.HOUR_OF_DAY] = hours.toInt()
        calendar[Calendar.MINUTE] = minutes.toInt()
        val fmt12 = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return fmt12.format(calendar.time)
    }

}