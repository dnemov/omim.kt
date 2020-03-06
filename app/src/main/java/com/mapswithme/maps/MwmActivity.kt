package com.mapswithme.maps

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.mapswithme.maps.Framework.MapObjectListener
import com.mapswithme.maps.Framework.RouterType
import com.mapswithme.maps.Framework.getHttpGe0Url
import com.mapswithme.maps.Framework.nativeGetDrawScale
import com.mapswithme.maps.Framework.nativeGetGe0Url
import com.mapswithme.maps.Framework.nativeGetRouteFollowingInfo
import com.mapswithme.maps.Framework.nativeIsDownloadedMapAtScreenCenter
import com.mapswithme.maps.Framework.nativeIsInChoosePositionMode
import com.mapswithme.maps.Framework.nativeRemoveMapObjectListener
import com.mapswithme.maps.Framework.nativeSetMapObjectListener
import com.mapswithme.maps.Framework.nativeShowBookmarkCategory
import com.mapswithme.maps.Framework.nativeShowFeatureByLatLon
import com.mapswithme.maps.Framework.nativeTurnOffChoosePositionMode
import com.mapswithme.maps.Framework.nativeTurnOnChoosePositionMode
import com.mapswithme.maps.MapFragment
import com.mapswithme.maps.MapFragment.Companion.nativeCompassUpdated
import com.mapswithme.maps.MapFragment.Companion.nativeIsEngineCreated
import com.mapswithme.maps.MapFragment.Companion.nativeScaleMinus
import com.mapswithme.maps.MapFragment.Companion.nativeScalePlus
import com.mapswithme.maps.MwmActivity
import com.mapswithme.maps.NavigationButtonsAnimationController.OnTranslationChangedListener
import com.mapswithme.maps.activity.CustomNavigateUpListener
import com.mapswithme.maps.ads.LikesManager
import com.mapswithme.maps.api.ParsedMwmRequest.Companion.currentRequest
import com.mapswithme.maps.auth.PassportAuthDialogFragment
import com.mapswithme.maps.background.AppBackgroundTracker.OnTransitionListener
import com.mapswithme.maps.background.NotificationCandidate.UgcReview
import com.mapswithme.maps.background.Notifier.Companion.from
import com.mapswithme.maps.base.BaseMwmFragmentActivity
import com.mapswithme.maps.base.OnBackPressListener
import com.mapswithme.maps.bookmarks.BookmarkCategoriesActivity
import com.mapswithme.maps.bookmarks.BookmarkCategoriesActivity.Companion.startForResult
import com.mapswithme.maps.bookmarks.BookmarksCatalogActivity
import com.mapswithme.maps.bookmarks.BookmarksCatalogActivity.Companion.start
import com.mapswithme.maps.bookmarks.BookmarksCatalogActivity.Companion.startForResult
import com.mapswithme.maps.bookmarks.data.*
import com.mapswithme.maps.bookmarks.data.BookmarkManager.*
import com.mapswithme.maps.bookmarks.data.MapObject.Companion.isOfType
import com.mapswithme.maps.dialog.*
import com.mapswithme.maps.dialog.DialogUtils.showAlertDialog
import com.mapswithme.maps.discovery.DiscoveryActivity
import com.mapswithme.maps.discovery.DiscoveryFragment
import com.mapswithme.maps.discovery.ItemType
import com.mapswithme.maps.downloader.DownloaderActivity
import com.mapswithme.maps.downloader.DownloaderFragment
import com.mapswithme.maps.downloader.OnmapDownloader
import com.mapswithme.maps.editor.Editor.nativeStartEdit
import com.mapswithme.maps.editor.EditorActivity
import com.mapswithme.maps.editor.EditorHostFragment
import com.mapswithme.maps.editor.FeatureCategoryActivity
import com.mapswithme.maps.editor.ReportFragment
import com.mapswithme.maps.gallery.Items.SearchItem
import com.mapswithme.maps.intent.Factory.RestoreRouteTask
import com.mapswithme.maps.intent.Factory.ShowDialogTask
import com.mapswithme.maps.intent.Factory.ShowUGCEditorTask
import com.mapswithme.maps.intent.MapTask
import com.mapswithme.maps.intent.RegularMapTask
import com.mapswithme.maps.location.CompassData
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.location.LocationHelper.UiCallback
import com.mapswithme.maps.maplayer.MapLayerCompositeController
import com.mapswithme.maps.maplayer.Mode
import com.mapswithme.maps.maplayer.subway.OnSubwayLayerToggleListener
import com.mapswithme.maps.maplayer.subway.SubwayManager.Companion.from
import com.mapswithme.maps.maplayer.traffic.OnTrafficLayerToggleListener
import com.mapswithme.maps.maplayer.traffic.TrafficManager
import com.mapswithme.maps.maplayer.traffic.widget.TrafficButton
import com.mapswithme.maps.metrics.UserActionsLogger.logPromoAfterBookingShown
import com.mapswithme.maps.metrics.UserActionsLogger.logTipClickedEvent
import com.mapswithme.maps.news.OnboardingStep
import com.mapswithme.maps.onboarding.IntroductionDialogFragment.Companion.show
import com.mapswithme.maps.onboarding.IntroductionScreenFactory
import com.mapswithme.maps.onboarding.OnboardingTip
import com.mapswithme.maps.onboarding.Utils.getOnboardingStepByTip
import com.mapswithme.maps.onboarding.WelcomeDialogFragment.Companion.showOnboardinStep
import com.mapswithme.maps.onboarding.WelcomeDialogFragment.OnboardingStepPassedListener
import com.mapswithme.maps.promo.Promo.Companion.nativeGetPromoAfterBooking
import com.mapswithme.maps.promo.PromoBookingDialogFragment
import com.mapswithme.maps.purchase.*
import com.mapswithme.maps.purchase.PurchaseFactory.createAdsRemovalPurchaseController
import com.mapswithme.maps.purchase.PurchaseFactory.createBookmarksAllSubscriptionController
import com.mapswithme.maps.purchase.PurchaseFactory.createBookmarksSightsSubscriptionController
import com.mapswithme.maps.purchase.PurchaseFactory.createFailedBookmarkPurchaseController
import com.mapswithme.maps.routing.*
import com.mapswithme.maps.routing.RoutePointInfo.RouteMarkType
import com.mapswithme.maps.routing.RoutingErrorDialogFragment.Companion.create
import com.mapswithme.maps.routing.RoutingOptions.addOption
import com.mapswithme.maps.routing.RoutingPlanInplaceController.RoutingPlanListener
import com.mapswithme.maps.search.*
import com.mapswithme.maps.search.FilterActivity.Companion.startForResult
import com.mapswithme.maps.search.FloatingSearchToolbarController.SearchToolbarListener
import com.mapswithme.maps.search.SearchFilterController.DefaultFilterListener
import com.mapswithme.maps.settings.DrivingOptionsActivity
import com.mapswithme.maps.settings.RoadType
import com.mapswithme.maps.settings.SettingsActivity
import com.mapswithme.maps.settings.StoragePathManager
import com.mapswithme.maps.settings.UnitLocale.initializeCurrentUnits
import com.mapswithme.maps.sound.TtsPlayer
import com.mapswithme.maps.taxi.TaxiInfo
import com.mapswithme.maps.taxi.TaxiManager
import com.mapswithme.maps.tips.Tutorial
import com.mapswithme.maps.tips.TutorialAction
import com.mapswithme.maps.widget.FadeView
import com.mapswithme.maps.widget.menu.BaseMenu
import com.mapswithme.maps.widget.menu.BaseMenu.ItemClickListener
import com.mapswithme.maps.widget.menu.MainMenu
import com.mapswithme.maps.widget.menu.MyPositionButton
import com.mapswithme.maps.widget.placepage.BottomSheetPlacePageController
import com.mapswithme.maps.widget.placepage.PlacePageController
import com.mapswithme.maps.widget.placepage.RoutingModeListener
import com.mapswithme.util.*
import com.mapswithme.util.log.LoggerFactory
import com.mapswithme.util.sharing.ShareOption
import com.mapswithme.util.sharing.SharingHelper
import com.mapswithme.util.sharing.TargetUtils
import com.mapswithme.util.statistics.AlohaHelper
import com.mapswithme.util.statistics.Statistics
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt.PromptStateChangeListener
import java.util.*

