package com.mapswithme.maps.widget

import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.mapswithme.maps.R
import com.mapswithme.util.ThemeUtils
import com.mapswithme.util.UiUtils

/**
 * This widget allows the user to flip left and right through pages of data.
 * Also it can be configured with indicator view that will be placed below of pages.
 * The indicator will be drawn as the gray dots. A dots count will be equal page count.
 * The bold dot corresponds the current page.
 *
 *
 * There are few dependencies that should be provided to get this
 * widget work:
 *
 *
 *
 *  * @see [ViewPager]
 *  * @see [PagerAdapter]
 *  * An indicator. It's a [ViewGroup] which will consist dots. If the indicator
 * is not needed this dependency can be missed or `null`
 *  * @see [Context]
 *  * A page listener is an observable mechanism for listening page changing. It can be missed or null
 *
 */
class DotPager private constructor(builder: Builder) :
    OnPageChangeListener {
    private val mPager: ViewPager
    private val mAdapter: PagerAdapter
    private val mIndicator: ViewGroup?
    private val mDots: Array<ImageView?>
    private val mContext: Context
    private val mListener: OnPageChangedListener?
    @StringRes
    private val mActiveDotDrawableResId: Int
    @StringRes
    private val mInactiveDotDrawableResId: Int

    fun show() {
        configure()
        updateIndicator()
    }

    private fun configure() {
        configurePager()
        configureIndicator()
    }

    private fun configurePager() {
        mPager.adapter = mAdapter
        mPager.addOnPageChangeListener(this)
    }

    private fun configureIndicator() {
        if (mIndicator == null) return
        mIndicator.removeAllViews()
        if (mAdapter.count == 1) return
        for (i in mDots.indices) {
            mDots[i] = ImageView(mContext)
            val layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(0, 0, UiUtils.dimen(mContext, R.dimen.margin_half), 0)
            mIndicator.addView(mDots[i], i, layoutParams)
        }
    }

    override fun onPageSelected(position: Int) {
        if (mIndicator != null) updateIndicator()
        mListener?.onPageChanged(position)
    }

    private fun updateIndicator() {
        if (mAdapter.count == 1) return
        val currentPage = mPager.currentItem
        for (i in 0 until mAdapter.count) {
            val isCurPage = i == currentPage
            @DrawableRes var dotDrawable: Int
            dotDrawable =
                if (ThemeUtils.isNightTheme) if (isCurPage) R.drawable.news_marker_active_night else R.drawable.news_marker_inactive_night else if (isCurPage) mActiveDotDrawableResId else mInactiveDotDrawableResId
            mDots[i]!!.setImageResource(dotDrawable)
        }
    }

    override fun onPageScrolled(
        position: Int,
        positionOffset: Float,
        positionOffsetPixels: Int
    ) { //no op
    }

    override fun onPageScrollStateChanged(state: Int) { //no op
    }

    class Builder(
        internal val mContext: Context,
        internal val mPager: ViewPager,
        internal val mAdapter: PagerAdapter
    ) {
        internal var mIndicatorContainer: ViewGroup? = null
        internal var mListener: OnPageChangedListener? = null
        @DrawableRes
        internal var mActiveDotDrawableResId = R.drawable.news_marker_active
        @DrawableRes
        internal var mInactiveDotDrawableResId = R.drawable.news_marker_inactive

        fun setIndicatorContainer(indicatorContainer: ViewGroup): Builder {
            mIndicatorContainer = indicatorContainer
            return this
        }

        fun setPageChangedListener(listener: OnPageChangedListener?): Builder {
            mListener = listener
            return this
        }

        fun setActiveDotDrawable(@DrawableRes resId: Int): Builder {
            mActiveDotDrawableResId = resId
            return this
        }

        fun setInactiveDotDrawable(@DrawableRes resId: Int): Builder {
            mInactiveDotDrawableResId = resId
            return this
        }

        fun build(): DotPager {
            return DotPager(this)
        }

    }

    interface OnPageChangedListener {
        fun onPageChanged(position: Int)
    }

    init {
        mContext = builder.mContext
        mPager = builder.mPager
        mAdapter = builder.mAdapter
        mIndicator = builder.mIndicatorContainer
        mListener = builder.mListener
        mDots = arrayOfNulls(mAdapter.count)
        mActiveDotDrawableResId = builder.mActiveDotDrawableResId
        mInactiveDotDrawableResId = builder.mInactiveDotDrawableResId
    }
}