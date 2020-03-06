package com.mapswithme.maps

import android.content.res.Resources
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils

import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.location.LocationState
import com.mapswithme.maps.routing.RoutingController
import com.mapswithme.util.Config
import com.mapswithme.util.UiUtils

internal class NavigationButtonsAnimationController(
    private val mZoomIn: View, private val mZoomOut: View,
    private val mMyPosition: View, contentView: View,
    private val mTranslationListener: OnTranslationChangedListener?,
    private var mObBoardingTipBtnContainer: View?
) {

    private val mMargin: Float
    private var mContentHeight: Float = 0.toFloat()
    private var mMyPositionBottom: Float = 0.toFloat()

    private var mTopLimit: Float = 0.toFloat()
    private var mBottomLimit: Float = 0.toFloat()

    private val mCompassHeight: Float

    init {
        checkZoomButtonsVisibility()
        val res = mZoomIn.resources
        mMargin = res.getDimension(R.dimen.margin_base_plus)
        mBottomLimit = res.getDimension(R.dimen.menu_line_height)
        mCompassHeight = res.getDimension(R.dimen.compass_height)
        calculateLimitTranslations()
        contentView.addOnLayoutChangeListener(ContentViewLayoutChangeListener(contentView))
        if (mObBoardingTipBtnContainer != null) {
            val animation = AnimationUtils.loadAnimation(
                mObBoardingTipBtnContainer!!.context,
                R.anim.dog_btn_rotation
            )
            mObBoardingTipBtnContainer!!.findViewById<View>(R.id.onboarding_btn).animation = animation
        }
    }

    private fun checkZoomButtonsVisibility() {
        UiUtils.showIf(showZoomButtons(), mZoomIn, mZoomOut)
    }


    private fun calculateLimitTranslations() {
        mTopLimit = mMargin
        mMyPosition.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View, left: Int, top: Int, right: Int, bottom: Int,
                oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
            ) {
                mMyPositionBottom = bottom.toFloat()
                mMyPosition.removeOnLayoutChangeListener(this)
            }
        })
    }

    fun setTopLimit(limit: Float) {
        mTopLimit = limit + mMargin
        update()
    }

    fun setBottomLimit(limit: Float) {
        mBottomLimit = limit
        update()
    }

    fun move(translationY: Float) {
        if (mMyPositionBottom == 0f || mContentHeight == 0f)
            return

        val translation = translationY - mMyPositionBottom
        update(if (translation <= 0) translation else 0F)
    }

    private fun update(translation: Float = mZoomIn.translationY) {
        mMyPosition.translationY = translation
        mZoomOut.translationY = translation
        mZoomIn.translationY = translation
        if (mObBoardingTipBtnContainer != null)
            mObBoardingTipBtnContainer!!.translationY = translation

        if (mZoomIn.visibility == View.VISIBLE && !isViewInsideLimits(mZoomIn)) {
            UiUtils.invisible(mZoomIn, mZoomOut)
            if (mObBoardingTipBtnContainer != null)
                UiUtils.invisible(mObBoardingTipBtnContainer!!)

            mTranslationListener?.onFadeOutZoomButtons()
        } else if (mZoomIn.visibility == View.INVISIBLE && isViewInsideLimits(mZoomIn)) {
            UiUtils.show(mZoomIn, mZoomOut)
            if (mObBoardingTipBtnContainer != null)
                UiUtils.show(mObBoardingTipBtnContainer!!)
            mTranslationListener?.onFadeInZoomButtons()
        }

        if (!shouldBeHidden() && mMyPosition.visibility == View.VISIBLE
            && !isViewInsideLimits(mMyPosition)
        ) {
            UiUtils.invisible(mMyPosition)
        } else if (!shouldBeHidden() && mMyPosition.visibility == View.INVISIBLE
            && isViewInsideLimits(mMyPosition)
        ) {
            UiUtils.show(mMyPosition)
        }
        mTranslationListener?.onTranslationChanged(translation)
    }

    private fun isViewInsideLimits(view: View): Boolean {
        return view.y >= mTopLimit && view.bottom + view.translationY <= mContentHeight - mBottomLimit
    }

    private fun shouldBeHidden(): Boolean {
        return LocationHelper.INSTANCE.myPositionMode == LocationState.FOLLOW_AND_ROTATE && (RoutingController.get().isPlanning || RoutingController.get().isNavigating)
    }

    fun disappearZoomButtons() {
        if (!showZoomButtons())
            return

        UiUtils.hide(mZoomIn, mZoomOut)
        if (mObBoardingTipBtnContainer == null)
            return

        UiUtils.hide(mObBoardingTipBtnContainer!!)
    }

    fun hideOnBoardingTipBtn() {
        if (mObBoardingTipBtnContainer == null)
            return

        mObBoardingTipBtnContainer!!.visibility = View.GONE
        mObBoardingTipBtnContainer = null
    }

    fun appearZoomButtons() {
        if (!showZoomButtons())
            return

        UiUtils.show(mZoomIn, mZoomOut)

        if (mObBoardingTipBtnContainer == null)
            return

        UiUtils.show(mObBoardingTipBtnContainer!!)
    }

    private fun showZoomButtons(): Boolean {
        return Config.showZoomButtons()
    }

    fun onResume() {
        checkZoomButtonsVisibility()
    }

    fun isConflictWithCompass(compassOffset: Int): Boolean {
        val zoomTop = mZoomIn.top
        return zoomTop != 0 && zoomTop <= compassOffset + mCompassHeight
    }

    internal interface OnTranslationChangedListener {
        fun onTranslationChanged(translation: Float)

        fun onFadeInZoomButtons()

        fun onFadeOutZoomButtons()
    }

    private inner class ContentViewLayoutChangeListener(private val mContentView: View) : View.OnLayoutChangeListener {

        override fun onLayoutChange(
            v: View, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int,
            oldTop: Int, oldRight: Int, oldBottom: Int
        ) {
            mContentHeight = (bottom - top).toFloat()
            mContentView.removeOnLayoutChangeListener(this)
        }
    }
}