class MwmActivity : BaseMwmFragmentActivity(), MapObjectListener, OnTouchListener,
    View.OnClickListener, MapRenderingListener, CustomNavigateUpListener,
    RoutingController.Container, UiCallback,
    FloatingSearchToolbarController.VisibilityListener,
    NativeSearchListener, OnTranslationChangedListener, RoutingPlanListener,
    RoutingBottomMenuListener, BookmarksLoadingListener,
    DiscoveryFragment.DiscoveryListener, SearchToolbarListener,
    OnTrafficLayerToggleListener, OnSubwayLayerToggleListener, BookmarksCatalogListener,
    AdsRemovalPurchaseControllerProvider, AdsRemovalActivationCallback,
    PlacePageController.SlideListener, AlertDialogCallback,
    RoutingModeListener, OnTransitionListener, PromptStateChangeListener,
    OnboardingStepPassedListener {
    // Map tasks that we run AFTER rendering initialized
    private val mTasks = Stack<MapTask>()
    private val mPathManager = StoragePathManager()
    private var mMapFragment: MapFragment? = null
    private lateinit var mFadeView: FadeView
    private lateinit var mPositionChooser: View
    private var mRoutingPlanInplaceController: RoutingPlanInplaceController? = null
    private lateinit var mNavigationController: NavigationController
    var mainMenu: MainMenu? = null
        private set
    private var mPanelAnimator: PanelAnimator? = null
    private var mOnmapDownloader: OnmapDownloader? = null
    private var mNavMyPosition: MyPositionButton? = null
    private var mNavAnimationController: NavigationButtonsAnimationController? = null
    private lateinit var mToggleMapLayerController: MapLayerCompositeController
    private var mFilterController: SearchFilterController? = null
    private var mIsTabletLayout = false
    private var mIsFullscreen = false
    private var mIsFullscreenAnimating = false
    private var mIsAppearMenuLater = false
    private var mSearchController: FloatingSearchToolbarController? = null
    private var mLocationErrorDialogAnnoying = false
    private var mLocationErrorDialog: Dialog? = null
    private var mRestoreRoutingPlanFragmentNeeded = false
    private var mSavedForTabletState: Bundle? = null

    override var adsRemovalPurchaseController: PurchaseController<PurchaseCallback>? = null

    private var mBookmarkInappPurchaseController: PurchaseController<FailedPurchaseChecker>? = null
    private var mBookmarksAllSubscriptionController: PurchaseController<PurchaseCallback>? = null
    private var mBookmarksSightsSubscriptionController: PurchaseController<PurchaseCallback>? = null
    private val mOnMyPositionClickListener: View.OnClickListener =
        CurrentPositionClickListener()
    private lateinit var mPlacePageController: PlacePageController
    private var mTutorial: Tutorial? = null
    private var mOnboardingTip: OnboardingTip? = null

    interface LeftAnimationTrackListener {
        fun onTrackStarted(collapsed: Boolean)
        fun onTrackFinished(collapsed: Boolean)
        fun onTrackLeftAnimation(offset: Float)
    }

    override fun onRenderingCreated() {
        checkMeasurementSystem()
        checkKitkatMigrationMove()
        LocationHelper.INSTANCE.attach(this)
    }

    override fun onRenderingRestored() {
        runTasks()
    }

    override fun onRenderingInitializationFinished() {
        runTasks()
    }

    private fun myPositionClick() {
        mLocationErrorDialogAnnoying = false
        LocationHelper.INSTANCE.setStopLocationUpdateByUser(false)
        LocationHelper.INSTANCE.switchToNextMode()
        LocationHelper.INSTANCE.restart()
    }

    private fun runTasks() {
        while (!mTasks.isEmpty()) mTasks.pop().run(this)
    }

    private fun checkKitkatMigrationMove() {
        mPathManager.checkKitkatMigration(this)
    }

    override val fragmentContentResId: Int
        protected get() = if (mIsTabletLayout) R.id.fragment_container else super.fragmentContentResId

    fun getFragment(clazz: Class<out Fragment?>): Fragment? {
        check(mIsTabletLayout) { "Must be called for tablets only!" }
        return supportFragmentManager.findFragmentByTag(clazz.name)
    }

    fun replaceFragmentInternal(
        fragmentClass: Class<out Fragment>?,
        args: Bundle?
    ) {
        super.replaceFragment(fragmentClass!!, args, null)
    }

    override fun replaceFragment(
        fragmentClass: Class<out Fragment?>,
        args: Bundle?,
        completionListener: Runnable?
    ) {
        if (mPanelAnimator!!.isVisible && getFragment(fragmentClass) != null) {
            completionListener?.run()
            return
        }
        mPanelAnimator!!.show(fragmentClass, args, completionListener)
    }

    fun containsFragment(fragmentClass: Class<out Fragment?>): Boolean {
        return mIsTabletLayout && getFragment(fragmentClass) != null
    }

    private fun showBookmarks() {
        startForResult(this)
    }

    private fun showTabletSearch(data: Intent?, query: String) {
        if (mFilterController == null || data == null) return
        val params: BookingFilterParams =
            data.getParcelableExtra(FilterActivity.EXTRA_FILTER_PARAMS)
        val filter: HotelsFilter = data.getParcelableExtra(FilterActivity.EXTRA_FILTER)
        mFilterController!!.setFilterAndParams(filter, params)
        showSearch(query)
    }

    fun showSearch(query: String?) {
        if (mIsTabletLayout) {
            mSearchController!!.hide()
            val args = Bundle()
            args.putString(SearchActivity.EXTRA_QUERY, query)
            if (mFilterController != null) {
                args.putParcelable(FilterActivity.EXTRA_FILTER, mFilterController!!.filter)
                args.putParcelable(
                    FilterActivity.EXTRA_FILTER_PARAMS,
                    mFilterController!!.bookingFilterParams
                )
            }
            replaceFragment(SearchFragment::class.java, args, null)
        } else {
            var filter: HotelsFilter? = null
            var params: BookingFilterParams? = null
            if (mFilterController != null) {
                filter = mFilterController!!.filter
                params = mFilterController!!.bookingFilterParams
            }
            SearchActivity.start(this, query, filter, params)
        }
        if (mFilterController != null) mFilterController!!.resetFilter()
    }

    fun showEditor() { // TODO(yunikkk) think about refactoring. It probably should be called in editor.
        nativeStartEdit()
        Statistics.INSTANCE.trackEditorLaunch(false)
        if (mIsTabletLayout) replaceFragment(
            EditorHostFragment::class.java,
            null,
            null
        ) else EditorActivity.start(this)
    }

    private fun shareMyLocation() {
        val loc = LocationHelper.INSTANCE.savedLocation
        if (loc != null) {
            val geoUrl = nativeGetGe0Url(
                loc.latitude,
                loc.longitude,
                nativeGetDrawScale().toDouble(),
                ""
            )
            val httpUrl = getHttpGe0Url(
                loc.latitude,
                loc.longitude,
                nativeGetDrawScale().toDouble(),
                ""
            )
            val body = getString(R.string.my_position_share_sms, geoUrl, httpUrl)
            ShareOption.AnyShareOption.ANY.share(this, body)
            return
        }
        AlertDialog.Builder(this@MwmActivity)
            .setMessage(R.string.unknown_current_position)
            .setCancelable(true)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun showDownloader(openDownloaded: Boolean) {
        val args = Bundle()
        args.putBoolean(DownloaderActivity.EXTRA_OPEN_DOWNLOADED, openDownloaded)
        if (mIsTabletLayout) {
            SearchEngine.INSTANCE.cancel()
            mSearchController!!.refreshToolbar()
            replaceFragment(DownloaderFragment::class.java, args, null)
        } else {
            startActivity(Intent(this, DownloaderActivity::class.java).putExtras(args))
        }
    }

    @StyleRes
    override fun getThemeResourceId(theme: String): Int {
        if (ThemeUtils.isDefaultTheme(theme)) return R.style.MwmTheme_MainActivity
        return if (ThemeUtils.isNightTheme(theme)) R.style.MwmTheme_Night_MainActivity else super.getThemeResourceId(
            theme
        )
    }

    @SuppressLint("InlinedApi")
    @CallSuper
    override fun onSafeCreate(savedInstanceState: Bundle?) {
        super.onSafeCreate(savedInstanceState)
        if (savedInstanceState != null) {
            mLocationErrorDialogAnnoying =
                savedInstanceState.getBoolean(EXTRA_LOCATION_DIALOG_IS_ANNOYING)
            mOnboardingTip =
                savedInstanceState.getParcelable(EXTRA_ONBOARDING_TIP)
        }
        mIsTabletLayout = resources.getBoolean(R.bool.tabletLayout)
        if (!mIsTabletLayout && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) window.addFlags(
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
        )
        setContentView(R.layout.activity_map)
        mPlacePageController = BottomSheetPlacePageController(
            this, this, this,
            this
        )
        mPlacePageController.initialize()
        mPlacePageController.onActivityCreated(this, savedInstanceState)
        val isLaunchByDeepLink =
            intent.getBooleanExtra(EXTRA_LAUNCH_BY_DEEP_LINK, false)
        initViews(isLaunchByDeepLink)
        Statistics.INSTANCE.trackConnectionState()
        mSearchController = FloatingSearchToolbarController(this, this)
        mSearchController!!.toolbar.viewTreeObserver
            .addOnGlobalLayoutListener(ToolbarLayoutChangeListener())
        mSearchController!!.setVisibilityListener(this)
        SearchEngine.INSTANCE.addListener(this)
        SharingHelper.INSTANCE.initialize()
        initControllersAndValidatePurchases(savedInstanceState)
        val isConsumed = savedInstanceState == null && processIntent(intent)
        // If the map activity is launched by any incoming intent (deeplink, update maps event, etc)
// or it's the first launch (onboarding) we haven't to try restoring the route,
// showing the tips, etc.
        if (isConsumed || MwmApplication.from(this).isFirstLaunch) return
        if (savedInstanceState == null && RoutingController.get().hasSavedRoute()) {
            addTask(RestoreRouteTask())
            return
        }
        if (savedInstanceState == null) tryToShowAdditionalViewOnTop()
    }

    private fun initControllersAndValidatePurchases(savedInstanceState: Bundle?) {
        adsRemovalPurchaseController = createAdsRemovalPurchaseController(this)
        adsRemovalPurchaseController!!.initialize(this)
        mBookmarkInappPurchaseController =
            createFailedBookmarkPurchaseController(this)
        mBookmarkInappPurchaseController!!.initialize(this)
        mBookmarksAllSubscriptionController =
            createBookmarksAllSubscriptionController(this)
        mBookmarksAllSubscriptionController!!.initialize(this)
        mBookmarksSightsSubscriptionController =
            createBookmarksSightsSubscriptionController(this)
        mBookmarksSightsSubscriptionController!!.initialize(this)
        // To reduce number of parasite validation requests during orientation change.
        if (savedInstanceState == null) {
            adsRemovalPurchaseController!!.validateExistingPurchases()
            mBookmarkInappPurchaseController!!.validateExistingPurchases()
            mBookmarksAllSubscriptionController!!.validateExistingPurchases()
            mBookmarksSightsSubscriptionController!!.validateExistingPurchases()
        }
    }

    private fun initViews(isLaunchByDeeplink: Boolean) {
        initMap(isLaunchByDeeplink)
        initNavigationButtons()
        if (!mIsTabletLayout) {
            mRoutingPlanInplaceController = RoutingPlanInplaceController(this, this, this)
            removeCurrentFragment(false)
        }
        mNavigationController = NavigationController(this)
        TrafficManager.INSTANCE.attach(mNavigationController)
        initMainMenu()
        initOnmapDownloader()
        initPositionChooser()
        initFilterViews()
    }

    private fun initFilterViews() {
        val frame = findViewById<View>(R.id.filter_frame)
        if (frame != null) {
            mFilterController = SearchFilterController(frame, object : DefaultFilterListener() {
                override fun onShowOnMapClick() {
                    showSearch(mSearchController!!.query)
                }

                override fun onFilterClick() {
                    var filter: HotelsFilter? = null
                    var params: BookingFilterParams? = null
                    if (mFilterController != null) {
                        filter = mFilterController!!.filter
                        params = mFilterController!!.bookingFilterParams
                    }
                    startForResult(
                        this@MwmActivity, filter, params,
                        FilterActivity.REQ_CODE_FILTER
                    )
                }

                override fun onFilterClear() {
                    runSearch()
                }
            }, R.string.search_in_table)
        }
    }

    private fun runSearch() { // The previous search should be cancelled before the new one is started, since previous search
// results are no longer needed.
        SearchEngine.INSTANCE.cancel()
        SearchEngine.INSTANCE.searchInteractive(
            mSearchController!!.query, System.nanoTime(),
            false /* isMapAndTable */,
            if (mFilterController != null) mFilterController!!.filter else null,
            if (mFilterController != null) mFilterController!!.bookingFilterParams else null
        )
        SearchEngine.INSTANCE.query = mSearchController!!.query
    }

    private fun initPositionChooser() {
        mPositionChooser = findViewById(R.id.position_chooser)
        if (mPositionChooser == null) return
        val toolbar: Toolbar =
            mPositionChooser.findViewById(R.id.toolbar_position_chooser)
        UiUtils.extendViewWithStatusBar(toolbar)
        UiUtils.showHomeUpButton(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> hidePositionChooser() }
        mPositionChooser.findViewById<View>(R.id.done).setOnClickListener { v: View? ->
            Statistics.INSTANCE.trackEditorLaunch(true)
            hidePositionChooser()
            if (nativeIsDownloadedMapAtScreenCenter()) startActivity(
                Intent(
                    this@MwmActivity,
                    FeatureCategoryActivity::class.java
                )
            ) else showAlertDialog(
                this@MwmActivity,
                R.string.message_invalid_feature_position
            )
        }
        UiUtils.hide(mPositionChooser)
    }

    fun showPositionChooser(
        isBusiness: Boolean,
        applyPosition: Boolean
    ) {
        UiUtils.show(mPositionChooser)
        setFullscreen(true)
        nativeTurnOnChoosePositionMode(isBusiness, applyPosition)
        closePlacePage()
        mSearchController!!.hide()
    }

    private fun hidePositionChooser() {
        UiUtils.hide(mPositionChooser)
        nativeTurnOffChoosePositionMode()
        setFullscreen(false)
    }

    private fun initMap(isLaunchByDeepLink: Boolean) {
        mFadeView = findViewById(R.id.fade_view)
        mFadeView.setListener(object : FadeView.Listener {
            override fun onTouch(): Boolean {
                return currentMenu.close(true)
            }
        })
        mMapFragment =
            supportFragmentManager.findFragmentByTag(MapFragment::class.java.name) as MapFragment?
        if (mMapFragment == null) {
            val args = Bundle()
            args.putBoolean(MapFragment.ARG_LAUNCH_BY_DEEP_LINK, isLaunchByDeepLink)


            mMapFragment = Fragment.instantiate(
                this,
                MapFragment::class.java.name,
                args
            ) as MapFragment

            supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.map_fragment_container,
                    mMapFragment!!,
                    MapFragment::class.java.name
                )
                .commit()
        }
        val container =
            findViewById<View>(R.id.map_fragment_container)
        container?.setOnTouchListener(this)
    }

    val isMapAttached: Boolean
        get() = mMapFragment != null && mMapFragment!!.isAdded

    private fun initNavigationButtons() {
        val frame = findViewById<View>(R.id.navigation_buttons) ?: return
        val zoomIn = frame.findViewById<View>(R.id.nav_zoom_in)
        zoomIn.setOnClickListener(this)
        val zoomOut = frame.findViewById<View>(R.id.nav_zoom_out)
        zoomOut.setOnClickListener(this)
        val myPosition = frame.findViewById<View>(R.id.my_position)
        mNavMyPosition = MyPositionButton(myPosition, mOnMyPositionClickListener)
        initToggleMapLayerController(frame)
        val openSubsScreenBtnContainer =
            frame.findViewById<View>(R.id.subs_screen_btn_container)
        val tip = OnboardingTip.get()
        val hasOnBoardingView =
            mOnboardingTip == null && tip != null && MwmApplication.from(this).isFirstLaunch
        mNavAnimationController = NavigationButtonsAnimationController(
            zoomIn, zoomOut, myPosition, window.decorView.rootView, this,
            if (hasOnBoardingView) openSubsScreenBtnContainer else null
        )
        UiUtils.showIf(hasOnBoardingView, openSubsScreenBtnContainer)
        if (hasOnBoardingView) {
            openSubsScreenBtnContainer.findViewById<View>(R.id.onboarding_btn)
                .setOnClickListener { v: View? ->
                    onBoardingBtnClicked(
                        tip!!
                    )
                }
            val builder =
                Statistics.makeGuidesSubscriptionBuilder()
            Statistics.INSTANCE.trackEvent(
                Statistics.EventName.MAP_SPONSORED_BUTTON_SHOW,
                builder
            )
        }
    }

    private fun onBoardingBtnClicked(tip: OnboardingTip) {
        val builder =
            Statistics.makeGuidesSubscriptionBuilder()
        Statistics.INSTANCE.trackEvent(
            Statistics.EventName.MAP_SPONSORED_BUTTON_CLICK,
            builder
        )
        if (mNavAnimationController == null) return
        mNavAnimationController!!.hideOnBoardingTipBtn()
        mOnboardingTip = tip
        val step =
            getOnboardingStepByTip(mOnboardingTip!!)
        showOnboardinStep(this, step)
    }

    private fun initToggleMapLayerController(frame: View) {
        val trafficBtn = frame.findViewById<ImageButton>(R.id.traffic)
        val traffic = TrafficButton(trafficBtn)
        val subway = frame.findViewById<View>(R.id.subway)
        mToggleMapLayerController = MapLayerCompositeController(traffic, subway, this)
        mToggleMapLayerController.attachCore()
    }

    fun closePlacePage(): Boolean {
        if (mPlacePageController.isClosed) return false
        mPlacePageController.close()
        return true
    }

    fun closeSidePanel(): Boolean {
        if (interceptBackPress()) return true
        if (removeCurrentFragment(true)) {
            InputUtils.hideKeyboard(mFadeView)
            mFadeView.fadeOut()
            return true
        }
        return false
    }

    private fun closeAllFloatingPanels() {
        if (!mIsTabletLayout) return
        closePlacePage()
        if (removeCurrentFragment(true)) {
            InputUtils.hideKeyboard(mFadeView)
            mFadeView.fadeOut()
        }
    }

    fun closeMenu(procAfterClose: Runnable?) {
        mFadeView.fadeOut()
        mainMenu!!.close(true, procAfterClose)
    }

    private fun closePositionChooser(): Boolean {
        if (UiUtils.isVisible(mPositionChooser)) {
            hidePositionChooser()
            return true
        }
        return false
    }

    fun startLocationToPoint(
        endPoint: MapObject?,
        canUseMyPositionAsStart: Boolean
    ) {
        closeMenu(Runnable {
            RoutingController.get().prepare(canUseMyPositionAsStart, endPoint)
            // TODO: check for tablet.
            closePlacePage()
        })
    }

    private fun toggleMenu() {
        currentMenu.toggle(true)
        refreshFade()
    }

    fun refreshFade() {
        if (currentMenu.isOpen) mFadeView.fadeIn() else mFadeView.fadeOut()
    }

    private fun initMainMenu() {
        mainMenu = MainMenu(
            findViewById(R.id.menu_frame),
            object : ItemClickListener<BaseMenu.Item> {
                override fun onItemClick(item: BaseMenu.Item) {
                    onItemClickOrSkipAnim(item)
                }
            }
        )
        if (mIsTabletLayout) {
            mPanelAnimator = PanelAnimator(this)
            mPanelAnimator!!.registerListener(mainMenu!!.leftAnimationTrackListener)
            return
        }
    }

    private fun onItemClickOrSkipAnim(item: BaseMenu.Item) {
        if (mIsFullscreenAnimating) return
        (item as MainMenu.Item).onClicked(this, item)
    }

    fun showDiscovery() {
        if (mIsTabletLayout) {
            replaceFragment(DiscoveryFragment::class.java, null, null)
        } else {
            val i = Intent(this@MwmActivity, DiscoveryActivity::class.java)
            startActivityForResult(i, REQ_CODE_DISCOVERY)
        }
    }

    private fun initOnmapDownloader() {
        mOnmapDownloader = OnmapDownloader(this)
        if (mIsTabletLayout) mPanelAnimator!!.registerListener(mOnmapDownloader!!)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mPlacePageController.onSave(outState)
        if (!mIsTabletLayout && RoutingController.get().isPlanning) mRoutingPlanInplaceController!!.onSaveState(
            outState
        )
        if (mIsTabletLayout) {
            val fragment =
                getFragment(RoutingPlanFragment::class.java) as RoutingPlanFragment?
            fragment?.saveRoutingPanelState(outState)
        }
        mNavigationController.onSaveState(outState)
        RoutingController.get().onSaveState()
        outState.putBoolean(
            EXTRA_LOCATION_DIALOG_IS_ANNOYING,
            mLocationErrorDialogAnnoying
        )
        if (mFilterController != null) mFilterController!!.onSaveState(outState)
        if (!isChangingConfigurations) RoutingController.get().saveRoute() else  // We no longer need in a saved route if it's a configuration changing: theme switching,
// orientation changing, etc. Otherwise, the saved route might be restored at undesirable moment.
            RoutingController.get().deleteSavedRoute()
        outState.putParcelable(EXTRA_ONBOARDING_TIP, mOnboardingTip)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mPlacePageController.onRestore(savedInstanceState)
        if (mIsTabletLayout) {
            val fragment =
                getFragment(RoutingPlanFragment::class.java) as RoutingPlanFragment?
            if (fragment != null) {
                fragment.restoreRoutingPanelState(savedInstanceState)
            } else if (RoutingController.get().isPlanning) {
                mRestoreRoutingPlanFragmentNeeded = true
                mSavedForTabletState = savedInstanceState
            }
        }
        if (!mIsTabletLayout && RoutingController.get().isPlanning) mRoutingPlanInplaceController!!.restoreState(
            savedInstanceState
        )
        mNavigationController.onRestoreState(savedInstanceState)
        if (mFilterController != null) mFilterController!!.onRestoreState(savedInstanceState)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return
        when (requestCode) {
            REQ_CODE_DISCOVERY -> handleDiscoveryResult(data!!)
            FilterActivity.REQ_CODE_FILTER, REQ_CODE_SHOW_SIMILAR_HOTELS -> {
                if (mIsTabletLayout) {
                    showTabletSearch(data, getString(R.string.hotel))
                    return
                }
                handleFilterResult(data)
            }
            BookmarkCategoriesActivity.REQ_CODE_DOWNLOAD_BOOKMARK_CATEGORY -> handleDownloadedCategoryResult(
                data!!
            )
            REQ_CODE_DRIVING_OPTIONS -> rebuildLastRoute()
            PurchaseUtils.REQ_CODE_PAY_SUBSCRIPTION -> showCatalogUnlimitedAccessDialog()
        }
    }

    private fun showCatalogUnlimitedAccessDialog() {
        val dialog =
            com.mapswithme.maps.dialog.AlertDialog.Builder()
                .setTitleId(R.string.popup_subscription_success_map_title)
                .setMessageId(R.string.popup_subscription_success_map_message)
                .setPositiveBtnId(R.string.popup_subscription_success_map_start_button)
                .setNegativeBtnId(R.string.popup_subscription_success_map_not_now_button)
                .setDialogViewStrategyType(com.mapswithme.maps.dialog.AlertDialog.DialogViewStrategyType.CONFIRMATION_DIALOG)
                .setDialogFactory(object : DialogFactory {
                    override fun createDialog(): com.mapswithme.maps.dialog.AlertDialog {
                        return DefaultConfirmationAlertDialog()
                    }
                })
                .setReqCode(REQ_CODE_CATALOG_UNLIMITED_ACCESS)
                .setNegativeBtnTextColor(
                    ThemeUtils.getResource(
                        this,
                        R.attr.buttonDialogTextColor
                    )
                )
                .setFragManagerStrategyType(com.mapswithme.maps.dialog.AlertDialog.FragManagerStrategyType.ACTIVITY_FRAGMENT_MANAGER)
                .build()
        dialog.show(this, CATALOG_UNLIMITED_ACCESS_DIALOG_TAG)
    }

    private fun rebuildLastRoute() {
        RoutingController.get().attach(this)
        rebuildLastRouteInternal()
    }

    private fun rebuildLastRouteInternal() {
        if (mRoutingPlanInplaceController == null) return
        mRoutingPlanInplaceController!!.hideDrivingOptionsView()
        RoutingController.get().rebuildLastRoute()
    }

    override fun toggleRouteSettings(roadType: RoadType) {
        mPlacePageController.close()
        addOption(roadType)
        rebuildLastRouteInternal()
    }

    private fun handleDownloadedCategoryResult(data: Intent) {
        val category: BookmarkCategory =
            data.getParcelableExtra(BookmarksCatalogActivity.EXTRA_DOWNLOADED_CATEGORY)
                ?: throw IllegalArgumentException("Category not found in bundle")
        val mapTask: MapTask = object : RegularMapTask() {
            @JvmField
            val serialVersionUID = -7417385158050827655L
            override fun run(target: MwmActivity): Boolean {
                target.showBookmarkCategory(category)
                return true
            }
        }
        addTask(mapTask)
        closePlacePage()
    }

    private fun showBookmarkCategory(category: BookmarkCategory): Boolean {
        nativeShowBookmarkCategory(category.id)
        return true
    }

    private fun handleDiscoveryResult(data: Intent) {
        val category: BookmarkCategory? =
            data.getParcelableExtra(BookmarksCatalogActivity.EXTRA_DOWNLOADED_CATEGORY)
        if (category != null) {
            handleDownloadedCategoryResult(data)
            return
        }
        val action = data.action
        if (TextUtils.isEmpty(action)) return
        when (action) {
            DiscoveryActivity.ACTION_ROUTE_TO -> {
                val destination: MapObject =
                    data.getParcelableExtra(DiscoveryActivity.EXTRA_DISCOVERY_OBJECT)
                        ?: return
                onRouteToDiscoveredObject(destination)
            }
            DiscoveryActivity.ACTION_SHOW_ON_MAP -> {
                val destination = data.getParcelableExtra<MapObject>(DiscoveryActivity.EXTRA_DISCOVERY_OBJECT)
                if (destination != null)
                    onShowDiscoveredObject(destination)
            }
            DiscoveryActivity.ACTION_SHOW_FILTER_RESULTS -> handleFilterResult(data)
        }
    }

    private fun handleFilterResult(data: Intent?) {
        if (data == null || mFilterController == null) return
        setupSearchQuery(data)
        val params: BookingFilterParams? =
            data.getParcelableExtra(FilterActivity.EXTRA_FILTER_PARAMS)
        mFilterController!!.setFilterAndParams(
            data.getParcelableExtra(FilterActivity.EXTRA_FILTER),
            params
        )
        mFilterController!!.updateFilterButtonVisibility(params != null)
        runSearch()
    }

    private fun setupSearchQuery(data: Intent) {
        if (mSearchController == null) return
        val query = data.getStringExtra(DiscoveryActivity.EXTRA_FILTER_SEARCH_QUERY)
        mSearchController!!.query =
            if (TextUtils.isEmpty(query)) getString(R.string.hotel) + " " else query
    }

    private fun runHotelCategorySearchOnMap() {
        if (mSearchController == null || mFilterController == null) return
        mSearchController!!.query = activity.getString(R.string.hotel) + " "
        runSearch()
        mSearchController!!.refreshToolbar()
        mFilterController!!.updateFilterButtonVisibility(true)
        mFilterController!!.show(true, true)
    }

    override fun onRouteToDiscoveredObject(`object`: MapObject) {
        addTask(object : RegularMapTask() {
            @JvmField val serialVersionUID = -219799471997583494L

            override fun run(target: MwmActivity): Boolean {
                RoutingController.get().attach(target)
                RoutingController.get().setRouterType(Framework.ROUTER_TYPE_PEDESTRIAN)
                RoutingController.get().prepare(true, `object`)
                return false
            }
        })
    }

    override fun onShowDiscoveredObject(`object`: MapObject) {
        addTask(object : RegularMapTask() {
            @JvmField
            val serialVersionUID = 7499190617762270631L
            override fun run(target: MwmActivity): Boolean {
                nativeShowFeatureByLatLon(`object`.lat, `object`.lon)
                return false
            }
        })
    }

    override fun onShowFilter() {
        startForResult(
            this@MwmActivity, null, null,
            FilterActivity.REQ_CODE_FILTER
        )
    }

    override fun onShowSimilarObjects(
        item: SearchItem,
        type: ItemType
    ) {
        val query = getString(type.searchCategory)
        showSearch(query)
    }

    fun onSearchSimilarHotels(filter: HotelsFilter?) {
        val params =
            if (mFilterController != null) mFilterController!!.bookingFilterParams else null
        startForResult(
            this@MwmActivity, filter, params,
            REQ_CODE_SHOW_SIMILAR_HOTELS
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQ_CODE_LOCATION_PERMISSION || grantResults.size == 0) return
        val result =
            PermissionsUtils.computePermissionsResult(permissions, grantResults)
        if (result.isLocationGranted) myPositionClick()
    }

    override fun onSubwayLayerSelected() {
        mToggleMapLayerController.toggleMode(Mode.SUBWAY)
    }

    override fun onTrafficLayerSelected() {
        mToggleMapLayerController.toggleMode(Mode.TRAFFIC)
    }

    override fun onImportStarted(serverId: String) { // Do nothing by default.
    }

    override fun onImportFinished(
        serverId: String,
        catId: Long,
        successful: Boolean
    ) {
        if (!successful) return
        Toast.makeText(this, R.string.guide_downloaded_title, Toast.LENGTH_LONG).show()
        Statistics.INSTANCE.trackEvent(Statistics.EventName.BM_GUIDEDOWNLOADTOAST_SHOWN)
    }

    override fun onTagsReceived(
        successful: Boolean, tagsGroups: List<CatalogTagsGroup>,
        tagsLimit: Int
    ) { //TODO(@alexzatsepin): Implement me if necessary
    }

    override fun onCustomPropertiesReceived(
        successful: Boolean,
        properties: List<CatalogCustomProperty>
    ) { //TODO(@alexzatsepin): Implement me if necessary
    }

    override fun onUploadStarted(originCategoryId: Long) { //TODO(@alexzatsepin): Implement me if necessary
    }

    override fun onUploadFinished(
        uploadResult: UploadResult, description: String,
        originCategoryId: Long, resultCategoryId: Long
    ) { //TODO(@alexzatsepin): Implement me if necessary
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processIntent(intent)
    }

    private fun processIntent(intent: Intent?): Boolean {
        if (intent == null) return false
        val notifier =
            from(application)
        notifier.processNotificationExtras(intent)
        if (intent.hasExtra(EXTRA_TASK)) {
            addTask(intent)
            return true
        }
        val filter: HotelsFilter? = intent.getParcelableExtra(FilterActivity.EXTRA_FILTER)
        val params: BookingFilterParams? =
            intent.getParcelableExtra(FilterActivity.EXTRA_FILTER_PARAMS)
        if (mFilterController != null && (filter != null || params != null)) {
            mFilterController!!.updateFilterButtonVisibility(true)
            mFilterController!!.show(!TextUtils.isEmpty(SearchEngine.INSTANCE.query), true)
            mFilterController!!.setFilterAndParams(filter, params)
            return true
        }
        return false
    }

    private fun addTask(intent: Intent?) {
        if (intent != null &&
            !intent.getBooleanExtra(EXTRA_CONSUMED, false) &&
            intent.hasExtra(EXTRA_TASK) &&
            intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0
        ) {
            val mapTask =
                intent.getSerializableExtra(EXTRA_TASK) as MapTask
            mTasks.add(mapTask)
            intent.removeExtra(EXTRA_TASK)
            if (isMapRendererActive) runTasks()
            // mark intent as consumed
            intent.putExtra(EXTRA_CONSUMED, true)
        }
    }

    private val isMapRendererActive: Boolean
        private get() = (mMapFragment != null && nativeIsEngineCreated()
                && mMapFragment!!.isContextCreated)

    private fun addTask(task: MapTask) {
        mTasks.add(task)
        if (isMapRendererActive) runTasks()
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        mSearchController!!.refreshToolbar()
        mainMenu!!.onResume(Runnable {
            if (nativeIsInChoosePositionMode()) {
                UiUtils.show(mPositionChooser)
                setFullscreen(true)
            }
        })
        if (mOnmapDownloader != null) mOnmapDownloader!!.onResume()
        mNavigationController.onResume()
        if (mNavAnimationController != null) mNavAnimationController!!.onResume()
        mPlacePageController.onActivityResumed(this)
    }

    override fun recreate() { // Explicitly destroy surface before activity recreation.
        if (mMapFragment != null) mMapFragment!!.destroySurface()
        super.recreate()
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        RoutingController.get().restore()
        if (!LikesManager.INSTANCE.isNewUser && Counters.isShowReviewForOldUser()) {
            LikesManager.INSTANCE.showRateDialogForOldUser(this)
            Counters.setShowReviewForOldUser(false)
        } else {
            LikesManager.INSTANCE.showDialogs(this)
        }
    }

    override fun onPause() {
        TtsPlayer.INSTANCE.stop()
        LikesManager.INSTANCE.cancelDialogs()
        if (mOnmapDownloader != null) mOnmapDownloader!!.onPause()
        mPlacePageController.onActivityPaused(this)
        super.onPause()
    }

    override fun onStart() {
        super.onStart()
        nativeSetMapObjectListener(this)
        INSTANCE.addLoadingListener(this)
        INSTANCE.addCatalogListener(this)
        RoutingController.get().attach(this)
        if (nativeIsEngineCreated()) LocationHelper.INSTANCE.attach(this)
        mPlacePageController.onActivityStarted(this)
        MwmApplication.backgroundTracker(activity).addListener(this)
    }

    override fun onStop() {
        super.onStop()
        nativeRemoveMapObjectListener()
        INSTANCE.removeLoadingListener(this)
        INSTANCE.removeCatalogListener(this)
        LocationHelper.INSTANCE.detach(!isFinishing)
        RoutingController.get().detach()
        mPlacePageController.onActivityStopped(this)
        MwmApplication.backgroundTracker(activity).removeListener(this)
    }

    @CallSuper
    override fun onSafeDestroy() {
        super.onSafeDestroy()
        if (adsRemovalPurchaseController != null) adsRemovalPurchaseController!!.destroy()
        if (mBookmarkInappPurchaseController != null) mBookmarkInappPurchaseController!!.destroy()
        if (mBookmarksAllSubscriptionController != null) mBookmarksAllSubscriptionController!!.destroy()
        if (mBookmarksSightsSubscriptionController != null) mBookmarksSightsSubscriptionController!!.destroy()
        mNavigationController.destroy()
        mToggleMapLayerController.detachCore()
        TrafficManager.INSTANCE.detachAll()
        mPlacePageController.destroy()
        SearchEngine.INSTANCE.removeListener(this)
    }

    override fun onBackPressed() {
        if (currentMenu.close(true)) {
            mFadeView.fadeOut()
            return
        }
        if (mSearchController != null && mSearchController!!.hide()) {
            SearchEngine.INSTANCE.cancelInteractiveSearch()
            if (mFilterController != null) mFilterController!!.resetFilter()
            mSearchController!!.clear()
            return
        }
        val isRoutingCancelled = RoutingController.get().cancel()
        if (isRoutingCancelled) {
            @RouterType val type = RoutingController.get().lastRouterType
            Statistics.INSTANCE.trackRoutingFinish(
                true, type,
                TrafficManager.INSTANCE.isEnabled
            )
        }
        if (!closePlacePage() && !closeSidePanel() && !isRoutingCancelled
            && !closePositionChooser()
        ) {
            try {
                super.onBackPressed()
            } catch (e: IllegalStateException) { // Sometimes this can be called after onSaveState() for unknown reason.
            }
        }
    }

    private fun interceptBackPress(): Boolean {
        val manager = supportFragmentManager
        for (tag in DOCKED_FRAGMENTS) {
            val fragment = manager.findFragmentByTag(tag)
            if (fragment != null && fragment.isResumed && fragment is OnBackPressListener) return (fragment as OnBackPressListener).onBackPressed()
        }
        return false
    }

    private fun removeFragmentImmediate(fragment: Fragment) {
        val fm = supportFragmentManager
        if (fm.isDestroyed) return
        fm.beginTransaction()
            .remove(fragment)
            .commitAllowingStateLoss()
        fm.executePendingTransactions()
    }

    private fun removeCurrentFragment(animate: Boolean): Boolean {
        for (tag in DOCKED_FRAGMENTS) if (removeFragment(
                tag,
                animate
            )
        ) return true
        return false
    }

    private fun removeFragment(className: String, animate: Boolean): Boolean {
        var animate = animate
        if (animate && mPanelAnimator == null) animate = false
        val fragment =
            supportFragmentManager.findFragmentByTag(className) ?: return false
        if (animate) mPanelAnimator!!.hide(Runnable { removeFragmentImmediate(fragment) }) else removeFragmentImmediate(
            fragment
        )
        return true
    }

    // Called from JNI.
    override fun onMapObjectActivated(`object`: MapObject?) {
        if (isOfType(MapObject.API_POINT, `object`)) {
            val request = currentRequest ?: return
            request.setPointData(`object`!!.lat, `object`.lon, `object`.title, `object`.apiId)
            `object`.subtitle = request.getCallerName(MwmApplication.get()).toString()
        }
        setFullscreen(false)
        mPlacePageController.openFor(`object`!!)
        if (UiUtils.isVisible(mFadeView)) mFadeView.fadeOut()
    }

    // Called from JNI.
    override fun onDismiss(switchFullScreenMode: Boolean) {
        if (switchFullScreenMode) {
            if (mPanelAnimator != null && mPanelAnimator!!.isVisible ||
                UiUtils.isVisible(mSearchController!!.toolbar)
            ) return
            setFullscreen(!mIsFullscreen)
        } else {
            mPlacePageController.close()
        }
    }

    private val currentMenu: BaseMenu
        private get() = if (RoutingController.get().isNavigating) mNavigationController.navMenu else mainMenu!!

    private fun setFullscreen(isFullscreen: Boolean) {
        if (RoutingController.get().isNavigating
            || RoutingController.get().isBuilding
            || RoutingController.get().isPlanning
        ) return
        mIsFullscreen = isFullscreen
        val menu = currentMenu
        if (isFullscreen) {
            if (menu.isAnimating) return
            mIsFullscreenAnimating = true
            UiUtils.invisible(menu.frame)
            val menuHeight = menu.frame.height
            adjustBottomWidgets(menuHeight)
            mIsFullscreenAnimating = false
            if (mIsAppearMenuLater) {
                appearMenu(menu)
                mIsAppearMenuLater = false
            }
            if (mNavAnimationController != null) mNavAnimationController!!.disappearZoomButtons()
            if (mNavMyPosition != null) mNavMyPosition!!.hide()
            mToggleMapLayerController.hide()
        } else {
            if (mPlacePageController.isClosed && mNavAnimationController != null) mNavAnimationController!!.appearZoomButtons()
            if (!mIsFullscreenAnimating) appearMenu(menu) else mIsAppearMenuLater = true
        }
    }

    private fun appearMenu(menu: BaseMenu) {
        appearMenuFrame(menu)
        showNavMyPositionBtn()
        mToggleMapLayerController.applyLastActiveMode()
    }

    private fun showNavMyPositionBtn() {
        if (mNavMyPosition != null) mNavMyPosition!!.show()
    }

    private fun appearMenuFrame(menu: BaseMenu) {
        UiUtils.show(menu.frame)
        adjustBottomWidgets(0)
    }

    override fun onPlacePageSlide(top: Int) {
        if (mNavAnimationController != null) mNavAnimationController!!.move(top.toFloat())
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.nav_zoom_in -> {
                Statistics.INSTANCE.trackEvent(Statistics.EventName.ZOOM_IN)
                AlohaHelper.logClick(AlohaHelper.ZOOM_IN)
                nativeScalePlus()
            }
            R.id.nav_zoom_out -> {
                Statistics.INSTANCE.trackEvent(Statistics.EventName.ZOOM_OUT)
                AlohaHelper.logClick(AlohaHelper.ZOOM_OUT)
                nativeScaleMinus()
            }
        }
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        return mMapFragment != null && mMapFragment!!.onTouch(view, event)
    }

    override fun customOnNavigateUp() {
        if (removeCurrentFragment(true)) {
            InputUtils.hideKeyboard(mainMenu!!.frame)
            mSearchController!!.refreshToolbar()
        }
    }

    fun adjustCompass(offsetY: Int) {
        if (mMapFragment == null || !mMapFragment!!.isAdded) return
        var resultOffset = offsetY
        //If the compass is covered by navigation buttons, we move it beyond the visible screen
        if (mNavAnimationController != null && mNavAnimationController!!.isConflictWithCompass(
                offsetY
            )
        ) {
            val halfHeight = (UiUtils.dimen(R.dimen.compass_height) * 0.5f).toInt()
            val margin = (UiUtils.dimen(R.dimen.margin_compass_top)
                    + UiUtils.dimen(R.dimen.nav_frame_padding))
            resultOffset = -(offsetY + halfHeight + margin)
        }
        mMapFragment!!.setupCompass(resultOffset, true)
        val compass = LocationHelper.INSTANCE.compassData
        if (compass != null) nativeCompassUpdated(
            compass.magneticNorth,
            compass.trueNorth,
            true
        )
    }

    private fun adjustBottomWidgets(offsetY: Int) {
        if (mMapFragment == null || !mMapFragment!!.isAdded) return
        mMapFragment!!.setupRuler(offsetY, false)
        mMapFragment!!.setupWatermark(offsetY, true)
    }

    override val activity: FragmentActivity
        get() = this

    override fun showSearch() {
        showSearch("")
    }

    override fun updateMenu() {
        adjustMenuLineFrameVisibility()
        mNavigationController.showSearchButtons(
            RoutingController.get().isPlanning
                    || RoutingController.get().isBuilt
        )
        if (RoutingController.get().isNavigating) {
            mNavigationController.show(true)
            mSearchController!!.hide()
            mainMenu!!.setState(MainMenu.State.NAVIGATION, false, mIsFullscreen)
            return
        }
        if (mIsTabletLayout) {
            mainMenu!!.setEnabled(MainMenu.Item.POINT_TO_POINT, !RoutingController.get().isPlanning)
            mainMenu!!.setEnabled(MainMenu.Item.SEARCH, !RoutingController.get().isWaitingPoiPick)
        } else if (RoutingController.get().isPlanning) {
            mainMenu!!.setState(MainMenu.State.ROUTE_PREPARE, false, mIsFullscreen)
            return
        }
        mainMenu!!.setState(MainMenu.State.MENU, false, mIsFullscreen)
    }

    override fun onAdsRemovalActivation() {
        closePlacePage()
    }

    private fun adjustMenuLineFrameVisibility() {
        val controller = RoutingController.get()
        if (controller.isBuilt || controller.isTaxiRequestHandled) {
            showLineFrame()
            return
        }
        if (controller.isPlanning || controller.isBuilding || controller.isErrorEncountered) {
            if (showAddStartOrFinishFrame(controller, true)) {
                return
            }
            showLineFrame(false)
            val menuHeight = currentMenu.frame.height
            adjustBottomWidgets(menuHeight)
            return
        }
        hideRoutingActionFrame()
        showLineFrame()
    }

    private fun showAddStartOrFinishFrame(
        controller: RoutingController,
        showFrame: Boolean
    ): Boolean { // S - start, F - finish, L - my position
// -S-F-L -> Start
// -S-F+L -> Finish
// -S+F-L -> Start
// -S+F+L -> Start + Use
// +S-F-L -> Finish
// +S-F+L -> Finish
// +S+F-L -> Hide
// +S+F+L -> Hide
        val myPosition = LocationHelper.INSTANCE.myPosition
        if (myPosition != null && !controller.hasEndPoint()) {
            showAddFinishFrame()
            if (showFrame) showLineFrame()
            return true
        }
        if (!controller.hasStartPoint()) {
            showAddStartFrame()
            if (showFrame) showLineFrame()
            return true
        }
        if (!controller.hasEndPoint()) {
            showAddFinishFrame()
            if (showFrame) showLineFrame()
            return true
        }
        return false
    }

    private fun showAddStartFrame() {
        if (!mIsTabletLayout) {
            mRoutingPlanInplaceController!!.showAddStartFrame()
            return
        }
        val fragment =
            getFragment(RoutingPlanFragment::class.java) as RoutingPlanFragment?
        fragment?.showAddStartFrame()
    }

    private fun showAddFinishFrame() {
        if (!mIsTabletLayout) {
            mRoutingPlanInplaceController!!.showAddFinishFrame()
            return
        }
        val fragment =
            getFragment(RoutingPlanFragment::class.java) as RoutingPlanFragment?
        fragment?.showAddFinishFrame()
    }

    private fun hideRoutingActionFrame() {
        if (!mIsTabletLayout) {
            mRoutingPlanInplaceController!!.hideActionFrame()
            return
        }
        val fragment =
            getFragment(RoutingPlanFragment::class.java) as RoutingPlanFragment?
        fragment?.hideActionFrame()
    }

    private fun showLineFrame() {
        showLineFrame(true)
        adjustBottomWidgets(0)
    }

    private fun showLineFrame(show: Boolean) {
        mainMenu!!.showLineFrame(show)
    }

    private fun setNavButtonsTopLimit(limit: Int) {
        if (mNavAnimationController == null) return
        mNavAnimationController!!.setTopLimit(limit.toFloat())
    }

    override fun onRoutingPlanStartAnimate(show: Boolean) {
        if (mNavAnimationController == null) return
        val totalHeight = calcFloatingViewsOffset()
        mNavAnimationController!!.setTopLimit(if (!show) 0F else totalHeight.toFloat())
        mNavAnimationController!!.setBottomLimit(if (!show) 0F else currentMenu.frame.height.toFloat())
        adjustCompassAndTraffic(if (!show) UiUtils.getStatusBarHeight(applicationContext) else totalHeight)
    }

    override fun showRoutePlan(
        show: Boolean,
        completionListener: Runnable?
    ) {
        if (show) {
            mSearchController!!.hide()
            if (mIsTabletLayout) {
                replaceFragment(RoutingPlanFragment::class.java, null, completionListener)
                if (mRestoreRoutingPlanFragmentNeeded && mSavedForTabletState != null) {
                    val fragment =
                        getFragment(RoutingPlanFragment::class.java) as RoutingPlanFragment?
                    fragment?.restoreRoutingPanelState(mSavedForTabletState!!)
                }
                showAddStartOrFinishFrame(RoutingController.get(), false)
                val width = UiUtils.dimen(R.dimen.panel_width)
                adjustTraffic(width, UiUtils.getStatusBarHeight(applicationContext))
                mNavigationController.adjustSearchButtons(width)
            } else {
                mRoutingPlanInplaceController!!.show(true)
                completionListener?.run()
            }
        } else {
            if (mIsTabletLayout) {
                adjustCompassAndTraffic(UiUtils.getStatusBarHeight(applicationContext))
                setNavButtonsTopLimit(0)
                mNavigationController.adjustSearchButtons(0)
            } else {
                mRoutingPlanInplaceController!!.show(false)
            }
            closeAllFloatingPanels()
            mNavigationController.resetSearchWheel()
            completionListener?.run()
            updateSearchBar()
        }
    }

    private fun adjustCompassAndTraffic(offsetY: Int) {
        addTask(object : RegularMapTask() {
            @JvmField
            val serialVersionUID = 9177064181621376624L
            override fun run(target: MwmActivity): Boolean {
                adjustCompass(offsetY)
                return true
            }
        })
        adjustTraffic(0, offsetY)
    }

    private fun adjustTraffic(offsetX: Int, offsetY: Int) {
        mToggleMapLayerController.adjust(offsetX, offsetY)
    }

    override fun onSearchVisibilityChanged(visible: Boolean) {
        if (mNavAnimationController == null) return
        adjustCompassAndTraffic(
            if (visible) calcFloatingViewsOffset() else UiUtils.getStatusBarHeight(
                applicationContext
            )
        )
        val toolbarHeight = mSearchController!!.toolbar.height
        setNavButtonsTopLimit(if (visible) toolbarHeight else 0)
        if (mFilterController != null) {
            val show = (visible && !TextUtils.isEmpty(SearchEngine.INSTANCE.query)
                    && !RoutingController.get().isNavigating)
            mFilterController!!.show(show, true)
            mainMenu!!.show(!show)
        }
    }

    private fun calcFloatingViewsOffset(): Int {
        var offset: Int = 0
        return if (mRoutingPlanInplaceController == null
            || mRoutingPlanInplaceController!!.calcHeight().also { offset = it } == 0
        ) UiUtils.getStatusBarHeight(
            this
        ) else offset
    }

    override fun onResultsUpdate(
        results: Array<SearchResult>?,
        timestamp: Long,
        isHotel: Boolean
    ) {
        if (mFilterController != null) mFilterController!!.updateFilterButtonVisibility(isHotel)
    }

    override fun onResultsEnd(timestamp: Long) {}
    override fun showNavigation(show: Boolean) { // TODO:
//    mPlacePage.refreshViews();
        mNavigationController.show(show)
        refreshFade()
        if (mOnmapDownloader != null) mOnmapDownloader!!.updateState(false)
        if (show) {
            mSearchController!!.clear()
            mSearchController!!.hide()
            if (mFilterController != null) mFilterController!!.show(false, true)
        }
    }

    override fun updateBuildProgress(progress: Int, @RouterType router: Int) {
        if (mIsTabletLayout) {
            val fragment =
                getFragment(RoutingPlanFragment::class.java) as RoutingPlanFragment?
            fragment?.updateBuildProgress(progress, router)
        } else {
            mRoutingPlanInplaceController!!.updateBuildProgress(progress, router)
        }
    }

    override fun onStartRouteBuilding() {
        if (mRoutingPlanInplaceController == null) return
        mRoutingPlanInplaceController!!.hideDrivingOptionsView()
    }

    override fun onTaxiInfoReceived(info: TaxiInfo) {
        if (mIsTabletLayout) {
            val fragment =
                getFragment(RoutingPlanFragment::class.java) as RoutingPlanFragment?
            fragment?.showTaxiInfo(info)
        } else {
            mRoutingPlanInplaceController!!.showTaxiInfo(info)
        }
    }

    override fun onTaxiError(code: TaxiManager.ErrorCode) {
        if (mIsTabletLayout) {
            val fragment =
                getFragment(RoutingPlanFragment::class.java) as RoutingPlanFragment?
            fragment?.showTaxiError(code)
        } else {
            mRoutingPlanInplaceController!!.showTaxiError(code)
        }
    }

    override fun onNavigationCancelled() {
        mNavigationController.stop(this)
        updateSearchBar()
        ThemeSwitcher.restart(isMapRendererActive)
        if (mRoutingPlanInplaceController == null) return
        mRoutingPlanInplaceController!!.hideDrivingOptionsView()
    }

    override fun onNavigationStarted() {
        ThemeSwitcher.restart(isMapRendererActive)
    }

    override fun onAddedStop() {
        closePlacePage()
    }

    override fun onRemovedStop() {
        closePlacePage()
    }

    override fun onBuiltRoute() {
        if (!RoutingController.get().isPlanning) return
        mNavigationController.resetSearchWheel()
    }

    override fun onDrivingOptionsWarning() {
        if (mRoutingPlanInplaceController == null) return
        mRoutingPlanInplaceController!!.showDrivingOptionView()
    }

    override val isSubwayEnabled: Boolean
        get() = from(this).isEnabled

    override fun onCommonBuildError(
        lastResultCode: Int,
        lastMissingMaps: Array<String>
    ) {
        val fragment =
            create(lastResultCode, lastMissingMaps)
        fragment.show(
            supportFragmentManager,
            RoutingErrorDialogFragment::class.java.simpleName
        )
    }

    override fun onDrivingOptionsBuildError() {
        val dialog =
            com.mapswithme.maps.dialog.AlertDialog.Builder()
                .setTitleId(R.string.unable_to_calc_alert_title)
                .setMessageId(R.string.unable_to_calc_alert_subtitle)
                .setPositiveBtnId(R.string.settings)
                .setNegativeBtnId(R.string.cancel)
                .setReqCode(REQ_CODE_ERROR_DRIVING_OPTIONS_DIALOG)
                .setFragManagerStrategyType(com.mapswithme.maps.dialog.AlertDialog.FragManagerStrategyType.ACTIVITY_FRAGMENT_MANAGER)
                .build()
        dialog.show(this, ERROR_DRIVING_OPTIONS_DIALOG_TAG)
    }

    private fun updateSearchBar() {
        if (!TextUtils.isEmpty(SearchEngine.INSTANCE.query)) mSearchController!!.refreshToolbar()
    }

    override fun onMyPositionModeChanged(newMode: Int) {
        if (mNavMyPosition != null) mNavMyPosition!!.update(newMode)
        val controller = RoutingController.get()
        if (controller.isPlanning) showAddStartOrFinishFrame(controller, true)
    }

    override fun onLocationUpdated(location: Location) {
        if (!RoutingController.get().isNavigating) return
        mNavigationController.update(nativeGetRouteFollowingInfo())
        TtsPlayer.INSTANCE.playTurnNotifications(applicationContext)
    }

    override fun onCompassUpdated(compass: CompassData) {
        nativeCompassUpdated(compass.magneticNorth, compass.trueNorth, false)
        mNavigationController.updateNorth(compass.north)
    }

    override fun onLocationError() {
        if (mLocationErrorDialogAnnoying) return
        val intent = TargetUtils.makeAppSettingsLocationIntent(applicationContext) ?: return
        showLocationErrorDialog(intent)
    }

    override fun onTranslationChanged(translation: Float) {
        mNavigationController.updateSearchButtonsTranslation(translation)
    }

    override fun onFadeInZoomButtons() {
        if (RoutingController.get().isPlanning || RoutingController.get().isNavigating) mNavigationController.fadeInSearchButtons()
    }

    override fun onFadeOutZoomButtons() {
        if (RoutingController.get().isPlanning || RoutingController.get().isNavigating) {
            if (UiUtils.isLandscape(this)) mToggleMapLayerController.hide() else mNavigationController.fadeOutSearchButtons()
        }
    }

    private fun showLocationErrorDialog(intent: Intent) {
        if (mLocationErrorDialog != null && mLocationErrorDialog!!.isShowing) return
        mLocationErrorDialog = AlertDialog.Builder(this)
            .setTitle(R.string.enable_location_services)
            .setMessage(R.string.location_is_disabled_long_text)
            .setNegativeButton(R.string.close) { dialog, which ->
                mLocationErrorDialogAnnoying = true
            }
            .setOnCancelListener { mLocationErrorDialogAnnoying = true }
            .setPositiveButton(
                R.string.connection_settings
            ) { dialog, which -> startActivity(intent) }.show()
    }

    override fun onLocationNotFound() {
        showLocationNotFoundDialog()
    }

    override fun onRoutingFinish() {
        Statistics.INSTANCE.trackRoutingFinish(
            false, RoutingController.get().lastRouterType,
            TrafficManager.INSTANCE.isEnabled
        )
    }

    private fun showLocationNotFoundDialog() {
        val message = String.format(
            "%s\n\n%s", getString(R.string.current_location_unknown_message),
            getString(R.string.current_location_unknown_title)
        )
        val stopClickListener =
            DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                LocationHelper.INSTANCE.setStopLocationUpdateByUser(true)
            }
        val continueClickListener =
            DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                if (!LocationHelper.INSTANCE.isActive) LocationHelper.INSTANCE.start()
                LocationHelper.INSTANCE.switchToNextMode()
            }
        AlertDialog.Builder(this)
            .setMessage(message)
            .setNegativeButton(R.string.current_location_unknown_stop_button, stopClickListener)
            .setPositiveButton(
                R.string.current_location_unknown_continue_button,
                continueClickListener
            )
            .setCancelable(false)
            .show()
    }

    override fun onPromptStateChanged(
        prompt: MaterialTapTargetPrompt,
        state: Int
    ) {
        if (mTutorial == null) return
        if (state != MaterialTapTargetPrompt.STATE_DISMISSED
            && state != MaterialTapTargetPrompt.STATE_FINISHED
        ) {
            return
        }
        logTipClickedEvent(mTutorial!!, TutorialAction.GOT_IT_CLICKED)
        Statistics.INSTANCE.trackTipsClose(mTutorial!!.ordinal)
        mTutorial = null
    }

    private fun tryToShowTutorial() {
        val tutorial = Tutorial.requestCurrent(this, javaClass)
        if (tutorial === Tutorial.STUB) return
        if (mTutorial != null) return
        mTutorial = tutorial
        mTutorial!!.show(activity, this)
        Statistics.INSTANCE.trackTipsEvent(
            Statistics.EventName.TIPS_TRICKS_SHOW,
            mTutorial!!.ordinal
        )
    }

    private fun tryToShowPromoAfterBooking(): Boolean {
        val policy =
            NetworkPolicy.newInstance(NetworkPolicy.getCurrentNetworkUsageStatus())
        val promo = nativeGetPromoAfterBooking(policy) ?: return false
        val dialogName = PromoBookingDialogFragment::class.java.name
        if (supportFragmentManager.findFragmentByTag(dialogName) != null) return true
        val args = Bundle()
        args.putString(PromoBookingDialogFragment.EXTRA_CITY_GUIDES_URL, promo.guidesUrl)
        args.putString(PromoBookingDialogFragment.EXTRA_CITY_IMAGE_URL, promo.imageUrl)
        val fragment =
            Fragment.instantiate(
                this,
                dialogName,
                args
            ) as DialogFragment
        fragment.show(supportFragmentManager, dialogName)
        logPromoAfterBookingShown(promo.id)
        Statistics.INSTANCE.trackEvent(
            Statistics.EventName.INAPP_SUGGESTION_SHOWN,
            Statistics.makeInAppSuggestionParamBuilder()
        )
        return true
    }

    private fun tryToShowAdditionalViewOnTop() {
        if (tryToShowPromoAfterBooking()) return
        tryToShowTutorial()
    }

    override fun onTransit(foreground: Boolean) {
        if (foreground) tryToShowAdditionalViewOnTop()
    }

    override fun onUseMyPositionAsStart() {
        RoutingController.get().setStartPoint(LocationHelper.INSTANCE.myPosition)
    }

    override fun onSearchRoutePoint(@RouteMarkType pointType: Int) {
        RoutingController.get().waitForPoiPick(pointType)
        mNavigationController.performSearchClick()
        Statistics.INSTANCE.trackRoutingTooltipEvent(pointType, true)
    }

    override fun onRoutingStart() {
        @RouterType val routerType = RoutingController.get().lastRouterType
        Statistics.INSTANCE.trackRoutingStart(
            routerType,
            TrafficManager.INSTANCE.isEnabled
        )
        closeMenu(Runnable { RoutingController.get().start() })
    }

    override fun onBookmarksLoadingStarted() { // Do nothing
    }

    override fun onBookmarksLoadingFinished() { // Do nothing
    }

    override fun onAlertDialogPositiveClick(requestCode: Int, which: Int) {
        if (requestCode == REQ_CODE_ERROR_DRIVING_OPTIONS_DIALOG) DrivingOptionsActivity.start(
            this
        ) else if (requestCode == REQ_CODE_CATALOG_UNLIMITED_ACCESS) start(
            this,
            INSTANCE.getCatalogFrontendUrl(UTM.UTM_NONE)
        )
    }

    override fun onAlertDialogNegativeClick(
        requestCode: Int,
        which: Int
    ) { // Do nothing
    }

    override fun onAlertDialogCancel(requestCode: Int) { // Do nothing
    }

    override fun onBookmarksFileLoaded(success: Boolean) {
        Utils.toastShortcut(
            this@MwmActivity,
            if (success) R.string.load_kmz_successful else R.string.load_kmz_failed
        )
    }

    override fun onSearchClearClick() {
        if (mFilterController != null) mFilterController!!.resetFilter()
    }

    override fun onSearchUpClick(query: String?) {
        showSearch(query)
    }

    override fun onSearchQueryClick(query: String?) {
        showSearch(query)
    }

    fun showIntroductionScreenForDeeplink(
        deepLink: String,
        factory: IntroductionScreenFactory
    ) {
        show(supportFragmentManager, deepLink, factory)
    }

    override fun onOnboardingStepPassed(step: OnboardingStep) {
        if (mOnboardingTip == null) throw AssertionError("Onboarding tip must be non-null at this point!")
        when (step) {
            OnboardingStep.DISCOVER_GUIDES, OnboardingStep.CHECK_OUT_SIGHTS -> startForResult(
                this,
                BookmarkCategoriesActivity.REQ_CODE_DOWNLOAD_BOOKMARK_CATEGORY,
                mOnboardingTip!!.url
            )
            OnboardingStep.SUBSCRIBE_TO_CATALOG -> BookmarksAllSubscriptionActivity.startForResult(
                this
            )
            else -> throw UnsupportedOperationException(
                "Onboarding step '" + step + "' not supported " +
                        "for sponsored button"
            )
        }
    }

    override fun onLastOnboardingStepPassed() { // Do nothing by default.
    }

    override fun onOnboardingStepCancelled() { // Do nothing by default.
    }

    private inner class CurrentPositionClickListener :
        View.OnClickListener {
        override fun onClick(v: View) {
            Statistics.INSTANCE.trackEvent(Statistics.EventName.TOOLBAR_MY_POSITION)
            AlohaHelper.logClick(AlohaHelper.TOOLBAR_MY_POSITION)
            if (!PermissionsUtils.isLocationGranted()) {
                if (PermissionsUtils.isLocationExplanationNeeded(this@MwmActivity)) PermissionsUtils.requestLocationPermission(
                    this@MwmActivity,
                    REQ_CODE_LOCATION_PERMISSION
                ) else Toast.makeText(
                    this@MwmActivity,
                    R.string.enable_location_services,
                    Toast.LENGTH_SHORT
                )
                    .show()
                return
            }
            myPositionClick()
        }
    }

    abstract class AbstractClickMenuDelegate(
        private val mActivity: MwmActivity,
        val item: MainMenu.Item
    ) : ClickMenuDelegate {
        fun getActivity(): MwmActivity {
            return mActivity
        }

        override fun onMenuItemClick() {
            val api = Tutorial.requestCurrent(getActivity(), getActivity().javaClass)
            LOGGER.d(TAG, "Tutorial = $api")
            if (item === api.siblingMenuItem) {
                api.createClickInterceptor().onInterceptClick(getActivity())
                Statistics.INSTANCE.trackTipsEvent(
                    Statistics.EventName.TIPS_TRICKS_CLICK,
                    api.ordinal
                )
            } else onMenuItemClickInternal()
        }

        abstract fun onMenuItemClickInternal()

    }

    class MenuClickDelegate(
        activity: MwmActivity,
        item: MainMenu.Item
    ) : AbstractClickMenuDelegate(activity, item) {
        override fun onMenuItemClickInternal() {
            if (!getActivity().mainMenu!!.isOpen) {
                Statistics.INSTANCE.trackToolbarClick(item)
                // TODO:
                if ( /*getActivity().mPlacePage.isDocked() &&*/getActivity().closePlacePage()) return
                if (getActivity().closeSidePanel()) return
            }
            getActivity().toggleMenu()
        }
    }

    class AddPlaceDelegate(
        activity: MwmActivity,
        item: MainMenu.Item
    ) : StatisticClickMenuDelegate(activity, item) {
        override fun onPostStatisticMenuItemClick() {
            getActivity().closePlacePage()
            if (getActivity().mIsTabletLayout) getActivity().closeSidePanel()
            getActivity().closeMenu(Runnable {
                getActivity().showPositionChooser(
                    false,
                    false
                )
            })
        }
    }

    class SearchClickDelegate(
        activity: MwmActivity,
        item: MainMenu.Item
    ) : AbstractClickMenuDelegate(activity, item) {
        override fun onMenuItemClickInternal() {
            Statistics.INSTANCE.trackToolbarClick(item)
            RoutingController.get().cancel()
            getActivity().closeMenu(Runnable { getActivity().showSearch(getActivity().mSearchController!!.query) })
        }
    }

    class SettingsDelegate(
        activity: MwmActivity,
        item: MainMenu.Item
    ) : StatisticClickMenuDelegate(activity, item) {
        override fun onPostStatisticMenuItemClick() {
            val intent = Intent(getActivity(), SettingsActivity::class.java)
            getActivity().closeMenu(Runnable { getActivity().startActivity(intent) })
        }
    }

    class DownloadGuidesDelegate(
        activity: MwmActivity,
        item: MainMenu.Item
    ) : StatisticClickMenuDelegate(activity, item) {
        override fun onPostStatisticMenuItemClick() {
            val requestCode = BookmarkCategoriesActivity.REQ_CODE_DOWNLOAD_BOOKMARK_CATEGORY
            val catalogUrl =
                INSTANCE.getCatalogFrontendUrl(UTM.UTM_TOOLBAR_BUTTON)
            getActivity().closeMenu(Runnable {
                startForResult(
                    getActivity(),
                    requestCode,
                    catalogUrl
                )
            })
        }
    }

    class HotelSearchDelegate(
        activity: MwmActivity,
        item: MainMenu.Item
    ) : StatisticClickMenuDelegate(activity, item) {
        override fun onPostStatisticMenuItemClick() {
            getActivity().closeMenu(Runnable { getActivity().runHotelCategorySearchOnMap() })
        }
    }

    abstract class StatisticClickMenuDelegate internal constructor(
        activity: MwmActivity,
        item: MainMenu.Item
    ) : AbstractClickMenuDelegate(activity, item) {
        override fun onMenuItemClickInternal() {
            Statistics.INSTANCE.trackToolbarMenu(item)
            onPostStatisticMenuItemClick()
        }

        abstract fun onPostStatisticMenuItemClick()
    }

    class DownloadMapsDelegate(
        activity: MwmActivity,
        item: MainMenu.Item
    ) : StatisticClickMenuDelegate(activity, item) {
        override fun onPostStatisticMenuItemClick() {
            RoutingController.get().cancel()
            getActivity().closeMenu(Runnable { getActivity().showDownloader(false) })
        }
    }

    class BookmarksDelegate(
        activity: MwmActivity,
        item: MainMenu.Item
    ) : StatisticClickMenuDelegate(activity, item) {
        override fun onPostStatisticMenuItemClick() {
            getActivity().closeMenu(Runnable { getActivity().showBookmarks() })
        }
    }

    class ShareMyLocationDelegate(
        activity: MwmActivity,
        item: MainMenu.Item
    ) : StatisticClickMenuDelegate(activity, item) {
        override fun onPostStatisticMenuItemClick() {
            getActivity().closeMenu(Runnable { getActivity().shareMyLocation() })
        }
    }

    class DiscoveryDelegate(
        activity: MwmActivity,
        item: MainMenu.Item
    ) : StatisticClickMenuDelegate(activity, item) {
        override fun onPostStatisticMenuItemClick() {
            getActivity().showDiscovery()
        }
    }

    class PointToPointDelegate(
        activity: MwmActivity,
        item: MainMenu.Item
    ) : StatisticClickMenuDelegate(activity, item) {
        override fun onPostStatisticMenuItemClick() {
            getActivity().startLocationToPoint(null, false)
        }
    }

    private inner class ToolbarLayoutChangeListener : OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) mSearchController!!.toolbar.viewTreeObserver
                .removeOnGlobalLayoutListener(this) else mSearchController!!.toolbar.viewTreeObserver
                .removeGlobalOnLayoutListener(this)
            adjustCompassAndTraffic(
                if (UiUtils.isVisible(mSearchController!!.toolbar)) calcFloatingViewsOffset() else UiUtils.getStatusBarHeight(
                    applicationContext
                )
            )
        }
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = MwmActivity::class.java.simpleName
        const val EXTRA_TASK = "map_task"
        const val EXTRA_LAUNCH_BY_DEEP_LINK = "launch_by_deep_link"
        private const val EXTRA_CONSUMED = "mwm.extra.intent.processed"
        private const val EXTRA_ONBOARDING_TIP = "extra_onboarding_tip"
        private val DOCKED_FRAGMENTS = arrayOf(
            SearchFragment::class.java.name,
            DownloaderFragment::class.java.name,
            RoutingPlanFragment::class.java.name,
            EditorHostFragment::class.java.name,
            ReportFragment::class.java.name,
            DiscoveryFragment::class.java.name
        )
        private const val EXTRA_LOCATION_DIALOG_IS_ANNOYING = "LOCATION_DIALOG_IS_ANNOYING"
        private const val REQ_CODE_LOCATION_PERMISSION = 1
        private const val REQ_CODE_DISCOVERY = 2
        private const val REQ_CODE_SHOW_SIMILAR_HOTELS = 3
        const val REQ_CODE_ERROR_DRIVING_OPTIONS_DIALOG = 5
        const val REQ_CODE_DRIVING_OPTIONS = 6
        const val REQ_CODE_CATALOG_UNLIMITED_ACCESS = 7
        const val ERROR_DRIVING_OPTIONS_DIALOG_TAG = "error_driving_options_dialog_tag"
        const val CATALOG_UNLIMITED_ACCESS_DIALOG_TAG =
            "catalog_unlimited_access_dialog_tag"

        fun createShowMapIntent(
            context: Context,
            countryId: String?
        ): Intent {
            return Intent(context, DownloadResourcesLegacyActivity::class.java)
                .putExtra(DownloadResourcesLegacyActivity.EXTRA_COUNTRY, countryId)
        }

        fun createAuthenticateIntent(context: Context): Intent {
            return Intent(context, MwmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(
                    EXTRA_TASK,
                    ShowDialogTask(PassportAuthDialogFragment::class.java.name)
                )
        }

        fun createLeaveReviewIntent(
            context: Context,
            nc: UgcReview
        ): Intent {
            return Intent(context, MwmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_TASK, ShowUGCEditorTask(nc))
        }

        private fun checkMeasurementSystem() {
            initializeCurrentUnits()
        }
    }
}