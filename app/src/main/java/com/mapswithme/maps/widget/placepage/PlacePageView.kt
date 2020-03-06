package com.mapswithme.maps.widget.placepage

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.util.Linkify
import android.util.AttributeSet
import android.view.*
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.webkit.WebView
import android.widget.*
import androidx.annotation.ColorInt
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.Framework
import com.mapswithme.maps.Framework.getFilterRating
import com.mapswithme.maps.Framework.logLocalAdsEvent
import com.mapswithme.maps.Framework.nativeDeleteBookmarkFromMapObject
import com.mapswithme.maps.Framework.nativeFormatAltitude
import com.mapswithme.maps.Framework.nativeFormatLatLon
import com.mapswithme.maps.Framework.nativeFormatLatLonToArr
import com.mapswithme.maps.Framework.nativeFormatSpeed
import com.mapswithme.maps.Framework.nativeGetActiveObjectFormattedCuisine
import com.mapswithme.maps.Framework.nativeGetDistanceAndAzimuthFromLatLon
import com.mapswithme.maps.MwmActivity
import com.mapswithme.maps.MwmApplication.Companion.prefs
import com.mapswithme.maps.R
import com.mapswithme.maps.api.ParsedMwmRequest.Companion.currentRequest
import com.mapswithme.maps.api.ParsedMwmRequest.Companion.hasRequest
import com.mapswithme.maps.api.ParsedMwmRequest.Companion.isPickPointMode
import com.mapswithme.maps.base.Detachable
import com.mapswithme.maps.bookmarks.data.*
import com.mapswithme.maps.bookmarks.data.MapObject.Companion.isOfType
import com.mapswithme.maps.bookmarks.data.MapObject.Companion.same
import com.mapswithme.maps.downloader.CountryItem
import com.mapswithme.maps.downloader.CountryItem.Companion.fill
import com.mapswithme.maps.downloader.DownloaderStatusIcon
import com.mapswithme.maps.downloader.MapManager.StorageCallback
import com.mapswithme.maps.downloader.MapManager.StorageCallbackData
import com.mapswithme.maps.downloader.MapManager.nativeCancel
import com.mapswithme.maps.downloader.MapManager.nativeGetSelectedCountry
import com.mapswithme.maps.downloader.MapManager.nativeSubscribe
import com.mapswithme.maps.downloader.MapManager.nativeUnsubscribe
import com.mapswithme.maps.downloader.MapManager.warn3gAndDownload
import com.mapswithme.maps.editor.Editor.nativeShouldShowAddBusiness
import com.mapswithme.maps.editor.Editor.nativeShouldShowAddPlace
import com.mapswithme.maps.editor.Editor.nativeShouldShowEditPlace
import com.mapswithme.maps.editor.OpeningHours.nativeTimetablesFromString
import com.mapswithme.maps.editor.data.TimeFormatUtils.formatTimetables
import com.mapswithme.maps.gallery.Constants
import com.mapswithme.maps.gallery.FullScreenGalleryActivity.Companion.start
import com.mapswithme.maps.gallery.GalleryActivity
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.metrics.UserActionsLogger.logBookingBookClicked
import com.mapswithme.maps.metrics.UserActionsLogger.logBookingDetailsClicked
import com.mapswithme.maps.metrics.UserActionsLogger.logBookingMoreClicked
import com.mapswithme.maps.metrics.UserActionsLogger.logBookingReviewsClicked
import com.mapswithme.maps.promo.CatalogPromoController
import com.mapswithme.maps.promo.PromoCityGallery
import com.mapswithme.maps.promo.PromoEntity
import com.mapswithme.maps.routing.RoutingController.Companion.get
import com.mapswithme.maps.search.FilterUtils
import com.mapswithme.maps.search.FilterUtils.RatingDef
import com.mapswithme.maps.search.FilterUtils.createHotelFilter
import com.mapswithme.maps.search.Popularity
import com.mapswithme.maps.settings.RoadType
import com.mapswithme.maps.ugc.Impress
import com.mapswithme.maps.ugc.UGCController
import com.mapswithme.maps.widget.ArrowView
import com.mapswithme.maps.widget.LineCountTextView
import com.mapswithme.maps.widget.LineCountTextView.OnLineCountCalculatedListener
import com.mapswithme.maps.widget.RatingView
import com.mapswithme.maps.widget.placepage.DirectionFragment
import com.mapswithme.maps.widget.placepage.EditBookmarkFragment.Companion.editBookmark
import com.mapswithme.maps.widget.placepage.EditBookmarkFragment.EditBookmarkListener
import com.mapswithme.maps.widget.placepage.PlacePageButtons.ButtonType
import com.mapswithme.maps.widget.placepage.PlacePageButtons.PlacePageButton
import com.mapswithme.maps.widget.placepage.PlacePageView
import com.mapswithme.maps.widget.placepage.Sponsored.*
import com.mapswithme.maps.widget.placepage.Sponsored.Companion.nativeGetCurrent
import com.mapswithme.maps.widget.recycler.ItemDecoratorFactory.createHotelGalleryDecorator
import com.mapswithme.maps.widget.recycler.RecyclerClickListener
import com.mapswithme.util.ConnectionState.isConnected
import com.mapswithme.util.Graphics.tint
import com.mapswithme.util.NetworkPolicy
import com.mapswithme.util.NetworkPolicy.NetworkPolicyListener
import com.mapswithme.util.SponsoredLinks
import com.mapswithme.util.StringUtils.getFileSizeString
import com.mapswithme.util.StringUtils.nativeIsHtml
import com.mapswithme.util.ThemeUtils.getColor
import com.mapswithme.util.UiUtils.clearTextAndHide
import com.mapswithme.util.UiUtils.dimen
import com.mapswithme.util.UiUtils.hide
import com.mapswithme.util.UiUtils.hideIf
import com.mapswithme.util.UiUtils.isLandscape
import com.mapswithme.util.UiUtils.isVisible
import com.mapswithme.util.UiUtils.setTextAndHideIfEmpty
import com.mapswithme.util.UiUtils.setTextAndShow
import com.mapswithme.util.UiUtils.show
import com.mapswithme.util.UiUtils.showIf
import com.mapswithme.util.Utils.PartnerAppOpenMode
import com.mapswithme.util.Utils.Proc
import com.mapswithme.util.Utils.callPhone
import com.mapswithme.util.Utils.castTo
import com.mapswithme.util.Utils.checkConnection
import com.mapswithme.util.Utils.copyTextToClipboard
import com.mapswithme.util.Utils.currencyCode
import com.mapswithme.util.Utils.formatCurrencyString
import com.mapswithme.util.Utils.openPartner
import com.mapswithme.util.Utils.openUrl
import com.mapswithme.util.Utils.sendTo
import com.mapswithme.util.Utils.toastShortcut
import com.mapswithme.util.Utils.unCapitalize
import com.mapswithme.util.concurrency.UiThread.runLater
import com.mapswithme.util.log.LoggerFactory
import com.mapswithme.util.log.LoggerFactory.Companion.INSTANCE
import com.mapswithme.util.sharing.ShareOption
import com.mapswithme.util.statistics.AlohaHelper
import com.mapswithme.util.statistics.AlohaHelper.logClick
import com.mapswithme.util.statistics.AlohaHelper.logException
import com.mapswithme.util.statistics.AlohaHelper.logLongClick
import com.mapswithme.util.statistics.Statistics
import com.mapswithme.util.statistics.Statistics.Companion.params
import com.mapswithme.util.statistics.Statistics.EventName.PP_HOTEL_DESCRIPTION_LAND
import com.mapswithme.util.statistics.Statistics.EventName.PP_HOTEL_FACILITIES
import com.mapswithme.util.statistics.Statistics.EventName.PP_HOTEL_GALLERY_OPEN
import com.mapswithme.util.statistics.Statistics.EventName.PP_HOTEL_REVIEWS_LAND
import com.mapswithme.util.statistics.Statistics.EventName.PP_SPONSORED_ACTION
import com.mapswithme.util.statistics.Statistics.EventName.PP_SPONSORED_DETAILS
import com.mapswithme.util.statistics.Statistics.EventName.PP_SPONSORED_OPENTABLE
import java.util.*
import kotlin.Result

