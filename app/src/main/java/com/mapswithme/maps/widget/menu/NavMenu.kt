package com.mapswithme.maps.widget.menu

import android.animation.ValueAnimator
import android.view.View
import android.widget.Button
import android.widget.ImageView
import com.mapswithme.maps.Framework
import com.mapswithme.maps.Framework.RouterType
import com.mapswithme.maps.R
import com.mapswithme.maps.maplayer.traffic.TrafficManager
import com.mapswithme.maps.sound.TtsPlayer
import com.mapswithme.maps.widget.RotateDrawable
import com.mapswithme.maps.widget.menu.BaseMenu
import com.mapswithme.util.Graphics
import com.mapswithme.util.UiUtils

class NavMenu(
    frame: View,
    listener: ItemClickListener<BaseMenu.Item>
) : BaseMenu(frame, listener) {
    private val mToggleImage: RotateDrawable
    private val mTts: ImageView
    private val mTraffic: ImageView

    enum class Item(override val viewId: Int) :
        BaseMenu.Item {
        TOGGLE(R.id.toggle), TTS_VOLUME(R.id.tts_volume), STOP(R.id.stop), SETTINGS(R.id.settings), TRAFFIC(
            R.id.traffic
        );

    }

    override fun onResume(procAfterMeasurement: Runnable?) {
        super.onResume(procAfterMeasurement)
        refresh()
    }

    fun refresh() {
        refreshTts()
        refreshTraffic()
    }

    fun refreshTts() {
        mTts.setImageDrawable(
            if (TtsPlayer.isEnabled) Graphics.tint(
                super.frame.context, R.drawable.ic_voice_on,
                R.attr.colorAccent
            ) else Graphics.tint(super.frame.context, R.drawable.ic_voice_off)
        )
    }

    fun refreshTraffic() {
        val onIcon = Graphics.tint(
            super.frame.context, R.drawable.ic_setting_traffic_on,
            R.attr.colorAccent
        )
        val offIcon = Graphics.tint(
            super.frame.context,
            R.drawable.ic_setting_traffic_off
        )
        mTraffic.setImageDrawable(if (TrafficManager.INSTANCE.isEnabled) onIcon else offIcon)
    }

    override fun setToggleState(
        open: Boolean,
        animate: Boolean
    ) {
        val to = if (open) -90.0f else 90.0f
        if (!animate) {
            mToggleImage.setAngle(to)
            return
        }
        val from = -to
        val animator = ValueAnimator.ofFloat(from, to)
        animator.addUpdateListener { animation -> mToggleImage.setAngle(animation.animatedValue as Float) }
        animator.duration = BaseMenu.Companion.ANIMATION_DURATION.toLong()
        animator.start()
    }

    override val heightResId: Int
        get() = R.dimen.nav_menu_height

    override fun adjustTransparency() {}
    override fun show(show: Boolean) {
        super.show(show)
        measureContent(null)
        @RouterType val routerType = Framework.nativeGetRouter()
        UiUtils.showIf(show && routerType != Framework.ROUTER_TYPE_PEDESTRIAN, mTts)
        UiUtils.showIf(show && routerType == Framework.ROUTER_TYPE_VEHICLE, mTraffic)
    }

    init {
        mToggleImage = RotateDrawable(
            Graphics.tint(
                super.frame.context,
                R.drawable.ic_menu_close,
                R.attr.iconTintLight
            )
        )
        val toggle =
            mLineFrame.findViewById<View>(R.id.toggle) as ImageView
        toggle.setImageDrawable(mToggleImage)
        setToggleState(false, false)
        mapItem(Item.TOGGLE, mLineFrame)
        val stop = mapItem(
            Item.STOP,
            super.frame
        ) as Button
        UiUtils.updateRedButton(stop)
        mapItem(Item.SETTINGS, super.frame)
        mTts = (mapItem(
            Item.TTS_VOLUME,
            super.frame
        ) as ImageView)
        mTraffic = (mapItem(
            Item.TRAFFIC,
            super.frame
        ) as ImageView)
    }
}