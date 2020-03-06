package com.mapswithme.maps.intent

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.crashlytics.android.Crashlytics
import com.mapswithme.maps.DownloadResourcesLegacyActivity
import com.mapswithme.maps.Framework
import com.mapswithme.maps.Framework.RouterType
import com.mapswithme.maps.MapFragment
import com.mapswithme.maps.MwmActivity
import com.mapswithme.maps.api.Const
import com.mapswithme.maps.api.ParsedMwmRequest.Companion.currentRequest
import com.mapswithme.maps.api.ParsedMwmRequest.Companion.extractFromIntent
import com.mapswithme.maps.api.ParsedMwmRequest.Companion.isPickPointMode
import com.mapswithme.maps.api.ParsedUrlMwmRequest
import com.mapswithme.maps.api.ParsedUrlMwmRequest.ParsingResult
import com.mapswithme.maps.background.NotificationCandidate.UgcReview
import com.mapswithme.maps.bookmarks.BookmarkCategoriesActivity.Companion.startForResult
import com.mapswithme.maps.bookmarks.BookmarksCatalogActivity.Companion.start
import com.mapswithme.maps.bookmarks.BookmarksPageFactory
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.bookmarks.data.FeatureId
import com.mapswithme.maps.bookmarks.data.MapObject
import com.mapswithme.maps.bookmarks.data.MapObject.Companion.createMapObject
import com.mapswithme.maps.intent.Factory.KmzKmlProcessor
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.onboarding.IntroductionScreenFactory
import com.mapswithme.maps.purchase.BookmarksAllSubscriptionActivity
import com.mapswithme.maps.purchase.BookmarksSightsSubscriptionActivity
import com.mapswithme.maps.purchase.PurchaseUtils
import com.mapswithme.maps.purchase.SubscriptionType
import com.mapswithme.maps.routing.RoutingController
import com.mapswithme.maps.search.SearchActivity
import com.mapswithme.maps.search.SearchEngine
import com.mapswithme.maps.ugc.EditParams
import com.mapswithme.maps.ugc.UGC
import com.mapswithme.maps.ugc.UGCEditorActivity
import com.mapswithme.util.Constants
import com.mapswithme.util.StorageUtils
import com.mapswithme.util.UTM
import com.mapswithme.util.Utils
import com.mapswithme.util.concurrency.ThreadPool
import com.mapswithme.util.log.LoggerFactory
import org.alohalytics.Statistics
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.*

object Factory {
    const val EXTRA_IS_FIRST_LAUNCH = "extra_is_first_launch"
    @kotlin.jvm.JvmStatic
    fun createBuildRouteProcessor(): IntentProcessor {
        return BuildRouteProcessor()
    }

    @kotlin.jvm.JvmStatic
    fun createShowOnMapProcessor(): IntentProcessor {
        return ShowOnMapProcessor()
    }

    @kotlin.jvm.JvmStatic
    fun createKmzKmlProcessor(activity: DownloadResourcesLegacyActivity): IntentProcessor {
        return KmzKmlProcessor(activity)
    }

    @kotlin.jvm.JvmStatic
    fun createOpenCountryTaskProcessor(): IntentProcessor {
        return OpenCountryTaskProcessor()
    }

    @kotlin.jvm.JvmStatic
    fun createOldCoreLinkAdapterProcessor(): IntentProcessor {
        return OldCoreLinkAdapterProcessor()
    }

    @kotlin.jvm.JvmStatic
    fun createDlinkBookmarkCatalogueProcessor(): IntentProcessor {
        return DlinkBookmarkCatalogueIntentProcessor()
    }

    @kotlin.jvm.JvmStatic
    fun createDlinkBookmarkGuidesPageProcessor(): IntentProcessor {
        return DlinkGuidesPageIntentProcessor()
    }

    @kotlin.jvm.JvmStatic
    fun createDlinkBookmarksSubscriptionProcessor(): IntentProcessor {
        return DlinkBookmarksSubscriptionIntentProcessor()
    }

    @kotlin.jvm.JvmStatic
    fun createOldLeadUrlProcessor(): IntentProcessor {
        return OldLeadUrlIntentProcessor()
    }

    @kotlin.jvm.JvmStatic
    fun createGoogleMapsIntentProcessor(): IntentProcessor {
        return GoogleMapsIntentProcessor()
    }

