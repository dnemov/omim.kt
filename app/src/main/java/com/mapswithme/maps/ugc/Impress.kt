package com.mapswithme.maps.ugc

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

import com.mapswithme.maps.R

enum class Impress private constructor(
    @param:DrawableRes @field:DrawableRes
    @get:DrawableRes
    val drawableId: Int, @param:ColorRes @field:ColorRes
    @get:ColorRes
    val textColor: Int, @param:ColorRes @field:ColorRes
    @get:ColorRes
    val bgColor: Int = textColor
) {
    NONE(R.drawable.ic_24px_rating_normal, R.color.rating_none),
    HORRIBLE(R.drawable.ic_24px_rating_horrible, R.color.rating_horrible),
    BAD(R.drawable.ic_24px_rating_bad, R.color.rating_bad),
    NORMAL(R.drawable.ic_24px_rating_normal, R.color.rating_normal),
    GOOD(R.drawable.ic_24px_rating_good, R.color.rating_good),
    EXCELLENT(R.drawable.ic_24px_rating_excellent, R.color.rating_excellent),
    COMING_SOON(R.drawable.ic_24px_rating_coming_soon, R.color.rating_coming_soon),
    POPULAR(R.drawable.ic_thumb_up, R.color.rating_coming_soon),
    DISCOUNT(R.drawable.ic_thumb_up, android.R.color.white, R.color.rating_coming_soon)
}
