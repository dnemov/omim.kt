package com.mapswithme.maps

import android.graphics.Bitmap
import android.text.TextUtils
import androidx.annotation.IntDef
import androidx.annotation.MainThread
import androidx.annotation.Size
import androidx.annotation.UiThread
import com.mapswithme.maps.ads.Banner
import com.mapswithme.maps.api.ParsedRoutingData
import com.mapswithme.maps.api.ParsedSearchRequest
import com.mapswithme.maps.api.ParsedUrlMwmRequest.ParsingResult
import com.mapswithme.maps.auth.AuthorizationListener
import com.mapswithme.maps.background.NotificationCandidate
import com.mapswithme.maps.bookmarks.data.DistanceAndAzimut
import com.mapswithme.maps.bookmarks.data.MapObject
import com.mapswithme.maps.downloader.DownloaderPromoBanner
import com.mapswithme.maps.gdpr.UserBindingListener
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.routing.RouteMarkData
import com.mapswithme.maps.routing.RoutePointInfo.RouteMarkType
import com.mapswithme.maps.routing.RoutingInfo
import com.mapswithme.maps.routing.TransitRouteInfo
import com.mapswithme.maps.search.FilterUtils
import com.mapswithme.maps.search.FilterUtils.RatingDef
import com.mapswithme.maps.settings.SettingsPrefsFragment.SpeedCameraMode
import com.mapswithme.util.Constants
import com.mapswithme.util.log.LoggerFactory
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * This class wraps android::Framework.cpp class
 * via static methods
 */
object Framework {
    private val LOGGER =
        LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
    private val TAG = Framework::class.java.simpleName
    const val MAP_STYLE_CLEAR = 0
    const val MAP_STYLE_DARK = 1
    const val MAP_STYLE_VEHICLE_CLEAR = 3
    const val MAP_STYLE_VEHICLE_DARK = 4
    const val ROUTER_TYPE_VEHICLE = 0
    const val ROUTER_TYPE_PEDESTRIAN = 1
    const val ROUTER_TYPE_BICYCLE = 2
    const val ROUTER_TYPE_TAXI = 3
    const val ROUTER_TYPE_TRANSIT = 4
    const val DO_AFTER_UPDATE_NOTHING = 0
    const val DO_AFTER_UPDATE_AUTO_UPDATE = 1
    const val DO_AFTER_UPDATE_ASK_FOR_UPDATE = 2
    const val ROUTE_REBUILD_AFTER_POINTS_LOADING = 0
    const val SOCIAL_TOKEN_INVALID = -1
    const val SOCIAL_TOKEN_FACEBOOK = 0
    const val SOCIAL_TOKEN_GOOGLE = 1
    const val SOCIAL_TOKEN_PHONE = 2
    //TODO(@alexzatsepin): remove TOKEN_MAPSME from this list.
    const val TOKEN_MAPSME = 3
    const val PURCHASE_VERIFIED = 0
    const val PURCHASE_NOT_VERIFIED = 1
    const val PURCHASE_VALIDATION_SERVER_ERROR = 2
    const val PURCHASE_VALIDATION_AUTH_ERROR = 3
    const val SUBSCRIPTION_TYPE_REMOVE_ADS = 0
    const val SUBSCRIPTION_TYPE_BOOKMARK_CATALOG = 1
    @JvmStatic
    fun getHttpGe0Url(
        lat: Double,
        lon: Double,
        zoomLevel: Double,
        name: String?
    ): String {
        return nativeGetGe0Url(lat, lon, zoomLevel, name).replaceFirst(
            Constants.Url.GE0_PREFIX.toRegex(),
            Constants.Url.HTTP_GE0_PREFIX
        )
    }

    /**
     * Generates Bitmap with route altitude image chart taking into account current map style.
     * @param width is width of the image.
     * @param height is height of the image.
     * @return Bitmap if there's pedestrian or bicycle route and null otherwise.
     */
    fun generateRouteAltitudeChart(
        width: Int, height: Int,
        limits: RouteAltitudeLimits
    ): Bitmap? {
        if (width <= 0 || height <= 0) return null
        val altitudeChartBits = nativeGenerateRouteAltitudeChartBits(
            width, height,
            limits
        )
            ?: return null
        return Bitmap.createBitmap(altitudeChartBits, width, height, Bitmap.Config.ARGB_8888)
    }