    @kotlin.jvm.JvmStatic
    fun createMapsWithMeIntentProcessor(): IntentProcessor {
        return MapsWithMeIntentProcessor()
    }

    @kotlin.jvm.JvmStatic
    fun createGe0IntentProcessor(): IntentProcessor {
        return Ge0IntentProcessor()
    }

    @kotlin.jvm.JvmStatic
    fun createHttpGe0IntentProcessor(): IntentProcessor {
        return HttpGe0IntentProcessor()
    }

    @kotlin.jvm.JvmStatic
    fun createMapsmeBookmarkCatalogueProcessor(): IntentProcessor {
        return MapsmeBookmarkCatalogueProcessor()
    }

    @kotlin.jvm.JvmStatic
    fun createGeoIntentProcessor(): IntentProcessor {
        return GeoIntentProcessor()
    }

    @kotlin.jvm.JvmStatic
    fun createMapsmeProcessor(): IntentProcessor {
        return MapsmeProcessor()
    }

    private fun convertUrlToGuidesPageDeeplink(url: String): String {
        val baseCatalogUrl =
            BookmarkManager.INSTANCE.getCatalogFrontendUrl(UTM.UTM_NONE)
        val relativePath = Uri.parse(url).getQueryParameter("url")
        return Uri.parse(baseCatalogUrl)
            .buildUpon().appendEncodedPath(relativePath).toString()
    }

    private fun getCoordinateFromIntent(intent: Intent, key: String): Double {
        var value = intent.getDoubleExtra(key, 0.0)
        if (java.lang.Double.compare(value, 0.0) == 0) value =
            intent.getFloatExtra(key, 0.0f).toDouble()
        return value
    }

    abstract class LogIntentProcessor : IntentProcessor {
        var isFirstLaunch = false
            private set

        override fun process(intent: Intent): MapTask {
            isFirstLaunch = intent.getBooleanExtra(
                EXTRA_IS_FIRST_LAUNCH,
                false
            )
            val data = intent.data ?: throw AssertionError("Data must be non-null!")
            val uri = data.toString()
            val msg =
                this.javaClass.simpleName + ": incoming intent uri: " + uri
            LOGGER.i(this.javaClass.simpleName, msg)
            Statistics.logEvent(msg)
            Crashlytics.log(msg)
            return createMapTask(uri)
        }

        abstract fun createMapTask(uri: String): MapTask

        companion object {
            private val LOGGER =
                LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        }
    }

    private abstract class BaseOpenUrlProcessor : LogIntentProcessor() {
        override fun createMapTask(uri: String): MapTask {
            return OpenUrlTask(uri, com.mapswithme.util.statistics.Statistics.ParamValue.UNKNOWN)
        }
    }

    private class GeoIntentProcessor : BaseOpenUrlProcessor() {
        override fun isSupported(intent: Intent): Boolean {
            return intent.data != null && "geo" == intent.scheme
        }

        override fun createMapTask(uri: String): MapTask {
            return StatisticMapTaskWrapper.Companion.wrap(OpenUrlTask(uri, "geo"))
        }
    }

    private class Ge0IntentProcessor : BaseOpenUrlProcessor() {
        override fun isSupported(intent: Intent): Boolean {
            return intent.data != null && "ge0" == intent.scheme
        }

        override fun createMapTask(uri: String): MapTask {
            return StatisticMapTaskWrapper.Companion.wrap(OpenUrlTask(uri, "ge0"))
        }
    }

    private open class MapsmeProcessor : BaseOpenUrlProcessor() {
        override fun isSupported(intent: Intent): Boolean {
            return "mapsme" == intent.scheme
        }

        override fun createMapTask(uri: String): MapTask {
            return StatisticMapTaskWrapper.Companion.wrap(OpenUrlTask(uri, "mapsme"))
        }
    }

    private class HttpGe0IntentProcessor : IntentProcessor {
        override fun isSupported(intent: Intent): Boolean {
            if ("http".equals(intent.scheme, ignoreCase = true)) {
                val data = intent.data
                if (data != null) return "ge0.me" == data.host
            }
            return false
        }

        override fun process(intent: Intent): MapTask {
            val data = intent.data
            val ge0Url = "ge0:/" + data!!.path
            Statistics.logEvent("HttpGe0IntentProcessor::process", ge0Url)
            return StatisticMapTaskWrapper.Companion.wrap(OpenUrlTask(ge0Url, "http_ge0_me"))
        }
    }

