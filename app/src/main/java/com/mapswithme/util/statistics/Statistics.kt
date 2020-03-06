package com.mapswithme.util.statistics

import android.app.Activity
import android.content.Context
import android.location.Location
import android.net.ConnectivityManager
import android.os.Build
import android.text.TextUtils
import android.util.Pair
import androidx.annotation.IntDef
import com.android.billingclient.api.BillingClient
import com.facebook.ads.AdError
import com.facebook.appevents.AppEventsLogger
import com.mapswithme.maps.BuildConfig
import com.mapswithme.maps.Framework
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.PrivateVariables
import com.mapswithme.maps.ads.MwmNativeAd
import com.mapswithme.maps.ads.NativeAdError
import com.mapswithme.maps.analytics.ExternalLibrariesMediator
import com.mapswithme.maps.api.ParsedMwmRequest
import com.mapswithme.maps.bookmarks.data.BookmarkCategory
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.bookmarks.data.MapObject
import com.mapswithme.maps.downloader.MapManager
import com.mapswithme.maps.editor.Editor
import com.mapswithme.maps.editor.OsmOAuth
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.purchase.ValidationStatus
import com.mapswithme.maps.routing.RoutePointInfo
import com.mapswithme.maps.routing.RoutingOptions
import com.mapswithme.maps.settings.RoadType
import com.mapswithme.maps.taxi.TaxiInfoError
import com.mapswithme.maps.taxi.TaxiManager
import com.mapswithme.maps.widget.menu.MainMenu
import com.mapswithme.maps.widget.placepage.Sponsored
import com.mapswithme.util.BatteryState.CHARGING_STATUS_PLUGGED
import com.mapswithme.util.BatteryState.CHARGING_STATUS_UNKNOWN
import com.mapswithme.util.BatteryState.CHARGING_STATUS_UNPLUGGED
import com.mapswithme.util.BatteryState.state
import com.mapswithme.util.Config.setStatisticsEnabled
import com.mapswithme.util.ConnectionState.isConnected
import com.mapswithme.util.ConnectionState.isConnectionFast
import com.mapswithme.util.ConnectionState.isInRoaming
import com.mapswithme.util.ConnectionState.isMobileConnected
import com.mapswithme.util.ConnectionState.isWifiConnected
import com.mapswithme.util.Counters.getInstallFlavor
import com.mapswithme.util.PowerManagment
import com.mapswithme.util.PowerManagment.SchemeType
import com.mapswithme.util.SharedPropertiesUtils
import com.mapswithme.util.statistics.Statistics.EventName.APPLICATION_COLD_STARTUP_INFO
import com.mapswithme.util.statistics.Statistics.EventName.BM_GUIDES_DOWNLOADDIALOGUE_CLICK
import com.mapswithme.util.statistics.Statistics.EventName.BM_RESTORE_PROPOSAL_CLICK
import com.mapswithme.util.statistics.Statistics.EventName.BM_RESTORE_PROPOSAL_ERROR
import com.mapswithme.util.statistics.Statistics.EventName.BM_RESTORE_PROPOSAL_SUCCESS
import com.mapswithme.util.statistics.Statistics.EventName.BM_SYNC_ERROR
import com.mapswithme.util.statistics.Statistics.EventName.BM_SYNC_PROPOSAL_APPROVED
import com.mapswithme.util.statistics.Statistics.EventName.BM_SYNC_PROPOSAL_ERROR
import com.mapswithme.util.statistics.Statistics.EventName.BM_SYNC_PROPOSAL_SHOWN
import com.mapswithme.util.statistics.Statistics.EventName.BM_SYNC_PROPOSAL_TOGGLE
import com.mapswithme.util.statistics.Statistics.EventName.BM_SYNC_SUCCESS
import com.mapswithme.util.statistics.Statistics.EventName.DOWNLOADER_DIALOG_ERROR
import com.mapswithme.util.statistics.Statistics.EventName.GUIDES_BOOKMARK_SELECT
import com.mapswithme.util.statistics.Statistics.EventName.GUIDES_OPEN
import com.mapswithme.util.statistics.Statistics.EventName.GUIDES_SHOWN
import com.mapswithme.util.statistics.Statistics.EventName.GUIDES_TRACK_SELECT
import com.mapswithme.util.statistics.Statistics.EventName.INAPP_PURCHASE_PREVIEW_SELECT
import com.mapswithme.util.statistics.Statistics.EventName.INAPP_PURCHASE_PREVIEW_SHOW
import com.mapswithme.util.statistics.Statistics.EventName.INAPP_PURCHASE_PRODUCT_DELIVERED
import com.mapswithme.util.statistics.Statistics.EventName.INAPP_PURCHASE_STORE_ERROR
import com.mapswithme.util.statistics.Statistics.EventName.INAPP_PURCHASE_VALIDATION_ERROR
import com.mapswithme.util.statistics.Statistics.EventName.PP_BANNER_BLANK
import com.mapswithme.util.statistics.Statistics.EventName.PP_BANNER_CLOSE
import com.mapswithme.util.statistics.Statistics.EventName.PP_BANNER_ERROR
import com.mapswithme.util.statistics.Statistics.EventName.PP_BANNER_SHOW
import com.mapswithme.util.statistics.Statistics.EventName.PP_OWNERSHIP_BUTTON_CLICK
import com.mapswithme.util.statistics.Statistics.EventName.PP_SPONSORED_BOOK
import com.mapswithme.util.statistics.Statistics.EventName.PP_SPONSORED_ERROR
import com.mapswithme.util.statistics.Statistics.EventName.PP_SPONSORED_OPEN
import com.mapswithme.util.statistics.Statistics.EventName.PP_SPONSORED_SHOWN
import com.mapswithme.util.statistics.Statistics.EventName.PP_SPONSOR_ITEM_SELECTED
import com.mapswithme.util.statistics.Statistics.EventName.ROUTING_PLAN_TOOLTIP_CLICK
import com.mapswithme.util.statistics.Statistics.EventName.ROUTING_ROUTE_FINISH
import com.mapswithme.util.statistics.Statistics.EventName.ROUTING_ROUTE_START
import com.mapswithme.util.statistics.Statistics.EventName.SEARCH_FILTER_CLICK
import com.mapswithme.util.statistics.Statistics.EventName.TIPS_TRICKS_CLOSE
import com.mapswithme.util.statistics.Statistics.EventName.TOOLBAR_CLICK
import com.mapswithme.util.statistics.Statistics.EventName.TOOLBAR_MENU_CLICK
import com.mapswithme.util.statistics.Statistics.EventName.UGC_AUTH_ERROR
import com.mapswithme.util.statistics.Statistics.EventName.UGC_AUTH_EXTERNAL_REQUEST_SUCCESS
import com.mapswithme.util.statistics.Statistics.EventName.UGC_AUTH_SHOWN
import com.mapswithme.util.statistics.Statistics.EventName.UGC_REVIEW_START
import com.mapswithme.util.statistics.Statistics.EventParam.ACTION
import com.mapswithme.util.statistics.Statistics.EventParam.BANNER
import com.mapswithme.util.statistics.Statistics.EventParam.BATTERY
import com.mapswithme.util.statistics.Statistics.EventParam.BUTTON
import com.mapswithme.util.statistics.Statistics.EventParam.CATEGORY
import com.mapswithme.util.statistics.Statistics.EventParam.CHARGING
import com.mapswithme.util.statistics.Statistics.EventParam.COUNT_LOWERCASE
import com.mapswithme.util.statistics.Statistics.EventParam.DESTINATION
import com.mapswithme.util.statistics.Statistics.EventParam.ERROR
import com.mapswithme.util.statistics.Statistics.EventParam.ERROR_CODE
import com.mapswithme.util.statistics.Statistics.EventParam.ERROR_MESSAGE
import com.mapswithme.util.statistics.Statistics.EventParam.FEATURE_ID
import com.mapswithme.util.statistics.Statistics.EventParam.FIRST_LAUNCH
import com.mapswithme.util.statistics.Statistics.EventParam.FROM
import com.mapswithme.util.statistics.Statistics.EventParam.HAS_AUTH
import com.mapswithme.util.statistics.Statistics.EventParam.HOTEL
import com.mapswithme.util.statistics.Statistics.EventParam.HOTEL_LAT
import com.mapswithme.util.statistics.Statistics.EventParam.HOTEL_LON
import com.mapswithme.util.statistics.Statistics.EventParam.INTERRUPTED
import com.mapswithme.util.statistics.Statistics.EventParam.ITEM
import com.mapswithme.util.statistics.Statistics.EventParam.MAP_DATA_SIZE
import com.mapswithme.util.statistics.Statistics.EventParam.METHOD
import com.mapswithme.util.statistics.Statistics.EventParam.MODE
import com.mapswithme.util.statistics.Statistics.EventParam.MWM_NAME
import com.mapswithme.util.statistics.Statistics.EventParam.MWM_VERSION
import com.mapswithme.util.statistics.Statistics.EventParam.NETWORK
import com.mapswithme.util.statistics.Statistics.EventParam.OBJECT_LAT
import com.mapswithme.util.statistics.Statistics.EventParam.OBJECT_LON
import com.mapswithme.util.statistics.Statistics.EventParam.OPTION
import com.mapswithme.util.statistics.Statistics.EventParam.PLACEMENT
import com.mapswithme.util.statistics.Statistics.EventParam.PRODUCT
import com.mapswithme.util.statistics.Statistics.EventParam.PROVIDER
import com.mapswithme.util.statistics.Statistics.EventParam.PURCHASE
import com.mapswithme.util.statistics.Statistics.EventParam.RESTAURANT
import com.mapswithme.util.statistics.Statistics.EventParam.RESTAURANT_LAT
import com.mapswithme.util.statistics.Statistics.EventParam.RESTAURANT_LON
import com.mapswithme.util.statistics.Statistics.EventParam.SERVER_ID
import com.mapswithme.util.statistics.Statistics.EventParam.SERVER_IDS
import com.mapswithme.util.statistics.Statistics.EventParam.STATE
import com.mapswithme.util.statistics.Statistics.EventParam.TYPE
import com.mapswithme.util.statistics.Statistics.EventParam.VALUE
import com.mapswithme.util.statistics.Statistics.EventParam.VENDOR
import com.mapswithme.util.statistics.Statistics.ParamValue.BACKUP
import com.mapswithme.util.statistics.Statistics.ParamValue.BICYCLE
import com.mapswithme.util.statistics.Statistics.ParamValue.BOOKING_COM
import com.mapswithme.util.statistics.Statistics.ParamValue.DISK_NO_SPACE
import com.mapswithme.util.statistics.Statistics.ParamValue.FACEBOOK
import com.mapswithme.util.statistics.Statistics.ParamValue.FALSE
import com.mapswithme.util.statistics.Statistics.ParamValue.GOOGLE
import com.mapswithme.util.statistics.Statistics.ParamValue.HOLIDAY
import com.mapswithme.util.statistics.Statistics.ParamValue.MAPSME
import com.mapswithme.util.statistics.Statistics.ParamValue.MAPSME_GUIDES
import com.mapswithme.util.statistics.Statistics.ParamValue.NO_BACKUP
import com.mapswithme.util.statistics.Statistics.ParamValue.OFFSCREEEN
import com.mapswithme.util.statistics.Statistics.ParamValue.OPENTABLE
import com.mapswithme.util.statistics.Statistics.ParamValue.PARTNER
import com.mapswithme.util.statistics.Statistics.ParamValue.PEDESTRIAN
import com.mapswithme.util.statistics.Statistics.ParamValue.PHONE
import com.mapswithme.util.statistics.Statistics.ParamValue.RESTORE
import com.mapswithme.util.statistics.Statistics.ParamValue.SEARCH_BOOKING_COM
import com.mapswithme.util.statistics.Statistics.ParamValue.TAXI
import com.mapswithme.util.statistics.Statistics.ParamValue.TRAFFIC
import com.mapswithme.util.statistics.Statistics.ParamValue.TRANSIT
import com.mapswithme.util.statistics.Statistics.ParamValue.TRUE
import com.mapswithme.util.statistics.Statistics.ParamValue.UNKNOWN
import com.mapswithme.util.statistics.Statistics.ParamValue.VEHICLE
import com.my.tracker.MyTracker
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.util.*

