package com.mapswithme.maps.widget.placepage

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Rect
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GestureDetectorCompat
import com.mapswithme.maps.Framework
import com.mapswithme.maps.R
import com.mapswithme.maps.ads.DefaultAdTracker
import com.mapswithme.maps.ads.Factory
import com.mapswithme.maps.ads.MwmNativeAd
import com.mapswithme.maps.bookmarks.data.MapObject
import com.mapswithme.maps.bookmarks.data.MapObject.OpeningMode
import com.mapswithme.maps.bookmarks.data.RoadWarningMarkType
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.location.LocationListener
import com.mapswithme.maps.promo.Promo
import com.mapswithme.maps.purchase.AdsRemovalPurchaseControllerProvider
import com.mapswithme.maps.widget.placepage.BannerController
import com.mapswithme.maps.widget.placepage.BannerController.BannerStateListener
import com.mapswithme.maps.widget.placepage.BannerController.BannerStateRequester
import com.mapswithme.util.Graphics
import com.mapswithme.util.NetworkPolicy
import com.mapswithme.util.UiUtils
import com.mapswithme.util.UiUtils.SimpleAnimatorListener
import com.mapswithme.util.log.LoggerFactory
import com.mapswithme.util.statistics.PlacePageTracker
import com.trafi.anchorbottomsheetbehavior.AnchorBottomSheetBehavior