    /**
     * Use this to invoke API task.
     */
    private class MapsWithMeIntentProcessor : IntentProcessor {
        override fun isSupported(intent: Intent): Boolean {
            return Const.ACTION_MWM_REQUEST == intent.action
        }

        override fun process(intent: Intent): MapTask {
            val apiUrl =
                intent.getStringExtra(Const.EXTRA_URL)
            Statistics.logEvent(
                "MapsWithMeIntentProcessor::process",
                apiUrl ?: "null"
            )
            if (apiUrl != null) {
                SearchEngine.INSTANCE.cancelInteractiveSearch()
                val request = extractFromIntent(intent)
                currentRequest = request
                com.mapswithme.util.statistics.Statistics.INSTANCE.trackApiCall(request)
                if (!isPickPointMode) return StatisticMapTaskWrapper.Companion.wrap(
                    OpenUrlTask(apiUrl, "action_api_request")
                )
            }
            throw AssertionError("Url must be provided!")
        }
    }

    private class GoogleMapsIntentProcessor : BaseOpenUrlProcessor() {
        override fun isSupported(intent: Intent): Boolean {
            val data = intent.data
            return data != null && "maps.google.com" == data.host
        }

        override fun createMapTask(uri: String): MapTask {
            return StatisticMapTaskWrapper.Companion.wrap(OpenUrlTask(uri, "maps_google_com"))
        }
    }

    private class OldLeadUrlIntentProcessor : BaseOpenUrlProcessor() {
        override fun isSupported(intent: Intent): Boolean {
            val data = intent.data ?: return false
            val scheme = intent.scheme
            val host = data.host
            return if (TextUtils.isEmpty(scheme) || TextUtils.isEmpty(host)) false else (scheme == "mapsme" || scheme == "mapswithme") && "lead" == host
        }

        override fun createMapTask(uri: String): MapTask {
            return StatisticMapTaskWrapper.Companion.wrap(OpenUrlTask(uri, "old_lead"))
        }
    }

    private class DlinkBookmarkCatalogueIntentProcessor : DlinkIntentProcessor() {
        override fun isLinkSupported(data: Uri): Boolean {
            return File.separator + CATALOGUE == data.path
        }

        override fun createIntroductionTask(url: String): MapTask? {
            return FreeGuideReadyToDownloadIntroductionTask(url)
        }

        override fun createTargetTask(url: String): MapTask {
            return StatisticMapTaskWrapper.Companion.wrap(ImportBookmarkCatalogueTask(url))
        }

        companion object {
            const val CATALOGUE = "catalogue"
        }
    }

    class DlinkGuidesPageIntentProcessor : DlinkIntentProcessor() {
        override fun isLinkSupported(data: Uri): Boolean {
            return File.separator + GUIDES_PAGE == data.path
        }

        override fun createIntroductionTask(url: String): MapTask? {
            return GuidesPageToOpenIntroductionTask(url)
        }

        override fun createTargetTask(url: String): MapTask {
            return StatisticMapTaskWrapper.Companion.wrap(GuidesPageToOpenTask(url))
        }

        companion object {
            const val GUIDES_PAGE = "guides_page"
        }
    }

    class DlinkBookmarksSubscriptionIntentProcessor : DlinkIntentProcessor() {
        override fun isLinkSupported(data: Uri): Boolean {
            return File.separator + SUBSCRIPTION == data.path
        }

        override fun createIntroductionTask(url: String): MapTask? { // In release 9.5 the introduction screen for this deeplink is forgotten.
            return null
        }

        override fun createTargetTask(url: String): MapTask {
            return StatisticMapTaskWrapper.Companion.wrap(BookmarksSubscriptionTask(url))
        }

        companion object {
            const val SUBSCRIPTION = "subscription"
        }
    }

    private class MapsmeBookmarkCatalogueProcessor : MapsmeProcessor() {
        override fun createMapTask(uri: String): MapTask {
            val url = Uri.parse(uri).buildUpon()
                .scheme(DlinkIntentProcessor.SCHEME_HTTPS)
                .authority(DlinkIntentProcessor.HOST)
                .path(DlinkBookmarkCatalogueIntentProcessor.CATALOGUE)
                .build().toString()
            return StatisticMapTaskWrapper.Companion.wrap(ImportBookmarkCatalogueTask(url))
        }

