package com.mapswithme.maps.ugc.routes

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.*
import android.graphics.drawable.shapes.RectShape
import com.mapswithme.maps.R
import com.mapswithme.util.ThemeUtils

object TagsResFactory {
    fun makeSelector(
        context: Context,
        color: Int
    ): StateListDrawable {
        val drawable =
            StateListDrawable()
        drawable.addState(
            intArrayOf(android.R.attr.state_selected, android.R.attr.state_enabled),
            makeSelectedDrawable(color)
        )
        drawable.addState(
            intArrayOf(-android.R.attr.state_selected, android.R.attr.state_enabled),
            makeDefaultDrawable(context, color)
        )
        val unselectedDisabledColor = getDisabledTagColor(context)
        drawable.addState(
            intArrayOf(-android.R.attr.state_selected, -android.R.attr.state_enabled),
            makeDefaultDrawable(context, unselectedDisabledColor)
        )
        return drawable
    }

    private fun getDisabledTagColor(context: Context): Int {
        val res = context.resources
        return if (ThemeUtils.isNightTheme) res.getColor(R.color.white_12) else res.getColor(
            R.color.black_12
        )
    }

    private fun makeDefaultDrawable(
        context: Context,
        color: Int
    ): Drawable {
        val res = context.resources
        val gradientDrawable = GradientDrawable()
        gradientDrawable.setStroke(res.getDimensionPixelSize(R.dimen.divider_height), color)
        val shapeDrawable = ShapeDrawable(RectShape())
        shapeDrawable.paint.color = Color.WHITE
        return LayerDrawable(
            arrayOf(
                shapeDrawable,
                gradientDrawable
            )
        )
    }

    fun makeColor(context: Context, color: Int): ColorStateList {
        return ColorStateList(
            arrayOf(
                intArrayOf(
                    android.R.attr.state_selected,
                    android.R.attr.state_enabled
                ),
                intArrayOf(-android.R.attr.state_selected, android.R.attr.state_enabled),
                intArrayOf(-android.R.attr.state_selected, -android.R.attr.state_enabled)
            ), intArrayOf(
                context.resources.getColor(android.R.color.white),
                color,
                getDisabledTagColor(context)
            )
        )
    }

    private fun makeSelectedDrawable(color: Int): ColorDrawable {
        return ColorDrawable(color)
    }
}