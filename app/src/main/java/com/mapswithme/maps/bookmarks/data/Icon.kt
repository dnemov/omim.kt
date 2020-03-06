package com.mapswithme.maps.bookmarks.data

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import com.mapswithme.maps.R
import kotlinx.android.parcel.Parcelize
import kotlin.annotation.Retention

@Parcelize
class Icon(
    @PredefinedColor val color: Int, @BookmarkIconType val type: Int
) : Parcelable {
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        PREDEFINED_COLOR_NONE,
        PREDEFINED_COLOR_RED,
        PREDEFINED_COLOR_BLUE,
        PREDEFINED_COLOR_PURPLE,
        PREDEFINED_COLOR_YELLOW,
        PREDEFINED_COLOR_PINK,
        PREDEFINED_COLOR_BROWN,
        PREDEFINED_COLOR_GREEN,
        PREDEFINED_COLOR_ORANGE,
        PREDEFINED_COLOR_DEEPPURPLE,
        PREDEFINED_COLOR_LIGHTBLUE,
        PREDEFINED_COLOR_CYAN,
        PREDEFINED_COLOR_TEAL,
        PREDEFINED_COLOR_LIME,
        PREDEFINED_COLOR_DEEPORANGE,
        PREDEFINED_COLOR_GRAY,
        PREDEFINED_COLOR_BLUEGRAY
    )
    internal annotation class PredefinedColor

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        BOOKMARK_ICON_TYPE_NONE,
        BOOKMARK_ICON_TYPE_HOTEL,
        BOOKMARK_ICON_TYPE_ANIMALS,
        BOOKMARK_ICON_TYPE_BUDDHISM,
        BOOKMARK_ICON_TYPE_BUILDING,
        BOOKMARK_ICON_TYPE_CHRISTIANITY,
        BOOKMARK_ICON_TYPE_ENTERTAINMENT,
        BOOKMARK_ICON_TYPE_EXCHANGE,
        BOOKMARK_ICON_TYPE_FOOD,
        BOOKMARK_ICON_TYPE_GAS,
        BOOKMARK_ICON_TYPE_JUDAISM,
        BOOKMARK_ICON_TYPE_MEDICINE,
        BOOKMARK_ICON_TYPE_MOUNTAIN,
        BOOKMARK_ICON_TYPE_MUSEUM,
        BOOKMARK_ICON_TYPE_ISLAM,
        BOOKMARK_ICON_TYPE_PARK,
        BOOKMARK_ICON_TYPE_PARKING,
        BOOKMARK_ICON_TYPE_SHOP,
        BOOKMARK_ICON_TYPE_SIGHTS,
        BOOKMARK_ICON_TYPE_SWIM,
        BOOKMARK_ICON_TYPE_WATER,
        BOOKMARK_ICON_TYPE_BAR,
        BOOKMARK_ICON_TYPE_TRANSPORT,
        BOOKMARK_ICON_TYPE_VIEWPOINT,
        BOOKMARK_ICON_TYPE_SPORT,
        BOOKMARK_ICON_TYPE_START,
        BOOKMARK_ICON_TYPE_FINISH
    )
    internal annotation class BookmarkIconType

    val name: String
        get() = PREDEFINED_COLOR_NAMES[color]

    fun argb(): Int {
        return ARGB_COLORS[color]
    }

    @get:DrawableRes
    val resId: Int
        get() = TYPE_ICONS[type]

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is Icon) return false
        return color == other.color
    }

    @PredefinedColor
    override fun hashCode(): Int {
        return color
    }

    companion object {
        const val PREDEFINED_COLOR_NONE = 0
        const val PREDEFINED_COLOR_RED = 1
        const val PREDEFINED_COLOR_BLUE = 2
        const val PREDEFINED_COLOR_PURPLE = 3
        const val PREDEFINED_COLOR_YELLOW = 4
        const val PREDEFINED_COLOR_PINK = 5
        const val PREDEFINED_COLOR_BROWN = 6
        const val PREDEFINED_COLOR_GREEN = 7
        const val PREDEFINED_COLOR_ORANGE = 8
        const val PREDEFINED_COLOR_DEEPPURPLE = 9
        const val PREDEFINED_COLOR_LIGHTBLUE = 10
        const val PREDEFINED_COLOR_CYAN = 11
        const val PREDEFINED_COLOR_TEAL = 12
        const val PREDEFINED_COLOR_LIME = 13
        const val PREDEFINED_COLOR_DEEPORANGE = 14
        const val PREDEFINED_COLOR_GRAY = 15
        const val PREDEFINED_COLOR_BLUEGRAY = 16
        private val PREDEFINED_COLOR_NAMES = arrayOf(
            "placemark-red",
            "placemark-red",
            "placemark-blue",
            "placemark-purple",
            "placemark-yellow",
            "placemark-pink",
            "placemark-brown",
            "placemark-green",
            "placemark-orange",
            "placemark-deeppurple",
            "placemark-lightblue",
            "placemark-cyan",
            "placemark-teal",
            "placemark-lime",
            "placemark-deeporange",
            "placemark-gray",
            "placemark-bluegray"
        )

        private fun shift(v: Int, bitCount: Int): Int {
            return v shl bitCount
        }

        private fun toARGB(r: Int, g: Int, b: Int): Int {
            return shift(
                255,
                24
            ) + shift(
                r,
                16
            ) + shift(g, 8) + b
        }

        private val ARGB_COLORS = intArrayOf(
            toARGB(229, 27, 35),  // none
            toARGB(229, 27, 35),  // red
            toARGB(0, 110, 199),  // blue
            toARGB(156, 39, 176),  // purple
            toARGB(255, 200, 0),  // yellow
            toARGB(255, 65, 130),  // pink
            toARGB(121, 85, 72),  // brown
            toARGB(56, 142, 60),  // green
            toARGB(255, 160, 0),  // orange
            toARGB(102, 57, 191),  // deeppurple
            toARGB(36, 156, 242),  // lightblue
            toARGB(20, 190, 205),  // cyan
            toARGB(0, 165, 140),  // teal
            toARGB(147, 191, 57),  // lime
            toARGB(240, 100, 50),  // deeporange
            toARGB(115, 115, 115),  // gray
            toARGB(89, 115, 128)
        ) // bluegray
        const val BOOKMARK_ICON_TYPE_NONE = 0
        const val BOOKMARK_ICON_TYPE_HOTEL = 1
        const val BOOKMARK_ICON_TYPE_ANIMALS = 2
        const val BOOKMARK_ICON_TYPE_BUDDHISM = 3
        const val BOOKMARK_ICON_TYPE_BUILDING = 4
        const val BOOKMARK_ICON_TYPE_CHRISTIANITY = 5
        const val BOOKMARK_ICON_TYPE_ENTERTAINMENT = 6
        const val BOOKMARK_ICON_TYPE_EXCHANGE = 7
        const val BOOKMARK_ICON_TYPE_FOOD = 8
        const val BOOKMARK_ICON_TYPE_GAS = 9
        const val BOOKMARK_ICON_TYPE_JUDAISM = 10
        const val BOOKMARK_ICON_TYPE_MEDICINE = 11
        const val BOOKMARK_ICON_TYPE_MOUNTAIN = 12
        const val BOOKMARK_ICON_TYPE_MUSEUM = 13
        const val BOOKMARK_ICON_TYPE_ISLAM = 14
        const val BOOKMARK_ICON_TYPE_PARK = 15
        const val BOOKMARK_ICON_TYPE_PARKING = 16
        const val BOOKMARK_ICON_TYPE_SHOP = 17
        const val BOOKMARK_ICON_TYPE_SIGHTS = 18
        const val BOOKMARK_ICON_TYPE_SWIM = 19
        const val BOOKMARK_ICON_TYPE_WATER = 20
        const val BOOKMARK_ICON_TYPE_BAR = 21
        const val BOOKMARK_ICON_TYPE_TRANSPORT = 22
        const val BOOKMARK_ICON_TYPE_VIEWPOINT = 23
        const val BOOKMARK_ICON_TYPE_SPORT = 24
        const val BOOKMARK_ICON_TYPE_START = 25
        const val BOOKMARK_ICON_TYPE_FINISH = 26
        @DrawableRes
        private val TYPE_ICONS = intArrayOf(
            R.drawable.ic_bookmark_none,
            R.drawable.ic_bookmark_hotel,
            R.drawable.ic_bookmark_animals,
            R.drawable.ic_bookmark_buddhism,
            R.drawable.ic_bookmark_building,
            R.drawable.ic_bookmark_christianity,
            R.drawable.ic_bookmark_entertainment,
            R.drawable.ic_bookmark_money,
            R.drawable.ic_bookmark_food,
            R.drawable.ic_bookmark_gas,
            R.drawable.ic_bookmark_judaism,
            R.drawable.ic_bookmark_medicine,
            R.drawable.ic_bookmark_mountain,
            R.drawable.ic_bookmark_museum,
            R.drawable.ic_bookmark_islam,
            R.drawable.ic_bookmark_park,
            R.drawable.ic_bookmark_parking,
            R.drawable.ic_bookmark_shop,
            R.drawable.ic_bookmark_sights,
            R.drawable.ic_bookmark_swim,
            R.drawable.ic_bookmark_water,
            R.drawable.ic_bookmark_bar,
            R.drawable.ic_bookmark_transport,
            R.drawable.ic_bookmark_viewpoint,
            R.drawable.ic_bookmark_sport,
            R.drawable.ic_bookmark_start,
            R.drawable.ic_bookmark_finish
        )
    }
}