        override fun isSupported(intent: Intent): Boolean {
            if (!super.isSupported(intent)) return false
            val data = intent.data ?: return false
            val host = data.host
            return DlinkBookmarkCatalogueIntentProcessor.CATALOGUE == host
        }
    }

    private class OldCoreLinkAdapterProcessor : DlinkIntentProcessor() {
        override fun createTargetTask(url: String): MapTask { // Transform deeplink to the core expected format,
// i.e https://host/path?query -> mapsme:///path?query.
            val uri = Uri.parse(url)
            val coreUri = uri.buildUpon()
                .scheme(SCHEME_CORE)
                .authority("").build()
            val query = coreUri.lastPathSegment
            return StatisticMapTaskWrapper.Companion.wrap(
                OpenUrlTask(
                    coreUri.toString(),
                    if (TextUtils.isEmpty(query)) com.mapswithme.util.statistics.Statistics.ParamValue.UNKNOWN else query!!
                )
            )
        }

        override fun isLinkSupported(data: Uri): Boolean {
            return true
        }

        override fun createIntroductionTask(url: String): MapTask? {
            return null
        }

        companion object {
            private const val SCHEME_CORE = "mapsme"
        }
    }

    abstract class DlinkIntentProcessor : LogIntentProcessor() {
        override fun isSupported(intent: Intent): Boolean {
            val data = intent.data ?: return false
            val scheme = intent.scheme
            val host = data.host
            return SCHEME_HTTPS == scheme && (HOST == host || HOST_DEV == host) &&
                    isLinkSupported(data)
        }

        abstract fun isLinkSupported(data: Uri): Boolean
        override fun createMapTask(url: String): MapTask {
            if (isFirstLaunch) {
                val introductionTask = createIntroductionTask(url)
                if (introductionTask != null) return introductionTask
            }
            return createTargetTask(url)
        }

        abstract fun createIntroductionTask(url: String): MapTask?
        abstract fun createTargetTask(url: String): MapTask

        companion object {
            const val SCHEME_HTTPS = "https"
            const val HOST = "dlink.maps.me"
            const val HOST_DEV = "dlink.mapsme.devmail.ru"
        }
    }

    private class OpenCountryTaskProcessor : IntentProcessor {
        override fun isSupported(intent: Intent): Boolean {
            return intent.hasExtra(DownloadResourcesLegacyActivity.EXTRA_COUNTRY)
        }

        override fun process(intent: Intent): MapTask {
            val countryId =
                intent.getStringExtra(DownloadResourcesLegacyActivity.EXTRA_COUNTRY)
            Statistics.logEvent(
                "OpenCountryTaskProcessor::process",
                arrayOf("autoDownload", "false"),
                LocationHelper.INSTANCE.savedLocation
            )
            return StatisticMapTaskWrapper.Companion.wrap(ShowCountryTask(countryId))
        }
    }

