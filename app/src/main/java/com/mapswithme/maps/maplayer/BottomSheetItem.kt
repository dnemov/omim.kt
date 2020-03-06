package com.mapswithme.maps.maplayer

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mapswithme.maps.R
import com.mapswithme.util.ThemeUtils

abstract class BottomSheetItem internal constructor(
    @field:DrawableRes @get:DrawableRes
    @param:DrawableRes val enabledStateDrawable: Int,
    @field:DrawableRes @get:DrawableRes
    @param:DrawableRes val disabledStateDrawable: Int,
    @field:StringRes @get:StringRes
    @param:StringRes val title: Int,
    val mode: Mode
) {

    class Subway private constructor(drawableResId: Int, disabledStateDrawableResId: Int) :
        BottomSheetItem(
            drawableResId,
            disabledStateDrawableResId,
            R.string.button_layer_subway,
            Mode.SUBWAY
        ) {
        companion object {
            fun makeInstance(mContext: Context): BottomSheetItem {
                val disabled =
                    ThemeUtils.getResource(mContext, R.attr.subwayMenuDisabled)
                return Subway(R.drawable.ic_subway_menu_on, disabled)
            }
        }
    }

    class Traffic private constructor(drawableResId: Int, disabledStateDrawableResId: Int) :
        BottomSheetItem(
            drawableResId,
            disabledStateDrawableResId,
            R.string.button_layer_traffic,
            Mode.TRAFFIC
        ) {
        companion object {
            fun makeInstance(mContext: Context): BottomSheetItem {
                val disabled =
                    ThemeUtils.getResource(mContext, R.attr.trafficMenuDisabled)
                return Traffic(R.drawable.ic_traffic_menu_on, disabled)
            }
        }
    }

}