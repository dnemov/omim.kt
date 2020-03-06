package com.mapswithme.maps.widget.recycler

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorCompat
import androidx.core.view.ViewPropertyAnimatorListener
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.mapswithme.util.UiUtils

class SingleChangeItemAnimator : SimpleItemAnimator() {
    private var mAnimation: ViewPropertyAnimatorCompat? = null
    private var mFinished = false
    override fun getChangeDuration(): Long {
        return DURATION.toLong()
    }

    override fun animateChange(
        oldHolder: RecyclerView.ViewHolder,
        newHolder: RecyclerView.ViewHolder, fromLeft: Int, fromTop: Int,
        toLeft: Int, toTop: Int
    ): Boolean {
        mAnimation = ViewCompat.animate(oldHolder.itemView)
        if (mAnimation == null) return false
        mFinished = false
        val group = (oldHolder.itemView as ViewGroup).getChildAt(0) as ViewGroup
        for (i in 0 until group.childCount) UiUtils.hide(group.getChildAt(i))
        val from = oldHolder.itemView.width
        val target = newHolder.itemView.width
        oldHolder.itemView.pivotX = 0.0f
        mAnimation!!
            .setDuration(changeDuration)
            .scaleX(target.toFloat() / from.toFloat())
            .setListener(object : ViewPropertyAnimatorListener {
                override fun onAnimationStart(view: View) {}
                override fun onAnimationCancel(view: View) {}
                override fun onAnimationEnd(view: View) {
                    mFinished = true
                    mAnimation!!.setListener(null)
                    UiUtils.hide(oldHolder.itemView, newHolder.itemView)
                    dispatchChangeFinished(oldHolder, true)
                    dispatchChangeFinished(newHolder, false)
                    dispatchAnimationsFinished()
                    onAnimationFinished()
                }
            })
            .start()
        return false
    }

    fun onAnimationFinished() {}
    override fun endAnimation(item: RecyclerView.ViewHolder) {
        if (mAnimation != null) mAnimation!!.cancel()
    }

    override fun endAnimations() {
        if (mAnimation != null) mAnimation!!.cancel()
    }

    override fun isRunning(): Boolean {
        return mAnimation != null && !mFinished
    }

    override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
        return false
    }

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        return false
    }

    override fun animateMove(
        holder: RecyclerView.ViewHolder,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int
    ): Boolean {
        return false
    }

    override fun runPendingAnimations() {}

    companion object {
        private const val DURATION = 350
    }
}