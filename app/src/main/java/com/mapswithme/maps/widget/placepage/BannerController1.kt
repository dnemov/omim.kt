package com.mapswithme.maps.widget.placepage

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.mapswithme.maps.R
import com.mapswithme.maps.ads.*
import com.mapswithme.maps.purchase.AdsRemovalPurchaseControllerProvider
import com.mapswithme.maps.purchase.AdsRemovalPurchaseDialog
import com.mapswithme.maps.purchase.PurchaseController
import com.mapswithme.maps.widget.placepage.BannerController
import com.mapswithme.maps.widget.placepage.NativeAdWrapper.UiType
import com.mapswithme.util.ThemeUtils
import com.mapswithme.util.UiUtils
import com.mapswithme.util.Utils
import com.mapswithme.util.log.LoggerFactory
import com.mapswithme.util.statistics.Statistics
import com.mapswithme.util.statistics.Statistics.EventName
import java.util.*

class BannerController(
    bannerContainer: ViewGroup, loader: CompoundNativeAdLoader,
    tracker: AdTracker?,
    adsRemovalProvider: AdsRemovalPurchaseControllerProvider,
    bannerStateRequester: BannerStateRequester,
    bannerStateListener: BannerStateListener
) : PlacePageStateObserver {
    private var mBanners: List<Banner>? = null
    private val mContainerView: ViewGroup
    private var mBannerView: View
    private lateinit var mIcon: ImageView
    private lateinit var mTitle: TextView
    private lateinit var mMessage: TextView
    private lateinit var mActionSmall: TextView
    private lateinit var mActionContainer: View
    private lateinit var mActionLarge: TextView
    private lateinit var mAdChoices: ImageView
    private lateinit var mAdChoicesLabel: ImageView
    private lateinit var mAdsRemovalIcon: View
    private lateinit var mAdsRemovalButton: View
    private var mState: BannerState? = null
    private var mError = false
    private var mCurrentAd: NativeAdWrapper? = null
    private val mAdsLoader: CompoundNativeAdLoader
    private val mAdTracker: AdTracker?
    private val mAdsListener = MyNativeAdsListener()
    private val mAdsRemovalProvider: AdsRemovalPurchaseControllerProvider
    var closedHeight = 0
        private set
    private var mOpenedHeight = 0
    private val mBannerStateRequester: BannerStateRequester
    private val mBannerStateListener: BannerStateListener
    private fun initBannerViews() {
        mIcon = mBannerView.findViewById(R.id.iv__banner_icon)
        mTitle = mBannerView.findViewById(R.id.tv__banner_title)
        mMessage = mBannerView.findViewById(R.id.tv__banner_message)
        mActionSmall = mBannerView.findViewById(R.id.tv__action_small)
        mActionContainer = mBannerView.findViewById(R.id.action_container)
        mActionLarge = mActionContainer.findViewById(R.id.tv__action_large)
        mAdsRemovalButton = mActionContainer.findViewById(R.id.tv__action_remove)
        mAdsRemovalButton.setOnClickListener { clickedView: View ->
            handleAdsRemoval(
                clickedView
            )
        }
        mAdChoices = mBannerView.findViewById(R.id.ad_choices_icon)
        mAdChoices.setOnClickListener { v: View? -> handlePrivacyInfoUrl() }
        mAdChoicesLabel = mBannerView.findViewById(R.id.ad_choices_label)
        mAdsRemovalIcon = mBannerView.findViewById(R.id.remove_btn)
        mAdsRemovalIcon.setOnClickListener { clickedView: View ->
            handleAdsRemoval(
                clickedView
            )
        }
        expandTouchArea()
    }

    private fun expandTouchArea() {
        val res = mBannerView.resources
        val tapArea = res.getDimensionPixelSize(R.dimen.margin_quarter_plus)
        UiUtils.expandTouchAreaForViews(tapArea, mAdChoices)
        val crossArea = res.getDimensionPixelSize(R.dimen.margin_base_plus)
        UiUtils.expandTouchAreaForView(mAdsRemovalIcon, tapArea, crossArea, tapArea, crossArea)
    }

    private fun handlePrivacyInfoUrl() {
        if (mCurrentAd == null) return
        val privacyUrl: String = mCurrentAd!!.privacyInfoUrl!!
        if (TextUtils.isEmpty(privacyUrl)) return
        Utils.openUrl(mBannerView.context, privacyUrl)
    }

    private fun handleAdsRemoval(clickedView: View) {
        val isCross = clickedView.id == R.id.remove_btn
        @Statistics.BannerState val state =
            if (isDetailsState(mState)) Statistics.PP_BANNER_STATE_DETAILS else Statistics.PP_BANNER_STATE_PREVIEW
        Statistics.INSTANCE.trackPPBannerClose(state, isCross)
        val activity = mBannerView.context as FragmentActivity
        AdsRemovalPurchaseDialog.show(activity)
    }

    private fun setErrorStatus(value: Boolean) {
        mError = value
    }

    private fun hasErrorOccurred(): Boolean {
        return mError
    }

    private fun updateVisibility() {
        if (mBanners == null) throw AssertionError("Banners must be non-null at this point!")
        UiUtils.hideIf(hasErrorOccurred() || mCurrentAd == null, mContainerView)
        if (mCurrentAd == null) throw AssertionError("Banners must be non-null at this point!")
        UiUtils.showIf(mCurrentAd!!.type.showAdChoiceIcon(), mAdChoices)
        val purchaseController: PurchaseController<*>? =
            mAdsRemovalProvider.adsRemovalPurchaseController
        val showRemovalButtons = (purchaseController != null
                && purchaseController.isPurchaseSupported)
        UiUtils.showIf(showRemovalButtons, mAdsRemovalIcon, mAdsRemovalButton)
        UiUtils.show(
            mIcon, mTitle, mMessage, mActionSmall, mActionContainer, mActionLarge,
            mAdsRemovalButton, mAdChoicesLabel
        )
        if (isDetailsState(mState)) UiUtils.hide(mActionSmall) else UiUtils.hide(
            mActionContainer,
            mActionLarge,
            mAdsRemovalButton,
            mIcon
        )
        UiUtils.show(mBannerView)
    }

    fun updateData(banners: List<Banner>?) {
        if (mBanners != null && mBanners != banners) {
            onChangedVisibility(false)
            unregisterCurrentAd()
        }
        setErrorStatus(false)
        mBanners = if (banners != null) Collections.unmodifiableList(banners) else null
        UiUtils.showIf(mBanners != null, mContainerView)
        if (mBanners == null) return
        UiUtils.hide(mBannerView)
        mAdsLoader.loadAd(mContainerView.context, mBanners!!)
    }

    private fun unregisterCurrentAd() {
        if (mCurrentAd != null) {
            LOGGER.d(
                TAG,
                "Unregister view for the ad: " + mCurrentAd!!.title
            )
            mCurrentAd!!.unregisterView(mBannerView)
            mCurrentAd = null
        }
    }

    private val isBannerContainerVisible: Boolean
        private get() = UiUtils.isVisible(mContainerView)

    fun open() {
        if (!isBannerContainerVisible || mBanners == null || isDetailsState(
                mState
            )
        ) return
        setOpenedStateInternal()
        if (mCurrentAd != null) {
            loadIcon(mCurrentAd!!)
            mCurrentAd!!.registerView(mBannerView)
            mBannerStateListener.onBannerDetails(mCurrentAd!!)
        }
    }

    fun zoomIn(ratio: Float) {
        val banner = mContainerView.findViewById<ViewGroup>(R.id.banner)
        val lp = banner.layoutParams
        lp.height = ((mOpenedHeight - closedHeight) * ratio + closedHeight).toInt()
        banner.layoutParams = lp
    }

    fun zoomOut(ratio: Float) {
        val banner = mContainerView.findViewById<ViewGroup>(R.id.banner)
        val lp = banner.layoutParams
        lp.height = (closedHeight - (closedHeight - mOpenedHeight) * ratio).toInt()
        banner.layoutParams = lp
    }

    fun close() {
        if (!isBannerContainerVisible || mBanners == null) return
        setClosedStateInternal()
        if (mCurrentAd != null) {
            mCurrentAd!!.registerView(mBannerView)
            mBannerStateListener.onBannerPreview(mCurrentAd!!)
        }
    }

    private fun discardBannerSize() {
        zoomOut(0f)
    }

    private fun measureBannerSizes() {
        val dm = mContainerView.resources.displayMetrics
        val screenWidth = dm.widthPixels.toFloat()
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
            0,
            View.MeasureSpec.UNSPECIFIED
        )
        val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(
            screenWidth.toInt(),
            View.MeasureSpec.AT_MOST
        )
        setClosedStateInternal()
        mBannerView.measure(widthMeasureSpec, heightMeasureSpec)
        closedHeight = mBannerView.measuredHeight
        LOGGER.d(
            TAG,
            "Banner close height = " + closedHeight
        )
        setOpenedStateInternal()
        mBannerView.measure(widthMeasureSpec, heightMeasureSpec)
        mOpenedHeight = mBannerView.measuredHeight
        LOGGER.d(
            TAG,
            "Banner open height = $mOpenedHeight"
        )
    }

    private fun setOpenedStateInternal() {
        mState = BannerState.DETAILS
        mMessage.maxLines = MAX_MESSAGE_LINES
        mTitle.maxLines = MAX_TITLE_LINES
        updateVisibility()
    }

    private fun setClosedStateInternal() {
        mState = BannerState.PREVIEW
        UiUtils.hide(mIcon)
        mMessage.maxLines = MIN_MESSAGE_LINES
        mTitle.maxLines = MIN_TITLE_LINES
        updateVisibility()
    }

    private fun loadIcon(ad: MwmNativeAd) {
        UiUtils.show(mIcon)
        ad.loadIcon(mIcon)
    }

    fun onChangedVisibility(isVisible: Boolean) {
        if (mAdTracker == null || mCurrentAd == null) return
        if (isVisible) {
            mAdTracker.onViewShown(mCurrentAd!!.provider, mCurrentAd!!.bannerId)
            mCurrentAd!!.registerView(mBannerView)
        } else {
            mAdTracker.onViewHidden(mCurrentAd!!.provider, mCurrentAd!!.bannerId)
            mCurrentAd!!.unregisterView(mBannerView)
        }
    }

    fun detach() {
        mAdsLoader.detach()
        mAdsLoader.setAdListener(null)
    }

    fun attach() {
        mAdsLoader.setAdListener(mAdsListener)
    }

    private fun fillViews(data: MwmNativeAd) {
        mTitle.text = data.title
        mMessage.text = data.description
        mActionSmall.text = data.action
        mActionLarge.text = data.action
    }

    private fun animateActionButton() {
        val animator: ObjectAnimator
        animator = if (isDetailsState(mState)) {
            val context = mBannerView.context
            val res = context.resources
            val colorFrom =
                if (ThemeUtils.isNightTheme) res.getColor(R.color.white_12) else res.getColor(
                    R.color.black_12
                )
            val colorTo =
                if (ThemeUtils.isNightTheme) res.getColor(R.color.white_24) else res.getColor(
                    R.color.black_24
                )
            ObjectAnimator.ofObject(
                mActionLarge, "backgroundColor", ArgbEvaluator(),
                colorFrom, colorTo, colorFrom
            )
        } else {
            ObjectAnimator.ofFloat(mActionSmall, "alpha", 0.3f, 1f)
        }
        animator.duration = 300
        animator.start()
    }

    fun hasAd(): Boolean {
        return mCurrentAd != null
    }

    private fun setBannerState(state: BannerState?) {
        if (mCurrentAd == null) throw AssertionError("Current ad must be non-null at this point!")
        if (state == null) {
            LOGGER.d(
                TAG,
                "Banner state not determined yet, discard banner size"
            )
            setBannerInitialHeight(0)
            mState = null
            return
        }
        if (isDetailsState(state)) {
            open()
            loadIcon(mCurrentAd!!)
            setBannerInitialHeight(mOpenedHeight)
            return
        }
        if (isPreviewState(state)) {
            close()
            setBannerInitialHeight(closedHeight)
            mBannerStateListener.onBannerPreview(mCurrentAd!!)
        }
    }

    private fun setBannerInitialHeight(height: Int) {
        LOGGER.d(
            TAG,
            "Set banner initial height = $height"
        )
        val banner = mContainerView.findViewById<ViewGroup>(R.id.banner)
        val lp = banner.layoutParams
        lp.height = height
        banner.layoutParams = lp
    }

    override fun onPlacePageStateChanged() {
        if (mCurrentAd == null) return
        val newState =
            mBannerStateRequester.requestBannerState()
        setBannerState(newState)
    }

    private inner class MyNativeAdsListener :
        NativeAdListener {
        private var mLastAdType: UiType? = null
        override fun onAdLoaded(ad: MwmNativeAd) {
            LOGGER.d(
                TAG,
                "onAdLoaded, ad = $ad"
            )
            if (mBanners == null) return
            unregisterCurrentAd()
            discardBannerSize()
            mCurrentAd = NativeAdWrapper(ad)
            if (mLastAdType != mCurrentAd!!.type) {
                mBannerView = inflateBannerLayout(
                    mCurrentAd!!.type,
                    mContainerView
                )
                initBannerViews()
            }
            mLastAdType = mCurrentAd!!.type
            fillViews(ad)
            measureBannerSizes()
            val state =
                mBannerStateRequester.requestBannerState()
            setBannerState(state)
            ad.registerView(mBannerView)
            if (mAdTracker != null) {
                onChangedVisibility(isBannerContainerVisible)
                mAdTracker.onContentObtained(ad.provider, ad.bannerId)
            }
        }

        override fun onError(
            bannerId: String, provider: String,
            error: NativeAdError
        ) {
            if (mBanners == null) return
            val isNotCached = mCurrentAd == null
            setErrorStatus(isNotCached)
            UiUtils.hide(mContainerView)
            Statistics.INSTANCE.trackPPBannerError(
                bannerId, provider, error,
                if (isDetailsState(mState)) 1 else 0
            )
        }

        override fun onClick(ad: MwmNativeAd) {
            Statistics.INSTANCE.trackPPBanner(
                EventName.PP_BANNER_CLICK, ad,
                if (isDetailsState(mState)) Statistics.PP_BANNER_STATE_DETAILS else Statistics.PP_BANNER_STATE_PREVIEW
            )
        }
    }

    interface BannerStateRequester {
        fun requestBannerState(): BannerState?
    }

    enum class BannerState {
        PREVIEW, DETAILS
    }

    interface BannerStateListener {
        fun onBannerDetails(ad: MwmNativeAd)
        fun onBannerPreview(ad: MwmNativeAd)
    }

    companion object {
        private val LOGGER = LoggerFactory.INSTANCE
            .getLogger(LoggerFactory.Type.MISC)
        private val TAG = BannerController::class.java.name
        private const val MAX_MESSAGE_LINES = 100
        private const val MIN_MESSAGE_LINES = 3
        private const val MAX_TITLE_LINES = 2
        private const val MIN_TITLE_LINES = 1
        private fun inflateBannerLayout(
            type: UiType,
            containerView: ViewGroup
        ): View {
            val context = containerView.context
            val li = LayoutInflater.from(context)
            val bannerView = li.inflate(type.layoutId, containerView, false)
            containerView.removeAllViews()
            containerView.addView(bannerView)
            return bannerView
        }

        private fun isDetailsState(state: BannerState?): Boolean {
            return state == BannerState.DETAILS
        }

        private fun isPreviewState(state: BannerState?): Boolean {
            return state == BannerState.PREVIEW
        }
    }

    init {
        LOGGER.d(TAG, "Constructor()")
        mContainerView = bannerContainer
        mContainerView.setOnClickListener { v: View? -> animateActionButton() }
        mBannerView = inflateBannerLayout(UiType.DEFAULT, mContainerView)
        mAdsLoader = loader
        mAdTracker = tracker
        mAdsRemovalProvider = adsRemovalProvider
        mBannerStateRequester = bannerStateRequester
        mBannerStateListener = bannerStateListener
        initBannerViews()
    }
}