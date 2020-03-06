package com.mapswithme.util

import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language
import java.text.DateFormat
import java.util.*

object DateUtils {
    fun getMediumDateFormatter(): DateFormat {
        return DateFormat.getDateInstance(
            DateFormat.MEDIUM,
            Locale.getDefault()
        )
    }

    fun getShortDateFormatter(): DateFormat {
        return DateFormat.getDateInstance(
            DateFormat.SHORT,
            Locale.getDefault()
        )
    }
}