    private class KmzKmlProcessor internal constructor(private val mActivity: DownloadResourcesLegacyActivity) :
        IntentProcessor {
        private var mData: Uri? = null
        override fun isSupported(intent: Intent): Boolean {
            mData = intent.data
            return mData != null
        }

        override fun process(intent: Intent): MapTask? {
            ThreadPool.storage.execute {
                readKmzFromIntent()
                mActivity.runOnUiThread { mActivity.showMap() }
            }
            return null
        }

        private fun readKmzFromIntent() {
            var path: String? = null
            var isTemporaryFile = false
            val scheme = mData!!.scheme
            if (scheme != null && !scheme.equals(
                    ContentResolver.SCHEME_FILE,
                    ignoreCase = true
                )
            ) { // scheme is "content" or "http" - need to download or read file first
                var input: InputStream? = null
                var output: OutputStream? = null
                try {
                    val resolver = mActivity.contentResolver
                    val ext = getExtensionFromMime(resolver.getType(mData!!))
                    if (ext != null) {
                        val filePath =
                            (StorageUtils.getTempPath(mActivity.application)
                                    + "Attachment" + ext)
                        val tmpFile = File(filePath)
                        output = FileOutputStream(tmpFile)
                        input = resolver.openInputStream(mData!!)
                        val buffer =
                            ByteArray(Constants.MB / 2)
                        var read: Int
                        while (input!!.read(buffer).also { read = it } != -1) output.write(
                            buffer,
                            0,
                            read
                        )
                        output.flush()
                        path = filePath
                        isTemporaryFile = true
                    }
                } catch (ex: Exception) {
                    LOGGER.w(
                        TAG,
                        "Attachment not found or io error: $ex",
                        ex
                    )
                } finally {
                    Utils.closeSafely(input!!)
                    Utils.closeSafely(output!!)
                }
            } else path = mData!!.path
            if (!TextUtils.isEmpty(path)) {
                LOGGER.d(
                    TAG,
                    "Loading bookmarks file from: $path"
                )
                loadKmzFile(path!!, isTemporaryFile)
            } else {
                LOGGER.w(
                    TAG,
                    "Can't get bookmarks file from URI: $mData"
                )
            }
        }

        private fun loadKmzFile(path: String, isTemporaryFile: Boolean) {
            mActivity.runOnUiThread {
                BookmarkManager.INSTANCE.loadKmzFile(
                    path,
                    isTemporaryFile
                )
            }
        }

        private fun getExtensionFromMime(mime: String?): String? {
            var mime = mime
            val i = mime!!.lastIndexOf('.')
            if (i == -1) return null
            mime = mime.substring(i + 1)
            return if (mime.equals("kmz", ignoreCase = true)) ".kmz" else if (mime.equals(
                    "kml+xml",
                    ignoreCase = true
                )
            ) ".kml" else null
        }

        companion object {
            private val LOGGER =
                LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
            private val TAG = KmzKmlProcessor::class.java.simpleName
        }

    }

    private class ShowOnMapProcessor : IntentProcessor {
        override fun isSupported(intent: Intent): Boolean {
            return ACTION_SHOW_ON_MAP == intent.action
        }

        override fun process(intent: Intent): MapTask {
            if (!intent.hasExtra(EXTRA_LAT) || !intent.hasExtra(
                    EXTRA_LON
                )
            ) throw AssertionError("Extra lat/lon must be provided!")
            val lat = getCoordinateFromIntent(
                intent,
                EXTRA_LAT
            )
            val lon = getCoordinateFromIntent(
                intent,
                EXTRA_LON
            )
            return StatisticMapTaskWrapper.Companion.wrap(ShowPointTask(lat, lon))
        }

        companion object {
            private const val ACTION_SHOW_ON_MAP =
                "com.mapswithme.maps.pro.action.SHOW_ON_MAP"
            private const val EXTRA_LAT = "lat"
            private const val EXTRA_LON = "lon"
        }
    }

    private class BuildRouteProcessor : IntentProcessor {
        override fun isSupported(intent: Intent): Boolean {
            return ACTION_BUILD_ROUTE == intent.action
        }

        override fun process(intent: Intent): MapTask {
            if (!intent.hasExtra(EXTRA_LAT_TO) || !intent.hasExtra(
                    EXTRA_LON_TO
                )
            ) throw AssertionError("Extra lat/lon must be provided!")
            val saddr =
                intent.getStringExtra(EXTRA_SADDR)
            val daddr =
                intent.getStringExtra(EXTRA_DADDR)
            val latTo = getCoordinateFromIntent(
                intent,
                EXTRA_LAT_TO
            )
            val lonTo = getCoordinateFromIntent(
                intent,
                EXTRA_LON_TO
            )
            val hasFrom =
                intent.hasExtra(EXTRA_LAT_FROM) && intent.hasExtra(
                    EXTRA_LON_FROM
                )
            val hasRouter =
                intent.hasExtra(EXTRA_ROUTER)
            val mapTaskToForward: MapTask
            mapTaskToForward = if (hasFrom && hasRouter) {
                val latFrom =
                    getCoordinateFromIntent(
                        intent,
                        EXTRA_LAT_FROM
                    )
                val lonFrom =
                    getCoordinateFromIntent(
                        intent,
                        EXTRA_LON_FROM
                    )
                BuildRouteTask(
                    latTo, lonTo, saddr, latFrom, lonFrom,
                    daddr, intent.getStringExtra(EXTRA_ROUTER)
                )
            } else if (hasFrom) {
                val latFrom =
                    getCoordinateFromIntent(
                        intent,
                        EXTRA_LAT_FROM
                    )
                val lonFrom =
                    getCoordinateFromIntent(
                        intent,
                        EXTRA_LON_FROM
                    )
                BuildRouteTask(
                    latTo, lonTo, saddr,
                    latFrom, lonFrom, daddr
                )
            } else {
                BuildRouteTask(
                    latTo, lonTo,
                    intent.getStringExtra(EXTRA_ROUTER)
                )
            }
            return mapTaskToForward
        }