    fun logLocalAdsEvent(
        type: LocalAdsEventType,
        mapObject: MapObject
    ) {
        val info = mapObject.localAdInfo
        if (info == null || !info.isCustomer && !info.isHidden) return
        val location = LocationHelper.INSTANCE.lastKnownLocation
        val lat: Double = location?.latitude ?: 0.0
        val lon: Double = location?.longitude ?: 0.0
        val accuracy = location?.accuracy?.toInt() ?: 0
        nativeLogLocalAdsEvent(type.ordinal, lat, lon, accuracy)
    }

    @RatingDef
    fun getFilterRating(ratingString: String?): Int {
        if (TextUtils.isEmpty(ratingString)) return FilterUtils.RATING_ANY
        try {
            val rawRating = java.lang.Float.valueOf(ratingString!!)
            return nativeGetFilterRating(rawRating)
        } catch (e: NumberFormatException) {
            LOGGER.w(TAG, "Rating string is not valid: $ratingString")
        }
        return FilterUtils.RATING_ANY
    }

    @JvmStatic
    fun disableAdProvider(type: Banner.Type) {
        nativeDisableAdProvider(type.ordinal, Banner.Place.DEFAULT.ordinal)
    }

    fun setSpeedCamerasMode(mode: SpeedCameraMode) {
        nativeSetSpeedCamManagerMode(mode.ordinal)
    }

    @JvmStatic external fun nativeShowTrackRect(track: Long)
    @JvmStatic external fun nativeGetDrawScale(): Int
    @JvmStatic external fun nativePokeSearchInViewport(): Int
    @Size(2)
    @JvmStatic external fun nativeGetScreenRectCenter(): DoubleArray?

    @JvmStatic external fun nativeGetDistanceAndAzimuth(
        dstMerX: Double,
        dstMerY: Double,
        srcLat: Double,
        srcLon: Double,
        north: Double
    ): DistanceAndAzimut

    @JvmStatic external fun nativeGetDistanceAndAzimuthFromLatLon(
        dstLat: Double,
        dstLon: Double,
        srcLat: Double,
        srcLon: Double,
        north: Double
    ): DistanceAndAzimut?

    @JvmStatic external fun nativeFormatLatLon(
        lat: Double,
        lon: Double,
        useDmsFormat: Boolean
    ): String

    @Size(2)
    @JvmStatic external fun nativeFormatLatLonToArr(
        lat: Double,
        lon: Double,
        useDmsFormat: Boolean
    ): Array<String>?

    @JvmStatic external fun nativeFormatAltitude(alt: Double): String?
    @JvmStatic external fun nativeFormatSpeed(speed: Double): String?
    @JvmStatic external fun nativeGetGe0Url(
        lat: Double,
        lon: Double,
        zoomLevel: Double,
        name: String?
    ): String

    @JvmStatic external fun nativeGetAddress(lat: Double, lon: Double): String?
    @JvmStatic external fun nativeSetMapObjectListener(listener: MapObjectListener?)
    @JvmStatic external fun nativeRemoveMapObjectListener()
    @UiThread
    @JvmStatic external fun nativeGetOutdatedCountriesString(): String?

    @UiThread
    @JvmStatic external fun nativeGetOutdatedCountries(): Array<String>

    @UiThread
    @DoAfterUpdate
    @JvmStatic external fun nativeToDoAfterUpdate(): Int

    @JvmStatic external fun nativeIsDataVersionChanged(): Boolean
    @JvmStatic external fun nativeUpdateSavedDataVersion()
    @JvmStatic external fun nativeGetDataVersion(): Long
    @JvmStatic external fun nativeClearApiPoints()
    @ParsingResult
    @JvmStatic external fun nativeParseAndSetApiUrl(url: String?): Int

    @JvmStatic external fun nativeGetParsedRoutingData(): ParsedRoutingData?
    @JvmStatic external fun nativeGetParsedSearchRequest(): ParsedSearchRequest?
    @JvmStatic external fun nativeDeactivatePopup()
    @JvmStatic external fun nativeGetMovableFilesExts(): Array<String>?
    @JvmStatic external fun nativeGetBookmarksExt(): String?
    @JvmStatic external fun nativeGetBookmarkDir(): String?
    @JvmStatic external fun nativeGetSettingsDir(): String?
    @JvmStatic external fun nativeGetWritableDir(): String?
    @JvmStatic external fun nativeSetWritableDir(newPath: String?)
    // Routing.
    @JvmStatic external fun nativeIsRoutingActive(): Boolean