enum class Statistics {
    INSTANCE;

    fun trackCategoryDescChanged() {
        trackEditSettingsScreenOptionClick(ParamValue.ADD_DESC)
    }

    fun trackSharingOptionsClick(value: String) {
        val builder =
            ParameterBuilder().add(OPTION, value)
        trackEvent(
            EventName.BM_SHARING_OPTIONS_CLICK,
            builder
        )
    }

    fun trackSharingOptionsError(
        error: String,
        value: NetworkErrorType
    ) {
        trackSharingOptionsError(error, value.ordinal)
    }

    fun trackSharingOptionsError(error: String, value: Int) {
        val builder =
            ParameterBuilder()
                .add(ERROR, value)
        trackEvent(error, builder)
    }

    fun trackSharingOptionsUploadSuccess(category: BookmarkCategory) {
        val builder: ParameterBuilder =
            ParameterBuilder().add(
                EventParam.TRACKS,
                category.tracksCount
            )
                .add(
                    EventParam.POINTS,
                    category.bookmarksCount
                )
        trackEvent(
            EventName.BM_SHARING_OPTIONS_UPLOAD_SUCCESS,
            builder
        )
    }

    fun trackBookmarkListSettingsClick(analytics: Analytics) {
        val builder =
            ParameterBuilder.from(
                OPTION,
                analytics
            )
        trackEvent(
            EventName.BM_BOOKMARKS_LIST_SETTINGS_CLICK,
            builder
        )
    }

    fun trackBookmarksListSort(@BookmarkManager.SortingType sortingType: Int) {
        trackBookmarksListSort(
            getStatisticsSortingType(
                sortingType
            )
        )
    }

    fun trackBookmarksListResetSort() {
        trackBookmarksListSort(ParamValue.BY_DEFAULT)
    }

    private fun trackBookmarksListSort(value: String) {
        val builder =
            ParameterBuilder()
                .add(OPTION, value)
        trackEvent(
            EventName.BM_BOOKMARKS_LIST_SORT,
            builder
        )
    }

    fun trackBookmarksListSearch() {
        trackBookmarksSearch(ParamValue.BOOKMARKS_LIST)
    }

    private fun trackBookmarksSearch(value: String) {
        val builder =
            ParameterBuilder()
                .add(FROM, value)
        trackEvent(EventName.BM_BOOKMARKS_SEARCH, builder)
    }

    fun trackBookmarksListSearchResultSelected() {
        trackBookmarksSearchResultSelected(ParamValue.BOOKMARKS_LIST)
    }

    private fun trackBookmarksSearchResultSelected(value: String) {
        val builder =
            ParameterBuilder()
                .add(FROM, value)
        trackEvent(
            EventName.BM_BOOKMARKS_SEARCH_RESULT_SELECTED,
            builder
        )
    }

    private fun trackEditSettingsScreenOptionClick(value: String) {
        val builder =
            ParameterBuilder().add(OPTION, value)
        trackEvent(
            EventName.BM_EDIT_SETTINGS_CLICK,
            builder
        )
    }

    fun trackEditSettingsCancel() {
        trackEvent(EventName.BM_EDIT_SETTINGS_CANCEL)
    }

    fun trackEditSettingsConfirm() {
        trackEvent(EventName.BM_EDIT_SETTINGS_CONFIRM)
    }

    fun trackEditSettingsSharingOptionsClick() {
        trackEditSettingsScreenOptionClick(ParamValue.SHARING_OPTIONS)
    }

    fun trackBookmarkListSharingOptions() {
        trackEvent(
            EventName.BM_BOOKMARKS_LIST_ITEM_SETTINGS,
            ParameterBuilder().add(
                OPTION,
                ParamValue.SHARING_OPTIONS
            )
        )
    }

