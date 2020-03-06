package com.mapswithme.maps.widget

import android.animation.Animator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.util.UiUtils

class FadeView : FrameLayout {

    private val mFadeInListener = object : UiUtils.SimpleAnimatorListener() {
        override fun onAnimationEnd(animation: Animator) {
            UiUtils.show(this@FadeView)
            animation.removeListener(this)
        }
    }

    private val mFadeOutListener = object : UiUtils.SimpleAnimatorListener() {
        override fun onAnimationEnd(animation: Animator) {
            UiUtils.hide(this@FadeView)
            animation.removeListener(this)
        }
    }

    private var mListener: Listener? = null


    interface Listener {
        fun onTouch(): Boolean
    }

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    fun setListener(listener: Listener) {
        mListener = listener
    }

    fun fadeIn() {
        alpha = 0.0f
        UiUtils.show(this)
        animate().alpha(FADE_ALPHA_VALUE)
            .setDuration(DURATION.toLong())
            .setListener(mFadeInListener)
            .start()
    }

    fun fadeOut() {
        alpha = FADE_ALPHA_VALUE
        animate().alpha(0.0f)
            .setDuration(DURATION.toLong())
            .setListener(mFadeOutListener)
            .start()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN)
            return true

        if (mListener == null || mListener!!.onTouch())
            fadeOut()

        return true
    }

    companion object {
        private val FADE_ALPHA_VALUE = 0.4f
        private val PROPERTY_ALPHA = "alpha"
        private val DURATION = MwmApplication.get().resources.getInteger(R.integer.anim_fade_main)
    }
}