    @JvmStatic external fun nativeIsRouteBuilt(): Boolean
    @JvmStatic external fun nativeIsRouteBuilding(): Boolean
    @JvmStatic external fun nativeCloseRouting()
    @JvmStatic external fun nativeBuildRoute()
    @JvmStatic external fun nativeRemoveRoute()
    @JvmStatic external fun nativeFollowRoute()
    @JvmStatic external fun nativeDisableFollowing()
    @JvmStatic external fun nativeGetUserAgent(): String
    @JvmStatic external fun nativeGetDeviceId(): String?
    @JvmStatic external fun nativeGetRouteFollowingInfo(): RoutingInfo?
    @JvmStatic external fun nativeGenerateRouteAltitudeChartBits(
        width: Int,
        height: Int,
        routeAltitudeLimits: RouteAltitudeLimits?
    ): IntArray?

    // When an end user is going to a turn he gets sound turn instructions.
// If C++ part wants the client to pronounce an instruction nativeGenerateTurnNotifications returns
// an array of one of more strings. C++ part assumes that all these strings shall be pronounced by the client's TTS.
// For example if C++ part wants the client to pronounce "Make a right turn." this method returns
// an array with one string "Make a right turn.". The next call of the method returns nothing.
// nativeGenerateTurnNotifications shall be called by the client when a new position is available.
    @JvmStatic external fun nativeGenerateNotifications(): Array<String>?

    @JvmStatic private external fun nativeSetSpeedCamManagerMode(mode: Int)
    @JvmStatic external fun nativeSetRoutingListener(listener: RoutingListener?)
    @JvmStatic external fun nativeSetRouteProgressListener(listener: RoutingProgressListener?)
    @JvmStatic external fun nativeSetRoutingRecommendationListener(listener: RoutingRecommendationListener?)
    @JvmStatic external fun nativeSetRoutingLoadPointsListener(
        listener: RoutingLoadPointsListener?
    )

    @JvmStatic external fun nativeShowCountry(
        countryId: String?,
        zoomToDownloadButton: Boolean
    )

    @JvmStatic external fun nativeSetMapStyle(mapStyle: Int)

    @MapStyle
    @JvmStatic external fun nativeGetMapStyle(): Int

    /**
     * This method allows to set new map style without immediate applying. It can be used before
     * engine recreation instead of nativeSetMapStyle to avoid huge flow of OpenGL invocations.
     * @param mapStyle style index
     */
    @JvmStatic external fun nativeMarkMapStyle(mapStyle: Int)

    @JvmStatic external fun nativeSetRouter(@RouterType routerType: Int)
    @RouterType
    @JvmStatic external fun nativeGetRouter(): Int

    @RouterType
    @JvmStatic external fun nativeGetLastUsedRouter(): Int

    @RouterType
    @JvmStatic external fun nativeGetBestRouter(
        srcLat: Double, srcLon: Double,
        dstLat: Double, dstLon: Double
    ): Int

    @JvmStatic external fun nativeAddRoutePoint(
        title: String?, subtitle: String?,
        @RouteMarkType markType: Int,
        intermediateIndex: Int, isMyPosition: Boolean,
        lat: Double, lon: Double
    )

    @JvmStatic external fun nativeRemoveRoutePoint(
        @RouteMarkType markType: Int,
        intermediateIndex: Int
    )

    @JvmStatic external fun nativeRemoveIntermediateRoutePoints()
    @JvmStatic external fun nativeCouldAddIntermediatePoint(): Boolean
    @JvmStatic external fun nativeGetRoutePoints(): Array<RouteMarkData?>
    @JvmStatic external fun nativeGetTransitRouteInfo(): TransitRouteInfo
    /**
     * Registers all maps(.mwms). Adds them to the models, generates indexes and does all necessary stuff.
     */
    @JvmStatic external fun nativeRegisterMaps()

    @JvmStatic external fun nativeDeregisterMaps()
    /**
     * Determines if currently is day or night at the given location. Used to switch day/night styles.
     * @param utcTimeSeconds Unix time in seconds.
     * @param lat latitude of the current location.
     * @param lon longitude of the current location.
     * @return `true` if it is day now or `false` otherwise.
     */
    @JvmStatic external fun nativeIsDayTime(
        utcTimeSeconds: Long,
        lat: Double,
        lon: Double
    ): Boolean

    @JvmStatic external fun nativeGet3dMode(result: Params3dMode?)
    @JvmStatic external fun nativeSet3dMode(
        allow3d: Boolean,
        allow3dBuildings: Boolean
    )

