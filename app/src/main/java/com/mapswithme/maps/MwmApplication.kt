package com.mapswithme.maps

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Message
import android.util.Log
import androidx.multidex.MultiDex
import com.appsflyer.AppsFlyerLib
import com.mapswithme.maps.analytics.ExternalLibrariesMediator
import com.mapswithme.maps.background.AppBackgroundTracker
import com.mapswithme.maps.background.AppBackgroundTracker.OnTransitionListener
import com.mapswithme.maps.background.AppBackgroundTracker.OnVisibleAppLaunchListener
import com.mapswithme.maps.background.NotificationChannelFactory.createProvider
import com.mapswithme.maps.background.Notifier
import com.mapswithme.maps.base.MediaPlayerWrapper
import com.mapswithme.maps.bookmarks.data.BookmarkManager.Companion.loadBookmarks
import com.mapswithme.maps.downloader.CountryItem
import com.mapswithme.maps.downloader.MapManager.StorageCallback
import com.mapswithme.maps.downloader.MapManager.StorageCallbackData
import com.mapswithme.maps.downloader.MapManager.nativeGetError
import com.mapswithme.maps.downloader.MapManager.nativeGetName
import com.mapswithme.maps.downloader.MapManager.nativeIsAutoretryFailed
import com.mapswithme.maps.downloader.MapManager.nativeSubscribe
import com.mapswithme.maps.downloader.MapManager.sendErrorStat
import com.mapswithme.maps.editor.Editor.init
import com.mapswithme.maps.geofence.GeofenceRegistry
import com.mapswithme.maps.geofence.GeofenceRegistryImpl
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.location.TrackRecorder.init
import com.mapswithme.maps.maplayer.subway.SubwayManager
import com.mapswithme.maps.maplayer.traffic.TrafficManager
import com.mapswithme.maps.routing.RoutingController
import com.mapswithme.maps.scheduling.ConnectivityJobScheduler
import com.mapswithme.maps.scheduling.ConnectivityListener
import com.mapswithme.maps.search.SearchEngine
import com.mapswithme.maps.sound.TtsPlayer
import com.mapswithme.util.*
import com.mapswithme.util.log.Logger
import com.mapswithme.util.log.LoggerFactory
import com.mapswithme.util.statistics.Statistics
import java.util.*
import kotlin.collections.set

class MwmApplication : Application(), OnTransitionListener {

    lateinit var logger: Logger
        private set
    private var mPrefs: SharedPreferences? = null
    private var mBackgroundTracker: AppBackgroundTracker? = null
    lateinit var subwayManager: SubwayManager
        private set
    private var mFrameworkInitialized = false
    private var mPlatformInitialized = false
    private var mMainLoopHandler: Handler? = null
    private val mMainQueueToken = Any()
    private val mVisibleAppLaunchListener: OnVisibleAppLaunchListener = VisibleAppLaunchListener()
    lateinit var connectivityListener: ConnectivityListener
        private set
    private val mStorageCallbacks: StorageCallback = StorageCallbackImpl()
    private lateinit var mBackgroundListener: OnTransitionListener
    lateinit var mediator: ExternalLibrariesMediator
        private set
    lateinit var purchaseOperationObservable: PurchaseOperationObservable
        private set
    lateinit var mediaPlayer: MediaPlayerWrapper
        private set
    lateinit var geofenceRegistry: GeofenceRegistry
        private set
    var isFirstLaunch = false

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        setContext(this);

        sSelf = this

