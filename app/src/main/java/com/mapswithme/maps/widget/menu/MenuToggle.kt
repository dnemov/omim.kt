package com.mapswithme.maps.widget.menu

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.view.View
import android.widget.ImageView
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import com.mapswithme.maps.R
import com.mapswithme.maps.widget.RotateByAlphaDrawable
import com.mapswithme.maps.widget.TrackedTransitionDrawable
import com.mapswithme.maps.widget.menu.BaseMenu
import com.mapswithme.util.UiUtils

internal class MenuToggle private constructor(frame: View, @DimenRes heightRes: Int, @DrawableRes src: Int, @DrawableRes dst: Int) {
    private val mButton: ImageView
    private val mAlwaysShow: Boolean
    private val mOpenImage: TransitionDrawable
    private val mCollapseImage: TransitionDrawable

    constructor(frame: View, @DimenRes heightRes: Int) : this(
        frame,
        heightRes,
        R.drawable.ic_menu_open,
        R.drawable.ic_menu_close
    ) {
    }

    private fun transitImage(
        image: TransitionDrawable,
        forward: Boolean,
        animate: Boolean
    ) {
        var animate = animate
        if (!UiUtils.isVisible(mButton)) animate = false
        mButton.setImageDrawable(image)
        if (forward) image.startTransition(if (animate) BaseMenu.Companion.ANIMATION_DURATION else 0) else image.reverseTransition(
            if (animate) BaseMenu.Companion.ANIMATION_DURATION else 0
        )
        if (!animate) image.getDrawable(if (forward) 1 else 0).alpha = 0xFF
    }

    fun show(show: Boolean) { //TODO: refactor mAlwaysShow logic, because now we shouldn't display
// the toggle button when we are in prepare routing state (create JIRA item for that)
// A temporary solution is the hide() method.
        UiUtils.showIf(mAlwaysShow || show, mButton)
    }

    fun hide() {
        UiUtils.hide(mButton)
    }

    fun setOpen(open: Boolean, animate: Boolean) {
        transitImage(mOpenImage, open, animate)
    }

    fun setCollapsed(collapse: Boolean, animate: Boolean) {
        transitImage(mCollapseImage, collapse, animate)
    }

    init {
        mButton = frame.findViewById<View>(R.id.toggle) as ImageView
        mAlwaysShow = frame.findViewById<View?>(R.id.disable_toggle) == null
        val sz = UiUtils.dimen(heightRes)
        val bounds = Rect(0, 0, sz, sz)
        mOpenImage = TrackedTransitionDrawable(
            arrayOf<Drawable>(
                RotateByAlphaDrawable(frame.context, src, R.attr.iconTint, false)
                    .setInnerBounds(bounds),
                RotateByAlphaDrawable(frame.context, dst, R.attr.iconTintLight, true)
                    .setInnerBounds(bounds)
                    .setBaseAngle(-90f)
            )
        )
        mCollapseImage = TrackedTransitionDrawable(
            arrayOf<Drawable>(
                RotateByAlphaDrawable(frame.context, src, R.attr.iconTint, false)
                    .setInnerBounds(bounds),
                RotateByAlphaDrawable(frame.context, dst, R.attr.iconTintLight, true)
                    .setInnerBounds(bounds)
            )
        )
        mOpenImage.setCrossFadeEnabled(true)
        mCollapseImage.setCrossFadeEnabled(true)
    }
}