    @JvmStatic external fun nativeGetAutoZoomEnabled(): Boolean
    @JvmStatic external fun nativeSetAutoZoomEnabled(enabled: Boolean)
    @JvmStatic external fun nativeSetTransitSchemeEnabled(enabled: Boolean)
    @JvmStatic external fun nativeSaveSettingSchemeEnabled(enabled: Boolean)
    @JvmStatic external fun nativeIsTransitSchemeEnabled(): Boolean
    @JvmStatic external fun nativeDeleteBookmarkFromMapObject(): MapObject
    @JvmStatic external fun nativeZoomToPoint(
        lat: Double,
        lon: Double,
        zoom: Int,
        animate: Boolean
    )

    /**
     * @param isBusiness selection area will be bounded by building borders, if its true(eg. true for businesses in buildings).
     * @param applyPosition if true, map'll be animated to currently selected object.
     */
    @JvmStatic external fun nativeTurnOnChoosePositionMode(
        isBusiness: Boolean,
        applyPosition: Boolean
    )

    @JvmStatic external fun nativeTurnOffChoosePositionMode()
    @JvmStatic external fun nativeIsInChoosePositionMode(): Boolean
    @JvmStatic external fun nativeIsDownloadedMapAtScreenCenter(): Boolean
    @JvmStatic external fun nativeGetActiveObjectFormattedCuisine(): String?
    @JvmStatic external fun nativeSetVisibleRect(left: Int, top: Int, right: Int, bottom: Int)
    // Navigation.
    @JvmStatic external fun nativeIsRouteFinished(): Boolean

    @JvmStatic private external fun nativeLogLocalAdsEvent(
        eventType: Int,
        lat: Double, lon: Double, accuracy: Int
    )

    @JvmStatic external fun nativeRunFirstLaunchAnimation()
    @JvmStatic external fun nativeOpenRoutePointsTransaction(): Int
    @JvmStatic external fun nativeApplyRoutePointsTransaction(transactionId: Int)
    @JvmStatic external fun nativeCancelRoutePointsTransaction(transactionId: Int)
    @JvmStatic external fun nativeInvalidRoutePointsTransactionId(): Int
    @JvmStatic external fun nativeHasSavedRoutePoints(): Boolean
    @JvmStatic external fun nativeLoadRoutePoints()
    @JvmStatic external fun nativeSaveRoutePoints()
    @JvmStatic external fun nativeDeleteSavedRoutePoints()
    @JvmStatic external fun nativeGetSearchBanners(): Array<Banner>?
    @JvmStatic external fun nativeAuthenticateUser(
        socialToken: String,
        @AuthTokenType socialTokenType: Int,
        privacyAccepted: Boolean,
        termsAccepted: Boolean,
        promoAccepted: Boolean,
        listener: AuthorizationListener
    )

    @JvmStatic external fun nativeIsUserAuthenticated(): Boolean
    @JvmStatic external fun nativeGetPhoneAuthUrl(redirectUrl: String): String
    @JvmStatic external fun nativeGetPrivacyPolicyLink(): String
    @JvmStatic external fun nativeGetTermsOfUseLink(): String
    @JvmStatic external fun nativeShowFeatureByLatLon(lat: Double, lon: Double)
    @JvmStatic external fun nativeShowBookmarkCategory(cat: Long)
    @JvmStatic private external fun nativeGetFilterRating(rawRating: Float): Int
    @JvmStatic external fun nativeMoPubInitializationBannerId(): String
    @JvmStatic external fun nativeGetDownloaderPromoBanner(mwmId: String): DownloaderPromoBanner
    @JvmStatic external fun nativeHasMegafonCategoryBanner(): Boolean
    @JvmStatic external fun nativeGetMegafonCategoryBannerUrl(): String
    @JvmStatic external fun nativeMakeCrash()
    @JvmStatic external fun nativeStartPurchaseTransaction(
        serverId: String,
        vendorId: String
    )

    @JvmStatic external fun nativeStartPurchaseTransactionListener(listener: StartTransactionListener?)
    @JvmStatic external fun nativeValidatePurchase(
        serverId: String,
        vendorId: String,
        purchaseData: String
    )

    @JvmStatic external fun nativeSetPurchaseValidationListener(listener: PurchaseValidationListener?)
    @JvmStatic external fun nativeHasActiveSubscription(@SubscriptionType type: Int): Boolean
    @JvmStatic external fun nativeSetActiveSubscription(
        @SubscriptionType type: Int,
        isActive: Boolean
    )