        companion object {
            private const val ACTION_BUILD_ROUTE =
                "com.mapswithme.maps.pro.action.BUILD_ROUTE"
            private const val EXTRA_LAT_TO = "lat_to"
            private const val EXTRA_LON_TO = "lon_to"
            private const val EXTRA_LAT_FROM = "lat_from"
            private const val EXTRA_LON_FROM = "lon_from"
            private const val EXTRA_SADDR = "saddr"
            private const val EXTRA_DADDR = "daddr"
            private const val EXTRA_ROUTER = "router"
        }
    }

    class BookmarksSubscriptionTask internal constructor(private val mUrl: String) : MapTask {
        override fun run(target: MwmActivity): Boolean {
            val uri = Uri.parse(mUrl)
            val serverId = uri.getQueryParameter(PurchaseUtils.GROUPS)
            if (TextUtils.isEmpty(serverId)) return false
            val type =
                SubscriptionType.getTypeByBookmarksGroup(serverId!!)
            if (type == SubscriptionType.BOOKMARKS_ALL) {
                BookmarksAllSubscriptionActivity.startForResult(target)
                return true
            }
            if (type == SubscriptionType.BOOKMARKS_SIGHTS) {
                BookmarksSightsSubscriptionActivity.startForResult(target)
                return true
            }
            return false
        }

        override fun toStatisticValue(): String {
            return "subscription"
        }

        companion object {
            private const val serialVersionUID = 8378582625122063605L
        }

    }

    class ImportBookmarkCatalogueTask internal constructor(private val mUrl: String) : MapTask {
        override fun run(target: MwmActivity): Boolean {
            startForResult(
                target,
                BookmarksPageFactory.DOWNLOADED.ordinal,
                mUrl
            )
            return true
        }

        override fun toStatisticValue(): String {
            return "catalogue"
        }

        companion object {
            private const val serialVersionUID = 5363722491377575159L
        }

    }

    class GuidesPageToOpenTask internal constructor(url: String) : BaseUrlTask(url) {
        override fun run(target: MwmActivity): Boolean {
            val deeplink =
                convertUrlToGuidesPageDeeplink(url)
            start(target, deeplink)
            return true
        }

        override fun toStatisticValue(): String {
            return "guides_page"
        }

        companion object {
            private const val serialVersionUID = 8388101038319062165L
        }
    }

    internal class FreeGuideReadyToDownloadIntroductionTask(url: String) :
        BaseUrlTask(url) {
        override fun run(target: MwmActivity): Boolean {
            target.showIntroductionScreenForDeeplink(url, IntroductionScreenFactory.FREE_GUIDE)
            return true
        }

        override fun toStatisticValue(): String {
            throw UnsupportedOperationException("This task not statistic tracked!")
        }

        companion object {
            private const val serialVersionUID = -6851782210156017186L
        }
    }

    class GuidesPageToOpenIntroductionTask internal constructor(url: String) :
        BaseUrlTask(url) {
        override fun run(target: MwmActivity): Boolean {
            val deeplink =
                convertUrlToGuidesPageDeeplink(url)
            target.showIntroductionScreenForDeeplink(
                deeplink,
                IntroductionScreenFactory.GUIDES_PAGE
            )
            return true
        }

        override fun toStatisticValue(): String {
            throw UnsupportedOperationException("This task not statistic tracked!")
        }

        companion object {
            private const val serialVersionUID = 8388101038319062165L
        }
    }

    abstract class BaseUrlTask(val url: String) : MapTask {

        companion object {
            private const val serialVersionUID = 9077126080900672394L
        }

    }