    fun trackSettingsDrivingOptionsChangeEvent(componentDescent: String) {
        val hasToll: Boolean = RoutingOptions.hasOption(RoadType.Toll)
        val hasFerry: Boolean = RoutingOptions.hasOption(RoadType.Ferry)
        val hasMoto: Boolean = RoutingOptions.hasOption(RoadType.Motorway)
        val hasDirty: Boolean = RoutingOptions.hasOption(RoadType.Dirty)
        val builder =
            ParameterBuilder()
        val parameterBuilder =
            builder.add(
                EventParam.TOLL,
                if (hasToll) 1 else 0
            )
                .add(
                    EventParam.FERRY,
                    if (hasFerry) 1 else 0
                )
                .add(
                    EventParam.MOTORWAY,
                    if (hasMoto) 1 else 0
                )
                .add(
                    EventParam.UNPAVED,
                    if (hasDirty) 1 else 0
                )
        parameterBuilder.add(
            FROM,
            componentDescent
        )
        trackEvent(
            EventName.SETTINGS_DRIVING_OPTIONS_CHANGE,
            parameterBuilder
        )
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        PP_BANNER_STATE_PREVIEW,
        PP_BANNER_STATE_DETAILS
    )
    annotation class BannerState

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        STATISTICS_CHANNEL_DEFAULT,
        STATISTICS_CHANNEL_REALTIME
    )
    annotation class StatisticsChannel

    // Statistics counters
    private var mBookmarksCreated = 0
    private var mSharedTimes = 0

    object EventName {
        // Downloader
        const val DOWNLOADER_ERROR = "Downloader_Map_error"
        const val DOWNLOADER_ACTION = "Downloader_Map_action"
        const val DOWNLOADER_CANCEL = "Downloader_Cancel_downloading"
        const val DOWNLOADER_DIALOG_SHOW = "Downloader_OnStartScreen_show"
        const val DOWNLOADER_DIALOG_MANUAL_DOWNLOAD =
            "Downloader_OnStartScreen_manual_download"
        const val DOWNLOADER_DIALOG_DOWNLOAD = "Downloader_OnStartScreen_auto_download"
        const val DOWNLOADER_DIALOG_LATER = "Downloader_OnStartScreen_select_later"
        const val DOWNLOADER_DIALOG_HIDE = "Downloader_OnStartScreen_select_hide"
        const val DOWNLOADER_DIALOG_CANCEL = "Downloader_OnStartScreen_cancel_download"
        const val SETTINGS_TRACKING_DETAILS = "Settings_Tracking_details"
        const val SETTINGS_TRACKING_TOGGLE = "Settings_Tracking_toggle"
        const val PLACEPAGE_DESCRIPTION_MORE = "Placepage_Description_more"
        const val PLACEPAGE_DESCRIPTION_OUTBOUND_CLICK =
            "Placepage_Description_Outbound_click"
        const val SETTINGS_SPEED_CAMS = "Settings. Speed_cameras"
        const val SETTINGS_MOBILE_INTERNET_CHANGE = "Settings_MobileInternet_change"
        const val SETTINGS_RECENT_TRACK_CHANGE = "Settings_RecentTrack_change"
        const val MOBILE_INTERNET_ALERT = "MobileInternet_alert"
        const val DOWNLOADER_BANNER_SHOW = "Downloader_Banner_show"
        const val DOWNLOADER_BANNER_CLICK = "Downloader_Banner_click"
        const val DOWNLOADER_FAB_CLICK = "Downloader_AddMap_click"
        const val DOWNLOADER_SEARCH_CLICK = "Downloader_Search_click"
        const val WHATS_NEW_ACTION = "WhatsNew_action"
        const val DOWNLOADER_DIALOG_ERROR = "Downloader_OnStartScreen_error"
        // bookmarks
        const val BM_SHARING_OPTIONS_UPLOAD_SUCCESS =
            "Bookmarks_SharingOptions_upload_success"
        const val BM_SHARING_OPTIONS_UPLOAD_ERROR = "Bookmarks_SharingOptions_upload_error"
        const val BM_SHARING_OPTIONS_ERROR = "Bookmarks_SharingOptions_error"
        const val BM_SHARING_OPTIONS_CLICK = "Bookmarks_SharingOptions_click"
        const val BM_EDIT_SETTINGS_CLICK = "Bookmarks_Bookmark_Settings_click"
        const val BM_EDIT_SETTINGS_CANCEL = "Bookmarks_Bookmark_Settings_cancel"
        const val BM_EDIT_SETTINGS_CONFIRM = "Bookmarks_Bookmark_Settings_confirm"
        const val BM_BOOKMARKS_LIST_SETTINGS_CLICK =
            "Bookmarks_BookmarksList_settings_click"
        const val BM_BOOKMARKS_LIST_ITEM_SETTINGS = "Bookmarks_BookmarksListItem_settings"
        const val BM_BOOKMARKS_LIST_SORT = "Bookmarks_BookmarksList_sort"
        const val BM_BOOKMARKS_SEARCH = "Bookmarks_Search"
        const val BM_BOOKMARKS_SEARCH_RESULT_SELECTED = "Bookmarks_Search_result_selected"
        const val BM_GROUP_CREATED = "Bookmark. Group created"
        const val BM_GROUP_CHANGED = "Bookmark. Group changed"
        const val BM_COLOR_CHANGED = "Bookmark. Color changed"
        const val BM_CREATED = "Bookmark. Bookmark created"
        const val BM_SYNC_PROPOSAL_SHOWN = "Bookmarks_SyncProposal_shown"
        const val BM_SYNC_PROPOSAL_APPROVED = "Bookmarks_SyncProposal_approved"
        const val BM_SYNC_PROPOSAL_ERROR = "Bookmarks_SyncProposal_error"
        const val BM_SYNC_PROPOSAL_ENABLED = "Bookmarks_SyncProposal_enabled"
        const val BM_SYNC_PROPOSAL_TOGGLE = "Settings_BookmarksSync_toggle"
        const val BM_SYNC_STARTED = "Bookmarks_sync_started"
        const val BM_SYNC_ERROR = "Bookmarks_sync_error"
        const val BM_SYNC_SUCCESS = "Bookmarks_sync_success"
        const val BM_EDIT_ON_WEB_CLICK = "Bookmarks_EditOnWeb_click"
        const val BM_RESTORE_PROPOSAL_CLICK = "Bookmarks_RestoreProposal_click"
        const val BM_RESTORE_PROPOSAL_CANCEL = "Bookmarks_RestoreProposal_cancel"
        const val BM_RESTORE_PROPOSAL_SUCCESS = "Bookmarks_RestoreProposal_success"
        const val BM_RESTORE_PROPOSAL_ERROR = "Bookmarks_RestoreProposal_error"
        const val BM_TAB_CLICK = "Bookmarks_Tab_click"
        const val BM_DOWNLOADED_CATALOGUE_OPEN =
            "Bookmarks_Downloaded_Catalogue_open"
        const val BM_DOWNLOADED_CATALOGUE_ERROR =
            "Bookmarks_Downloaded_Catalogue_error"
        const val BM_GUIDEDOWNLOADTOAST_SHOWN = "Bookmarks_GuideDownloadToast_shown"
        const val BM_GUIDES_DOWNLOADDIALOGUE_CLICK =
            "Bookmarks_Guides_DownloadDialogue_click"
        const val SETTINGS_DRIVING_OPTIONS_CHANGE =
            "Settings_Navigation_DrivingOptions_change"
        const val PP_DRIVING_OPTIONS_ACTION = "Placepage_DrivingOptions_action"
        // search
        const val SEARCH_CAT_CLICKED = "Search. Category clicked"
        const val SEARCH_ITEM_CLICKED = "Search. Key clicked"
        const val SEARCH_ON_MAP_CLICKED = "Search. View on map clicked."
        const val SEARCH_TAB_SELECTED = "Search_Tab_selected"
        const val SEARCH_SPONSOR_CATEGORY_SHOWN = "Search_SponsoredCategory_shown"
        const val SEARCH_SPONSOR_CATEGORY_SELECTED = "Search_SponsoredCategory_selected"
        const val SEARCH_FILTER_OPEN = "Search_Filter_Open"
        const val SEARCH_FILTER_CANCEL = "Search_Filter_Cancel"
        const val SEARCH_FILTER_RESET = "Search_Filter_Reset"
        const val SEARCH_FILTER_APPLY = "Search_Filter_Apply"
        const val SEARCH_FILTER_CLICK = "Search_Filter_Click"
        // place page
        const val PP_DETAILS_OPEN = "Placepage_Details_open"
        const val PP_SHARE = "PP. Share"
        const val PP_BOOKMARK = "PP. Bookmark"
        const val PP_ROUTE = "PP. Route"
        const val PP_SPONSORED_DETAILS = "Placepage_Hotel_details"
        const val PP_SPONSORED_BOOK = "Placepage_Hotel_book"
        const val PP_SPONSORED_OPENTABLE = "Placepage_Restaurant_book"
        const val PP_SPONSORED_OPEN = "Placepage_SponsoredGalleryPage_opened"
        const val PP_SPONSORED_SHOWN = "Placepage_SponsoredGallery_shown"
        const val PP_SPONSORED_ERROR = "Placepage_SponsoredGallery_error"
        const val PP_SPONSORED_ACTION = "Placepage_SponsoredActionButton_click"
        const val PP_SPONSOR_ITEM_SELECTED =
            "Placepage_SponsoredGallery_ProductItem_selected"
        const val PP_SPONSOR_MORE_SELECTED = "Placepage_SponsoredGallery_MoreItem_selected"
        const val PP_SPONSOR_LOGO_SELECTED = "Placepage_SponsoredGallery_LogoItem_selected"
        const val PP_DIRECTION_ARROW = "PP. DirectionArrow"
        const val PP_DIRECTION_ARROW_CLOSE = "PP. DirectionArrowClose"
        const val PP_METADATA_COPY = "PP. CopyMetadata"
        const val PP_BANNER_CLICK = "Placepage_Banner_click"
        const val PP_BANNER_SHOW = "Placepage_Banner_show"
        const val PP_BANNER_ERROR = "Placepage_Banner_error"
        const val PP_BANNER_BLANK = "Placepage_Banner_blank"
        const val PP_BANNER_CLOSE = "Placepage_Banner_close"
        const val PP_HOTEL_GALLERY_OPEN = "PlacePage_Hotel_Gallery_open"
        const val PP_HOTEL_REVIEWS_LAND = "PlacePage_Hotel_Reviews_land"
        const val PP_HOTEL_DESCRIPTION_LAND = "PlacePage_Hotel_Description_land"
        const val PP_HOTEL_FACILITIES = "PlacePage_Hotel_Facilities_open"
        const val PP_HOTEL_SEARCH_SIMILAR = "Placepage_Hotel_search_similar"
        const val PP_OWNERSHIP_BUTTON_CLICK = "Placepage_OwnershipButton_click"
        // toolbar actions
        const val TOOLBAR_MY_POSITION = "Toolbar. MyPosition"
        const val TOOLBAR_CLICK = "Toolbar_click"
        const val TOOLBAR_MENU_CLICK = "Toolbar_Menu_click"
        // dialogs
        const val PLUS_DIALOG_LATER = "GPlus dialog cancelled."
        const val RATE_DIALOG_LATER = "GPlay dialog cancelled."
        const val FACEBOOK_INVITE_LATER = "Facebook invites dialog cancelled."
        const val FACEBOOK_INVITE_INVITED = "Facebook invites dialog accepted."
        const val RATE_DIALOG_RATED = "GPlay dialog. Rating set"
        // misc
        const val ZOOM_IN = "Zoom. In"
        const val ZOOM_OUT = "Zoom. Out"
        const val PLACE_SHARED = "Place Shared"
        const val API_CALLED = "API called"
        const val DOWNLOAD_COUNTRY_NOTIFICATION_SHOWN =
            "Download country notification shown"
        const val ACTIVE_CONNECTION = "Connection"
        const val STATISTICS_STATUS_CHANGED = "Statistics status changed"
        const val TTS_FAILURE_LOCATION = "TTS failure location"
        const val UGC_NOT_AUTH_NOTIFICATION_SHOWN = "UGC_UnsentNotification_shown"
        const val UGC_NOT_AUTH_NOTIFICATION_CLICKED = "UGC_UnsentNotification_clicked"
        const val UGC_REVIEW_NOTIFICATION_SHOWN = "UGC_ReviewNotification_shown"
        const val UGC_REVIEW_NOTIFICATION_CLICKED = "UGC_ReviewNotification_clicked"
        // routing
        const val ROUTING_BUILD = "Routing. Build"
        const val ROUTING_START_SUGGEST_REBUILD = "Routing. Suggest rebuild"
        const val ROUTING_ROUTE_START = "Routing_Route_start"
        const val ROUTING_ROUTE_FINISH = "Routing_Route_finish"
        const val ROUTING_CANCEL = "Routing. Cancel"
        const val ROUTING_VEHICLE_SET = "Routing. Set vehicle"
        const val ROUTING_PEDESTRIAN_SET = "Routing. Set pedestrian"
        const val ROUTING_BICYCLE_SET = "Routing. Set bicycle"
        const val ROUTING_TAXI_SET = "Routing. Set taxi"
        const val ROUTING_TRANSIT_SET = "Routing. Set transit"
        const val ROUTING_SWAP_POINTS = "Routing. Swap points"
        const val ROUTING_SETTINGS = "Routing. Settings"
        const val ROUTING_TAXI_ORDER = "Routing_Taxi_order"
        const val ROUTING_TAXI_INSTALL = "Routing_Taxi_install"
        const val ROUTING_TAXI_SHOW = "Placepage_Taxi_show"
        const val ROUTING_TAXI_CLICK_IN_PP = "Placepage_Taxi_click"
        const val ROUTING_TAXI_ROUTE_BUILT = "Routing_Build_Taxi"
        const val ROUTING_POINT_ADD = "Routing_Point_add"
        const val ROUTING_POINT_REMOVE = "Routing_Point_remove"
        const val ROUTING_SEARCH_CLICK = "Routing_Search_click"
        const val ROUTING_BOOKMARKS_CLICK = "Routing_Bookmarks_click"
        const val ROUTING_PLAN_TOOLTIP_CLICK = "Routing_PlanTooltip_click"
        // editor
        const val EDITOR_START_CREATE = "Editor_Add_start"
        const val EDITOR_ADD_CLICK = "Editor_Add_click"
        const val EDITOR_START_EDIT = "Editor_Edit_start"
        const val EDITOR_SUCCESS_CREATE = "Editor_Add_success"
        const val EDITOR_SUCCESS_EDIT = "Editor_Edit_success"
        const val EDITOR_ERROR_CREATE = "Editor_Add_error"
        const val EDITOR_ERROR_EDIT = "Editor_Edit_error"
        const val EDITOR_AUTH_DECLINED = "Editor_Auth_declined_by_user"
        const val EDITOR_AUTH_REQUEST = "Editor_Auth_request"
        const val EDITOR_AUTH_REQUEST_RESULT = "Editor_Auth_request_result"
        const val EDITOR_REG_REQUEST = "Editor_Reg_request"
        const val EDITOR_LOST_PASSWORD = "Editor_Lost_password"
        const val EDITOR_SHARE_SHOW = "Editor_SecondTimeShare_show"
        const val EDITOR_SHARE_CLICK = "Editor_SecondTimeShare_click"
        // Cold start
        const val APPLICATION_COLD_STARTUP_INFO = "Application_ColdStartup_info"
        // Ugc.
        const val UGC_REVIEW_START = "UGC_Review_start"
        const val UGC_REVIEW_CANCEL = "UGC_Review_cancel"
        const val UGC_REVIEW_SUCCESS = "UGC_Review_success"
        const val UGC_AUTH_SHOWN = "UGC_Auth_shown"
        const val UGC_AUTH_DECLINED = "UGC_Auth_declined"
        const val UGC_AUTH_EXTERNAL_REQUEST_SUCCESS = "UGC_Auth_external_request_success"
        const val UGC_AUTH_ERROR = "UGC_Auth_error"
        const val MAP_LAYERS_ACTIVATE = "Map_Layers_activate"
        // Purchases.
        const val INAPP_PURCHASE_PREVIEW_SHOW = "InAppPurchase_Preview_show"
        const val INAPP_PURCHASE_PREVIEW_SELECT = "InAppPurchase_Preview_select"
        const val INAPP_PURCHASE_PREVIEW_PAY = "InAppPurchase_Preview_pay"
        const val INAPP_PURCHASE_PREVIEW_CANCEL = "InAppPurchase_Preview_cancel"
        const val INAPP_PURCHASE_PREVIEW_RESTORE = "InAppPurchase_Preview_restore"
        const val INAPP_PURCHASE_STORE_SUCCESS = "InAppPurchase_Store_success"
        const val INAPP_PURCHASE_STORE_ERROR = "InAppPurchase_Store_error"
        const val INAPP_PURCHASE_VALIDATION_SUCCESS = "InAppPurchase_Validation_success"
        const val INAPP_PURCHASE_VALIDATION_ERROR = "InAppPurchase_Validation_error"
        const val INAPP_PURCHASE_PRODUCT_DELIVERED = "InAppPurchase_Product_delivered"
        const val ONBOARDING_SCREEN_SHOW = "OnboardingScreen_show"
        const val ONBOARDING_SCREEN_ACCEPT = "OnboardingScreen_accept"
        const val ONBOARDING_SCREEN_DECLINE = "OnboardingScreen_decline"
        const val ONBOARDING_DEEPLINK_SCREEN_SHOW = "OnboardingDeeplinkScreen_show"
        const val ONBOARDING_DEEPLINK_SCREEN_ACCEPT = "OnboardingDeeplinkScreen_accept"
        const val ONBOARDING_DEEPLINK_SCREEN_DECLINE = "OnboardingDeeplinkScreen_decline"
        const val TIPS_TRICKS_SHOW = "TipsTricks_show"
        const val TIPS_TRICKS_CLOSE = "TipsTricks_close"
        const val TIPS_TRICKS_CLICK = "TipsTricks_click"
        const val INAPP_SUGGESTION_SHOWN = "MapsMe_InAppSuggestion_shown"
        const val INAPP_SUGGESTION_CLICKED = "MapsMe_InAppSuggestion_clicked"
        const val INAPP_SUGGESTION_CLOSED = "MapsMe_InAppSuggestion_closed"
        const val GUIDES_SHOWN = "Bookmarks_Downloaded_Guides_list"
        const val GUIDES_OPEN = "Bookmarks_Downloaded_Guide_open"
        const val GUIDES_BOOKMARK_SELECT = "Bookmarks_BookmarksList_Bookmark_select"
        const val GUIDES_TRACK_SELECT = "Bookmarks_BookmarksList_Track_select"
        const val MAP_SPONSORED_BUTTON_CLICK = "Map_SponsoredButton_click"
        const val MAP_SPONSORED_BUTTON_SHOW = "Map_SponsoredButton_show"
        const val DEEPLINK_CALL = "Deeplink_call"
        const val DEEPLINK_CALL_MISSED = "Deeplink_call_missed"

        object Settings {
            const val WEB_SITE = "Setings. Go to website"
            const val FEEDBACK_GENERAL = "Send general feedback to android@maps.me"
            const val REPORT_BUG = "Settings. Bug reported"
            const val RATE = "Settings. Rate app called"
            const val TELL_FRIEND = "Settings. Tell to friend"
            const val FACEBOOK = "Settings. Go to FB."
            const val TWITTER = "Settings. Go to twitter."
            const val HELP = "Settings. Help."
            const val ABOUT = "Settings. About."
            const val OSM_PROFILE = "Settings. Profile."
            const val COPYRIGHT = "Settings. Copyright."
            const val UNITS = "Settings. Change units."
            const val ZOOM = "Settings. Switch zoom."
            const val MAP_STYLE = "Settings. Map style."
            const val VOICE_ENABLED = "Settings. Switch voice."
            const val VOICE_LANGUAGE = "Settings. Voice language."
            const val ENERGY_SAVING = "Settings_EnergySaving_change"
        }
    }

    object EventParam {
        const val FROM = "from"
        const val TO = "to"
        const val OPTION = "option"
        const val TRACKS = "tracks"
        const val POINTS = "points"
        const val URL = "url"
        const val TOLL = "toll"
        const val UNPAVED = "unpaved"
        const val FERRY = "ferry"
        const val MOTORWAY = "motorway"
        const val SETTINGS = "settings"
        const val ROUTE = "route"
        const val SCENARIO = "scenario"
        const val BUTTON = "button"
        const val SCREEN = "screen"
        const val VERSION = "version"
        const val TARGET = "target"
        const val CATEGORY = "category"
        const val TAB = "tab"
        const val COUNT = "Count"
        const val COUNT_LOWERCASE = "count"
        const val CHANNEL = "Channel"
        const val CALLER_ID = "Caller ID"
        const val ENABLED = "Enabled"
        const val RATING = "Rating"
        const val CONNECTION_TYPE = "Connection name"
        const val CONNECTION_FAST = "Connection fast"
        const val CONNECTION_METERED = "Connection limit"
        const val MY_POSITION = "my position"
        const val POINT = "point"
        const val LANGUAGE = "language"
        const val NAME = "Name"
        const val ACTION = "action"
        const val TYPE = "type"
        const val IS_AUTHENTICATED = "is_authenticated"
        const val IS_ONLINE = "is_online"
        const val IS_SUCCESS = "is_success_message"
        const val FEATURE_ID = "feature_id"
        const val MWM_NAME = "mwm_name"
        const val MWM_VERSION = "mwm_version"
        const val ERR_MSG = "error_message"
        const val OSM = "OSM"
        const val FACEBOOK = "Facebook"
        const val PROVIDER = "provider"
        const val HOTEL = "hotel"
        const val HOTEL_LAT = "hotel_lat"
        const val HOTEL_LON = "hotel_lon"
        const val RESTAURANT = "restaurant"
        const val RESTAURANT_LAT = "restaurant_lat"
        const val RESTAURANT_LON = "restaurant_lon"
        const val FROM_LAT = "from_lat"
        const val FROM_LON = "from_lon"
        const val TO_LAT = "to_lat"
        const val TO_LON = "to_lon"
        const val BANNER = "banner"
        const val STATE = "state"
        const val ERROR_CODE = "error_code"
        const val ERROR = "error"
        const val ERROR_MESSAGE = "error_message"
        const val MAP_DATA_SIZE = "map_data_size:"
        const val BATTERY = "battery"
        const val CHARGING = "charging"
        const val NETWORK = "network"
        const val VALUE = "value"
        const val METHOD = "method"
        const val MODE = "mode"
        const val OBJECT_LAT = "object_lat"
        const val OBJECT_LON = "object_lon"
        const val ITEM = "item"
        const val DESTINATION = "destination"
        const val PLACEMENT = "placement"
        const val PRICE_CATEGORY = "price_category"
        const val DATE = "date"
        const val HAS_AUTH = "has_auth"
        const val STATUS = "status"
        const val INTERRUPTED = "interrupted"
        const val VENDOR = "vendor"
        const val PRODUCT = "product"
        const val PURCHASE = "purchase"
        const val SERVER_ID = "server_id"
        const val SERVER_IDS = "server_ids"
        const val SOURCE = "source"
        const val FIRST_LAUNCH = "first_launch"
    }

    object ParamValue {
        const val BOOKING_COM = "Booking.Com"
        const val OSM = "OSM"
        const val ON = "on"
        const val OFF = "off"
        const val CRASH_REPORTS = "crash_reports"
        const val PERSONAL_ADS = "personal_ads"
        const val SHARING_OPTIONS = "sharing_options"
        const val EDIT_ON_WEB = "edit_on_web"
        const val PUBLIC = "public"
        const val PRIVATE = "private"
        const val COPY_LINK = "copy_link"
        const val CANCEL = "Cancel"
        const val MEGAFON = "Megafon"
        const val MAP = "map"
        const val ALWAYS = "always"
        const val NEVER = "never"
        const val ASK = "ask"
        const val TODAY = "today"
        const val NOT_TODAY = "not_today"
        const val CARD = "card"
        const val SPONSORED_BUTTON = "sponsored_button"
        const val POPUP = "popup"
        const val WEBVIEW = "webview"
        const val ONBOARDING_GUIDES_SUBSCRIPTION = "onboarding_guides_subscription"
        const val PLUS = "plus"
        const val DOWNLOAD = "download"
        const val OPEN = "open"
        const val CLOSE = "close"
        const val NEXT = "next"
        const val GUIDES_SUBSCRIPTION = "OnboardingGuidesSubscription"
        const val SEARCH_BOOKING_COM = "Search.Booking.Com"
        const val OPENTABLE = "OpenTable"
        const val LOCALS_EXPERTS = "Locals.Maps.Me"
        const val SEARCH_RESTAURANTS = "Search.Restaurants"
        const val SEARCH_ATTRACTIONS = "Search.Attractions"
        const val HOLIDAY = "Holiday"
        const val NO_PRODUCTS = "no_products"
        const val ADD = "add"
        const val EDIT = "edit"
        const val AFTER_SAVE = "after_save"
        const val PLACEPAGE_PREVIEW = "placepage_preview"
        const val PLACEPAGE = "placepage"
        const val NOTIFICATION = "notification"
        const val FACEBOOK = "facebook"
        const val CHECKIN = "check_in"
        const val CHECKOUT = "check_out"
        const val ANY = "any"
        const val GOOGLE = "google"
        const val MAPSME = "mapsme"
        const val PHONE = "phone"
        const val UNKNOWN = "unknown"
        const val NETWORK = "network"
        const val DISK = "disk"
        const val AUTH = "auth"
        const val USER_INTERRUPTED = "user_interrupted"
        const val INVALID_CALL = "invalid_call"
        const val NO_BACKUP = "no_backup"
        const val DISK_NO_SPACE = "disk_no_space"
        const val BACKUP = "backup"
        const val RESTORE = "restore"
        const val NO_INTERNET = "no_internet"
        const val MY = "my"
        const val DOWNLOADED = "downloaded"
        const val SUBWAY = "subway"
        const val TRAFFIC = "traffic"
        const val SUCCESS = "success"
        const val UNAVAILABLE = "unavailable"
        const val PEDESTRIAN = "pedestrian"
        const val VEHICLE = "vehicle"
        const val BICYCLE = "bicycle"
        const val TAXI = "taxi"
        const val TRANSIT = "transit"
        const val VIEW_ON_MAP = "view on map"
        const val NOT_NOW = "not now"
        const val CLICK_OUTSIDE = "click outside pop-up"
        const val ADD_DESC = "add_description"
        const val SEND_AS_FILE = "send_as_file"
        const val MAKE_INVISIBLE_ON_MAP = "make_invisible_on_map"
        const val LIST_SETTINGS = "list_settings"
        const val DELETE_GROUP = "delete_group"
        const val OFFSCREEEN = "Offscreen"
        const val MAPSME_GUIDES = "MapsMeGuides"
        const val BY_DEFAULT = "Default"
        const val BY_DATE = "Date"
        const val BY_DISTANCE = "Distance"
        const val BY_TYPE = "Type"
        const val BOOKMARKS_LIST = "BookmarksList"
        const val PARTNER = "Partner"
        const val WIKIPEDIA = "wikipedia"
        const val TRUE = "True"
        const val FALSE = "False"
    }

    // Initialized once in constructor and does not change until the process restarts.
