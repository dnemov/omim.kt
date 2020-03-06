package com.mapswithme.maps.routing

import android.animation.Animator
import android.animation.AnimatorInflater
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.mapswithme.maps.MwmActivity
import com.mapswithme.maps.R
import com.mapswithme.maps.routing.SearchWheel.SearchOption
import com.mapswithme.maps.search.SearchEngine
import com.mapswithme.util.Graphics
import com.mapswithme.util.UiUtils
import com.mapswithme.util.UiUtils.SimpleAnimatorListener
import com.mapswithme.util.concurrency.UiThread
import com.mapswithme.util.statistics.Statistics
import com.mapswithme.util.statistics.Statistics.EventName

internal class SearchWheel(private val mFrame: View) :
    View.OnClickListener {
    private val mSearchLayout: View
    private val mSearchButton: ImageView
    private val mTouchInterceptor: View
    private var mIsExpanded = false
    private var mCurrentOption: SearchOption? = null
    private val mCloseRunnable = Runnable {
        // if the search bar is already closed, i.e. nothing should be done here.
        if (!mIsExpanded) return@Runnable
        toggleSearchLayout()
    }

    private enum class SearchOption(
        @field:IdRes @param:IdRes val mResId: Int, @field:DrawableRes @param:DrawableRes val mDrawableOff: Int, @field:DrawableRes @param:DrawableRes private val mDrawableOn: Int,
        @field:StringRes @param:StringRes val mQueryId: Int
    ) {
        FUEL(
            R.id.search_fuel,
            R.drawable.ic_routing_fuel_off,
            R.drawable.ic_routing_fuel_on,
            R.string.fuel
        ),
        PARKING(
            R.id.search_parking,
            R.drawable.ic_routing_parking_off,
            R.drawable.ic_routing_parking_on,
            R.string.parking
        ),
        EAT(
            R.id.search_eat,
            R.drawable.ic_routing_eat_off,
            R.drawable.ic_routing_eat_on,
            R.string.eat
        ),
        FOOD(
            R.id.search_food,
            R.drawable.ic_routing_food_off,
            R.drawable.ic_routing_food_on,
            R.string.food
        ),
        ATM(
            R.id.search_atm,
            R.drawable.ic_routing_atm_off,
            R.drawable.ic_routing_atm_on,
            R.string.atm
        );

        companion object {
            fun fromResId(@IdRes resId: Int): SearchOption {
                for (searchOption in values()) {
                    if (searchOption.mResId == resId) return searchOption
                }
                throw IllegalArgumentException("No navigation search for id $resId")
            }

            fun fromSearchQuery(
                query: String,
                context: Context
            ): SearchOption? {
                val normalizedQuery = query.trim { it <= ' ' }.toLowerCase()
                for (searchOption in values()) {
                    val searchOptionQuery =
                        context.getString(searchOption.mQueryId).trim { it <= ' ' }.toLowerCase()
                    if (searchOptionQuery == normalizedQuery) return searchOption
                }
                return null
            }
        }

    }

    fun saveState(outState: Bundle) {
        outState.putSerializable(EXTRA_CURRENT_OPTION, mCurrentOption)
    }

    fun restoreState(savedState: Bundle) {
        mCurrentOption =
            savedState.getSerializable(EXTRA_CURRENT_OPTION) as SearchOption?
    }

    fun reset() {
        mIsExpanded = false
        mCurrentOption = null
        SearchEngine.INSTANCE.cancelInteractiveSearch()
        resetSearchButtonImage()
    }

    fun onResume() {
        if (mCurrentOption != null) {
            refreshSearchButtonImage()
            return
        }
        val query = SearchEngine.INSTANCE.query
        if (TextUtils.isEmpty(query)) {
            resetSearchButtonImage()
            return
        }
        mCurrentOption = SearchOption.fromSearchQuery(query!!, mFrame.context)
        refreshSearchButtonImage()
    }

    private fun toggleSearchLayout() {
        val animRes: Int
        if (mIsExpanded) {
            animRes = R.animator.show_zoom_out_alpha
        } else {
            animRes = R.animator.show_zoom_in_alpha
            UiUtils.show(mSearchLayout)
        }
        mIsExpanded = !mIsExpanded
        val animator =
            AnimatorInflater.loadAnimator(mSearchLayout.context, animRes)
        animator.setTarget(mSearchLayout)
        animator.start()
        UiUtils.visibleIf(mIsExpanded, mTouchInterceptor)
        animator.addListener(object : SimpleAnimatorListener() {
            override fun onAnimationEnd(animation: Animator) {
                refreshSearchVisibility()
            }
        })
    }

    private fun refreshSearchVisibility() {
        for (searchOption in SearchOption.values()) UiUtils.visibleIf(
            mIsExpanded,
            mSearchLayout.findViewById(searchOption.mResId)
        )
        UiUtils.visibleIf(mIsExpanded, mSearchLayout, mTouchInterceptor)
        if (mIsExpanded) {
            UiThread.cancelDelayedTasks(mCloseRunnable)
            UiThread.runLater(
                mCloseRunnable,
                CLOSE_DELAY_MILLIS
            )
        }
    }

    private fun resetSearchButtonImage() {
        mSearchButton.setImageDrawable(
            Graphics.tint(
                mSearchButton.context,
                R.drawable.ic_routing_search_on
            )
        )
    }

    private fun refreshSearchButtonImage() {
        mSearchButton.setImageDrawable(
            Graphics.tint(
                mSearchButton.context,
                if (mCurrentOption == null) R.drawable.ic_routing_search_off else mCurrentOption!!.mDrawableOff,
                R.attr.colorAccent
            )
        )
    }

    fun performClick(): Boolean {
        return mSearchButton.performClick()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_search -> {
                if (RoutingController.Companion.get().isPlanning) {
                    if (TextUtils.isEmpty(SearchEngine.INSTANCE.query)) {
                        showSearchInParent()
                        Statistics.INSTANCE.trackRoutingEvent(
                            EventName.ROUTING_SEARCH_CLICK,
                            true
                        )
                    } else {
                        reset()
                    }
                    return
                }
                Statistics.INSTANCE.trackRoutingEvent(
                    EventName.ROUTING_SEARCH_CLICK,
                    false
                )
                if (mCurrentOption != null || !TextUtils.isEmpty(SearchEngine.INSTANCE.query)) {
                    SearchEngine.INSTANCE.cancelInteractiveSearch()
                    mCurrentOption = null
                    mIsExpanded = false
                    resetSearchButtonImage()
                    refreshSearchVisibility()
                    return
                }
                if (mIsExpanded) {
                    showSearchInParent()
                    return
                }
                toggleSearchLayout()
            }
            R.id.touch_interceptor -> toggleSearchLayout()
            R.id.search_fuel, R.id.search_parking, R.id.search_eat, R.id.search_food, R.id.search_atm -> startSearch(
                SearchOption.fromResId(v.id)
            )
        }
    }

    private fun showSearchInParent() {
        val context = mFrame.context
        val parent: MwmActivity
        parent =
            if (context is ContextThemeWrapper) context.baseContext as MwmActivity else if (context is androidx.appcompat.view.ContextThemeWrapper) context.baseContext as MwmActivity else context as MwmActivity
        parent.showSearch()
        mIsExpanded = false
        refreshSearchVisibility()
    }

    private fun startSearch(searchOption: SearchOption) {
        mCurrentOption = searchOption
        val query = mFrame.context.getString(searchOption.mQueryId)
        SearchEngine.INSTANCE.searchInteractive(
            query, System.nanoTime(), false /* isMapAndTable */,
            null /* hotelsFilter */, null /* bookingParams */
        )
        refreshSearchButtonImage()
        toggleSearchLayout()
    }

    companion object {
        private const val EXTRA_CURRENT_OPTION = "extra_current_option"
        private const val CLOSE_DELAY_MILLIS = 5000L
    }

    init {
        mTouchInterceptor = mFrame.findViewById(R.id.touch_interceptor)
        mTouchInterceptor.setOnClickListener(this)
        mSearchButton =
            mFrame.findViewById<View>(R.id.btn_search) as ImageView
        mSearchButton.setOnClickListener(this)
        mSearchLayout = mFrame.findViewById(R.id.search_frame)
        if (UiUtils.isLandscape(mFrame.context)) {
            UiUtils.waitLayout(mSearchLayout, object: ViewTreeObserver.OnGlobalLayoutListener{
                override fun onGlobalLayout() {
                    mSearchLayout.pivotX = 0f
                    mSearchLayout.pivotY = mSearchLayout.measuredHeight / 2.toFloat()
                }
            })
        }
        for (searchOption in SearchOption.values()) mFrame.findViewById<View>(
            searchOption.mResId
        ).setOnClickListener(this)
        refreshSearchVisibility()
    }
}