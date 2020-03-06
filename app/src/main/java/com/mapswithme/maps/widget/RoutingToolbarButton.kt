package com.mapswithme.maps.widget

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatRadioButton
import com.mapswithme.maps.R
import com.mapswithme.util.ThemeUtils

class RoutingToolbarButton : AppCompatRadioButton {
    private var mInProgress = false
    @DrawableRes
    private var mIcon = 0

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        initView()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context,
        attrs
    ) {
        initView()
    }

    constructor(context: Context?) : super(context) {
        initView()
    }

    private fun initView() {
        setBackgroundResource(if (ThemeUtils.isNightTheme) R.drawable.routing_toolbar_button_night else R.drawable.routing_toolbar_button)
        setButtonTintList(if (ThemeUtils.isNightTheme) R.color.routing_toolbar_icon_tint_night else R.color.routing_toolbar_icon_tint)
    }

    fun progress() {
        if (mInProgress) return
        setButtonDrawable(mIcon)
        mInProgress = true
        isActivated = false
        isSelected = true
    }

    fun error() {
        mInProgress = false
        isSelected = false
        setButtonDrawable(R.drawable.ic_routing_error)
        isActivated = true
    }

    fun activate() {
        if (!mInProgress) {
            setButtonDrawable(mIcon)
            isSelected = false
            isActivated = true
        }
    }

    fun complete() {
        mInProgress = false
        activate()
    }

    fun deactivate() {
        isActivated = false
        mInProgress = false
    }

    fun setButtonTintList(@ColorRes color: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) supportButtonTintList =
            resources.getColorStateList(color) else buttonTintList =
            resources.getColorStateList(color)
    }

    fun setIcon(@DrawableRes icon: Int) {
        mIcon = icon
        setButtonDrawable(icon)
    }
}