    class OpenUrlTask internal constructor(url: String, statisticValue: String) :
        MapTask {
        private val mUrl: String
        private val mStatisticValue: String
        override fun run(target: MwmActivity): Boolean {
            @ParsingResult val result = Framework.nativeParseAndSetApiUrl(mUrl)
            when (result) {
                ParsedUrlMwmRequest.RESULT_INCORRECT ->  // TODO: Kernel recognizes "mapsme://", "mwm://" and "mapswithme://" schemas only!!!
                    return MapFragment.nativeShowMapForUrl(mUrl)
                ParsedUrlMwmRequest.RESULT_MAP -> return MapFragment.nativeShowMapForUrl(mUrl)
                ParsedUrlMwmRequest.RESULT_ROUTE -> {
                    val data = Framework.nativeGetParsedRoutingData()
                    RoutingController.get().setRouterType(data!!.mRouterType)
                    val from = data.mPoints[0]
                    val to = data.mPoints[1]
                    RoutingController.get().prepare(
                        createMapObject(
                            FeatureId.EMPTY, MapObject.API_POINT,
                            from.mName, "", from.mLat, from.mLon
                        ),
                        createMapObject(
                            FeatureId.EMPTY, MapObject.API_POINT,
                            to.mName, "", to.mLat, to.mLon
                        ), true
                    )
                    return true
                }
                ParsedUrlMwmRequest.RESULT_SEARCH -> {
                    val request = Framework.nativeGetParsedSearchRequest()
                    if (request!!.mLat != 0.0 || request.mLon != 0.0) {
                        Framework.nativeStopLocationFollow()
                        Framework.nativeSetViewportCenter(
                            request.mLat,
                            request.mLon,
                            SEARCH_IN_VIEWPORT_ZOOM,
                            false
                        )
                        // We need to update viewport for search api manually because of drape engine
// will not notify subscribers when search activity is shown.
                        if (!request.mIsSearchOnMap) Framework.nativeSetSearchViewport(
                            request.mLat,
                            request.mLon,
                            SEARCH_IN_VIEWPORT_ZOOM
                        )
                    }
                    SearchActivity.start(
                        target, request.mQuery, request.mLocale, request.mIsSearchOnMap,
                        null, null
                    )
                    return true
                }
                ParsedUrlMwmRequest.RESULT_LEAD -> return true
            }
            return false
        }

        override fun toStatisticValue(): String {
            return mStatisticValue
        }

        companion object {
            private const val serialVersionUID = -7257820771228127413L
            private const val SEARCH_IN_VIEWPORT_ZOOM = 16
        }

        init {
            Utils.checkNotNull(url)
            mUrl = url
            mStatisticValue = statisticValue
        }
    }

    class ShowCountryTask(private val mCountryId: String) : MapTask {
        override fun run(target: MwmActivity): Boolean {
            Framework.nativeShowCountry(mCountryId, false)
            return true
        }

        override fun toStatisticValue(): String {
            return "open_country"
        }

        companion object {
            private const val serialVersionUID = 256630934543189768L
        }

    }

    class ShowBookmarkCategoryTask(val mCategoryId: Long) : RegularMapTask() {
        override fun run(target: MwmActivity): Boolean {
            BookmarkManager.INSTANCE.showBookmarkCategoryOnMap(mCategoryId)
            return true
        }

        companion object {
            private const val serialVersionUID = 8285565041410550281L
        }

    }

    abstract class BaseUserMarkTask(val mCategoryId: Long, val mId: Long) :
        RegularMapTask() {

        companion object {
            private const val serialVersionUID = -3348320422813422144L
        }

    }

    class ShowBookmarkTask(categoryId: Long, bookmarkId: Long) :
        BaseUserMarkTask(categoryId, bookmarkId) {
        override fun run(target: MwmActivity): Boolean {
            BookmarkManager.INSTANCE.showBookmarkOnMap(mId)
            return true
        }

        companion object {
            private const val serialVersionUID = 7582931785363515736L
        }
    }

    class ShowTrackTask(categoryId: Long, trackId: Long) :
        BaseUserMarkTask(categoryId, trackId) {
        override fun run(target: MwmActivity): Boolean {
            Framework.nativeShowTrackRect(mId)
            return true
        }

        companion object {
            private const val serialVersionUID = 1091286722919338991L
        }
    }

    class ShowPointTask internal constructor(private val mLat: Double, private val mLon: Double) :
        MapTask {
        override fun run(target: MwmActivity): Boolean {
            MapFragment.nativeShowMapForUrl(
                String.format(
                    Locale.US,
                    "mapsme://map?ll=%f,%f", mLat, mLon
                )
            )
            return true
        }

        override fun toStatisticValue(): String {
            return "show_on_map_intent"
        }

        companion object {
            private const val serialVersionUID = -2467635346469323664L
        }

    }