class PlacePageView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NestedScrollView(context!!, attrs), OnClickListener, OnLongClickListener,
    OnPriceReceivedListener, OnHotelInfoReceivedListener, OnLineCountCalculatedListener,
    RecyclerClickListener, NearbyAdapter.OnItemClickListener,
    EditBookmarkListener, Detachable<Activity?> {
    var isDocked = false
        private set
    var isFloating = false
        private set
    // Preview.
    private var mPreview: ViewGroup? = null
    private var mToolbar: Toolbar? = null
    private var mTvTitle: TextView? = null
    private var mTvSecondaryTitle: TextView? = null
    private var mTvSubtitle: TextView? = null
    private var mAvDirection: ArrowView? = null
    private var mTvDistance: TextView? = null
    private var mTvAddress: TextView? = null
    private var mPreviewRatingInfo: View? = null
    private var mRatingView: RatingView? = null
    private var mTvSponsoredPrice: TextView? = null
    private lateinit var mHotelDiscount: RatingView
    private var mPhone: View? = null
    private var mTvPhone: TextView? = null
    private var mWebsite: View? = null
    private var mTvWebsite: TextView? = null
    private var mTvLatlon: TextView? = null
    private var mOpeningHours: View? = null
    private var mFullOpeningHours: TextView? = null
    private var mTodayOpeningHours: TextView? = null
    private var mWifi: View? = null
    private var mEmail: View? = null
    private var mTvEmail: TextView? = null
    private var mOperator: View? = null
    private var mTvOperator: TextView? = null
    private var mCuisine: View? = null
    private var mTvCuisine: TextView? = null
    private var mWiki: View? = null
    private var mEntrance: View? = null
    private var mTvEntrance: TextView? = null
    private var mTaxiShadow: View? = null
    private var mTaxiDivider: View? = null
    private var mTaxi: View? = null
    private var mEditPlace: View? = null
    private var mAddOrganisation: View? = null
    private var mAddPlace: View? = null
    private var mLocalAd: View? = null
    private var mTvLocalAd: TextView? = null
    private var mEditTopSpace: View? = null
    // Bookmark
    private var mBookmarkFrame: View? = null
    private var mWvBookmarkNote: WebView? = null
    private var mTvBookmarkNote: TextView? = null
    private var mBookmarkSet = false
    // Place page buttons
    private var mButtons: PlacePageButtons? = null
    private var mBookmarkButtonIcon: ImageView? = null
    // Hotel
    private var mHotelDescription: View? = null
    private var mTvHotelDescription: LineCountTextView? = null
    private var mHotelMoreDescription: View? = null
    private var mHotelFacilities: View? = null
    private var mHotelMoreFacilities: View? = null
    private var mHotelGallery: View? = null
    private var mRvHotelGallery: RecyclerView? = null
    private var mHotelNearby: View? = null
    private var mHotelReview: View? = null
    private var mHotelRating: TextView? = null
    private var mHotelRatingBase: TextView? = null
    private var mHotelMore: View? = null
    private var mBookmarkButtonFrame: View? = null
    private lateinit var mPlaceDescriptionContainer: View
    private lateinit var mPlaceDescriptionView: TextView
    private lateinit var mPlaceDescriptionMoreBtn: View
    private lateinit var mPopularityView: View
    private lateinit var mUgcController: UGCController
    private lateinit var mCatalogPromoController: CatalogPromoController
    // Data
    var mapObject: MapObject? = null
        private set
    private var mSponsored: Sponsored? = null
    private var mSponsoredPrice: String? = null
    private var mIsLatLonDms: Boolean
    private val mFacilitiesAdapter = FacilitiesAdapter()
    private val mGalleryAdapter: GalleryAdapter
    private val mNearbyAdapter = NearbyAdapter(this)
    private val mReviewAdapter =
        ReviewAdapter()
    // Downloader`s stuff
    private var mDownloaderIcon: DownloaderStatusIcon? = null
    private var mDownloaderInfo: TextView? = null
    private var mStorageCallbackSlot = 0
    private var mCurrentCountry: CountryItem? = null
    private var mScrollable = true
    private val mStorageCallback: StorageCallback = object : StorageCallback {
        override fun onStatusChanged(data: List<StorageCallbackData>) {
            if (mCurrentCountry == null) return
            for (item in data) if (mCurrentCountry!!.id == item.countryId) {
                updateDownloader()
                return
            }
        }

        override fun onProgress(
            countryId: String,
            localSize: Long,
            remoteSize: Long
        ) {
            if (mCurrentCountry != null && mCurrentCountry!!.id == countryId) updateDownloader()
        }
    }
    private val mDownloaderDeferredDetachProc = Runnable { detachCountry() }
    private val mEditBookmarkClickListener = EditBookmarkClickListener()
    private val mDownloadClickListener: OnClickListener =
        object : OnClickListener {
            override fun onClick(v: View) {
                warn3gAndDownload(
                    activity,
                    mCurrentCountry!!.id,
                    Runnable { onDownloadClick() }
                )
            }

            private fun onDownloadClick() {
                val scenario =
                    if (mCurrentCountry!!.isExpandable) "download_group" else "download"
                Statistics.INSTANCE.trackEvent(
                    Statistics.EventName.DOWNLOADER_ACTION,
                    params()
                        .add(
                            Statistics.EventParam.ACTION,
                            "download"
                        )
                        .add(Statistics.EventParam.FROM, "placepage")
                        .add("is_auto", "false")
                        .add("scenario", scenario)
                )
            }
        }
    private val mCancelDownloadListener =
        OnClickListener {
            nativeCancel(mCurrentCountry!!.id)
            Statistics.INSTANCE.trackEvent(
                Statistics.EventName.DOWNLOADER_CANCEL,
                params()
                    .add(Statistics.EventParam.FROM, "placepage")
            )
        }
    private var mClosable: Closable? = null
    private var mRoutingModeListener: RoutingModeListener? = null
    private lateinit var mCatalogPromoTitleView: View
    fun setScrollable(scrollable: Boolean) {
        mScrollable = scrollable
    }

    fun addClosable(closable: Closable) {
        mClosable = closable
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        return when (ev?.action) {
            MotionEvent.ACTION_DOWN -> mScrollable && super.onTouchEvent(ev)
            else -> super.onTouchEvent(ev)
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return mScrollable && super.onInterceptTouchEvent(event)
    }

    override fun attach(`object`: Activity?) {
        mCatalogPromoController.attach(`object`)
    }

    override fun detach() {
        mCatalogPromoController.detach()
    }

    interface SetMapObjectListener {
        fun onSetMapObjectComplete(
            policy: NetworkPolicy,
            isSameObject: Boolean
        )
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        mPreview = findViewById(R.id.pp__preview)
        mTvTitle = mPreview?.findViewById(R.id.tv__title)
        mPopularityView = findViewById(R.id.popular_rating_view)
        mTvSecondaryTitle = mPreview?.findViewById(R.id.tv__secondary_title)
        mToolbar = findViewById(R.id.toolbar)
        mTvSubtitle = mPreview?.findViewById(R.id.tv__subtitle)
        val directionFrame =
            mPreview?.findViewById<View>(R.id.direction_frame)
        mTvDistance = mPreview?.findViewById(R.id.tv__straight_distance)
        mAvDirection = mPreview?.findViewById(R.id.av__direction)
        directionFrame?.setOnClickListener(this)
        mTvAddress = mPreview?.findViewById(R.id.tv__address)
        mPreview?.findViewById<View>(R.id.search_hotels_btn)?.setOnClickListener(this)
        mPreviewRatingInfo = mPreview?.findViewById(R.id.preview_rating_info)
        mRatingView = mPreviewRatingInfo?.findViewById(R.id.rating_view)
        mTvSponsoredPrice = mPreviewRatingInfo?.findViewById(R.id.tv__hotel_price)
        mHotelDiscount = mPreviewRatingInfo?.findViewById(R.id.discount_in_percents)!!
        val address = findViewById<RelativeLayout>(R.id.ll__place_name)
        mPhone = findViewById(R.id.ll__place_phone)
        mPhone?.setOnClickListener(this)
        mTvPhone = findViewById(R.id.tv__place_phone)
        mWebsite = findViewById(R.id.ll__place_website)
        mWebsite?.setOnClickListener(this)
        mTvWebsite = findViewById(R.id.tv__place_website)
        val latlon = findViewById<LinearLayout>(R.id.ll__place_latlon)
        latlon.setOnClickListener(this)
        mTvLatlon = findViewById(R.id.tv__place_latlon)
        mOpeningHours = findViewById(R.id.ll__place_schedule)
        mFullOpeningHours = findViewById(R.id.opening_hours)
        mTodayOpeningHours = findViewById(R.id.today_opening_hours)
        mWifi = findViewById(R.id.ll__place_wifi)
        mEmail = findViewById(R.id.ll__place_email)
        mEmail?.setOnClickListener(this)
        mTvEmail = findViewById(R.id.tv__place_email)
        mOperator = findViewById(R.id.ll__place_operator)
        mOperator?.setOnClickListener(this)
        mTvOperator = findViewById(R.id.tv__place_operator)
        mCuisine = findViewById(R.id.ll__place_cuisine)
        mTvCuisine = findViewById(R.id.tv__place_cuisine)
        mWiki = findViewById(R.id.ll__place_wiki)
        mWiki?.setOnClickListener(this)
        mEntrance = findViewById(R.id.ll__place_entrance)
        mTvEntrance = mEntrance?.findViewById(R.id.tv__place_entrance)
        mTaxiShadow = findViewById(R.id.place_page_taxi_shadow)
        mTaxiDivider = findViewById(R.id.place_page_taxi_divider)
        mTaxi = findViewById(R.id.ll__place_page_taxi)
        val orderTaxi = mTaxi?.findViewById<TextView>(R.id.tv__place_page_order_taxi)
        orderTaxi?.setOnClickListener(this)
        mEditPlace = findViewById(R.id.ll__place_editor)
        mEditPlace?.setOnClickListener(this)
        mAddOrganisation = findViewById(R.id.ll__add_organisation)
        mAddOrganisation?.setOnClickListener(this)
        mAddPlace = findViewById(R.id.ll__place_add)
        mAddPlace?.setOnClickListener(this)
        mLocalAd = findViewById(R.id.ll__local_ad)
        mLocalAd?.setOnClickListener(this)
        mTvLocalAd = mLocalAd?.findViewById(R.id.tv__local_ad)
        mEditTopSpace = findViewById(R.id.edit_top_space)
        latlon.setOnLongClickListener(this)
        address.setOnLongClickListener(this)
        mPhone?.setOnLongClickListener(this)
        mWebsite?.setOnLongClickListener(this)
        mOpeningHours?.setOnLongClickListener(this)
        mEmail?.setOnLongClickListener(this)
        mOperator?.setOnLongClickListener(this)
        mWiki?.setOnLongClickListener(this)
        mBookmarkFrame = findViewById(R.id.bookmark_frame)
        mWvBookmarkNote = mBookmarkFrame?.findViewById(R.id.wv__bookmark_notes)
        mWvBookmarkNote?.getSettings()?.javaScriptEnabled = false
        mTvBookmarkNote = mBookmarkFrame?.findViewById(R.id.tv__bookmark_notes)
        initEditMapObjectBtn()
        mHotelMore = findViewById(R.id.ll__more)
        mHotelMore?.setOnClickListener(this)
        initHotelDescriptionView()
        initHotelFacilitiesView()
        initHotelGalleryView()
        initHotelNearbyView()
        initHotelRatingView()
        mCatalogPromoController = CatalogPromoController(this)
        mUgcController = UGCController(this)
        mDownloaderIcon =
            DownloaderStatusIcon(mPreview?.findViewById(R.id.downloader_status_frame)!!)
        mDownloaderInfo = mPreview?.findViewById(R.id.tv__downloader_details)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation =
            dimen(R.dimen.placepage_elevation).toFloat()
        if (isLandscape(context)) setBackgroundResource(0)
        Sponsored.setPriceListener(this)
        Sponsored.setInfoListener(this)
        initPlaceDescriptionView()
    }

    fun initButtons(buttons: ViewGroup) {
        mButtons = PlacePageButtons(this, buttons, object : PlacePageButtons.ItemListener {
            override fun onPrepareVisibleView(
                item: PlacePageButton,
                frame: View, icon: ImageView,
                title: TextView
            ) {
                val color: Int
                when (item.type) {
                    ButtonType.BOOKING, ButtonType.BOOKING_SEARCH, ButtonType.OPENTABLE, ButtonType.PARTNER1, ButtonType.PARTNER3, ButtonType.PARTNER18, ButtonType.PARTNER19, ButtonType.PARTNER20 -> {
                        // /%PartnersExtender.Switch
// End of autogenerated code.
// ---------------------------------------------------------------------------------------
                        frame.setBackgroundResource(item.backgroundResource)
                        color = Color.WHITE
                    }
                    ButtonType.PARTNER2 -> {
                        frame.setBackgroundResource(item.backgroundResource)
                        color = Color.BLACK
                    }
                    ButtonType.BOOKMARK -> {
                        mBookmarkButtonIcon = icon
                        mBookmarkButtonFrame = frame
                        updateBookmarkButton()
                        color =
                            getColor(context, R.attr.iconTint)
                    }
                    else -> {
                        color =
                            getColor(context, R.attr.iconTint)
                        icon.setColorFilter(color)
                    }
                }
                title.setTextColor(color)
            }

            override fun onItemClick(item: PlacePageButton?) {
                when (item?.type) {
                    ButtonType.BOOKMARK -> onBookmarkBtnClicked()
                    ButtonType.SHARE -> onShareBtnClicked()
                    ButtonType.BACK -> onBackBtnClicked()
                    ButtonType.ROUTE_FROM -> onRouteFromBtnClicked()
                    ButtonType.ROUTE_TO -> onRouteToBtnClicked()
                    ButtonType.ROUTE_ADD -> onRouteAddBtnClicked()
                    ButtonType.ROUTE_REMOVE -> onRouteRemoveBtnClicked()
                    ButtonType.ROUTE_AVOID_TOLL -> onAvoidTollBtnClicked()
                    ButtonType.ROUTE_AVOID_UNPAVED -> onAvoidUnpavedBtnClicked()
                    ButtonType.ROUTE_AVOID_FERRY -> onAvoidFerryBtnClicked()
                    ButtonType.BOOKING, ButtonType.OPENTABLE, ButtonType.PARTNER1, ButtonType.PARTNER2, ButtonType.PARTNER3, ButtonType.PARTNER18, ButtonType.PARTNER19, ButtonType.PARTNER20 ->  // /%PartnersExtender.SwitchClick
// End of autogenerated code.
// -----------------------------------------------------------------------------------------
                        onSponsoredClick(true /* book */, false)
                    ButtonType.BOOKING_SEARCH -> onBookingSearchBtnClicked()
                    ButtonType.CALL -> onCallBtnClicked()
                }
            }
        })
    }

    private fun onBookmarkBtnClicked() {
        if (mapObject == null) {
            LOGGER.e(
                TAG,
                "Bookmark cannot be managed, mMapObject is null!"
            )
            return
        }
        Statistics.INSTANCE.trackEvent(Statistics.EventName.PP_BOOKMARK)
        logClick(AlohaHelper.PP_BOOKMARK)
        toggleIsBookmark(mapObject!!)
    }

    private fun onShareBtnClicked() {
        if (mapObject == null) {
            LOGGER.e(
                TAG,
                "A map object cannot be shared, it's null!"
            )
            return
        }
        Statistics.INSTANCE.trackEvent(Statistics.EventName.PP_SHARE)
        logClick(AlohaHelper.PP_SHARE)
        ShareOption.AnyShareOption.ANY.shareMapObject(activity, mapObject!!, mSponsored)
    }

    private fun onBackBtnClicked() {
        if (mapObject == null) {
            LOGGER.e(
                TAG,
                "A mwm request cannot be handled, mMapObject is null!"
            )
            activity.finish()
            return
        }
        if (hasRequest()) {
            val request = currentRequest
            if (isPickPointMode) request!!.setPointData(
                mapObject!!.lat,
                mapObject!!.lon,
                mapObject!!.title,
                ""
            )
            request!!.sendResponseAndFinish(activity, true)
        } else activity.finish()
    }

    private fun onRouteFromBtnClicked() {
        val controller = get()
        if (!controller.isPlanning) {
            controller.prepare(mapObject, null)
            close()
        } else if (controller.setStartPoint(mapObject)) {
            close()
        }
    }

    private fun onRouteToBtnClicked() {
        if (get().isPlanning) {
            get().setEndPoint(mapObject)
            close()
        } else {
            activity.startLocationToPoint(mapObject, true)
        }
    }

    private fun onRouteAddBtnClicked() {
        if (mapObject != null) get().addStop(mapObject!!)
    }

    private fun onRouteRemoveBtnClicked() {
        if (mapObject != null) get().removeStop(mapObject!!)
    }

    private fun onCallBtnClicked() {
        callPhone(context, mTvPhone!!.text.toString())
    }

    private fun onBookingSearchBtnClicked() {
        if (mapObject != null && !TextUtils.isEmpty(mapObject!!.bookingSearchUrl)) {
            Statistics.INSTANCE.trackBookingSearchEvent(mapObject!!)
            openUrl(context, mapObject!!.bookingSearchUrl)
        }
    }

    fun setRoutingModeListener(routingModeListener: RoutingModeListener?) {
        mRoutingModeListener = routingModeListener
    }

    private fun onAvoidUnpavedBtnClicked() {
        onAvoidBtnClicked(RoadType.Dirty)
    }

    private fun onAvoidFerryBtnClicked() {
        onAvoidBtnClicked(RoadType.Ferry)
    }

    private fun onAvoidTollBtnClicked() {
        onAvoidBtnClicked(RoadType.Toll)
    }

    private fun onAvoidBtnClicked(roadType: RoadType) {
        if (mRoutingModeListener == null) return
        mRoutingModeListener!!.toggleRouteSettings(roadType)
        Statistics.INSTANCE.trackEvent(
            Statistics.EventName.PP_DRIVING_OPTIONS_ACTION,
            params().add(
                Statistics.EventParam.TYPE,
                roadType.name
            )
        )
    }

    private fun initPlaceDescriptionView() {
        mPlaceDescriptionContainer = findViewById(R.id.poi_description_container)
        mPlaceDescriptionView = findViewById(R.id.poi_description)
        mPlaceDescriptionMoreBtn = findViewById(R.id.more_btn)
        mPlaceDescriptionMoreBtn.setOnClickListener { v: View? -> showDescriptionScreen() }
    }

    private fun showDescriptionScreen() {
        val context = mPlaceDescriptionContainer.context
        val description = mapObject!!.description
        PlaceDescriptionActivity.start(
            context,
            description,
            Statistics.ParamValue.WIKIPEDIA
        )
    }

    private fun initEditMapObjectBtn() {
        val isEditSupported = isEditableMapObject
        val editBookmarkBtn =
            mBookmarkFrame!!.findViewById<View>(R.id.tv__bookmark_edit)
        showIf(isEditSupported, editBookmarkBtn)
        editBookmarkBtn.setOnClickListener(if (isEditSupported) mEditBookmarkClickListener else null)
    }

    val isEditableMapObject: Boolean
        get() {
            val isBookmark = isOfType(MapObject.BOOKMARK, mapObject)
            if (isBookmark) {
                val id = castTo<Bookmark>(mapObject!!).bookmarkId
                return BookmarkManager.INSTANCE.isEditableBookmark(id)
            }
            return true
        }

    private fun initHotelRatingView() {
        mHotelReview = findViewById(R.id.ll__place_hotel_rating)
        val rvHotelReview: RecyclerView = findViewById(R.id.rv__place_hotel_review)
        rvHotelReview.layoutManager = LinearLayoutManager(context)
        rvHotelReview.layoutManager!!.isAutoMeasureEnabled = true
        rvHotelReview.isNestedScrollingEnabled = false
        rvHotelReview.setHasFixedSize(false)
        rvHotelReview.adapter = mReviewAdapter
        mHotelRating = findViewById(R.id.tv__place_hotel_rating)
        mHotelRatingBase = findViewById(R.id.tv__place_hotel_rating_base)
        val hotelMoreReviews =
            findViewById<View>(R.id.tv__place_hotel_reviews_more)
        hotelMoreReviews.setOnClickListener(this)
    }

    private fun initHotelNearbyView() {
        mHotelNearby = findViewById(R.id.ll__place_hotel_nearby)
        val gvHotelNearby = findViewById<GridView>(R.id.gv__place_hotel_nearby)
        gvHotelNearby.adapter = mNearbyAdapter
    }

    private fun initHotelGalleryView() {
        mHotelGallery = findViewById(R.id.ll__place_hotel_gallery)
        mRvHotelGallery = findViewById(R.id.rv__place_hotel_gallery)
        mRvHotelGallery?.setNestedScrollingEnabled(false)
        val layoutManager =
            LinearLayoutManager(
                context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        mRvHotelGallery?.setLayoutManager(layoutManager)
        val decor = createHotelGalleryDecorator(
            context,
            LinearLayoutManager.HORIZONTAL
        )
        mRvHotelGallery?.addItemDecoration(decor)
        mGalleryAdapter.setListener(this)
        mRvHotelGallery?.setAdapter(mGalleryAdapter)
    }

    private fun initHotelFacilitiesView() {
        mHotelFacilities = findViewById(R.id.ll__place_hotel_facilities)
        val rvHotelFacilities: RecyclerView = findViewById(R.id.rv__place_hotel_facilities)
        rvHotelFacilities.layoutManager = GridLayoutManager(context, 2)
        rvHotelFacilities.layoutManager!!.isAutoMeasureEnabled = true
        rvHotelFacilities.isNestedScrollingEnabled = false
        rvHotelFacilities.setHasFixedSize(false)
        mHotelMoreFacilities = findViewById(R.id.tv__place_hotel_facilities_more)
        rvHotelFacilities.adapter = mFacilitiesAdapter
        mHotelMoreFacilities?.setOnClickListener(this)
    }

    private fun initHotelDescriptionView() {
        mHotelDescription = findViewById(R.id.ll__place_hotel_description)
        mTvHotelDescription = findViewById(R.id.tv__place_hotel_details)
        mHotelMoreDescription = findViewById(R.id.tv__place_hotel_more)
        val hotelMoreDescriptionOnWeb =
            findViewById<View>(R.id.tv__place_hotel_more_on_web)
        mTvHotelDescription?.setListener(this)
        mHotelMoreDescription?.setOnClickListener(this)
        hotelMoreDescriptionOnWeb.setOnClickListener(this)
    }

    override fun onPriceReceived(priceInfo: HotelPriceInfo) {
        if (mSponsored == null || !TextUtils.equals(priceInfo.id, mSponsored!!.id)) return
        val price = makePrice(context, priceInfo)
        if (price != null) mSponsoredPrice = price
        if (mapObject == null) {
            LOGGER.e(
                TAG,
                "A sponsored info cannot be updated, mMapObject is null!"
            )
            return
        }
        refreshPreview(mapObject!!, priceInfo)
    }

    override fun onHotelInfoReceived(id: String, info: HotelInfo) {
        if (mSponsored == null || !TextUtils.equals(id, mSponsored!!.id)) return
        updateHotelDetails(info)
        updateHotelFacilities(info)
        updateHotelGallery(info)
        updateHotelNearby(info)
        updateHotelRating(info)
    }

    private fun updateHotelRating(info: HotelInfo) {
        if (info.mReviews == null || info.mReviews.size == 0) {
            hide(mHotelReview)
        } else {
            show(mHotelReview)
            mReviewAdapter.items =
                ArrayList(Arrays.asList(*info.mReviews))
            mHotelRating!!.text = mSponsored!!.rating
            val reviewsCount = info.mReviewsAmount.toInt()
            val text = resources.getQuantityString(
                R.plurals.placepage_summary_rating_description, reviewsCount, reviewsCount
            )
            mHotelRatingBase!!.text = text
            val previewReviewCountView =
                mPreviewRatingInfo!!.findViewById<TextView>(R.id.tv__review_count)
            previewReviewCountView.text = text
        }
    }

    private fun updateHotelNearby(info: HotelInfo) {
        if (info.mNearby == null || info.mNearby.size == 0) {
            hide(mHotelNearby)
        } else {
            show(mHotelNearby)
            mNearbyAdapter.setItems(Arrays.asList(*info.mNearby))
        }
    }

    private fun updateHotelGallery(info: HotelInfo) {
        if (info.mPhotos == null || info.mPhotos.size == 0) {
            hide(mHotelGallery)
        } else {
            show(mHotelGallery)
            mGalleryAdapter.items =
                ArrayList(Arrays.asList(*info.mPhotos))
            mRvHotelGallery!!.scrollToPosition(0)
        }
    }

    private fun updateHotelFacilities(info: HotelInfo) {
        if (info.mFacilities == null || info.mFacilities.size == 0) {
            hide(mHotelFacilities)
        } else {
            show(mHotelFacilities)
            mFacilitiesAdapter.setShowAll(false)
            mFacilitiesAdapter.setItems(Arrays.asList(*info.mFacilities))
            mHotelMoreFacilities!!.visibility =
                if (info.mFacilities.size > FacilitiesAdapter.MAX_COUNT) View.VISIBLE else View.GONE
        }
    }

    private fun updateHotelDetails(info: HotelInfo) {
        mTvHotelDescription!!.maxLines = resources.getInteger(R.integer.pp_hotel_description_lines)
        refreshMetadataOrHide(
            info.mDescription,
            mHotelDescription,
            mTvHotelDescription
        )
        mHotelMoreDescription!!.visibility = View.GONE
    }

    private fun clearHotelViews() {
        mTvHotelDescription!!.text = ""
        mHotelMoreDescription!!.visibility = View.GONE
        mFacilitiesAdapter.setItems(emptyList())
        mHotelMoreFacilities!!.visibility = View.GONE
        mGalleryAdapter.items = ArrayList()
        mNearbyAdapter.setItems(emptyList())
        mReviewAdapter.items = ArrayList()
        mHotelRating!!.text = ""
        mHotelRatingBase!!.text = ""
        mTvSponsoredPrice!!.text = ""
        mGalleryAdapter.items = ArrayList()
    }

    override fun onLineCountCalculated(grater: Boolean) {
        showIf(grater, mHotelMoreDescription)
    }

    override fun onItemClick(v: View?, position: Int) {
        if (mapObject == null || mSponsored == null) {
            LOGGER.e(
                TAG,
                "A photo gallery cannot be started, mMapObject/mSponsored is null!"
            )
            return
        }
        Statistics.INSTANCE.trackHotelEvent(
            PP_HOTEL_GALLERY_OPEN,
            mSponsored!!,
            mapObject!!
        )
        if (position == GalleryAdapter.MAX_COUNT - 1
            && mGalleryAdapter.items.size > GalleryAdapter.MAX_COUNT
        ) {
            GalleryActivity.start(context, mGalleryAdapter.items, mapObject!!.title)
        } else {
            start(context, mGalleryAdapter.items, position)
        }
    }

    override fun onItemClick(item: NearbyObject) { //  TODO go to selected object on map
    }

    private fun onSponsoredClick(book: Boolean, isDetails: Boolean) {
        checkConnection(
            activity,
            R.string.common_check_internet_connection_dialog,
            object : Proc<Boolean> {
                override fun invoke(result: Boolean) {
                    if (!result) return
                    val info = mSponsored ?: return
                    var partnerAppOpenMode =
                        PartnerAppOpenMode.None
                    when (info.type) {
                        Sponsored.TYPE_BOOKING -> {
                            if (mapObject != null) {
                                if (book) {
                                    partnerAppOpenMode =
                                        PartnerAppOpenMode.Direct
                                    logBookingBookClicked()
                                    Statistics.INSTANCE.trackBookHotelEvent(
                                        info,
                                        mapObject!!
                                    )
                                } else if (isDetails) {
                                    logBookingDetailsClicked()
                                    Statistics.INSTANCE.trackHotelEvent(
                                        PP_SPONSORED_DETAILS,
                                        info,
                                        mapObject!!
                                    )
                                } else {
                                    logBookingMoreClicked()
                                    Statistics.INSTANCE.trackHotelEvent(
                                        PP_HOTEL_DESCRIPTION_LAND,
                                        info,
                                        mapObject!!
                                    )
                                }
                            }

                        }
                        Sponsored.TYPE_OPENTABLE -> if (mapObject != null) Statistics.INSTANCE.trackRestaurantEvent(
                            PP_SPONSORED_OPENTABLE, info,
                            mapObject!!
                        )
                        Sponsored.TYPE_PARTNER -> if (mapObject != null && !info.partnerName.isEmpty()) Statistics.INSTANCE.trackSponsoredObjectEvent(
                            PP_SPONSORED_ACTION, info,
                            mapObject!!
                        )
                        Sponsored.TYPE_NONE -> {
                        }
                    }
                    try {
                        if (partnerAppOpenMode !== PartnerAppOpenMode.None) {
                            val links = SponsoredLinks(info.deepLink, info.url)
                            val packageName = Sponsored.getPackageName(info.type)
                            openPartner(
                                context,
                                links,
                                packageName,
                                partnerAppOpenMode
                            )
                        } else {
                            if (book) openUrl(
                                context,
                                info.url
                            ) else openUrl(
                                context,
                                if (isDetails) info.descriptionUrl else info.moreUrl
                            )
                        }
                    } catch (e: ActivityNotFoundException) {
                        LOGGER.e(
                            TAG,
                            "Failed to handle click on sponsored: ",
                            e
                        )
                        logException(e)
                    }
                }

            })
    }

    private fun init(attrs: AttributeSet?, defStyleAttr: Int) {
        LayoutInflater.from(context).inflate(R.layout.place_page, this)
        if (isInEditMode) return
        val attrArray =
            context.obtainStyledAttributes(attrs, R.styleable.PlacePageView, defStyleAttr, 0)
        val animationType = attrArray.getInt(R.styleable.PlacePageView_animationType, 0)
        isDocked = attrArray.getBoolean(R.styleable.PlacePageView_docked, false)
        isFloating = attrArray.getBoolean(R.styleable.PlacePageView_floating, false)
        attrArray.recycle()
    }

    /**
     * @param mapObject new MapObject
     * @param listener  listener
     */
    fun setMapObject(mapObject: MapObject?, listener: SetMapObjectListener?) {
        if (same(this.mapObject, mapObject)) {
            this.mapObject = mapObject
            val policy =
                NetworkPolicy.newInstance(NetworkPolicy.getCurrentNetworkUsageStatus())
            refreshViews(policy)
            listener?.onSetMapObjectComplete(policy, true)
            return
        }
        this.mapObject = mapObject
        mSponsored = if (this.mapObject == null) null else nativeGetCurrent()
        if (isNetworkNeeded) {
            NetworkPolicy.checkNetworkPolicy(
                activity.supportFragmentManager,
                object : NetworkPolicyListener {
                    override fun onResult(policy: NetworkPolicy) {
                        setMapObjectInternal(policy)
                        listener?.onSetMapObjectComplete(policy, false)
                    }
                })
        } else {
            val policy = NetworkPolicy.newInstance(false)
            setMapObjectInternal(policy)
            listener?.onSetMapObjectComplete(policy, false)
        }
    }

    private fun setMapObjectInternal(policy: NetworkPolicy) {
        detachCountry()
        if (mapObject != null) {
            clearHotelViews()
            processSponsored(policy)
            initEditMapObjectBtn()
            mUgcController.clearViewsFor(mapObject!!)
            val country = nativeGetSelectedCountry()
            if (country != null && !get().isNavigating) attachCountry(country)
        }
        refreshViews(policy)
    }

    private fun processSponsored(policy: NetworkPolicy) {
        mCatalogPromoController.updateCatalogPromo(policy, mapObject)
        if (mSponsored == null || mapObject == null) return
        mSponsored!!.updateId(mapObject!!)
        mSponsoredPrice = mSponsored!!.price
        val currencyCode = currencyCode
        if (mSponsored!!.id == null || TextUtils.isEmpty(currencyCode)) return
        if (mSponsored!!.type != Sponsored.TYPE_BOOKING) return
        Sponsored.requestPrice(mSponsored!!.id!!, currencyCode!!, policy)
        Sponsored.requestInfo(mSponsored!!, Locale.getDefault().toString(), policy)
    }

    private val isNetworkNeeded: Boolean
        private get() = mapObject != null && (isSponsored || mapObject!!.banners != null)

    fun refreshViews(policy: NetworkPolicy) {
        if (mapObject == null) {
            LOGGER.e(
                TAG,
                "A place page views cannot be refreshed, mMapObject is null"
            )
            return
        }
        refreshPreview(mapObject!!, null)
        refreshDetails(mapObject!!)
        refreshHotelDetailViews(policy)
        refreshViewsInternal(mapObject!!)
        mUgcController.getUGC(mapObject!!)
        mCatalogPromoController.updateCatalogPromo(policy, mapObject)
    }

    private fun refreshViewsInternal(mapObject: MapObject) {
        val loc = LocationHelper.INSTANCE.savedLocation
        when (mapObject.mapObjectType) {
            MapObject.BOOKMARK -> {
                refreshDistanceToObject(mapObject, loc)
                showBookmarkDetails(mapObject)
                updateBookmarkButton()
                setButtons(mapObject, false, true)
            }
            MapObject.POI, MapObject.SEARCH -> {
                refreshDistanceToObject(mapObject, loc)
                hideBookmarkDetails()
                setButtons(mapObject, false, true)
                setPlaceDescription(mapObject)
            }
            MapObject.API_POINT -> {
                refreshDistanceToObject(mapObject, loc)
                hideBookmarkDetails()
                setButtons(mapObject, true, true)
            }
            MapObject.MY_POSITION -> {
                refreshMyPosition(mapObject, loc)
                hideBookmarkDetails()
                setButtons(mapObject, false, false)
            }
        }
    }

    private fun setPlaceDescription(mapObject: MapObject) {
        if (TextUtils.isEmpty(mapObject.description)) {
            hide(mPlaceDescriptionContainer)
            return
        }
        if (isOfType(MapObject.BOOKMARK, mapObject)) {
            val bmk = mapObject as Bookmark
            if (!TextUtils.isEmpty(bmk.bookmarkDescription)) {
                hide(mPlaceDescriptionContainer)
                return
            }
        }
        show(mPlaceDescriptionContainer)
        mPlaceDescriptionView.text = Html.fromHtml(mapObject.description)
    }

    private fun colorizeSubtitle() {
        val text = mTvSubtitle!!.text.toString()
        if (TextUtils.isEmpty(text)) return
        val start = text.indexOf("★")
        if (start > -1) {
            val sb = SpannableStringBuilder(text)
            sb.setSpan(
                ForegroundColorSpan(resources.getColor(R.color.base_yellow)),
                start, sb.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
            mTvSubtitle!!.text = sb
        }
    }

    private fun refreshPreview(mapObject: MapObject, priceInfo: HotelPriceInfo?) {
        setTextAndHideIfEmpty(mTvTitle, mapObject.title)
        setTextAndHideIfEmpty(mTvSecondaryTitle, mapObject.secondaryTitle)
        val isPopular = mapObject.getPopularity().type === Popularity.Type.POPULAR
        showIf(isPopular, mPopularityView)
        if (mToolbar != null) mToolbar!!.title = mapObject.title
        setTextAndHideIfEmpty(mTvSubtitle, mapObject.subtitle)
        colorizeSubtitle()
        hide(mAvDirection)
        setTextAndHideIfEmpty(mTvAddress, mapObject.address)
        val sponsored = isSponsored
        showIf(sponsored || mapObject.shouldShowUGC(), mPreviewRatingInfo)
        showIf(sponsored, mHotelDiscount)
        showIf(mapObject.hotelType != null, mPreview!!, R.id.search_hotels_btn)
        if (sponsored) refreshSponsoredViews(mapObject, priceInfo)
    }

    private fun refreshSponsoredViews(
        mapObject: MapObject,
        priceInfo: HotelPriceInfo?
    ) {
        val isPriceEmpty = TextUtils.isEmpty(mSponsoredPrice)
        val isRatingEmpty = TextUtils.isEmpty(mSponsored!!.rating)
        val impress =
            Impress.values()[mSponsored!!.impress]
        mRatingView!!.setRating(impress, mSponsored!!.rating)
        showIf(!isRatingEmpty, mRatingView)
        mTvSponsoredPrice!!.text = mSponsoredPrice
        showIf(!isPriceEmpty, mTvSponsoredPrice)
        val isBookingInfoExist = (!isRatingEmpty || !isPriceEmpty) &&
                mSponsored!!.type == Sponsored.TYPE_BOOKING
        showIf(isBookingInfoExist || mapObject.shouldShowUGC(), mPreviewRatingInfo)
        val discount = getHotelDiscount(priceInfo)
        hideIf(TextUtils.isEmpty(discount), mHotelDiscount)
        mHotelDiscount.setRating(Impress.DISCOUNT, discount)
    }

    private fun getHotelDiscount(priceInfo: HotelPriceInfo?): String? {
        val hasPercentsDiscount = priceInfo != null && priceInfo.discount > 0
        if (hasPercentsDiscount) return DISCOUNT_PREFIX + priceInfo!!.discount + DISCOUNT_SUFFIX
        return if (priceInfo != null && priceInfo.hasSmartDeal()) DISCOUNT_SUFFIX else null
    }

    private val isSponsored: Boolean
        get() = mSponsored != null && mSponsored!!.type != Sponsored.TYPE_NONE

    fun getSponsored(): Sponsored? {
        return mSponsored
    }

    private fun refreshDetails(mapObject: MapObject) {
        refreshLatLon(mapObject)
        if (mSponsored == null || mSponsored!!.type != Sponsored.TYPE_BOOKING) {
            val website =
                mapObject.getMetadata(Metadata.MetadataType.FMD_WEBSITE)
            val url =
                mapObject.getMetadata(Metadata.MetadataType.FMD_URL)
            refreshMetadataOrHide(
                if (TextUtils.isEmpty(website)) url else website,
                mWebsite,
                mTvWebsite
            )
        }
        refreshMetadataOrHide(
            mapObject.getMetadata(Metadata.MetadataType.FMD_PHONE_NUMBER),
            mPhone,
            mTvPhone
        )
        refreshMetadataOrHide(
            mapObject.getMetadata(Metadata.MetadataType.FMD_EMAIL),
            mEmail,
            mTvEmail
        )
        refreshMetadataOrHide(
            mapObject.getMetadata(Metadata.MetadataType.FMD_OPERATOR),
            mOperator,
            mTvOperator
        )
        refreshMetadataOrHide(
            nativeGetActiveObjectFormattedCuisine(),
            mCuisine,
            mTvCuisine
        )
        refreshMetadataOrHide(
            mapObject.getMetadata(Metadata.MetadataType.FMD_WIKIPEDIA),
            mWiki,
            null
        )
        refreshMetadataOrHide(
            mapObject.getMetadata(Metadata.MetadataType.FMD_INTERNET),
            mWifi,
            null
        )
        refreshMetadataOrHide(
            mapObject.getMetadata(Metadata.MetadataType.FMD_FLATS),
            mEntrance,
            mTvEntrance
        )
        refreshOpeningHours(mapObject)
        showTaxiOffer(mapObject)
        if (get().isNavigating || get().isPlanning) {
            hide(mEditPlace, mAddOrganisation, mAddPlace, mLocalAd, mEditTopSpace)
        } else {
            showIf(
                nativeShouldShowEditPlace(),
                mEditPlace
            )
            showIf(
                nativeShouldShowAddBusiness(),
                mAddOrganisation
            )
            showIf(nativeShouldShowAddPlace(), mAddPlace)
            showIf(
                isVisible(mEditPlace!!)
                        || isVisible(mAddOrganisation!!)
                        || isVisible(mAddPlace!!), mEditTopSpace
            )
            refreshLocalAdInfo(mapObject)
        }
        setPlaceDescription(mapObject)
    }

    private fun refreshHotelDetailViews(policy: NetworkPolicy) {
        if (mSponsored == null) {
            hideHotelDetailViews()
            return
        }
        val isConnected = isConnected
        if (isConnected && policy.canUseNetwork()) showHotelDetailViews() else hideHotelDetailViews()
        if (mSponsored!!.type == Sponsored.TYPE_BOOKING) {
            hide(mWebsite)
            show(mHotelMore)
        }
        if (mSponsored!!.type != Sponsored.TYPE_BOOKING) hideHotelDetailViews()
    }

    private fun showTaxiOffer(mapObject: MapObject) {
        val taxiTypes = mapObject.getReachableByTaxiTypes()
        val showTaxiOffer =
            (taxiTypes != null && !taxiTypes.isEmpty() && LocationHelper.INSTANCE.myPosition != null &&
                    isConnected
                    && mapObject.roadWarningMarkType === RoadWarningMarkType.UNKNOWN)
        showIf(showTaxiOffer, mTaxi, mTaxiShadow, mTaxiDivider)
        if (!showTaxiOffer) return
        // At this moment we display only a one taxi provider at the same time.
        val type = taxiTypes!![0]
        val logo =
            mTaxi!!.findViewById<ImageView>(R.id.iv__place_page_taxi)
        logo.setImageResource(type.icon)
        val title = mTaxi!!.findViewById<TextView>(R.id.tv__place_page_taxi)
        title.setText(type.title)
    }

    private fun hideHotelDetailViews() {
        hide(
            mHotelDescription, mHotelFacilities, mHotelGallery, mHotelNearby,
            mHotelReview, mHotelMore
        )
    }

    private fun showHotelDetailViews() {
        show(
            mHotelDescription, mHotelFacilities, mHotelGallery, mHotelNearby,
            mHotelReview, mHotelMore
        )
    }

    private fun refreshLocalAdInfo(mapObject: MapObject) {
        val localAdInfo = mapObject.localAdInfo
        val isLocalAdAvailable = localAdInfo != null && localAdInfo.isAvailable
        if (isLocalAdAvailable && !TextUtils.isEmpty(localAdInfo!!.url) && !localAdInfo.isHidden) {
            mTvLocalAd!!.setText(if (localAdInfo.isCustomer) R.string.view_campaign_button else R.string.create_campaign_button)
            show(mLocalAd)
        } else {
            hide(mLocalAd)
        }
    }

    private fun refreshOpeningHours(mapObject: MapObject) {
        val timetables =
            nativeTimetablesFromString(mapObject.getMetadata(Metadata.MetadataType.FMD_OPEN_HOURS))
        if (timetables == null || timetables.size == 0) {
            hide(mOpeningHours)
            return
        }
        show(mOpeningHours)
        val resources = resources
        if (timetables[0].isFullWeek) {
            refreshTodayOpeningHours(
                if (timetables[0].isFullday) resources.getString(R.string.twentyfour_seven) else resources.getString(
                    R.string.daily
                ) + " " + timetables[0].workingTimespan,
                getColor(
                    context,
                    android.R.attr.textColorPrimary
                )
            )
            clearTextAndHide(mFullOpeningHours!!)
            return
        }
        var containsCurrentWeekday = false
        val currentDay = Calendar.getInstance()[Calendar.DAY_OF_WEEK]
        for (tt in timetables) {
            if (tt.containsWeekday(currentDay)) {
                containsCurrentWeekday = true
                val workingTime: String?
                workingTime = if (tt.isFullday) {
                    val allDay = resources.getString(R.string.editor_time_allday)
                    unCapitalize(allDay)
                } else {
                    tt.workingTimespan.toString()
                }
                refreshTodayOpeningHours(
                    resources.getString(R.string.today) + " " + workingTime,
                    getColor(
                        context,
                        android.R.attr.textColorPrimary
                    )
                )
                break
            }
        }
        setTextAndShow(mFullOpeningHours, formatTimetables(timetables))
        if (!containsCurrentWeekday) refreshTodayOpeningHours(
            resources.getString(R.string.day_off_today),
            resources.getColor(R.color.base_red)
        )
    }

    private fun refreshTodayOpeningHours(text: String, @ColorInt color: Int) {
        setTextAndShow(mTodayOpeningHours, text)
        mTodayOpeningHours!!.setTextColor(color)
    }

    private fun updateBookmarkButton() {
        if (mBookmarkButtonIcon == null || mBookmarkButtonFrame == null) return
        if (mBookmarkSet) mBookmarkButtonIcon!!.setImageResource(R.drawable.ic_bookmarks_on) else mBookmarkButtonIcon!!.setImageDrawable(
            tint(
                context,
                R.drawable.ic_bookmarks_off,
                R.attr.iconTint
            )
        )
        val isEditable = isEditableMapObject
        mBookmarkButtonFrame!!.isEnabled = isEditable
        if (isEditable) return
        val resId = PlacePageButtons.Item.BOOKMARK.icon.disabledStateResId
        val drawable =
            tint(context, resId, R.attr.iconTintDisabled)
        mBookmarkButtonIcon!!.setImageDrawable(drawable)
    }

    private fun hideBookmarkDetails() {
        mBookmarkSet = false
        hide(mBookmarkFrame)
        updateBookmarkButton()
    }

    private fun showBookmarkDetails(mapObject: MapObject) {
        mBookmarkSet = true
        show(mBookmarkFrame)
        val notes = (mapObject as Bookmark).bookmarkDescription
        if (TextUtils.isEmpty(notes)) {
            hide(mTvBookmarkNote, mWvBookmarkNote)
            return
        }
        if (nativeIsHtml(notes)) {
            mWvBookmarkNote!!.loadData(notes, "text/html; charset=utf-8", null)
            show(mWvBookmarkNote)
            hide(mTvBookmarkNote)
        } else {
            mTvBookmarkNote!!.text = notes
            Linkify.addLinks(mTvBookmarkNote!!, Linkify.ALL)
            show(mTvBookmarkNote)
            hide(mWvBookmarkNote)
        }
    }

    private fun setButtons(
        mapObject: MapObject,
        showBackButton: Boolean,
        showRoutingButton: Boolean
    ) {
        val buttons: MutableList<PlacePageButton> =
            ArrayList()
        if (mapObject.roadWarningMarkType !== RoadWarningMarkType.UNKNOWN) {
            val markType = mapObject.roadWarningMarkType
            val roadType =
                toPlacePageButton(markType)
            buttons.add(roadType)
            mButtons!!.setItems(buttons)
            return
        }
        if (get().isRoutePoint(mapObject)) {
            buttons.add(PlacePageButtons.Item.ROUTE_REMOVE)
            mButtons!!.setItems(buttons)
            return
        }
        if (showBackButton || isPickPointMode) buttons.add(PlacePageButtons.Item.BACK)
        if (mSponsored != null) {
            when (mSponsored!!.type) {
                Sponsored.TYPE_BOOKING -> buttons.add(PlacePageButtons.Item.BOOKING)
                Sponsored.TYPE_OPENTABLE -> buttons.add(PlacePageButtons.Item.OPENTABLE)
                Sponsored.TYPE_PARTNER -> {
                    val partnerIndex = mSponsored!!.partnerIndex
                    if (partnerIndex >= 0 && !mSponsored!!.url.isEmpty()) buttons.add(
                        PlacePageButtons.getPartnerItem(
                            partnerIndex
                        )
                    )
                }
                Sponsored.TYPE_NONE -> {
                }
            }
        }
        if (!TextUtils.isEmpty(mapObject.bookingSearchUrl)) buttons.add(PlacePageButtons.Item.BOOKING_SEARCH)
        if (mapObject.hasPhoneNumber()) buttons.add(PlacePageButtons.Item.CALL)
        buttons.add(PlacePageButtons.Item.BOOKMARK)
        if (get().isPlanning || showRoutingButton) {
            buttons.add(PlacePageButtons.Item.ROUTE_FROM)
            buttons.add(PlacePageButtons.Item.ROUTE_TO)
            if (get().isStopPointAllowed) buttons.add(PlacePageButtons.Item.ROUTE_ADD)
        }
        buttons.add(PlacePageButtons.Item.SHARE)
        mButtons!!.setItems(buttons)
    }

    fun refreshLocation(l: Location?) {
        if (mapObject == null) {
            LOGGER.e(
                TAG,
                "A location cannot be refreshed, mMapObject is null!"
            )
            return
        }
        if (isOfType(MapObject.MY_POSITION, mapObject)) refreshMyPosition(
            mapObject!!,
            l
        ) else refreshDistanceToObject(mapObject!!, l)
    }

    private fun refreshMyPosition(
        mapObject: MapObject,
        l: Location?
    ) {
        hide(mTvDistance)
        if (l == null) return
        val builder = StringBuilder()
        if (l.hasAltitude()) {
            val altitude = l.altitude
            builder.append(if (altitude >= 0) "▲" else "▼")
            builder.append(nativeFormatAltitude(altitude))
        }
        if (l.hasSpeed()) builder.append("   ")
            .append(nativeFormatSpeed(l.speed.toDouble()))
        setTextAndHideIfEmpty(mTvSubtitle, builder.toString())
        mapObject.setLat(l.latitude)
        mapObject.setLon(l.longitude)
        refreshLatLon(mapObject)
    }

    private fun refreshDistanceToObject(
        mapObject: MapObject,
        l: Location?
    ) {
        showIf(l != null, mTvDistance)
        if (l == null) return
        mTvDistance!!.visibility = View.VISIBLE
        val lat = mapObject.lat
        val lon = mapObject.lon
        val distanceAndAzimuth =
            nativeGetDistanceAndAzimuthFromLatLon(
                lat,
                lon,
                l.latitude,
                l.longitude,
                0.0
            )
        mTvDistance!!.text = distanceAndAzimuth!!.distance
    }

    private fun refreshLatLon(mapObject: MapObject) {
        val lat = mapObject.lat
        val lon = mapObject.lon
        val latLon =
            nativeFormatLatLonToArr(lat, lon, mIsLatLonDms)
        if (latLon!!.size == 2) mTvLatlon!!.text = String.format(
            Locale.US,
            "%1\$s, %2\$s",
            latLon[0],
            latLon[1]
        )
    }

    fun refreshAzimuth(northAzimuth: Double) {
        if (mapObject == null || isOfType(MapObject.MY_POSITION, mapObject)) return
        val location = LocationHelper.INSTANCE.savedLocation ?: return
        val azimuth = nativeGetDistanceAndAzimuthFromLatLon(
            mapObject!!.lat,
            mapObject!!.lon,
            location.latitude,
            location.longitude,
            northAzimuth
        )!!.azimuth
        if (azimuth >= 0) {
            show(mAvDirection)
            mAvDirection!!.setAzimuth(azimuth)
        }
    }

    private fun addOrganisation() {
        Statistics.INSTANCE.trackEvent(
            Statistics.EventName.EDITOR_ADD_CLICK,
            params()
                .add(Statistics.EventParam.FROM, "placepage")
        )
        activity.showPositionChooser(true, false)
    }

    private fun addPlace() { // TODO add statistics
        activity.showPositionChooser(false, true)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.ll__place_editor -> {
                if (mapObject != null) {
                    activity.showEditor()
                } else {
                    LOGGER.e(
                        TAG,
                        "Cannot start editor, map object is null!"
                    )
                }
            }
            R.id.ll__add_organisation -> addOrganisation()
            R.id.ll__place_add -> addPlace()
            R.id.ll__local_ad -> if (mapObject != null) {
                val localAdInfo = mapObject!!.localAdInfo
                    ?: throw AssertionError("A local ad must be non-null if button is shown!")
                if (!TextUtils.isEmpty(localAdInfo.url)) {
                    Statistics.INSTANCE.trackPPOwnershipButtonClick(
                        mapObject!!
                    )
                    openUrl(context, localAdInfo.url)
                }
            }
            R.id.ll__more -> onSponsoredClick(false /* book */, true /* isMoreDetails */)
            R.id.tv__place_hotel_more_on_web -> onSponsoredClick(
                false /* book */,
                false /* isMoreDetails */
            )
            R.id.ll__place_latlon -> {
                mIsLatLonDms = !mIsLatLonDms
                prefs().edit()
                    .putBoolean(PREF_USE_DMS, mIsLatLonDms).apply()
                if (mapObject != null) {
                    refreshLatLon(mapObject!!)
                } else {
                    LOGGER.e(
                        TAG,
                        "A LatLon cannot be refreshed, mMapObject is null"
                    )
                }

            }
            R.id.ll__place_phone -> {
                callPhone(context, mTvPhone!!.text.toString())
                if (mapObject != null) logLocalAdsEvent(
                    Framework.LocalAdsEventType.LOCAL_ADS_EVENT_CLICKED_PHONE,
                    mapObject!!
                )
            }
            R.id.ll__place_website -> {
                openUrl(context, mTvWebsite!!.text.toString())
                if (mapObject != null) logLocalAdsEvent(
                    Framework.LocalAdsEventType.LOCAL_ADS_EVENT_CLICKED_WEBSITE,
                    mapObject!!
                )
            }
            R.id.ll__place_wiki -> {
                // TODO: Refactor and use separate getters for Wiki and all other PP meta info too.
                if (mapObject != null) {
                    openUrl(
                        context,
                        mapObject!!.getMetadata(Metadata.MetadataType.FMD_WIKIPEDIA)
                    )
                } else {
                    LOGGER.e(
                        TAG,
                        "Cannot follow url, mMapObject is null!"
                    )
                }

            }
            R.id.direction_frame -> {
                Statistics.INSTANCE.trackEvent(Statistics.EventName.PP_DIRECTION_ARROW)
                logClick(AlohaHelper.PP_DIRECTION_ARROW)
                showBigDirection()
            }
            R.id.ll__place_email -> sendTo(
                context,
                mTvEmail!!.text.toString()
            )
            R.id.tv__place_hotel_more -> {
                hide(mHotelMoreDescription)
                mTvHotelDescription!!.maxLines = Int.MAX_VALUE
            }
            R.id.tv__place_hotel_facilities_more -> {
                if (mSponsored != null && mapObject != null) Statistics.INSTANCE.trackHotelEvent(
                    PP_HOTEL_FACILITIES,
                    mSponsored!!,
                    mapObject!!
                )
                hide(mHotelMoreFacilities)
                mFacilitiesAdapter.setShowAll(true)
            }
            R.id.tv__place_hotel_reviews_more -> if (isSponsored) { //null checking is done in 'isSponsored' method
                openUrl(context, mSponsored!!.reviewUrl)
                logBookingReviewsClicked()
                if (mapObject != null) Statistics.INSTANCE.trackHotelEvent(
                    PP_HOTEL_REVIEWS_LAND,
                    mSponsored!!,
                    mapObject!!
                )
            }
            R.id.tv__place_page_order_taxi -> {
                get().prepare(
                    LocationHelper.INSTANCE.myPosition, mapObject,
                    Framework.ROUTER_TYPE_TAXI
                )
                close()
                if (mapObject != null) {
                    val types =
                        mapObject!!.getReachableByTaxiTypes()
                    if (types != null && !types.isEmpty()) {
                        val providerName = types[0].providerName
                        Statistics.INSTANCE.trackTaxiEvent(
                            Statistics.EventName.ROUTING_TAXI_CLICK_IN_PP,
                            providerName
                        )
                    }
                }
            }
            R.id.search_hotels_btn -> {
                if (mapObject != null) {
                    @RatingDef val filterRating =
                        if (mSponsored != null) getFilterRating(mSponsored!!.rating) else FilterUtils.RATING_ANY
                    val filter = createHotelFilter(
                        filterRating,
                        mapObject!!.priceRate,
                        mapObject!!.hotelType
                    )
                    activity.onSearchSimilarHotels(filter)
                    val provider =
                        if (mSponsored != null && mSponsored!!.type == Sponsored.TYPE_BOOKING) Statistics.ParamValue.BOOKING_COM else Statistics.ParamValue.OSM
                    Statistics.INSTANCE.trackEvent(
                        Statistics.EventName.PP_HOTEL_SEARCH_SIMILAR,
                        params().add(
                            Statistics.EventParam.PROVIDER,
                            provider
                        )
                    )
                }

            }
        }
    }

    private fun toggleIsBookmark(mapObject: MapObject) {
        if (isOfType(
                MapObject.BOOKMARK,
                mapObject
            )
        ) setMapObject(nativeDeleteBookmarkFromMapObject(), null) else setMapObject(
            BookmarkManager.INSTANCE.addNewBookmark(mapObject.lat, mapObject.lon),
            null
        )
    }

    private fun showBigDirection() {
        val fragment = Fragment.instantiate(
            activity, DirectionFragment::class.java
                .name, null
        ) as DirectionFragment
        fragment.setMapObject(mapObject)
        fragment.show(activity.supportFragmentManager, null)
    }

    override fun onLongClick(v: View): Boolean {
        val tag = v.tag
        val tagStr = tag?.toString() ?: ""
        logLongClick(tagStr)
        val popup = PopupMenu(context, v)
        val menu = popup.menu
        val items: MutableList<String> =
            ArrayList()
        when (v.id) {
            R.id.ll__place_latlon -> {
                if (mapObject != null) {
                    val lat = mapObject!!.lat
                    val lon = mapObject!!.lon
                    items.add(nativeFormatLatLon(lat, lon, false))
                    items.add(nativeFormatLatLon(lat, lon, true))

                } else {
                    LOGGER.e(
                        TAG,
                        "A long click tap on LatLon cannot be handled, mMapObject is null!"
                    )
                }

            }
            R.id.ll__place_website -> items.add(mTvWebsite!!.text.toString())
            R.id.ll__place_email -> items.add(mTvEmail!!.text.toString())
            R.id.ll__place_phone -> items.add(mTvPhone!!.text.toString())
            R.id.ll__place_schedule -> {
                val text =
                    if (isVisible(mFullOpeningHours!!)) mFullOpeningHours!!.text.toString() else mTodayOpeningHours!!.text.toString()
                items.add(text)
            }
            R.id.ll__place_operator -> items.add(mTvOperator!!.text.toString())
            R.id.ll__place_wiki -> {
                if (mapObject != null) {
                    items.add(mapObject!!.getMetadata(Metadata.MetadataType.FMD_WIKIPEDIA))
                } else {
                    LOGGER.e(
                        TAG,
                        "A long click tap on wiki cannot be handled, mMapObject is null!"
                    )
                }
            }
        }
        val copyText = resources.getString(android.R.string.copy)
        for (i in items.indices) menu.add(
            Menu.NONE,
            i,
            i,
            String.format("%s %s", copyText, items[i])
        )
        popup.setOnMenuItemClickListener { item ->
            val id = item.itemId
            val ctx = context
            copyTextToClipboard(ctx, items[id])
            toastShortcut(
                ctx,
                ctx.getString(R.string.copied_to_clipboard, items[id])
            )
            Statistics.INSTANCE.trackEvent(Statistics.EventName.PP_METADATA_COPY + ":" + tagStr)
            logClick(AlohaHelper.PP_METADATA_COPY + ":" + tagStr)
            true
        }
        popup.show()
        return true
    }

    private fun close() {
        if (mClosable != null) mClosable!!.closePlacePage()
    }

    fun reset() {
        resetScroll()
        detachCountry()
    }

    fun resetScroll() {
        scrollTo(0, 0)
    }

    private fun updateDownloader(country: CountryItem?) {
        if (isInvalidDownloaderStatus(country!!.status)) {
            if (mStorageCallbackSlot != 0) runLater(
                mDownloaderDeferredDetachProc
            )
            return
        }
        mDownloaderIcon!!.update(country)
        val sb =
            StringBuilder(getFileSizeString(country.totalSize))
        if (country.isExpandable) sb.append(
            String.format(
                Locale.US,
                "  •  %s: %d",
                context.getString(R.string.downloader_status_maps),
                country.totalChildCount
            )
        )
        mDownloaderInfo!!.text = sb.toString()
    }

    private fun updateDownloader() {
        if (mCurrentCountry == null) return
        mCurrentCountry!!.update()
        updateDownloader(mCurrentCountry)
    }

    private fun attachCountry(country: String) {
        val map = fill(country)
        if (isInvalidDownloaderStatus(map.status)) return
        mCurrentCountry = map
        if (mStorageCallbackSlot == 0) mStorageCallbackSlot =
            nativeSubscribe(mStorageCallback)
        mDownloaderIcon!!.setOnIconClickListener(mDownloadClickListener)
            .setOnCancelClickListener(mCancelDownloadListener)
        mDownloaderIcon!!.show(true)
        show(mDownloaderInfo)
        updateDownloader(mCurrentCountry)
    }

    private fun detachCountry() {
        if (mStorageCallbackSlot == 0) return
        nativeUnsubscribe(mStorageCallbackSlot)
        mStorageCallbackSlot = 0
        mCurrentCountry = null
        mDownloaderIcon!!.setOnIconClickListener(null)
            .setOnCancelClickListener(null)
        mDownloaderIcon!!.show(false)
        hide(mDownloaderInfo)
    }

    val activity: MwmActivity
        get() = context as MwmActivity

    override fun onBookmarkSaved(
        bookmarkId: Long,
        movedFromCategory: Boolean
    ) {
        val updatedBookmark =
            BookmarkManager.INSTANCE.updateBookmarkPlacePage(bookmarkId)
                ?: return
        setMapObject(updatedBookmark, null)
        val policy =
            NetworkPolicy.newInstance(NetworkPolicy.getCurrentNetworkUsageStatus())
        refreshViews(policy)
    }

    val previewHeight: Int
        get() = mPreview!!.height

    private inner class EditBookmarkClickListener :
        OnClickListener {
        override fun onClick(v: View) {
            if (mapObject == null) {
                LOGGER.e(
                    TAG,
                    "A bookmark cannot be edited, mMapObject is null!"
                )
                return
            }
            val bookmark = mapObject as Bookmark
            editBookmark(
                bookmark.categoryId,
                bookmark.bookmarkId,
                activity,
                activity.supportFragmentManager,
                this@PlacePageView
            )
        }
    }

    companion object {
        private val LOGGER =
            INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = PlacePageView::class.java.simpleName
        private const val PREF_USE_DMS = "use_dms"
        private const val DISCOUNT_PREFIX = "-"
        private const val DISCOUNT_SUFFIX = "%"
        private fun makePrice(
            context: Context,
            priceInfo: HotelPriceInfo
        ): String? {
            if (TextUtils.isEmpty(priceInfo.price) || TextUtils.isEmpty(priceInfo.currency)) return null
            val text =
                formatCurrencyString(priceInfo.price, priceInfo.currency)
            return context.getString(R.string.place_page_starting_from, text)
        }

        private fun toPlacePageButton(type: RoadWarningMarkType): PlacePageButtons.Item {
            return when (type) {
                RoadWarningMarkType.DIRTY -> PlacePageButtons.Item.ROUTE_AVOID_UNPAVED
                RoadWarningMarkType.FERRY -> PlacePageButtons.Item.ROUTE_AVOID_FERRY
                RoadWarningMarkType.TOLL -> PlacePageButtons.Item.ROUTE_AVOID_TOLL
                else -> throw AssertionError("Unsupported road warning type: $type")
            }
        }

        private fun refreshMetadataOrHide(
            metadata: String?,
            metaLayout: View?,
            metaTv: TextView?
        ) {
            if (!TextUtils.isEmpty(metadata)) {
                metaLayout!!.visibility = View.VISIBLE
                if (metaTv != null) metaTv.text = metadata
            } else metaLayout!!.visibility = View.GONE
        }

        private fun isInvalidDownloaderStatus(status: Int): Boolean {
            return status != CountryItem.STATUS_DOWNLOADABLE && status != CountryItem.STATUS_ENQUEUED && status != CountryItem.STATUS_FAILED && status != CountryItem.STATUS_PARTLY && status != CountryItem.STATUS_PROGRESS && status != CountryItem.STATUS_APPLYING
        }

        fun toEntities(gallery: PromoCityGallery): List<PromoEntity> {
            val items: MutableList<PromoEntity> =
                ArrayList()
            for (each in gallery.items) {
                val subtitle =
                    if (TextUtils.isEmpty(each.tourCategory)) each.author.name else each.tourCategory
                val item = PromoEntity(
                    Constants.TYPE_PRODUCT,
                    each.name,
                    subtitle,
                    each.url,
                    each.luxCategory,
                    each.imageUrl
                )
                items.add(item)
            }
            return items
        }
    }

    init {
        mIsLatLonDms =
            prefs(context!!).getBoolean(PREF_USE_DMS, false)
        mGalleryAdapter = GalleryAdapter(context)
        init(attrs, defStyleAttr)
    }
}