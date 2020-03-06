package com.mapswithme.maps.routing

import android.animation.Animator
import android.animation.ValueAnimator
import android.os.Bundle
import com.mapswithme.maps.MwmActivity
import com.mapswithme.maps.R
import com.mapswithme.maps.routing.RoutingPlanController
import com.mapswithme.util.UiUtils
import com.mapswithme.util.UiUtils.SimpleAnimatorListener

class RoutingPlanInplaceController(
    activity: MwmActivity,
    private val mRoutingPlanListener: RoutingPlanListener,
    listener: RoutingBottomMenuListener
) : RoutingPlanController(
    activity.findViewById(R.id.routing_plan_frame),
    activity,
    mRoutingPlanListener,
    listener
) {
    private var mAnimator: Animator? = null
    fun show(show: Boolean) {
        if (mAnimator != null) {
            mAnimator!!.cancel()
            mAnimator!!.removeAllListeners()
        }
        if (show) UiUtils.show(frame)
        mAnimator = animateFrame(show, Runnable { if (!show) UiUtils.hide(frame) })
    }

    fun onSaveState(outState: Bundle) {
        saveRoutingPanelState(outState)
    }

    fun restoreState(state: Bundle) {
        restoreRoutingPanelState(state)
    }

    private fun animateFrame(
        show: Boolean,
        completion: Runnable?
    ): ValueAnimator? {
        if (!checkFrameHeight()) {
            frame.post { animateFrame(show, completion) }
            return null
        }
        mRoutingPlanListener.onRoutingPlanStartAnimate(show)
        val animator = ValueAnimator.ofFloat(
            if (show) -frame.height.toFloat() else 0.toFloat(),
            if (show) 0F else -frame.height.toFloat()
        )
        animator.addUpdateListener { animation ->
            frame.translationY = animation.animatedValue as Float
        }
        animator.addListener(object : SimpleAnimatorListener() {
            override fun onAnimationEnd(animation: Animator) {
                completion?.run()
            }
        })
        animator.duration = RoutingPlanController.Companion.ANIM_TOGGLE.toLong()
        animator.start()
        return animator
    }

    interface RoutingPlanListener {
        fun onRoutingPlanStartAnimate(show: Boolean)
    }

}