package com.mapswithme.maps.widget.menu

import android.animation.Animator
import android.view.View
import androidx.annotation.DimenRes
import androidx.annotation.IdRes
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.util.ThemeUtils
import com.mapswithme.util.UiUtils
import com.mapswithme.util.UiUtils.SimpleAnimatorListener

abstract class BaseMenu internal constructor(
    val frame: View,
    val mItemClickListener: ItemClickListener<BaseMenu.Item>
) {
    var isOpen = false
        private set
    var isAnimating = false
        private set
    val mLineFrame: View
    val mContentFrame: View
    var mContentHeight = 0
    var mLayoutMeasured = false

    interface Item {
        @get:IdRes
        val viewId: Int
    }

    interface ItemClickListener<T : Item> {
        fun onItemClick(item: T)
    }

    private inner open class AnimationListener : SimpleAnimatorListener() {
        override fun onAnimationStart(animation: Animator) {
            isAnimating = true
        }

        override fun onAnimationEnd(animation: Animator) {
            isAnimating = false
        }
    }

    open fun mapItem(
        item: Item,
        frame: View
    ): View? {
        val res = frame.findViewById<View>(item.viewId)
        res?.setOnClickListener { mItemClickListener.onItemClick(item) }
        return res
    }

    protected open fun adjustTransparency() {
        frame.setBackgroundColor(
            ThemeUtils.getColor(
                frame.context,
                if (isOpen) R.attr.menuBackgroundOpen else R.attr.menuBackground
            )
        )
    }

    open fun afterLayoutMeasured(procAfterCorrection: Runnable?) {
        procAfterCorrection?.run()
    }

    fun measureContent(procAfterMeasurement: Runnable?) {
        if (mLayoutMeasured) return
        UiUtils.measureView(mContentFrame, object: UiUtils.OnViewMeasuredListener {
            override fun onViewMeasured(width: Int, height: Int) {
                if (height != 0) {
                    mContentHeight = height
                    mLayoutMeasured = true
                    UiUtils.hide(mContentFrame)
                }
                afterLayoutMeasured(procAfterMeasurement)
            }
        } )
    }

    open fun onResume(procAfterMeasurement: Runnable?) {
        measureContent(procAfterMeasurement)
        updateMarker()
    }

    fun open(animate: Boolean): Boolean {
        if (animate && isAnimating || isOpen) return false
        isOpen = true
        UiUtils.show(mContentFrame)
        adjustCollapsedItems()
        adjustTransparency()
        updateMarker()
        setToggleState(isOpen, animate)
        if (!animate) return true
        frame.translationY = mContentHeight.toFloat()
        frame.animate()
            .setDuration(ANIMATION_DURATION.toLong())
            .translationY(0.0f)
            .setListener(AnimationListener())
            .start()
        return true
    }

    @JvmOverloads
    fun close(
        animate: Boolean,
        onCloseListener: Runnable? = null
    ): Boolean {
        if (isAnimating || !isOpen) {
            onCloseListener?.run()
            return false
        }
        isOpen = false
        adjustCollapsedItems()
        setToggleState(isOpen, animate)
        if (!animate) {
            UiUtils.hide(mContentFrame)
            adjustTransparency()
            updateMarker()
            onCloseListener?.run()
            return true
        }
        frame.animate()
            .setDuration(ANIMATION_DURATION.toLong())
            .translationY(mContentHeight.toFloat())
            .setListener(object : AnimationListener() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    frame.translationY = 0.0f
                    UiUtils.hide(mContentFrame)
                    adjustTransparency()
                    updateMarker()
                    onCloseListener?.run()
                }
            }).start()
        return true
    }

    fun toggle(animate: Boolean) {
        if (isAnimating) return
        val show = !isOpen
        if (show) open(animate) else close(animate)
    }

    open fun show(show: Boolean) {
        if (show && frame.isShown) return
        UiUtils.showIf(show, frame)
    }

    protected open fun adjustCollapsedItems() {}
    protected open fun updateMarker() {}
    protected open fun setToggleState(open: Boolean, animate: Boolean) {}
    @get:DimenRes
    protected abstract val heightResId: Int

    companion object {
        val ANIMATION_DURATION =
            MwmApplication.get().resources.getInteger(R.integer.anim_menu)
    }

    init {
        mLineFrame = frame.findViewById(R.id.line_frame)
        mContentFrame = frame.findViewById(R.id.content_frame)
        adjustTransparency()
    }
}