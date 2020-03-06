package com.mapswithme.maps.promo

import android.graphics.Color
import androidx.annotation.ColorInt


class PromoCityGallery internal constructor(
    val items: Array<Item>,
    val moreUrl: String,
    val category: String
) {

    class Item(
        val name: String,
        val url: String,
        val imageUrl: String,
        val access: String,
        val tier: String,
        val tourCategory: String,
        val place: Place,
        val author: Author,
        val luxCategory: LuxCategory?
    )

    class Place internal constructor(val name: String, val description: String)

    class Author internal constructor(val id: String, val name: String)

    class LuxCategory internal constructor(val name: String, color: String) {
        @ColorInt
        val color: Int

        companion object {
            private fun makeColorSafely(color: String): Int {
                return try {
                    makeColor(color)
                } catch (exception: IllegalArgumentException) {
                    0
                }
            }

            private fun makeColor(color: String): Int {
                return Color.parseColor("#$color")
            }
        }

        init {
            this.color = makeColorSafely(color)
        }
    }

}