// In this way we can correctly finish all statistics sessions and completely
// avoid their initialization if user has disabled statistics collection.
    private val mEnabled: Boolean
    private lateinit var mMediator: ExternalLibrariesMediator
    fun setMediator(mediator: ExternalLibrariesMediator) {
        mMediator = mediator
    }

    private fun configure(context: Context) { // At the moment, need to always initialize engine for correct JNI http part reusing.
// Statistics is still enabled/disabled separately and never sent anywhere if turned off.
// TODO (AlexZ): Remove this initialization dependency from JNI part.
        org.alohalytics.Statistics.setDebugMode(BuildConfig.DEBUG)
        org.alohalytics.Statistics.setup(
            arrayOf<String>(
                PrivateVariables.alohalyticsUrl(),
                PrivateVariables.alohalyticsRealtimeUrl()
            ), context
        )
    }

    @JvmOverloads
    fun trackEvent(name: String, @StatisticsChannel channel: Int = STATISTICS_CHANNEL_DEFAULT) {
        if (mEnabled) org.alohalytics.Statistics.logEvent(name, channel)
        mMediator.eventLogger.logEvent(name, emptyMap())
    }

    @JvmOverloads
    fun trackEvent(
        name: String, params: Map<String?, String?>,
        @StatisticsChannel channel: Int = STATISTICS_CHANNEL_DEFAULT
    ) {
        if (mEnabled) org.alohalytics.Statistics.logEvent(name, params, channel)
        mMediator.eventLogger.logEvent(name, params)
    }

    @JvmOverloads
    fun trackEvent(
        name: String, location: Location?,
        params: MutableMap<String?, String?>, @StatisticsChannel channel: Int = STATISTICS_CHANNEL_DEFAULT
    ) {
        val eventDictionary: MutableList<String?> =
            ArrayList()
        for ((key, value) in params) {
            eventDictionary.add(key)
            eventDictionary.add(value)
        }
        params["lat"] = location?.latitude?.toString() ?: "N/A"
        params["lon"] = location?.longitude?.toString() ?: "N/A"
        if (mEnabled) org.alohalytics.Statistics.logEvent(
            name,
            eventDictionary.toTypedArray(),
            location,
            channel
        )
        mMediator.eventLogger.logEvent(name, params)
    }

    fun trackEvent(
        name: String,
        builder: ParameterBuilder
    ) {
        trackEvent(
            name,
            builder.get(),
            STATISTICS_CHANNEL_DEFAULT
        )
    }

    fun trackEvent(
        name: String, builder: ParameterBuilder,
        @StatisticsChannel channel: Int
    ) {
        trackEvent(name, builder.get(), channel)
    }

    fun startActivity(activity: Activity) {
        if (mEnabled) {
            AppEventsLogger.activateApp(activity)
            org.alohalytics.Statistics.onStart(activity)
        }
        mMediator.eventLogger.startActivity(activity)
    }

    fun stopActivity(activity: Activity) {
        if (mEnabled) {
            AppEventsLogger.deactivateApp(activity)
            org.alohalytics.Statistics.onStop(activity)
        }
        mMediator.eventLogger.stopActivity(activity)
    }

    fun setStatEnabled(isEnabled: Boolean) {
        SharedPropertiesUtils.isStatisticsEnabled = isEnabled
        setStatisticsEnabled(isEnabled)
        // We track if user turned on/off statistics to understand data better.
        trackEvent(
            EventName.STATISTICS_STATUS_CHANGED + " " + getInstallFlavor(),
            params().add(
                EventParam.ENABLED,
                isEnabled.toString()
            )
        )
    }

    fun trackSearchTabSelected(tab: String) {
        trackEvent(
            EventName.SEARCH_TAB_SELECTED,
            params().add(
                EventParam.TAB,
                tab
            )
        )
    }

    fun trackSearchCategoryClicked(category: String?) {
        trackEvent(
            EventName.SEARCH_CAT_CLICKED,
            params().add(
                CATEGORY,
                category
            )
        )
    }

    fun trackColorChanged(from: String?, to: String?) {
        trackEvent(
            EventName.BM_COLOR_CHANGED,
            params().add(
                FROM,
                from
            )
                .add(EventParam.TO, to)
        )
    }

    fun trackBookmarkCreated() {
        trackEvent(
            EventName.BM_CREATED,
            params().add(
                EventParam.COUNT,
                (++mBookmarksCreated).toString()
            )
        )
    }

    fun trackPlaceShared(channel: String?) {
        trackEvent(
            EventName.PLACE_SHARED,
            params().add(
                EventParam.CHANNEL,
                channel
            ).add(
                EventParam.COUNT,
                (++mSharedTimes).toString()
            )
        )
    }

    fun trackApiCall(request: ParsedMwmRequest) {
        trackEvent(
            EventName.API_CALLED,
            params().add(
                EventParam.CALLER_ID,
                if (request.callerInfo == null) "null" else request.callerInfo?.packageName
            )
        )
    }

    fun trackRatingDialog(rating: Float) {
        trackEvent(
            EventName.RATE_DIALOG_RATED,
            params().add(
                EventParam.RATING,
                rating.toString()
            )
        )
    }

    fun trackConnectionState() {
        if (isConnected) {
            /*val info = activeNetwork
            var isConnectionMetered = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) isConnectionMetered =
                (MwmApplication.get().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).isActiveNetworkMetered
            trackEvent(
                EventName.ACTIVE_CONNECTION,
                params().add(
                    EventParam.CONNECTION_TYPE,
                    info!!.typeName + ":" + info.subtypeName
                )
                    .add(
                        EventParam.CONNECTION_FAST,
                        isConnectionFast(info).toString()
                    )
                    .add(
                        EventParam.CONNECTION_METERED,
                        isConnectionMetered.toString()
                    )
            )*/
        } else trackEvent(
            EventName.ACTIVE_CONNECTION,
            params().add(
                EventParam.CONNECTION_TYPE,
                "Not connected."
            )
        )
    }

    // FIXME Call to track map changes to MyTracker to correctly deal with preinstalls.
    fun trackMapChanged(event: String) {
        if (mEnabled) {
            val params: ParameterBuilder =
                params().add(
                    EventParam.COUNT,
                    java.lang.String.valueOf(MapManager.nativeGetDownloadedCount())
                )
            trackEvent(event, params)
        }
    }

    fun trackRouteBuild(routerType: Int, from: MapObject?, to: MapObject?) {
        trackEvent(
            EventName.ROUTING_BUILD,
            params().add(
                FROM,
                getPointType(from)
            )
                .add(
                    EventParam.TO,
                    getPointType(to)
                )
        )
    }

    fun trackEditorLaunch(newObject: Boolean) {
        trackEvent(
            if (newObject) EventName.EDITOR_START_CREATE else EventName.EDITOR_START_EDIT,
            editorMwmParams().add(
                EventParam.IS_AUTHENTICATED,
                java.lang.String.valueOf(OsmOAuth.isAuthorized)
            )
                .add(
                    EventParam.IS_ONLINE,
                    isConnected.toString()
                )
        )
    }

    fun trackSubwayEvent(status: String) {
        trackMapLayerEvent(ParamValue.SUBWAY, status)
    }

    fun trackTrafficEvent(status: String) {
        trackMapLayerEvent(TRAFFIC, status)
    }

    private fun trackMapLayerEvent(eventName: String, status: String) {
        val builder =
            params()
                .add(EventParam.NAME, eventName)
                .add(EventParam.STATUS, status)
        trackEvent(EventName.MAP_LAYERS_ACTIVATE, builder)
    }

    fun trackEditorSuccess(newObject: Boolean) {
        trackEvent(
            if (newObject) EventName.EDITOR_SUCCESS_CREATE else EventName.EDITOR_SUCCESS_EDIT,
            editorMwmParams().add(
                EventParam.IS_AUTHENTICATED,
                java.lang.String.valueOf(OsmOAuth.isAuthorized)
            )
                .add(
                    EventParam.IS_ONLINE,
                    isConnected.toString()
                )
        )
    }

    fun trackEditorError(newObject: Boolean) {
        trackEvent(
            if (newObject) EventName.EDITOR_ERROR_CREATE else EventName.EDITOR_ERROR_EDIT,
            editorMwmParams().add(
                EventParam.IS_AUTHENTICATED,
                java.lang.String.valueOf(OsmOAuth.isAuthorized)
            )
                .add(
                    EventParam.IS_ONLINE,
                    isConnected.toString()
                )
        )
    }

    fun trackNetworkUsageAlert(event: String, param: String?) {
        trackEvent(
            event,
            params().add(VALUE, param)
        )
    }

    fun trackAuthRequest(type: OsmOAuth.AuthType) {
        trackEvent(
            EventName.EDITOR_AUTH_REQUEST,
            params().add(
                TYPE,
                type.type
            )
        )
    }

    fun trackTaxiInRoutePlanning(
        from: MapObject?, to: MapObject?,
        location: Location?, providerName: String,
        isAppInstalled: Boolean
    ) {
        val params =
            params()
        params.add(PROVIDER, providerName)
        params.add(
            EventParam.FROM_LAT,
            if (from != null) java.lang.String.valueOf(from.lat) else "N/A"
        )
            .add(
                EventParam.FROM_LON,
                if (from != null) java.lang.String.valueOf(from.lon) else "N/A"
            )
        params.add(
            EventParam.TO_LAT,
            if (to != null) java.lang.String.valueOf(to.lat) else "N/A"
        )
            .add(
                EventParam.TO_LON,
                if (to != null) java.lang.String.valueOf(to.lon) else "N/A"
            )
        val event =
            if (isAppInstalled) EventName.ROUTING_TAXI_ORDER else EventName.ROUTING_TAXI_INSTALL
        trackEvent(event, location, params.get())
    }

    fun trackTaxiEvent(eventName: String, providerName: String) {
        val params =
            params()
        params.add(PROVIDER, providerName)
        trackEvent(eventName, params)
    }

    fun trackTaxiError(error: TaxiInfoError) {
        val params =
            params()
        params.add(
            PROVIDER,
            error.providerName
        )
        params.add(ERROR_CODE, error.code.name)
        trackEvent(
            EventName.ROUTING_TAXI_ROUTE_BUILT,
            params
        )
    }

    fun trackNoTaxiProvidersError() {
        val params =
            params()
        params.add(ERROR_CODE, TaxiManager.ErrorCode.NoProviders.name)
        trackEvent(
            EventName.ROUTING_TAXI_ROUTE_BUILT,
            params
        )
    }

    fun trackRestaurantEvent(
        eventName: String, restaurant: Sponsored,
        mapObject: MapObject
    ) {
        val provider =
            if (restaurant.type === Sponsored.TYPE_OPENTABLE) OPENTABLE else "Unknown restaurant"
        trackEvent(
            eventName, LocationHelper.INSTANCE.lastKnownLocation,
            params().add(PROVIDER, provider)
                .add(RESTAURANT, restaurant.id)
                .add(RESTAURANT_LAT, mapObject.lat)
                .add(RESTAURANT_LON, mapObject.lon).get()
        )
    }

    fun trackHotelEvent(
        eventName: String, hotel: Sponsored,
        mapObject: MapObject
    ) {
        val provider =
            if (hotel.type === Sponsored.TYPE_BOOKING) BOOKING_COM else "Unknown hotel"
        trackEvent(
            eventName, LocationHelper.INSTANCE.lastKnownLocation,
            params().add(PROVIDER, provider)
                .add(HOTEL, hotel.id)
                .add(HOTEL_LAT, mapObject.lat)
                .add(HOTEL_LON, mapObject.lon).get()
        )
    }

    fun trackBookHotelEvent(hotel: Sponsored, mapObject: MapObject) {
        trackHotelEvent(PP_SPONSORED_BOOK, hotel, mapObject)
    }

    fun trackBookmarksTabEvent(param: String) {
        val params =
            params()
                .add(VALUE, param)
        trackEvent(EventName.BM_TAB_CLICK, params)
    }

    @Suppress("UNCHECKED_CAST")
    fun trackOpenCatalogScreen() {
        trackEvent(
            EventName.BM_DOWNLOADED_CATALOGUE_OPEN,
            emptyMap<String, String>() as Map<String?, String?>
        )
    }

    fun trackDownloadCatalogError(value: String) {
        val params =
            params()
                .add(ERROR, value)
        trackEvent(
            EventName.BM_DOWNLOADED_CATALOGUE_ERROR,
            params
        )
    }

    fun trackPPBanner(
        eventName: String,
        ad: MwmNativeAd, @BannerState state: Int
    ) {
        trackEvent(
            eventName, params()
                .add(BANNER, ad.bannerId)
                .add(PROVIDER, ad.provider)
                .add(STATE, state.toString())
        )
        if (eventName != PP_BANNER_SHOW || state == PP_BANNER_STATE_PREVIEW) MyTracker.trackEvent(
            eventName
        )
    }

    fun trackPPBannerError(
        bannerId: String, provider: String,
        error: NativeAdError?, state: Int
    ) {
        val isAdBlank =
            error != null && error.code === AdError.NO_FILL_ERROR_CODE
        val eventName = if (isAdBlank) PP_BANNER_BLANK else PP_BANNER_ERROR
        val builder =
            params()
        builder.add(BANNER, if (!TextUtils.isEmpty(bannerId)) bannerId else "N/A")
            .add(
                ERROR_CODE,
                if (error != null) java.lang.String.valueOf(error.code) else "N/A"
            )
            .add(ERROR_MESSAGE, if (error != null) error.message else "N/A")
            .add(PROVIDER, provider)
            .add(STATE, state.toString())
        trackEvent(eventName, builder.get())
        MyTracker.trackEvent(eventName)
    }

    fun trackBookingSearchEvent(mapObject: MapObject) {
        trackEvent(
            PP_SPONSORED_BOOK, LocationHelper.INSTANCE.lastKnownLocation,
            params()
                .add(PROVIDER, SEARCH_BOOKING_COM)
                .add(HOTEL, "")
                .add(HOTEL_LAT, mapObject.lat)
                .add(HOTEL_LON, mapObject.lon)
                .get()
        )
    }

    fun trackDownloaderDialogEvent(eventName: String, size: Long) {
        trackEvent(
            eventName, params()
                .add(MAP_DATA_SIZE, size)
        )
    }

    fun trackDownloaderDialogError(size: Long, error: String) {
        trackEvent(
            DOWNLOADER_DIALOG_ERROR, params()
                .add(MAP_DATA_SIZE, size)
                .add(TYPE, error)
        )
    }

    fun trackPPOwnershipButtonClick(mapObject: MapObject) {
        trackEvent(
            PP_OWNERSHIP_BUTTON_CLICK, LocationHelper.INSTANCE.lastKnownLocation,
            params()
                .add(MWM_NAME, mapObject.featureId.mMwmName)
                .add(MWM_VERSION, mapObject.featureId.mMwmVersion.toInt())
                .add(FEATURE_ID, mapObject.featureId.mFeatureIndex)
                .get()
        )
    }

    fun trackColdStartupInfo() {
        val state = state
        val charging: String
        charging = when (state.chargingStatus) {
            CHARGING_STATUS_UNKNOWN.toInt() -> "unknown"
            CHARGING_STATUS_PLUGGED.toInt() -> ParamValue.ON
            CHARGING_STATUS_UNPLUGGED.toInt() -> ParamValue.OFF
            else -> "unknown"
        }
        val network = connectionState
        trackEvent(
            APPLICATION_COLD_STARTUP_INFO,
            params()
                .add(BATTERY, state.level)
                .add(CHARGING, charging)
                .add(NETWORK, network)
                .get()
        )
    }

    private val connectionState: String
        private get() {
            val network: String
            network = if (isWifiConnected) {
                "wifi"
            } else if (isMobileConnected) {
                if (isInRoaming) "roaming" else "mobile"
            } else {
                "off"
            }
            return network
        }

    fun trackSponsoredOpenEvent(sponsored: Sponsored) {
        val builder =
            params()
        builder.add(NETWORK, connectionState)
            .add(
                PROVIDER,
                convertToSponsor(sponsored)
            )
        trackEvent(PP_SPONSORED_OPEN, builder.get())
    }

    fun trackGalleryShown(
        type: GalleryType, state: GalleryState,
        placement: GalleryPlacement, itemsCount: Int
    ) {
        trackEvent(
            PP_SPONSORED_SHOWN, params()
                .add(PROVIDER, type.provider)
                .add(PLACEMENT, placement.toString())
                .add(STATE, state.toString())
                .add(COUNT_LOWERCASE, itemsCount)
        )
        if (state === GalleryState.ONLINE) MyTracker.trackEvent(PP_SPONSORED_SHOWN + "_" + type.provider)
    }

    fun trackGalleryError(
        type: GalleryType,
        placement: GalleryPlacement, code: String?
    ) {
        trackEvent(
            PP_SPONSORED_ERROR, params()
                .add(PROVIDER, type.provider)
                .add(PLACEMENT, placement.toString())
                .add(ERROR, code).get()
        )
    }

    fun trackGalleryProductItemSelected(
        type: GalleryType,
        placement: GalleryPlacement, position: Int,
        destination: Destination
    ) {
        trackEvent(
            PP_SPONSOR_ITEM_SELECTED, params()
                .add(PROVIDER, type.provider)
                .add(PLACEMENT, placement.toString())
                .add(ITEM, position)
                .add(DESTINATION, destination.toString())
        )
    }

    fun trackGalleryEvent(
        eventName: String, type: GalleryType,
        placement: GalleryPlacement
    ) {
        trackEvent(
            eventName, params()
                .add(PROVIDER, type.provider)
                .add(PLACEMENT, placement.toString())
                .get()
        )
    }

    fun trackSearchPromoCategory(eventName: String, provider: String) {
        trackEvent(
            eventName,
            params().add(
                PROVIDER,
                provider
            ).get()
        )
        MyTracker.trackEvent(eventName + "_" + provider)
    }

    fun trackSettingsToggle(value: Boolean) {
        trackEvent(
            EventName.SETTINGS_TRACKING_TOGGLE,
            params()
                .add(TYPE, ParamValue.CRASH_REPORTS)
                .add(
                    VALUE,
                    if (value) ParamValue.ON else ParamValue.OFF
                ).get()
        )
    }

    fun trackSettingsDetails() {
        trackEvent(
            EventName.SETTINGS_TRACKING_DETAILS,
            params().add(
                TYPE,
                ParamValue.PERSONAL_ADS
            ).get()
        )
    }

    fun trackRoutingPoint(
        eventName: String, @RoutePointInfo.RouteMarkType type: Int,
        isPlanning: Boolean, isNavigating: Boolean, isMyPosition: Boolean,
        isApi: Boolean
    ) {
        val mode: String?
        mode = if (isNavigating) "onroute" else if (isPlanning) "planning" else null
        val method: String
        method = if (isPlanning) "planning_pp" else if (isApi) "api" else "outside_pp"
        val builder =
            params()
                .add(
                    TYPE,
                    convertRoutePointType(type)
                )
                .add(VALUE, if (isMyPosition) "gps" else "point")
                .add(METHOD, method)
        if (mode != null) builder.add(MODE, mode)
        trackEvent(eventName, builder.get())
    }

    fun trackRoutingEvent(eventName: String, isPlanning: Boolean) {
        trackEvent(
            eventName,
            params()
                .add(MODE, if (isPlanning) "planning" else "onroute")
                .get()
        )
    }

    fun trackRoutingStart(
        @Framework.RouterType type: Int,
        trafficEnabled: Boolean
    ) {
        trackEvent(
            ROUTING_ROUTE_START,
            prepareRouteParams(
                type,
                trafficEnabled
            )
        )
    }

    fun trackRoutingFinish(
        interrupted: Boolean, @Framework.RouterType type: Int,
        trafficEnabled: Boolean
    ) {
        val params =
            prepareRouteParams(
                type,
                trafficEnabled
            )
        trackEvent(ROUTING_ROUTE_FINISH, params.add(INTERRUPTED, if (interrupted) 1 else 0))
    }

    fun trackRoutingTooltipEvent(
        @RoutePointInfo.RouteMarkType type: Int,
        isPlanning: Boolean
    ) {
        trackEvent(
            ROUTING_PLAN_TOOLTIP_CLICK,
            params()
                .add(
                    TYPE,
                    convertRoutePointType(type)
                )
                .add(MODE, if (isPlanning) "planning" else "onroute")
                .get()
        )
    }

    fun trackSponsoredObjectEvent(
        eventName: String, sponsoredObj: Sponsored,
        mapObject: MapObject
    ) { // Here we code category by means of rating.
        trackEvent(
            eventName, LocationHelper.INSTANCE.lastKnownLocation,
            params().add(
                PROVIDER,
                convertToSponsor(sponsoredObj)
            )
                .add(CATEGORY, sponsoredObj.rating)
                .add(OBJECT_LAT, mapObject.lat)
                .add(OBJECT_LON, mapObject.lon).get()
        )
    }

    fun trackUGCStart(
        isEdit: Boolean,
        isPPPreview: Boolean,
        isFromNotification: Boolean
    ) {
        trackEvent(
            UGC_REVIEW_START,
            params()
                .add(
                    EventParam.IS_AUTHENTICATED,
                    Framework.nativeIsUserAuthenticated()
                )
                .add(
                    EventParam.IS_ONLINE,
                    isConnected
                )
                .add(
                    MODE,
                    if (isEdit) ParamValue.EDIT else ParamValue.ADD
                )
                .add(
                    FROM,
                    if (isPPPreview) ParamValue.PLACEPAGE_PREVIEW else if (isFromNotification) ParamValue.NOTIFICATION else ParamValue.PLACEPAGE
                )
                .get()
        )
    }

    fun trackUGCAuthDialogShown() {
        trackEvent(
            UGC_AUTH_SHOWN,
            params().add(
                FROM,
                ParamValue.AFTER_SAVE
            ).get()
        )
    }

    fun trackUGCExternalAuthSucceed(provider: String) {
        trackEvent(
            UGC_AUTH_EXTERNAL_REQUEST_SUCCESS,
            params().add(
                PROVIDER,
                provider
            )
        )
    }

    fun trackUGCAuthFailed(@Framework.AuthTokenType type: Int, error: String?) {
        trackEvent(
            UGC_AUTH_ERROR, params()
                .add(
                    PROVIDER,
                    getAuthProvider(type)
                )
                .add(ERROR, error)
                .get()
        )
    }

    fun trackFilterEvent(event: String, category: String) {
        trackEvent(
            event, params()
                .add(CATEGORY, category)
                .get()
        )
    }

    fun trackFilterClick(
        category: String,
        params: Pair<String?, String?>
    ) {
        trackEvent(
            SEARCH_FILTER_CLICK, params()
                .add(CATEGORY, category)
                .add(params.first, params.second)
                .get()
        )
    }

    fun trackBmSyncProposalShown(hasAuth: Boolean) {
        trackEvent(
            BM_SYNC_PROPOSAL_SHOWN,
            params().add(
                HAS_AUTH,
                if (hasAuth) 1 else 0
            ).get()
        )
    }

    fun trackBmSyncProposalApproved(hasAuth: Boolean) {
        trackEvent(
            BM_SYNC_PROPOSAL_APPROVED, params()
                .add(HAS_AUTH, if (hasAuth) 1 else 0)
                .add(NETWORK, connectionState)
                .get()
        )
    }

    fun trackBmRestoreProposalClick() {
        trackEvent(
            BM_RESTORE_PROPOSAL_CLICK, params()
                .add(NETWORK, connectionState)
                .get()
        )
    }

    fun trackBmSyncProposalError(@Framework.AuthTokenType type: Int, message: String?) {
        trackEvent(
            BM_SYNC_PROPOSAL_ERROR, params()
                .add(
                    PROVIDER,
                    getAuthProvider(type)
                )
                .add(ERROR, message)
                .get()
        )
    }

    fun trackBmSettingsToggle(checked: Boolean) {
        trackEvent(
            BM_SYNC_PROPOSAL_TOGGLE, params()
                .add(STATE, if (checked) 1 else 0)
                .get()
        )
    }

    fun trackBmSynchronizationFinish(
        @BookmarkManager.SynchronizationType type: Int,
        @BookmarkManager.SynchronizationResult result: Int,
        errorString: String
    ) {
        if (result == BookmarkManager.CLOUD_SUCCESS) {
            if (type == BookmarkManager.CLOUD_BACKUP) trackEvent(BM_SYNC_SUCCESS) else trackEvent(
                BM_RESTORE_PROPOSAL_SUCCESS
            )
            return
        }
        trackEvent(
            if (type == BookmarkManager.CLOUD_BACKUP) BM_SYNC_ERROR else BM_RESTORE_PROPOSAL_ERROR,
            params().add(
                TYPE,
                getTypeForErrorSyncResult(result)
            ).add(ERROR, errorString)
        )
    }

    fun trackBmRestoringRequestResult(@BookmarkManager.RestoringRequestResult result: Int) {
        if (result == BookmarkManager.CLOUD_BACKUP_EXISTS) return
        trackEvent(
            BM_RESTORE_PROPOSAL_ERROR, params()
                .add(
                    TYPE,
                    getTypeForRequestRestoringError(
                        result
                    )
                )
        )
    }

    fun trackToolbarClick(button: MainMenu.Item) {
        trackEvent(
            TOOLBAR_CLICK,
            getToolbarParams(button)
        )
    }

    fun trackToolbarMenu(button: MainMenu.Item) {
        trackEvent(
            TOOLBAR_MENU_CLICK,
            getToolbarParams(button)
        )
    }

    fun trackDownloadBookmarkDialog(button: String) {
        trackEvent(
            BM_GUIDES_DOWNLOADDIALOGUE_CLICK,
            params().add(ACTION, button)
        )
    }

    fun trackPPBannerClose(
        @BannerState state: Int, isCross: Boolean
    ) {
        trackEvent(
            PP_BANNER_CLOSE,
            params().add(BANNER, state)
                .add(BUTTON, if (isCross) 0 else 1)
        )
    }

    @JvmOverloads
    fun trackPurchasePreviewShow(
        purchaseId: String, vendor: String,
        productId: String, from: String? = null
    ) {
        val params =
            params().add(VENDOR, vendor)
                .add(PRODUCT, productId)
                .add(PURCHASE, purchaseId)
        if (!TextUtils.isEmpty(from)) params.add(FROM, from)
        trackEvent(
            INAPP_PURCHASE_PREVIEW_SHOW, params,
            STATISTICS_CHANNEL_REALTIME
        )
    }

    @JvmOverloads
    fun trackPurchaseEvent(
        event: String, purchaseId: String,
        @StatisticsChannel channel: Int = STATISTICS_CHANNEL_DEFAULT
    ) {
        trackEvent(
            event,
            params().add(PURCHASE, purchaseId),
            channel
        )
    }

    fun trackPurchasePreviewSelect(
        purchaseId: String,
        productId: String
    ) {
        trackEvent(
            INAPP_PURCHASE_PREVIEW_SELECT,
            params().add(PRODUCT, productId)
                .add(PURCHASE, purchaseId)
        )
    }

    fun trackPurchaseStoreError(
        purchaseId: String,
        @BillingClient.BillingResponse error: Int
    ) {
        trackEvent(
            INAPP_PURCHASE_STORE_ERROR,
            params().add(
                ERROR,
                "Billing error: $error"
            )
                .add(PURCHASE, purchaseId)
        )
    }

    fun trackPurchaseValidationError(
        purchaseId: String,
        status: ValidationStatus
    ) {
        if (status === ValidationStatus.VERIFIED) return
        val errorCode: Int
        errorCode = when (status) {
            ValidationStatus.NOT_VERIFIED -> 0
            ValidationStatus.AUTH_ERROR -> 1
            ValidationStatus.SERVER_ERROR -> 2
            else -> throw UnsupportedOperationException("Unsupported status: $status")
        }
        trackEvent(
            INAPP_PURCHASE_VALIDATION_ERROR,
            params().add(ERROR_CODE, errorCode)
                .add(PURCHASE, purchaseId)
        )
    }

    fun trackPowerManagmentSchemeChanged(@SchemeType scheme: Int) {
        var statisticValue = ""
        when (scheme) {
            PowerManagment.NONE, PowerManagment.MEDIUM -> throw AssertionError("Incorrect scheme type")
            PowerManagment.NORMAL -> statisticValue = "never"
            PowerManagment.AUTO -> statisticValue = "auto"
            PowerManagment.HIGH -> statisticValue = "max"
        }
        trackEvent(
            EventName.Settings.ENERGY_SAVING,
            params().add(
                VALUE,
                statisticValue
            )
        )
    }

    fun trackPurchaseProductDelivered(
        purchaseId: String,
        vendor: String
    ) {
        trackEvent(
            INAPP_PURCHASE_PRODUCT_DELIVERED,
            params().add(VENDOR, vendor)
                .add(PURCHASE, purchaseId),
            STATISTICS_CHANNEL_REALTIME
        )
    }

    fun trackTipsEvent(eventName: String, type: Int) {
        trackEvent(
            eventName,
            params().add(TYPE, type)
        )
    }

    fun trackTipsClose(type: Int) {
        trackEvent(
            TIPS_TRICKS_CLOSE,
            params().add(TYPE, type).add(
                OPTION,
                OFFSCREEEN
            )
        )
    }

    fun trackGuidesShown(serverIds: String) {
        if (!serverIds.isEmpty()) trackEvent(
            GUIDES_SHOWN,
            params().add(SERVER_IDS, serverIds),
            STATISTICS_CHANNEL_REALTIME
        )
    }

    fun trackGuideOpen(serverId: String) {
        trackEvent(
            GUIDES_OPEN,
            params().add(SERVER_ID, serverId),
            STATISTICS_CHANNEL_REALTIME
        )
    }

    fun trackGuideBookmarkSelect(serverId: String) {
        trackEvent(
            GUIDES_BOOKMARK_SELECT,
            params().add(SERVER_ID, serverId),
            STATISTICS_CHANNEL_REALTIME
        )
    }

    fun trackGuideTrackSelect(serverId: String) {
        trackEvent(
            GUIDES_TRACK_SELECT,
            params().add(SERVER_ID, serverId),
            STATISTICS_CHANNEL_REALTIME
        )
    }

    fun trackDeeplinkEvent(
        event: String,
        type: String,
        isFirstLaunch: Boolean
    ) {
        trackEvent(
            event,
            params().add(TYPE, type).add(
                FIRST_LAUNCH,
                if (isFirstLaunch) TRUE else FALSE
            )
        )
    }

    class ParameterBuilder {
        private val mParams: MutableMap<String?, String?> =
            HashMap()

        fun add(
            key: String?,
            value: String?
        ): ParameterBuilder {
            mParams[key] = value
            return this
        }

        fun add(
            key: String?,
            value: Boolean
        ): ParameterBuilder {
            mParams[key] = value.toString()
            return this
        }

        fun add(
            key: String?,
            value: Int
        ): ParameterBuilder {
            mParams[key] = value.toString()
            return this
        }

        fun add(
            key: String?,
            value: Long
        ): ParameterBuilder {
            mParams[key] = value.toString()
            return this
        }

        fun add(
            key: String?,
            value: Float
        ): ParameterBuilder {
            mParams[key] = value.toString()
            return this
        }

        fun add(
            key: String?,
            value: Double
        ): ParameterBuilder {
            mParams[key] = value.toString()
            return this
        }

        fun get(): MutableMap<String?, String?> {
            return mParams
        }

        companion object {
            fun from(
                key: String,
                analytics: Analytics
            ): ParameterBuilder {
                return ParameterBuilder()
                    .add(key, analytics.name)
            }
        }
    }

    enum class NetworkErrorType {
        NO_NETWORK, AUTH_FAILED
    }

    companion object {
        fun makeInAppSuggestionParamBuilder(): ParameterBuilder {
            return ParameterBuilder()
                .add(EventParam.SCENARIO, BOOKING_COM)
                .add(PROVIDER, MAPSME_GUIDES)
        }

        fun makeDownloaderBannerParamBuilder(provider: String): ParameterBuilder {
            return ParameterBuilder()
                .add(
                    FROM,
                    ParamValue.MAP
                )
                .add(PROVIDER, provider)
        }

        fun makeGuidesSubscriptionBuilder(): ParameterBuilder {
            return ParameterBuilder().add(
                EventParam.TARGET,
                ParamValue.GUIDES_SUBSCRIPTION
            )
        }

        private fun getStatisticsSortingType(@BookmarkManager.SortingType sortingType: Int): String {
            when (sortingType) {
                BookmarkManager.SORT_BY_TYPE -> return ParamValue.BY_TYPE
                BookmarkManager.SORT_BY_DISTANCE -> return ParamValue.BY_DISTANCE
                BookmarkManager.SORT_BY_TIME -> return ParamValue.BY_DATE
            }
            throw AssertionError("Invalid sorting type")
        }

        const val PP_BANNER_STATE_PREVIEW = 0
        const val PP_BANNER_STATE_DETAILS = 1
        const val STATISTICS_CHANNEL_DEFAULT: Int = org.alohalytics.Statistics.ONLY_CHANNEL
        private const val REALTIME_CHANNEL_INDEX = 1
        const val STATISTICS_CHANNEL_REALTIME =
            STATISTICS_CHANNEL_DEFAULT or (1 shl REALTIME_CHANNEL_INDEX)

        private fun convertToSponsor(sponsored: Sponsored): String {
            return if (sponsored.type === Sponsored.TYPE_PARTNER) sponsored.partnerName else convertToSponsor(
                sponsored.type
            )
        }

        private fun convertToSponsor(@Sponsored.SponsoredType type: Int): String {
            return when (type) {
                Sponsored.TYPE_BOOKING -> BOOKING_COM
                Sponsored.TYPE_OPENTABLE -> OPENTABLE
                Sponsored.TYPE_HOLIDAY -> HOLIDAY
                Sponsored.TYPE_PARTNER -> PARTNER
                Sponsored.TYPE_PROMO_CATALOG_CITY, Sponsored.TYPE_PROMO_CATALOG_SIGHTSEEINGS, Sponsored.TYPE_PROMO_CATALOG_OUTDOOR -> MAPSME_GUIDES
                Sponsored.TYPE_NONE -> "N/A"
                else -> throw AssertionError("Unknown sponsor type: $type")
            }
        }

        private fun prepareRouteParams(
            @Framework.RouterType type: Int,
            trafficEnabled: Boolean
        ): ParameterBuilder {
            return params()
                .add(MODE, toRouterType(type))
                .add(TRAFFIC, if (trafficEnabled) 1 else 0)
        }

        private fun toRouterType(@Framework.RouterType type: Int): String {
            return when (type) {
                Framework.ROUTER_TYPE_VEHICLE -> VEHICLE
                Framework.ROUTER_TYPE_PEDESTRIAN -> PEDESTRIAN
                Framework.ROUTER_TYPE_BICYCLE -> BICYCLE
                Framework.ROUTER_TYPE_TAXI -> TAXI
                Framework.ROUTER_TYPE_TRANSIT -> TRANSIT
                else -> throw AssertionError("Unsupported router type: $type")
            }
        }

        private fun convertRoutePointType(@RoutePointInfo.RouteMarkType type: Int): String {
            return when (type) {
                RoutePointInfo.ROUTE_MARK_FINISH -> "finish"
                RoutePointInfo.ROUTE_MARK_INTERMEDIATE -> "inter"
                RoutePointInfo.ROUTE_MARK_START -> "start"
                else -> throw AssertionError("Wrong parameter 'type'")
            }
        }

        fun getAuthProvider(@Framework.AuthTokenType type: Int): String {
            return when (type) {
                Framework.SOCIAL_TOKEN_FACEBOOK -> FACEBOOK
                Framework.SOCIAL_TOKEN_GOOGLE -> GOOGLE
                Framework.SOCIAL_TOKEN_PHONE -> PHONE
                Framework.TOKEN_MAPSME -> MAPSME
                Framework.SOCIAL_TOKEN_INVALID -> UNKNOWN
                else -> throw AssertionError("Unknown social token type: $type")
            }
        }

        fun getSynchronizationType(@BookmarkManager.SynchronizationType type: Int): String {
            return if (type == 0) BACKUP else RESTORE
        }

        private fun getTypeForErrorSyncResult(@BookmarkManager.SynchronizationResult result: Int): String {
            return when (result) {
                BookmarkManager.CLOUD_AUTH_ERROR -> ParamValue.AUTH
                BookmarkManager.CLOUD_NETWORK_ERROR -> ParamValue.NETWORK
                BookmarkManager.CLOUD_DISK_ERROR -> ParamValue.DISK
                BookmarkManager.CLOUD_USER_INTERRUPTED -> ParamValue.USER_INTERRUPTED
                BookmarkManager.CLOUD_INVALID_CALL -> ParamValue.INVALID_CALL
                BookmarkManager.CLOUD_SUCCESS -> throw AssertionError("It's not a error result!")
                else -> throw AssertionError("Unsupported error type: $result")
            }
        }

        private fun getTypeForRequestRestoringError(@BookmarkManager.RestoringRequestResult result: Int): String {
            return when (result) {
                BookmarkManager.CLOUD_BACKUP_EXISTS -> throw AssertionError("It's not a error result!")
                BookmarkManager.CLOUD_NOT_ENOUGH_DISK_SPACE -> DISK_NO_SPACE
                BookmarkManager.CLOUD_NO_BACKUP -> NO_BACKUP
                else -> throw AssertionError("Unsupported restoring request result: $result")
            }
        }

        private fun getToolbarParams(button: MainMenu.Item): ParameterBuilder {
            return params()
                .add(BUTTON, button.toStatisticValue())
        }



        fun editorMwmParams(): ParameterBuilder {
            return params()
                .add(MWM_NAME, Editor.nativeGetMwmName())
                .add(MWM_VERSION, Editor.nativeGetMwmVersion())
        }

        fun getPointType(point: MapObject?): String {
            return if (MapObject.isOfType(
                    MapObject.MY_POSITION,
                    point
                )
            ) EventParam.MY_POSITION else EventParam.POINT
        }

        @JvmStatic
        fun params(): ParameterBuilder {
            return ParameterBuilder()
        }
    }



    init {
        mEnabled = SharedPropertiesUtils.isStatisticsEnabled
        val context: Context = MwmApplication.get()
        // At the moment we need special handling for Alohalytics to enable/disable logging of events in core C++ code.
        if (mEnabled) org.alohalytics.Statistics.enable(context) else org.alohalytics.Statistics.disable(
            context
        )
        configure(context)
    }
}