class BottomSheetPlacePageController(
    private val mActivity: Activity,
    private val mPurchaseControllerProvider: AdsRemovalPurchaseControllerProvider,
    private val mSlideListener: PlacePageController.SlideListener,
    routingModeListener: RoutingModeListener?
) : PlacePageController, LocationListener,
    View.OnLayoutChangeListener, BannerStateRequester, BannerStateListener, Closable {
    private lateinit var mPlacePageBehavior: AnchorBottomSheetBehavior<View>
    private lateinit var mButtonsLayout: View
    private lateinit var mPlacePage: PlacePageView
    private lateinit var mPlacePageTracker: PlacePageTracker
    private lateinit var mToolbar: Toolbar
    private var mViewportMinHeight = 0
    private var mCurrentTop = 0
    private var mPeekHeightAnimating = false
    private var mOpenBannerTouchSlop = 0
    /**
     * Represents a value that describes how much banner details are opened.
     * Must be in the range [0;1]. 0 means that the banner details are completely closed,
     * 1 - the details are completely opened.
     */
    private var mBannerRatio = 0f
    private lateinit var mBannerController: BannerController
    private val mGestureDetector: GestureDetectorCompat
    private val mRoutingModeListener: RoutingModeListener?
    private val mSheetCallback: AnchorBottomSheetBehavior.BottomSheetCallback =
        object : AnchorBottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(
                bottomSheet: View,
                oldState: Int,
                newState: Int
            ) {
                LOGGER.d(
                    TAG,
                    "State change, new = " + toString(
                        newState
                    )
                            + " old = " + toString(oldState)
                            + " placepage height = " + mPlacePage.height
                )
                if (isSettlingState(newState) || isDraggingState(
                        newState
                    )
                ) {
                    return
                }
                if (isHiddenState(newState)) {
                    onHiddenInternal()
                    return
                }
                setPullDrawable()
                if (isAnchoredState(newState) || isExpandedState(
                        newState
                    )
                ) {
                    mBannerController.onPlacePageStateChanged()
                    mPlacePageTracker.onDetails()
                    return
                }
                mBannerController.onPlacePageStateChanged()
                setPeekHeight()
            }

            override fun onSlide(
                bottomSheet: View,
                slideOffset: Float
            ) {
                mSlideListener.onPlacePageSlide(bottomSheet.top)
                mPlacePageTracker.onMove()
                if (slideOffset < 0) return
                updateViewPortRect()
                resizeBanner()
            }
        }

    private fun onHiddenInternal() {
        Framework.nativeDeactivatePopup()
        updateViewPortRect()
        UiUtils.invisible(mButtonsLayout)
        mPlacePageTracker.onHidden()
    }

    private fun setPullDrawable() {
        @AnchorBottomSheetBehavior.State val state = mPlacePageBehavior.state
        @DrawableRes var drawableId = UiUtils.NO_ID
        if (isCollapsedState(state)) drawableId =
            R.drawable.ic_disclosure_up else if (isAnchoredState(
                state
            ) || isExpandedState(state)
        ) drawableId = R.drawable.ic_disclosure_down
        if (drawableId == UiUtils.NO_ID) return
        val img =
            mPlacePage.findViewById<ImageView>(R.id.pull_icon)
        val drawable = Graphics.tint(
            mActivity,
            drawableId,
            R.attr.bannerButtonBackgroundColor
        )
        img.setImageDrawable(drawable)
    }

    private fun resizeBanner() {
        val lastTop = mCurrentTop
        mCurrentTop = mPlacePage.top
        if (!mBannerController.hasAd()) return
        val bannerMaxY = calculateBannerMaxY()
        val bannerMinY = calculateBannerMinY()
        val maxDistance = Math.abs(bannerMaxY - bannerMinY)
        val yDistance = Math.abs(mCurrentTop - bannerMinY)
        val ratio = yDistance.toFloat() / maxDistance
        mBannerRatio = ratio
        if (ratio >= 1) {
            mBannerController.zoomOut(1f)
            mBannerController.open()
            return
        }
        if (ratio == 0f) {
            mBannerController.zoomIn(ratio)
            mBannerController.close()
            return
        }
        if (mCurrentTop < lastTop) mBannerController.zoomOut(ratio) else mBannerController.zoomIn(
            ratio
        )
    }

    private fun calculateBannerMaxY(): Int {
        val coordinatorLayout: View = mPlacePage.parent as ViewGroup
        val height = coordinatorLayout.height
        val maxY =
            if (mPlacePage.height > height * (1 - ANCHOR_RATIO)) (height * ANCHOR_RATIO).toInt() else height - mPlacePage.height
        return maxY + mOpenBannerTouchSlop
    }

    private fun calculateBannerMinY(): Int {
        val coordinatorLayout: View = mPlacePage.parent as ViewGroup
        val height = coordinatorLayout.height
        return height - mPlacePageBehavior.peekHeight
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initialize() {
        val res = mActivity.resources
        mViewportMinHeight = res.getDimensionPixelSize(R.dimen.viewport_min_height)
        mOpenBannerTouchSlop = res.getDimensionPixelSize(R.dimen.placepage_banner_open_touch_slop)
        mToolbar = mActivity.findViewById(R.id.pp_toolbar)
        UiUtils.extendViewWithStatusBar(mToolbar)
        UiUtils.showHomeUpButton(mToolbar)
        mToolbar.setNavigationOnClickListener { v: View? -> close() }
        mPlacePage = mActivity.findViewById(R.id.placepage)
        mPlacePageBehavior = AnchorBottomSheetBehavior.from(mPlacePage)
        mPlacePageBehavior.addBottomSheetCallback(mSheetCallback)
        mPlacePage.setOnTouchListener { v: View?, event: MotionEvent? ->
            mGestureDetector.onTouchEvent(
                event
            )
        }
        mPlacePage.addOnLayoutChangeListener(this)
        mPlacePage.addClosable(this)
        mPlacePage.setRoutingModeListener(mRoutingModeListener)
        val bannerContainer = mPlacePage.findViewById<ViewGroup>(R.id.banner_container)
        val tracker = DefaultAdTracker()
        val loader = Factory.createCompoundLoader(
            tracker,
            tracker
        )
        mBannerController = BannerController(
            bannerContainer, loader, tracker,
            mPurchaseControllerProvider, this, this
        )
        mButtonsLayout = mActivity.findViewById(R.id.pp_buttons_layout)
        val buttons = mButtonsLayout.findViewById<ViewGroup>(R.id.container)
        mPlacePage.initButtons(buttons)
        UiUtils.bringViewToFrontOf(mButtonsLayout, mPlacePage)
        UiUtils.bringViewToFrontOf(
            mActivity.findViewById(R.id.app_bar),
            mPlacePage
        )
        mPlacePageTracker = PlacePageTracker(mPlacePage, mButtonsLayout)
        LocationHelper.INSTANCE.addListener(this)
    }

    override fun destroy() {
        LocationHelper.INSTANCE.removeListener(this)
    }

    override fun openFor(`object`: MapObject) {
        mPlacePage.setMapObject(
            `object`,
            object: PlacePageView.SetMapObjectListener {
                override fun onSetMapObjectComplete(policy: NetworkPolicy, isSameObject: Boolean) {
                    @AnchorBottomSheetBehavior.State val state = mPlacePageBehavior.state
                    if (isSameObject && !isHiddenState(state)) return
                    mBannerRatio = 0f
                    mPlacePage.resetScroll()
                    if (`object`.openingMode == MapObject.OPENING_MODE_DETAILS) {
                        mPlacePageBehavior.state = AnchorBottomSheetBehavior.STATE_ANCHORED
                        return
                    }
                    UiUtils.show(mButtonsLayout)
                    openPlacePage()
                    showBanner(`object`, policy)
                }
            }
        )
        mToolbar.title = `object`.title
        mPlacePageTracker.setMapObject(`object`)
        Framework.logLocalAdsEvent(Framework.LocalAdsEventType.LOCAL_ADS_EVENT_OPEN_INFO, `object`)
    }

    private fun showBanner(`object`: MapObject, policy: NetworkPolicy) {
        val canShowBanner =
            (`object`.mapObjectType != MapObject.MY_POSITION && policy.canUseNetwork()
                    && `object`.roadWarningMarkType === RoadWarningMarkType.UNKNOWN)
        mBannerController.updateData(if (canShowBanner) `object`.banners?.toList() else null)
    }

    private fun openPlacePage() {
        mPlacePage.post {
            setPeekHeight()
            mPlacePageBehavior.state = AnchorBottomSheetBehavior.STATE_COLLAPSED
            setPlacePageAnchor()
        }
    }

    private fun setPeekHeight() {
        if (mPeekHeightAnimating) {
            Log.d(
                TAG,
                "Peek animation in progress, ignore."
            )
            return
        }
        // If banner details are little bit or completely opened we haven't to change the peek height,
// because the peek height is reasonable only for collapsed state and banner details are always
// closed in collapsed state.
        if (mBannerRatio > 0) return
        val peekHeight = calculatePeekHeight()
        if (peekHeight == mPlacePageBehavior.peekHeight) return
        @AnchorBottomSheetBehavior.State val currentState = mPlacePageBehavior.state
        if (isSettlingState(currentState) || isDraggingState(
                currentState
            )
        ) {
            LOGGER.d(
                TAG,
                "Sheet state inappropriate, ignore."
            )
            return
        }
        if (isCollapsedState(currentState) && mPlacePageBehavior.peekHeight > 0) {
            setPeekHeightAnimatedly(peekHeight)
            return
        }
        mPlacePageBehavior.peekHeight = peekHeight
    }

    private fun setPeekHeightAnimatedly(peekHeight: Int) {
        val delta = peekHeight - mPlacePageBehavior.peekHeight
        val animator =
            ObjectAnimator.ofFloat(mPlacePage, "translationY", -delta.toFloat())
        animator.duration =
            if (delta == mBannerController.closedHeight) ANIM_BANNER_APPEARING_MS.toLong() else ANIM_CHANGE_PEEK_HEIGHT_MS.toLong()
        animator.addListener(object : SimpleAnimatorListener() {
            override fun onAnimationStart(animation: Animator) {
                mPeekHeightAnimating = true
                mPlacePage.setScrollable(false)
                mPlacePageBehavior.allowUserDragging = false
            }

            override fun onAnimationEnd(animation: Animator) {
                mPlacePage.translationY = 0f
                mPeekHeightAnimating = false
                mPlacePage.setScrollable(true)
                mPlacePageBehavior.allowUserDragging = true
                mPlacePageBehavior.peekHeight = peekHeight
            }
        })
        animator.addUpdateListener { animation: ValueAnimator? -> onUpdateTranslation() }
        animator.start()
    }

    private fun onUpdateTranslation() {
        mSlideListener.onPlacePageSlide((mPlacePage.top + mPlacePage.translationY).toInt())
    }

    private fun setPlacePageAnchor() {
        val parent = mPlacePage.parent as View
        mPlacePageBehavior.anchorOffset = (parent.height * ANCHOR_RATIO).toInt()
    }

    private fun calculatePeekHeight(): Int {
        val organicPeekHeight = mPlacePage.previewHeight + mButtonsLayout.height
        val `object` = mPlacePage.mapObject
        if (`object` != null) {
            @OpeningMode val mode = `object`.openingMode
            if (mode == MapObject.OPENING_MODE_PREVIEW_PLUS) {
                val parent = mPlacePage.parent as View
                val promoPeekHeight =
                    (parent.height * PREVIEW_PLUS_RATIO).toInt()
                return if (promoPeekHeight <= organicPeekHeight) organicPeekHeight else promoPeekHeight
            }
        }
        return organicPeekHeight
    }

    override fun close() {
        mPlacePageBehavior.state = AnchorBottomSheetBehavior.STATE_HIDDEN
        mPlacePage.reset()
    }

    override val isClosed: Boolean
        get() = isHiddenState(mPlacePageBehavior.state)

    override fun onLocationUpdated(location: Location) {
        mPlacePage.refreshLocation(location)
    }

    override fun onCompassUpdated(
        time: Long,
        magneticNorth: Double,
        trueNorth: Double,
        accuracy: Double
    ) {
        @AnchorBottomSheetBehavior.State val currentState = mPlacePageBehavior.state
        if (isHiddenState(currentState) || isDraggingState(
                currentState
            ) || isSettlingState(currentState)
        ) return
        val north = if (trueNorth >= 0.0) trueNorth else magneticNorth
        mPlacePage.refreshAzimuth(north)
    }

    override fun onLocationError(errorCode: Int) { // Do nothing by default.
    }

    override fun onLayoutChange(
        v: View,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int
    ) {
        if (mPlacePageBehavior.peekHeight == 0) {
            LOGGER.d(
                TAG,
                "Layout change ignored, peek height not calculated yet"
            )
            return
        }
        mPlacePage.post { setPeekHeight() }
        if (isHiddenState(mPlacePageBehavior.state)) return
        updateViewPortRect()
    }

    private fun updateViewPortRect() {
        mPlacePage.post {
            val coordinatorLayout: View = mPlacePage.parent as ViewGroup
            val viewPortWidth = coordinatorLayout.width
            var viewPortHeight = coordinatorLayout.height
            val sheetRect = Rect()
            mPlacePage.getGlobalVisibleRect(sheetRect)
            if (sheetRect.top < mViewportMinHeight) return@post
            if (sheetRect.top >= viewPortHeight) {
                Framework.nativeSetVisibleRect(0, 0, viewPortWidth, viewPortHeight)
                return@post
            }
            viewPortHeight -= sheetRect.height()
            Framework.nativeSetVisibleRect(0, 0, viewPortWidth, viewPortHeight)
        }
    }

    override fun onSave(outState: Bundle?) {
        mPlacePageTracker.onSave(outState)
        outState?.putParcelable(
            EXTRA_MAP_OBJECT,
            mPlacePage.mapObject
        )
    }

    override fun onRestore(inState: Bundle?) {
        mPlacePageTracker.onRestore(inState)
        if (mPlacePageBehavior.state == AnchorBottomSheetBehavior.STATE_HIDDEN) return
        if (!Framework.nativeHasPlacePageInfo()) {
            close()
            return
        }
        val `object`: MapObject =
            inState?.getParcelable(EXTRA_MAP_OBJECT) ?: return
        @AnchorBottomSheetBehavior.State val state = mPlacePageBehavior.state
        mPlacePage.setMapObject(
            `object`,
            object : PlacePageView.SetMapObjectListener {
                override fun onSetMapObjectComplete(policy: NetworkPolicy, isSameObject: Boolean) {
                    restorePlacePageState(
                        `object`,
                        policy,
                        state
                    )
                }
            }
        )
        mToolbar.title = `object`.title
    }

    private fun restorePlacePageState(
        `object`: MapObject, policy: NetworkPolicy,
        @AnchorBottomSheetBehavior.State state: Int
    ) {
        mPlacePage.post {
            setPlacePageAnchor()
            mPlacePageBehavior.state = state
            UiUtils.show(mButtonsLayout)
            setPeekHeight()
            showBanner(`object`, policy)
            setPullDrawable()
        }
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?
    ) {
    }

    override fun onActivityStarted(activity: Activity) {
        mBannerController.attach()
        mPlacePage.attach(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        mBannerController.onChangedVisibility(true)
    }

    override fun onActivityPaused(activity: Activity) {
        mBannerController.onChangedVisibility(false)
    }

    override fun onActivityStopped(activity: Activity) {
        mBannerController.detach()
        mPlacePage.detach()
    }

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle
    ) { // No op.
    }

    override fun onActivityDestroyed(activity: Activity) {
        Promo.INSTANCE.setListener(null)
    }

    override fun requestBannerState(): BannerController.BannerState? {
        @AnchorBottomSheetBehavior.State val state = mPlacePageBehavior.state
        if (isSettlingState(state) || isDraggingState(
                state
            ) || isHiddenState(state)
        ) return null
        return if (isAnchoredState(state) || isExpandedState(
                state
            )
        ) BannerController.BannerState.DETAILS else BannerController.BannerState.PREVIEW
    }

    override fun onBannerDetails(ad: MwmNativeAd) {
        mPlacePageTracker.onBannerDetails(ad)
    }

    override fun onBannerPreview(ad: MwmNativeAd) {
        mPlacePageTracker.onBannerPreview(ad)
    }

    override fun closePlacePage() {
        close()
    }

    private inner class PlacePageGestureListener : SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            @AnchorBottomSheetBehavior.State val state = mPlacePageBehavior.state
            if (isCollapsedState(state)) {
                mPlacePageBehavior.state = AnchorBottomSheetBehavior.STATE_ANCHORED
                return true
            }
            if (isAnchoredState(state) || isExpandedState(
                    state
                )
            ) {
                mPlacePage.resetScroll()
                mPlacePageBehavior.state = AnchorBottomSheetBehavior.STATE_COLLAPSED
                return true
            }
            return false
        }
    }

    companion object {
        private const val ANCHOR_RATIO = 0.3f
        private const val PREVIEW_PLUS_RATIO = 0.45f
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = BottomSheetPlacePageController::class.java.simpleName
        private const val EXTRA_MAP_OBJECT = "extra_map_object"
        private const val ANIM_BANNER_APPEARING_MS = 300
        private const val ANIM_CHANGE_PEEK_HEIGHT_MS = 100
        private fun toString(@AnchorBottomSheetBehavior.State state: Int): String {
            return when (state) {
                AnchorBottomSheetBehavior.STATE_EXPANDED -> "EXPANDED"
                AnchorBottomSheetBehavior.STATE_COLLAPSED -> "COLLAPSED"
                AnchorBottomSheetBehavior.STATE_ANCHORED -> "ANCHORED"
                AnchorBottomSheetBehavior.STATE_DRAGGING -> "DRAGGING"
                AnchorBottomSheetBehavior.STATE_SETTLING -> "SETTLING"
                AnchorBottomSheetBehavior.STATE_HIDDEN -> "HIDDEN"
                else -> throw AssertionError("Unsupported state detected: $state")
            }
        }

        private fun isSettlingState(@AnchorBottomSheetBehavior.State state: Int): Boolean {
            return state == AnchorBottomSheetBehavior.STATE_SETTLING
        }

        private fun isDraggingState(@AnchorBottomSheetBehavior.State state: Int): Boolean {
            return state == AnchorBottomSheetBehavior.STATE_DRAGGING
        }

        private fun isCollapsedState(@AnchorBottomSheetBehavior.State state: Int): Boolean {
            return state == AnchorBottomSheetBehavior.STATE_COLLAPSED
        }

        private fun isAnchoredState(@AnchorBottomSheetBehavior.State state: Int): Boolean {
            return state == AnchorBottomSheetBehavior.STATE_ANCHORED
        }

        private fun isExpandedState(@AnchorBottomSheetBehavior.State state: Int): Boolean {
            return state == AnchorBottomSheetBehavior.STATE_EXPANDED
        }

        private fun isHiddenState(@AnchorBottomSheetBehavior.State state: Int): Boolean {
            return state == AnchorBottomSheetBehavior.STATE_HIDDEN
        }
    }

    init {
        mGestureDetector = GestureDetectorCompat(mActivity, PlacePageGestureListener())
        mRoutingModeListener = routingModeListener
    }
}