package com.mapswithme.maps.widget.menu

import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.util.SparseArray
import android.view.View
import android.widget.ImageView
import com.mapswithme.maps.R
import com.mapswithme.maps.location.LocationState
import com.mapswithme.maps.routing.RoutingController.Companion.get
import com.mapswithme.util.Graphics
import com.mapswithme.util.ThemeUtils
import com.mapswithme.util.UiUtils

class MyPositionButton(
    button: View,
    listener: View.OnClickListener
) {
    private val mButton: ImageView
    private var mMode = 0
    private val mVisible: Boolean
    private val mFollowPaddingShift: Int
    fun update(mode: Int) {
        mMode = mode
        var image = mIcons[mode]
        if (image == null) {
            image = when (mode) {
                LocationState.PENDING_POSITION -> mButton.resources
                    .getDrawable(
                        ThemeUtils.getResource(
                            mButton.context,
                            R.attr.myPositionButtonAnimation
                        )
                    )
                LocationState.NOT_FOLLOW_NO_POSITION, LocationState.NOT_FOLLOW -> Graphics.tint(
                    mButton.context,
                    R.drawable.ic_not_follow
                )
                LocationState.FOLLOW -> Graphics.tint(
                    mButton.context,
                    R.drawable.ic_follow,
                    R.attr.colorAccent
                )
                LocationState.FOLLOW_AND_ROTATE -> Graphics.tint(
                    mButton.context,
                    R.drawable.ic_follow_and_rotate,
                    R.attr.colorAccent
                )
                else -> throw IllegalArgumentException("Invalid button mode: $mode")
            }
            mIcons.put(mode, image)
        }
        mButton.setImageDrawable(image)
        updatePadding(mode)
        if (image is AnimationDrawable) image.start()
        UiUtils.visibleIf(!shouldBeHidden(), mButton)
    }

    private fun updatePadding(mode: Int) {
        if (mode == LocationState.FOLLOW) mButton.setPadding(
            0,
            mFollowPaddingShift,
            mFollowPaddingShift,
            0
        ) else mButton.setPadding(0, 0, 0, 0)
    }

    private fun shouldBeHidden(): Boolean {
        return ((mMode == LocationState.FOLLOW_AND_ROTATE
                && get().isPlanning)
                || !mVisible)
    }

    fun show() {
        UiUtils.show(mButton)
    }

    fun hide() {
        UiUtils.hide(mButton)
    }

    companion object {
        private const val FOLLOW_SHIFT = 1
        private val mIcons =
            SparseArray<Drawable?>() // Location mode -> Button icon
    }

    init {
        mButton = button as ImageView
        mVisible = UiUtils.isVisible(mButton)
        mButton.setOnClickListener(listener)
        mIcons.clear()
        mFollowPaddingShift =
            (FOLLOW_SHIFT * button.getResources().displayMetrics.density).toInt()
    }
}