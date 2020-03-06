package com.mapswithme.util

import android.animation.Animator
import android.view.View
import androidx.annotation.IntDef
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient
import com.mapswithme.util.Language
import com.mapswithme.util.UiUtils.SimpleAnimatorListener
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

object Animations {
    const val LEFT = 0
    const val RIGHT = 1
    const val TOP = 2
    const val BOTTOM = 3
    private val DURATION_DEFAULT: Int =
        MwmApplication.get().getResources().getInteger(R.integer.anim_default)

    fun appearSliding(
        view: View, @AnimationDirection appearFrom: Int,
        completionListener: Runnable?
    ) {
        if (UiUtils.isVisible(view)) {
            completionListener?.run()
            return
        }
        val animator =
            view.animate().setDuration(DURATION_DEFAULT.toLong())
                .alpha(1f).setListener(object : SimpleAnimatorListener() {
                    override fun onAnimationEnd(animation: Animator) {
                        completionListener?.run()
                    }
                })
        when (appearFrom) {
            LEFT, RIGHT -> animator.translationX(
                0f
            )
            TOP, BOTTOM -> animator.translationY(
                0f
            )
        }
        UiUtils.show(view)
    }

    fun disappearSliding(
        view: View, @AnimationDirection disappearTo: Int,
        completionListener: Runnable?
    ) {
        if (!UiUtils.isVisible(view)) {
            completionListener?.run()
            return
        }
        val animator =
            view.animate().setDuration(DURATION_DEFAULT.toLong())
                .alpha(0f).setListener(object : SimpleAnimatorListener() {
                    override fun onAnimationEnd(animation: Animator) {
                        UiUtils.hide(view)
                        completionListener?.run()
                    }
                })
        when (disappearTo) {
            RIGHT -> animator.translationX(view.width.toFloat())
            LEFT -> animator.translationX(-view.width.toFloat())
            BOTTOM -> animator.translationY(view.height.toFloat())
            TOP -> animator.translationY(-view.height.toFloat())
        }
    }

    @IntDef(
        LEFT,
        RIGHT,
        TOP,
        BOTTOM
    )
    @Retention(RetentionPolicy.SOURCE)
    internal annotation class AnimationDirection
}