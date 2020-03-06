package com.mapswithme.maps.search

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.View
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.util.UiUtils.SimpleAnimatorListener

internal class SearchAnimationController(
    private val mToolbar: View,
    private val mTabBar: View
) {
    fun animate(show: Boolean, completion: Runnable?) {
        if (mToolbar.height == 0 || mTabBar.height == 0) {
            mToolbar.post { animate(show, completion) }
            return
        }
        val translation = -mTabBar.height - mToolbar.height.toFloat()
        val animator =
            ValueAnimator.ofFloat(if (show) translation else 0F, if (show) 0F else translation)
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            mToolbar.translationY = value
            mTabBar.translationY = value
        }
        animator.addListener(object : SimpleAnimatorListener() {
            override fun onAnimationEnd(animation: Animator) {
                completion?.run()
            }
        })
        animator.duration = DURATION.toLong()
        animator.start()
    }

    companion object {
        private val DURATION =
            MwmApplication.get().resources.getInteger(R.integer.anim_menu)
    }

}