    class BuildRouteTask @JvmOverloads internal constructor(
        private val mLatTo: Double, private val mLonTo: Double, private val mSaddr: String?,
        private val mLatFrom: Double?, private val mLonFrom: Double?, private val mDaddr: String?,
        private val mRouter: String? = null
    ) : MapTask {

        internal constructor(
            latTo: Double,
            lonTo: Double,
            router: String?
        ) : this(latTo, lonTo, null, null, null, null, router) {
        }

        override fun run(target: MwmActivity): Boolean {
            @RouterType var routerType = -1
            if (!TextUtils.isEmpty(mRouter)) {
                when (mRouter) {
                    "vehicle" -> routerType = Framework.ROUTER_TYPE_VEHICLE
                    "pedestrian" -> routerType = Framework.ROUTER_TYPE_PEDESTRIAN
                    "bicycle" -> routerType = Framework.ROUTER_TYPE_BICYCLE
                    "taxi" -> routerType = Framework.ROUTER_TYPE_TAXI
                    "transit" -> routerType = Framework.ROUTER_TYPE_TRANSIT
                }
            }
            if (mLatFrom != null && mLonFrom != null && routerType >= 0) {
                RoutingController.get().prepare(
                    fromLatLon(mLatFrom, mLonFrom, mSaddr),
                    fromLatLon(mLatTo, mLonTo, mDaddr), routerType,
                    true /* fromApi */
                )
            } else if (mLatFrom != null && mLonFrom != null) {
                RoutingController.get().prepare(
                    fromLatLon(mLatFrom, mLonFrom, mSaddr),
                    fromLatLon(mLatTo, mLonTo, mDaddr), true /* fromApi */
                )
            } else if (routerType > 0) {
                RoutingController.get().prepare(
                    true /* canUseMyPositionAsStart */,
                    fromLatLon(mLatTo, mLonTo, mDaddr), routerType,
                    true /* fromApi */
                )
            } else {
                RoutingController.get().prepare(
                    true /* canUseMyPositionAsStart */,
                    fromLatLon(mLatTo, mLonTo, mDaddr), true /* fromApi */
                )
            }
            return true
        }

        override fun toStatisticValue(): String {
            return "build_route_intent"
        }

        companion object {
            private const val serialVersionUID = 5301468481040195957L
            private fun fromLatLon(
                lat: Double,
                lon: Double,
                addr: String?
            ): MapObject {
                return createMapObject(
                    FeatureId.EMPTY, MapObject.API_POINT,
                    if (TextUtils.isEmpty(addr)) "" else addr!!, "", lat, lon
                )
            }
        }

    }

    class RestoreRouteTask : RegularMapTask() {
        override fun run(target: MwmActivity): Boolean {
            RoutingController.get().restoreRoute()
            return true
        }

        companion object {
            private const val serialVersionUID = 6123893958975977040L
        }
    }

    class ShowUGCEditorTask(// Nullable because of possible serialization from previous incompatible version of class.
        private val mNotificationCandidate: UgcReview?
    ) : RegularMapTask() {

        override fun run(target: MwmActivity): Boolean {
            if (mNotificationCandidate == null) return false
            val mapObject = Framework.nativeGetMapObject(mNotificationCandidate) ?: return false
            val builder = EditParams.Builder.fromMapObject(mapObject)
                .setDefaultRating(UGC.RATING_NONE)
                .setFromNotification(true)
            UGCEditorActivity.start(target, builder.build())
            return true
        }

        companion object {
            private const val serialVersionUID = 1636712824900113568L
        }

    }

    class ShowDialogTask(private val mDialogName: String) : RegularMapTask() {
        override fun run(target: MwmActivity): Boolean {
            val f =
                target.supportFragmentManager.findFragmentByTag(mDialogName)
            if (f != null) return true
            val fragment =
                Fragment.instantiate(
                    target,
                    mDialogName
                ) as DialogFragment
            fragment.show(target.supportFragmentManager, mDialogName)
            return true
        }

        companion object {
            private const val serialVersionUID = 1548931513812565018L
        }

    }
}