        LoggerFactory.INSTANCE.initialize(this)
        logger = LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        mBackgroundListener = AppBaseTransitionListener(this)
        logger.d(TAG, "Application is created")
        mMainLoopHandler = Handler(mainLooper)
        mediator = ExternalLibrariesMediator(this)
        mediator.initSensitiveDataToleranceLibraries()
        mediator.initSensitiveDataStrictLibrariesAsync()
        Statistics.INSTANCE.setMediator(mediator)
        mPrefs = getSharedPreferences(
            getString(R.string.pref_file_name),
            Context.MODE_PRIVATE
        )
        initNotificationChannels()
        mBackgroundTracker = AppBackgroundTracker()
        mBackgroundTracker!!.addListener(mVisibleAppLaunchListener)
        subwayManager = SubwayManager(this)
        connectivityListener = ConnectivityJobScheduler(this)
        connectivityListener.listen()
        purchaseOperationObservable = PurchaseOperationObservable()
        mediaPlayer = MediaPlayerWrapper(this)
        geofenceRegistry = GeofenceRegistryImpl(this)
    }

    private fun initNotificationChannels() {
        val channelProvider =
            createProvider(this)
        channelProvider.setUGCChannel()
        channelProvider.setDownloadingChannel()
    }

    /**
     * Initialize native core of application: platform and framework. Caller must handle returned value
     * and do nothing with native code if initialization is failed.
     *
     * @return boolean - indicator whether native initialization is successful or not.
     */
    fun initCore(): Boolean {
        initNativePlatform()
        if (!mPlatformInitialized) return false
        initNativeFramework()
        return mFrameworkInitialized
    }

    private fun initNativePlatform() {
        if (mPlatformInitialized) return
        val isInstallationIdFound = mediator.setInstallationIdToCrashlytics()
        val settingsPath = StorageUtils.getSettingsPath()
        logger.d(TAG, "onCreate(), setting path = $settingsPath")
        val filesPath = StorageUtils.getFilesPath(this)
        logger.d(TAG, "onCreate(), files path = $filesPath")
        val tempPath = StorageUtils.getTempPath(this)
        logger.d(TAG, "onCreate(), temp path = $tempPath")
        // If platform directories are not created it means that native part of app will not be able
// to work at all. So, we just ignore native part initialization in this case, e.g. when the
// external storage is damaged or not available (read-only).
        if (!createPlatformDirectories(settingsPath, filesPath, tempPath)) return
        // First we need initialize paths and platform to have access to settings and other components.
        nativeInitPlatform(
            StorageUtils.getApkPath(this),
            StorageUtils.getStoragePath(settingsPath),
            filesPath,
            tempPath,
            StorageUtils.getObbGooglePath(),
            BuildConfig.FLAVOR,
            BuildConfig.BUILD_TYPE,
            UiUtils.isTablet
        )
        Config.setStatisticsEnabled(SharedPropertiesUtils.isStatisticsEnabled)
        val s =
            Statistics.INSTANCE
        if (!isInstallationIdFound) mediator.setInstallationIdToCrashlytics()
        mBackgroundTracker!!.addListener(mBackgroundListener)
        init()
        init(this)
        mPlatformInitialized = true
    }

    private fun createPlatformDirectories(
        settingsPath: String, filesPath: String,
        tempPath: String
    ): Boolean {
        return if (SharedPropertiesUtils.shouldEmulateBadExternalStorage()) false else StorageUtils.createDirectory(
            settingsPath
        ) &&
                StorageUtils.createDirectory(filesPath) &&
                StorageUtils.createDirectory(tempPath)
    }

    private fun initNativeFramework() {
        if (mFrameworkInitialized) return
        nativeInitFramework()
        nativeSubscribe(mStorageCallbacks)
        initNativeStrings()
        SearchEngine.INSTANCE.initialize()
        loadBookmarks()
        TtsPlayer.INSTANCE.init(this)
        ThemeSwitcher.restart(false)
        LocationHelper.INSTANCE.initialize()
        RoutingController.get().initialize()
        TrafficManager.INSTANCE.initialize()
        SubwayManager.from(this).initialize()
        purchaseOperationObservable.initialize()
        mBackgroundTracker!!.addListener(this)
        mFrameworkInitialized = true
    }

    private fun initNativeStrings() {
        nativeAddLocalization(
            "core_entrance",
            getString(R.string.core_entrance)
        )
        nativeAddLocalization("core_exit", getString(R.string.core_exit))
        nativeAddLocalization(
            "core_my_places",
            getString(R.string.core_my_places)
        )
        nativeAddLocalization(
            "core_my_position",
            getString(R.string.core_my_position)
        )
        nativeAddLocalization(
            "core_placepage_unknown_place",
            getString(R.string.core_placepage_unknown_place)
        )
        nativeAddLocalization(
            "postal_code",
            getString(R.string.postal_code)
        )
        nativeAddLocalization("wifi", getString(R.string.wifi))
    }

    fun arePlatformAndCoreInitialized(): Boolean {
        return mFrameworkInitialized && mPlatformInitialized
    }

    val backgroundTracker: AppBackgroundTracker
        get() = mBackgroundTracker!!

    external fun nativeInitPlatform(
            apkPath: String, storagePath: String, privatePath: String,
            tmpPath: String, obbGooglePath: String, flavorName: String,
            buildType: String, isTablet: Boolean
    )

    companion object {
        const val TAG = "MwmApplication"


        private lateinit var context: Context

        fun setContext(con: Context) {
            context=con
        }

        private lateinit var sSelf: MwmApplication
        /**
         * Use the [.from] method instead.
         */
        @JvmStatic
        @Deprecated("")
        fun get(): MwmApplication {
            return sSelf
        }

        @JvmStatic
        fun from(context: Context): MwmApplication {
            return context.applicationContext as MwmApplication
        }

        /**
         *
         * Use [.backgroundTracker] instead.
         */
        @JvmStatic
        @Deprecated("")
        fun backgroundTracker(): AppBackgroundTracker? {
            return sSelf.mBackgroundTracker
        }

        @JvmStatic
        fun backgroundTracker(context: Context): AppBackgroundTracker {
            return (context.applicationContext as MwmApplication).backgroundTracker
        }

        /**
         *
         * Use [.prefs] instead.
         */
        @Deprecated("")
        @Synchronized
        @JvmStatic fun prefs(): SharedPreferences {
            return prefs(context)
        }


        @JvmStatic fun prefs(context: Context): SharedPreferences {
            val prefFile = context.getString(R.string.pref_file_name)
            return context.getSharedPreferences(prefFile, Context.MODE_PRIVATE)
        }

        @JvmStatic
        fun onUpgrade() {
            Counters.resetAppSessionCounters()
        }

        @JvmStatic private external fun nativeInitFramework()
        @JvmStatic private external fun nativeProcessTask(taskPointer: Long)
        @JvmStatic private external fun nativeAddLocalization(
            name: String,
            value: String
        )

        private @JvmStatic external fun nativeOnTransit(foreground: Boolean)

        init {
            System.loadLibrary("mapswithme")
        }
    }


    fun sendAppsFlyerTags(
        tag: String,
        params: Array<KeyValue>
    ) {
        val paramsMap =
            HashMap<String, Any>()
        for (p in params) paramsMap[p.mKey] = p.mValue
        AppsFlyerLib.getInstance().trackEvent(this, tag, paramsMap)
    }

    fun sendPushWooshTags(
        tag: String,
        values: Array<String>?
    ) {
        mediator.eventLogger.sendTags(tag, values)
    }

    fun forwardToMainThread(taskPointer: Long) {
        val m =
            Message.obtain(mMainLoopHandler) { nativeProcessTask(taskPointer) }
        m.obj = mMainQueueToken
        mMainLoopHandler!!.sendMessage(m)
    }



    override fun onTransit(foreground: Boolean) {
        nativeOnTransit(foreground)
    }

    private class VisibleAppLaunchListener : OnVisibleAppLaunchListener {
        override fun onVisibleAppLaunch() {
            Statistics.INSTANCE.trackColdStartupInfo()
        }
    }

    private inner class StorageCallbackImpl : StorageCallback {
        override fun onStatusChanged(data: List<StorageCallbackData>) {
            val notifier =
                Notifier.from(this@MwmApplication)
            for (item in data) if (item.isLeafNode && item.newStatus == CountryItem.STATUS_FAILED) {
                if (nativeIsAutoretryFailed()) {
                    notifier.notifyDownloadFailed(
                        item.countryId,
                        nativeGetName(item.countryId)
                    )
                    sendErrorStat(
                        Statistics.EventName.DOWNLOADER_ERROR,
                        nativeGetError(item.countryId)
                    )
                }
                return
            }
        }

        override fun onProgress(
            countryId: String,
            localSize: Long,
            remoteSize: Long
        ) {
        }
    }
    init {
        Log.v("started", true.toString())
    }
}