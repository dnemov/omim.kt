package com.mapswithme

import android.content.res.Resources
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import com.mapswithme.maps.R

object HotelUtils {
    fun formatStars(stars: Int, resources: Resources): CharSequence {
        var stars = stars
        if (stars <= 0) throw AssertionError("Start count must be > 0")
        stars = Math.min(stars, 5)
        // Colorize last dimmed stars
        val sb = SpannableStringBuilder("★ ★ ★ ★ ★")
        if (stars < 5) {
            val start = sb.length - ((5 - stars) * 2 - 1)
            sb.setSpan(
                ForegroundColorSpan(resources.getColor(R.color.search_star_dimmed)),
                start, sb.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }
        return sb
    }
}