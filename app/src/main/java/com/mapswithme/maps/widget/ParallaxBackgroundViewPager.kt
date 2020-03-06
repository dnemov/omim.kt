package com.mapswithme.maps.widget

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager
import com.mapswithme.maps.R

class ParallaxBackgroundViewPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewPager(context, attrs) {
    private var mAutoScrollPeriod = 0
    private var mAutoScroll = false
    private val mAutoScrollHandler: Handler
    private val mAutoScrollMessage: Runnable
    private var mCurrentPagePosition = 0
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAutoScroll()
        clearOnPageChangeListeners()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        if (action == MotionEvent.ACTION_DOWN && mAutoScroll) stopAutoScroll() else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_OUTSIDE) startAutoScroll()
        return super.dispatchTouchEvent(ev)
    }

    fun startAutoScroll() {
        mAutoScrollHandler.postDelayed(mAutoScrollMessage, mAutoScrollPeriod.toLong())
    }

    fun stopAutoScroll() {
        mAutoScrollHandler.removeCallbacks(mAutoScrollMessage)
    }

    private val isLastAutoScrollPosition: Boolean
        private get() {
            val adapter = adapter
            return adapter != null && adapter.count - 1 == mCurrentPagePosition
        }

    private inner class AutoScrollPageListener : OnPageChangeListener {
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
        }

        override fun onPageSelected(position: Int) {
            mCurrentPagePosition = position
            mAutoScrollHandler.removeCallbacks(mAutoScrollMessage)
            mAutoScrollHandler.postDelayed(mAutoScrollMessage, mAutoScrollPeriod.toLong())
        }

        override fun onPageScrollStateChanged(state: Int) {}
    }

    private inner class AutoScrollMessage : Runnable {
        override fun run() {
            val adapter = adapter
            if (adapter == null || adapter.count < CAROUSEL_ITEMS_MIN_COUNT || !mAutoScroll
            ) return
            if (isLastAutoScrollPosition) mCurrentPagePosition = 0 else mCurrentPagePosition++
            setCurrentItem(mCurrentPagePosition, true)
        }
    }

    companion object {
        private const val DEFAULT_AUTO_SCROLL_PERIOD = 3000
        private const val CAROUSEL_ITEMS_MIN_COUNT = 2
    }

    init {
        val array = context.theme
            .obtainStyledAttributes(attrs, R.styleable.ParallaxViewPagerBg, 0, 0)
        try {
            mAutoScroll = array.getBoolean(R.styleable.ParallaxViewPagerBg_autoScroll, false)
            mAutoScrollPeriod = array.getInt(
                R.styleable.ParallaxViewPagerBg_scrollPeriod,
                DEFAULT_AUTO_SCROLL_PERIOD
            )
        } finally {
            array.recycle()
        }
        mAutoScrollHandler = Handler()
        mAutoScrollMessage = AutoScrollMessage()
        val autoScrollPageListener: OnPageChangeListener = AutoScrollPageListener()
        addOnPageChangeListener(autoScrollPageListener)
    }
}