    @JvmStatic external fun nativeGetCurrentTipIndex(): Int
    @JvmStatic private external fun nativeDisableAdProvider(provider: Int, bannerPlace: Int)
    @JvmStatic external fun nativeBindUser(listener: UserBindingListener)
    @JvmStatic external fun nativeGetAccessToken(): String?
    @JvmStatic external fun nativeGetMapObject(
        notificationCandidate: NotificationCandidate
    ): MapObject?

    @JvmStatic external fun nativeSetPowerManagerFacility(
        facilityType: Int,
        state: Boolean
    )

    @JvmStatic external fun nativeGetPowerManagerScheme(): Int
    @JvmStatic external fun nativeSetPowerManagerScheme(schemeType: Int)
    @JvmStatic external fun nativeSetViewportCenter(
        lat: Double, lon: Double, zoom: Int,
        isAnim: Boolean
    )

    @JvmStatic external fun nativeStopLocationFollow()
    @JvmStatic external fun nativeSetSearchViewport(
        lat: Double,
        lon: Double,
        zoom: Int
    )

    /**
     * In case of the app was dumped by system to the hard drive, Java map object can be
     * restored from parcelable, but c++ framework is created from scratch and internal
     * place page object is not initialized. So, do not restore place page in this case.
     *
     * @return true if c++ framework has initialized internal place page object, otherwise - false.
     */
    @JvmStatic external fun nativeHasPlacePageInfo(): Boolean

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        MAP_STYLE_CLEAR,
        MAP_STYLE_DARK,
        MAP_STYLE_VEHICLE_CLEAR,
        MAP_STYLE_VEHICLE_DARK
    )
    annotation class MapStyle

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        ROUTER_TYPE_VEHICLE,
        ROUTER_TYPE_PEDESTRIAN,
        ROUTER_TYPE_BICYCLE,
        ROUTER_TYPE_TAXI,
        ROUTER_TYPE_TRANSIT
    )
    annotation class RouterType

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        DO_AFTER_UPDATE_NOTHING,
        DO_AFTER_UPDATE_AUTO_UPDATE,
        DO_AFTER_UPDATE_ASK_FOR_UPDATE
    )
    annotation class DoAfterUpdate

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(ROUTE_REBUILD_AFTER_POINTS_LOADING)
    annotation class RouteRecommendationType

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        SOCIAL_TOKEN_INVALID,
        SOCIAL_TOKEN_FACEBOOK,
        SOCIAL_TOKEN_GOOGLE,
        SOCIAL_TOKEN_PHONE,
        TOKEN_MAPSME
    )
    annotation class AuthTokenType

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        PURCHASE_VERIFIED,
        PURCHASE_NOT_VERIFIED,
        PURCHASE_VALIDATION_SERVER_ERROR,
        PURCHASE_VALIDATION_AUTH_ERROR
    )
    annotation class PurchaseValidationCode

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(SUBSCRIPTION_TYPE_REMOVE_ADS, SUBSCRIPTION_TYPE_BOOKMARK_CATALOG)
    annotation class SubscriptionType

    interface MapObjectListener {
        fun onMapObjectActivated(`object`: MapObject?)
        fun onDismiss(switchFullScreenMode: Boolean)
    }

    interface RoutingListener {
        @MainThread
        fun onRoutingEvent(
            resultCode: Int,
            missingMaps: Array<String>?
        )
    }

    interface RoutingProgressListener {
        @MainThread
        fun onRouteBuildingProgress(progress: Float)
    }

    interface RoutingRecommendationListener {
        fun onRecommend(@RouteRecommendationType recommendation: Int)
    }

    interface RoutingLoadPointsListener {
        fun onRoutePointsLoaded(success: Boolean)
    }

    interface PurchaseValidationListener {
        fun onValidatePurchase(
            @PurchaseValidationCode code: Int, serverId: String,
            vendorId: String, encodedPurchaseData: String
        )
    }

    interface StartTransactionListener {
        fun onStartTransaction(
            success: Boolean,
            serverId: String,
            vendorId: String
        )
    }

    class Params3dMode {
        var enabled = false
        var buildings = false
    }

    class RouteAltitudeLimits {
        var minRouteAltitude = 0
        var maxRouteAltitude = 0
        var isMetricUnits = false
    }

    enum class LocalAdsEventType {
        LOCAL_ADS_EVENT_SHOW_POINT, LOCAL_ADS_EVENT_OPEN_INFO, LOCAL_ADS_EVENT_CLICKED_PHONE, LOCAL_ADS_EVENT_CLICKED_WEBSITE, LOCAL_ADS_EVENT_VISIT
    }
}