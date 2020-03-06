package com.mapswithme.maps.widget

import android.app.Activity
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import java.util.*

class ParallaxBackgroundPageListener internal constructor(
    private val mActivity: Activity,
    private val mPager: ViewPager,
    items: List<Int?>,
    pageViewProvider: PageViewProvider
) : OnPageChangeListener {
    private val mItems: List<Int?>
    private var mCurrentPagePosition = 0
    private var mScrollToRight = true
    private var mScrollStarted = false
    private var mShouldCalculateScrollDirection = false
    private val mPageViewProvider: PageViewProvider

    constructor(
        activity: FragmentActivity,
        pager: ViewPager,
        items: List<Int?>
    ) : this(activity, pager, items, PageViewProviderFactory.defaultProvider(activity, pager)) {
    }

    override fun onPageScrollStateChanged(state: Int) {
        val isIdle = state == ViewPager.SCROLL_STATE_IDLE
        if (isIdle) setIdlePosition()
        mScrollStarted = isIdle && !mScrollStarted
        if (mScrollStarted) mShouldCalculateScrollDirection = true
    }

    private fun setIdlePosition() {
        mCurrentPagePosition = mPager.currentItem
    }

    override fun onPageSelected(position: Int) {
        if (position == 0) onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE)
        if (Math.abs(mCurrentPagePosition - position) > 1) mCurrentPagePosition =
            if (mScrollToRight) Math.max(
                0,
                position - 1
            ) else Math.min(position + 1, mItems.size - 1)
    }

    override fun onPageScrolled(
        position: Int,
        positionOffset: Float,
        positionOffsetPixels: Int
    ) {
        if (mShouldCalculateScrollDirection) {
            mScrollToRight =
                MIDDLE_POSITION_OFFSET > positionOffset && positionOffsetPixels > MIDDLE_POSITION_OFFSET_PIXELS
            mShouldCalculateScrollDirection = false
        }
        val scrollX = mPager.scrollX
        val animatedItemIndex = if (mScrollToRight) Math.min(
            mCurrentPagePosition,
            mItems.size - 1
        ) else Math.max(0, mCurrentPagePosition - 1)
        setAlpha(animatedItemIndex, scrollX)
        if (scrollX == 0) restoreInitialAlphaValues()
    }

    private fun setAlpha(animatedItemIndex: Int, scrollX: Int) {
        val view = mPageViewProvider.findViewByIndex(animatedItemIndex) ?: return
        val lp = view.layoutParams as ViewPager.LayoutParams
        if (lp.isDecor) return
        val transformPos =
            (view.left - scrollX).toFloat() / pagerWidth.toFloat()
        val currentImage =
            mActivity.findViewById<ImageView>(mItems[animatedItemIndex]!!)
        if (transformPos <= MINUS_INFINITY_EDGE || transformPos >= PLUS_INFINITY_EDGE) currentImage.alpha =
            ALPHA_TRANSPARENT else if (transformPos == SETTLED_PAGE_POSITION) currentImage.alpha =
            ALPHA_OPAQUE else currentImage.alpha = ALPHA_OPAQUE - Math.abs(
            transformPos
        )
    }

    private fun restoreInitialAlphaValues() {
        for (j in mItems.indices.reversed()) {
            val view = mActivity.findViewById<View>(mItems[j]!!)
            view.alpha = ALPHA_OPAQUE
        }
    }

    private val pagerWidth: Int
        private get() = mPager.measuredWidth - mPager.paddingLeft - mPager.paddingRight

    companion object {
        private const val MIDDLE_POSITION_OFFSET = 0.5f
        private const val MIDDLE_POSITION_OFFSET_PIXELS = 1
        private const val ALPHA_TRANSPARENT = 0f
        private const val ALPHA_OPAQUE = 1f
        private const val MINUS_INFINITY_EDGE = -1
        private const val PLUS_INFINITY_EDGE = 1
        private const val SETTLED_PAGE_POSITION = 0.0f
    }

    init {
        mItems = Collections.unmodifiableList(items)
        mPageViewProvider = pageViewProvider
    }
}