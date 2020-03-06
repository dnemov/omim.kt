package com.mapswithme.maps.widget

import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable

/**
 * Same as [TransitionDrawable] but correctly tracks transition direction.
 * I.e. if transition is in "start" state, [.reverseTransition] does not start straight transition.
 */
class TrackedTransitionDrawable(layers: Array<Drawable>?) :
    TransitionDrawable(layers) {
    private var mStart = true
    override fun startTransition(durationMillis: Int) {
        if (!mStart) return
        mStart = false
        super.startTransition(durationMillis)
    }

    override fun reverseTransition(duration: Int) {
        if (mStart) return
        mStart = true
        super.reverseTransition(duration)
    }

    override fun resetTransition() {
        mStart = true
        super.resetTransition()
    }
}