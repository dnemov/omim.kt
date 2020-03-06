package com.mapswithme.maps.maplayer.traffic.widget

import android.graphics.drawable.AnimationDrawable
import android.view.View
import android.widget.ImageButton
import android.widget.RelativeLayout
import com.mapswithme.maps.R

import com.mapswithme.util.ThemeUtils
import com.mapswithme.util.UiUtils
import com.mapswithme.util.Utils

class TrafficButton(private val mButton: ImageButton) {
    private val mLoadingAnim: AnimationDrawable
    fun turnOff() {
        stopWaitingAnimation()
        mButton.setImageResource(if (ThemeUtils.isNightTheme) R.drawable.bg_subway_night_default else R.drawable.bg_subway_light_default)
    }

    fun turnOn() {
        stopWaitingAnimation()
        mButton.setImageResource(if (ThemeUtils.isNightTheme) R.drawable.ic_traffic_on_night else R.drawable.ic_traffic_on)
    }

    fun markAsOutdated() {
        stopWaitingAnimation()
        mButton.setImageResource(if (ThemeUtils.isNightTheme) R.drawable.ic_traffic_outdated_night else R.drawable.ic_traffic_outdated)
    }

    fun startWaitingAnimation() {
        mButton.setImageDrawable(mLoadingAnim)
        val anim = mButton.drawable as AnimationDrawable
        anim.start()
    }

    private fun stopWaitingAnimation() {
        val drawable = mButton.drawable
        if (drawable is AnimationDrawable) {
            drawable.stop()
            mButton.setImageDrawable(null)
        }
    }

    fun setOffset(offsetX: Int, offsetY: Int) {
        val params =
            mButton.layoutParams as RelativeLayout.LayoutParams
        params.setMargins(offsetX, offsetY, 0, 0)
        mButton.requestLayout()
    }

    fun show() {
        UiUtils.show(mButton)
    }

    fun hide() {
        UiUtils.hide(mButton)
    }

    fun hideImmediately() {
        mButton.visibility = View.GONE
    }

    fun showImmediately() {
        mButton.visibility = View.VISIBLE
    }

    fun setOnclickListener(onclickListener: View.OnClickListener?) {
        mButton.setOnClickListener(onclickListener)
    }

    companion object {
        private fun getLoadingAnim(trafficBtn: ImageButton): AnimationDrawable {
            val context = trafficBtn.context
            val res = context.resources
            val animResId =
                ThemeUtils.getResource(context, R.attr.trafficLoadingAnimation)
            return if (Utils.isLollipopOrLater) res.getDrawable(
                animResId,
                context.theme
            ) as AnimationDrawable else (res.getDrawable(animResId) as AnimationDrawable)
        }
    }

    init {
        mLoadingAnim = getLoadingAnim(mButton)
        val params =
            mButton.layoutParams as RelativeLayout.LayoutParams
        params.setMargins(0, UiUtils.getStatusBarHeight(mButton.context), 0, 0)
    }
}