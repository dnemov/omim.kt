package com.mapswithme.maps

import android.animation.Animator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.fragment.app.Fragment
import com.mapswithme.maps.MwmActivity.LeftAnimationTrackListener
import com.mapswithme.util.Listeners
import com.mapswithme.util.UiUtils
import com.mapswithme.util.UiUtils.SimpleAnimatorListener

internal class PanelAnimator(private val mActivity: MwmActivity) {
    private val mAnimationTrackListeners =
        Listeners<LeftAnimationTrackListener>()
    private val mPanel: View
    fun registerListener(animationTrackListener: LeftAnimationTrackListener) {
        mAnimationTrackListeners.register(animationTrackListener)
    }

    private fun track(animation: ValueAnimator) {
        val offset = animation.animatedValue as Float
        mPanel.translationX = offset
        mPanel.alpha = offset / WIDTH + 1.0f
        for (listener in mAnimationTrackListeners) listener.onTrackLeftAnimation(
            offset + WIDTH
        )
        mAnimationTrackListeners.finishIterate()
    }

    /** @param completionListener will be called before the fragment becomes actually visible
     */
    fun show(
        clazz: Class<out Fragment>?,
        args: Bundle?,
        completionListener: Runnable?
    ) {
        if (isVisible) {
            if (mActivity.getFragment(clazz!!) != null) {
                completionListener?.run()
                return
            }
            hide(Runnable { show(clazz, args, completionListener) })
            return
        }
        mActivity.replaceFragmentInternal(clazz, args)
        completionListener?.run()
        UiUtils.show(mPanel)
        for (listener in mAnimationTrackListeners) listener.onTrackStarted(
            false
        )
        mAnimationTrackListeners.finishIterate()
        val animator =
            ValueAnimator.ofFloat(-WIDTH.toFloat(), 0.0f)
        animator.addUpdateListener { animation -> track(animation) }
        animator.addListener(object : SimpleAnimatorListener() {
            override fun onAnimationEnd(animation: Animator) {
                for (listener in mAnimationTrackListeners) listener.onTrackStarted(
                    true
                )
                mAnimationTrackListeners.finishIterate()
                mActivity.adjustCompass(UiUtils.getCompassYOffset(mActivity))
            }
        })
        animator.duration = DURATION.toLong()
        animator.interpolator = AccelerateInterpolator()
        animator.start()
    }

    fun hide(completionListener: Runnable?) {
        if (!isVisible) {
            completionListener?.run()
            return
        }
        for (listener in mAnimationTrackListeners) listener.onTrackStarted(
            true
        )
        mAnimationTrackListeners.finishIterate()
        val animator =
            ValueAnimator.ofFloat(0.0f, -WIDTH.toFloat())
        animator.addUpdateListener { animation -> track(animation) }
        animator.addListener(object : SimpleAnimatorListener() {
            override fun onAnimationEnd(animation: Animator) {
                UiUtils.hide(mPanel)
                for (listener in mAnimationTrackListeners) listener.onTrackStarted(
                    false
                )
                mAnimationTrackListeners.finishIterate()
                mActivity.adjustCompass(UiUtils.getCompassYOffset(mActivity))
                completionListener?.run()
            }
        })
        animator.duration = DURATION.toLong()
        animator.interpolator = AccelerateInterpolator()
        animator.start()
    }

    val isVisible: Boolean
        get() = UiUtils.isVisible(mPanel)



    companion object {
        val DURATION: Int
            get() = MwmApplication.get().resources.getInteger(R.integer.anim_panel)

        private val WIDTH = UiUtils.dimen(R.dimen.panel_width)
    }

    init {
        mPanel = mActivity.findViewById(R.id.fragment_container)
    }
}