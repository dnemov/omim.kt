package com.mapswithme.maps

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.*
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import com.mapswithme.maps.MwmApplication.Companion.from
import com.mapswithme.maps.base.BaseMwmFragmentActivity
import com.mapswithme.maps.downloader.CountryItem
import com.mapswithme.maps.downloader.MapManager
import com.mapswithme.maps.intent.Factory
import com.mapswithme.maps.intent.MapTask
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.location.LocationListener
import com.mapswithme.util.*
import com.mapswithme.util.log.LoggerFactory

@SuppressLint("StringFormatMatches")
class DownloadResourcesLegacyActivity : BaseMwmFragmentActivity() {

    private var mTvMessage: TextView? = null
    private var mTvLocation: TextView? = null
    private var mProgress: ProgressBar? = null
    private var mBtnDownload: Button? = null
    private var mChbDownloadCountry: CheckBox? = null

    private var mCurrentCountry: String? = null
    private var mMapTaskToForward: MapTask? = null

    private var mAreResourcesDownloaded: Boolean = false

    private var mBtnListeners: Array<View.OnClickListener>? = null
    private var mBtnNames: Array<String>? = null

    private var mCountryDownloadListenerSlot: Int = 0

    private val mIntentProcessors = arrayOf(
        Factory.createGeoIntentProcessor(),
        Factory.createHttpGe0IntentProcessor(),
        Factory.createGe0IntentProcessor(),
        Factory.createMapsWithMeIntentProcessor(),
        Factory.createGoogleMapsIntentProcessor(),
        Factory.createOldLeadUrlProcessor(),
        Factory.createDlinkBookmarkCatalogueProcessor(),
        Factory.createMapsmeBookmarkCatalogueProcessor(),
        Factory.createDlinkBookmarkGuidesPageProcessor(),
        Factory.createDlinkBookmarksSubscriptionProcessor(),
        Factory.createOldCoreLinkAdapterProcessor(),
        Factory.createOpenCountryTaskProcessor(),
        Factory.createMapsmeProcessor(),
        Factory.createKmzKmlProcessor(this),
        Factory.createShowOnMapProcessor(),
        Factory.createBuildRouteProcessor()
    )

    private val mLocationListener = object : LocationListener.Simple() {
        override fun onLocationUpdated(location: Location) {
            if (mCurrentCountry != null)
                return

            val lat = location.latitude
            val lon = location.longitude
            mCurrentCountry = MapManager.nativeFindCountry(lat, lon)
            if (TextUtils.isEmpty(mCurrentCountry)) {
                mCurrentCountry = null
                return
            }

            val status = MapManager.nativeGetStatus(mCurrentCountry)
            val name = MapManager.nativeGetName(mCurrentCountry)

            UiUtils.show(mTvLocation!!)

            if (status == CountryItem.STATUS_DONE)
                mTvLocation!!.text = String.format(getString(R.string.download_location_map_up_to_date), name)
            else {
                val checkBox = findViewById<View>(R.id.chb__download_country) as CheckBox
                UiUtils.show(checkBox)

                val locationText: String
                val checkBoxText: String

                if (status == CountryItem.STATUS_UPDATABLE) {
                    locationText = getString(R.string.download_location_update_map_proposal)
                    checkBoxText = String.format(getString(R.string.update_country_ask), name)
                } else {
                    locationText = getString(R.string.download_location_map_proposal)
                    checkBoxText = String.format(getString(R.string.download_country_ask), name)
                }

                mTvLocation!!.text = locationText
                checkBox.text = checkBoxText
            }

            LocationHelper.INSTANCE.removeListener(this)
        }
    }

    private val mResourcesDownloadListener = object : Listener {
        override fun onProgress(percent: Int) {
            if (!isFinishing)
                mProgress!!.progress = percent
        }

        override fun onFinish(errorCode: Int) {
            if (isFinishing)
                return

            if (errorCode == ERR_DOWNLOAD_SUCCESS) {
                val res = nativeStartNextFileDownload(this)
                if (res == ERR_NO_MORE_FILES)
                    finishFilesDownload(res)
            } else
                finishFilesDownload(errorCode)
        }
    }

    private val mCountryDownloadListener = object : MapManager.StorageCallback {
        override fun onStatusChanged(data: List<MapManager.StorageCallbackData>) {
            for (item in data) {
                if (!item.isLeafNode)
                    continue

                when (item.newStatus) {
                    CountryItem.STATUS_DONE -> {
                        mAreResourcesDownloaded = true
                        showMap()
                        return
                    }

                    CountryItem.STATUS_FAILED -> {
                        MapManager.showError(this@DownloadResourcesLegacyActivity, item, null)
                        return
                    }
                }
            }
        }

        override fun onProgress(countryId: String, localSize: Long, remoteSize: Long) {
            mProgress!!.progress = localSize.toInt()
        }
    }

    private interface Listener {
        fun onProgress(percent: Int)
        fun onFinish(errorCode: Int)
    }

    @CallSuper
    override fun onSafeCreate(savedInstanceState: Bundle?) {
        super.onSafeCreate(savedInstanceState)
        setContentView(R.layout.activity_download_resources)
        initViewsAndListeners()

        if (prepareFilesDownload(false)) {
            Utils.keepScreenOn(true, window)
            suggestRemoveLiteOrSamsung()

            setAction(DOWNLOAD)

            if (ConnectionState.isWifiConnected)
                onDownloadClicked()

            return
        }

        mMapTaskToForward = processIntent()
        showMap()
    }

    @CallSuper
    override fun onSafeDestroy() {
        super.onSafeDestroy()
        Utils.keepScreenOn(false, window)
        if (mCountryDownloadListenerSlot != 0) {
            MapManager.nativeUnsubscribe(mCountryDownloadListenerSlot)
            mCountryDownloadListenerSlot = 0
        }
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        if (!isFinishing)
            LocationHelper.INSTANCE.addListener(mLocationListener, true)
    }

    override fun onPause() {
        super.onPause()
        LocationHelper.INSTANCE.removeListener(mLocationListener)
    }

    private fun suggestRemoveLiteOrSamsung() {
        if (Utils.isPackageInstalled(Constants.Package.MWM_LITE_PACKAGE) || Utils.isPackageInstalled(Constants.Package.MWM_SAMSUNG_PACKAGE))
            Toast.makeText(this, R.string.suggest_uninstall_lite, Toast.LENGTH_LONG).show()
    }

    private fun setDownloadMessage(bytesToDownload: Int) {
        mTvMessage!!.text =
            getString(R.string.download_resources, StringUtils.getFileSizeString(bytesToDownload.toLong()))
    }

    private fun prepareFilesDownload(showMap: Boolean): Boolean {
        val bytes = nativeGetBytesToDownload()
        if (bytes == 0) {
            mAreResourcesDownloaded = true
            if (showMap)
                showMap()

            return false
        }

        if (bytes > 0) {
            setDownloadMessage(bytes)

            mProgress!!.max = bytes
            mProgress!!.progress = 0
        } else
            finishFilesDownload(bytes)

        return true
    }

    private fun initViewsAndListeners() {
        mTvMessage = findViewById<View>(R.id.tv__download_message) as TextView
        mProgress = findViewById<View>(R.id.pb__download_resources) as ProgressBar
        mBtnDownload = findViewById<View>(R.id.btn__download_resources) as Button
        mChbDownloadCountry = findViewById<View>(R.id.chb__download_country) as CheckBox
        mTvLocation = findViewById<View>(R.id.tv__location) as TextView

        val btnListeners = ArrayList<View.OnClickListener>()
        val btnNames = ArrayList<String>()

        btnListeners.add(DOWNLOAD, View.OnClickListener { onDownloadClicked() })
        btnNames.add(DOWNLOAD, getString(R.string.download))

        btnListeners.add(PAUSE, View.OnClickListener { onPauseClicked() })
        btnNames.add(PAUSE, getString(R.string.pause))

        btnListeners.add(RESUME, View.OnClickListener { onResumeClicked() })
        btnNames.add(RESUME, getString(R.string.continue_download))

        btnListeners.add(TRY_AGAIN, View.OnClickListener { onTryAgainClicked() })
        btnNames.add(TRY_AGAIN, getString(R.string.try_again))

        btnListeners.add(PROCEED_TO_MAP, View.OnClickListener { onProceedToMapClicked() })
        btnNames.add(PROCEED_TO_MAP, getString(R.string.download_resources_continue))

        mBtnListeners = btnListeners.toTypedArray()
        mBtnNames = btnNames.toTypedArray()
    }

    private fun setAction(action: Int) {
        mBtnDownload!!.setOnClickListener(mBtnListeners!![action])
        mBtnDownload!!.text = mBtnNames!![action]
    }

    private fun doDownload() {
        if (nativeStartNextFileDownload(mResourcesDownloadListener) == ERR_NO_MORE_FILES)
            finishFilesDownload(ERR_NO_MORE_FILES)
    }

    private fun onDownloadClicked() {
        setAction(PAUSE)
        doDownload()
    }

    private fun onPauseClicked() {
        setAction(RESUME)
        nativeCancelCurrentFile()
    }

    private fun onResumeClicked() {
        setAction(PAUSE)
        doDownload()
    }

    private fun onTryAgainClicked() {
        if (prepareFilesDownload(true)) {
            setAction(PAUSE)
            doDownload()
        }
    }

    private fun onProceedToMapClicked() {
        mAreResourcesDownloaded = true
        showMap()
    }

    fun showMap() {
        if (!mAreResourcesDownloaded)
            return

        val intent = Intent(this, MwmActivity::class.java)

        // Disable animation because MwmActivity should appear exactly over this one
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        // Add saved task to forward to map activity.
        if (mMapTaskToForward != null) {
            intent.putExtra(MwmActivity.EXTRA_TASK, mMapTaskToForward)
            intent.putExtra(MwmActivity.EXTRA_LAUNCH_BY_DEEP_LINK, true)
            mMapTaskToForward = null
        }

        startActivity(intent)

        finish()
    }

    private fun finishFilesDownload(result: Int) {
        if (result == ERR_NO_MORE_FILES) {
            // World and WorldCoasts has been downloaded, we should register maps again to correctly add them to the model and generate indexes etc.
            // TODO fix the hack when separate download of World-s will be removed or refactored
            Framework.nativeDeregisterMaps()
            Framework.nativeRegisterMaps()
            if (mCurrentCountry != null && mChbDownloadCountry!!.isChecked) {
                val item = CountryItem.fill(mCurrentCountry)

                UiUtils.hide(mChbDownloadCountry!!, mTvLocation!!)
                mTvMessage!!.text = getString(R.string.downloading_country_can_proceed, item.name)
                mProgress!!.max = item.totalSize.toInt()
                mProgress!!.progress = 0

                mCountryDownloadListenerSlot = MapManager.nativeSubscribe(mCountryDownloadListener)
                MapManager.nativeDownload(mCurrentCountry)
                setAction(PROCEED_TO_MAP)
            } else {
                mAreResourcesDownloaded = true
                mMapTaskToForward = processIntent()
                showMap()
            }
        } else {
            mTvMessage!!.setText(getErrorMessage(result))
            mTvMessage!!.setTextColor(Color.RED)
            setAction(TRY_AGAIN)
        }
    }

    private fun processIntent(): MapTask? {
        val intent = intent ?: return null
        val application = from(this)
        intent.putExtra(
            Factory.EXTRA_IS_FIRST_LAUNCH,
            application.isFirstLaunch
        )
        if (intent.data == null) {
            val firstLaunchDeeplink =
                application.mediator.retrieveFirstLaunchDeeplink()
            if (!TextUtils.isEmpty(firstLaunchDeeplink)) intent.data = Uri.parse(
                firstLaunchDeeplink
            )
        }
        var mapTaskToForward: MapTask? = null
        for (ip in mIntentProcessors) {
            if (ip.isSupported(intent) && ip.process(intent).also {
                    mapTaskToForward = it
                } != null) return mapTaskToForward
        }
        return null
    }

    companion object {
        private val LOGGER = LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.DOWNLOADER)
        private val TAG = DownloadResourcesLegacyActivity::class.java.name

        val EXTRA_COUNTRY = "country"

        // Error codes, should match the same codes in JNI
        private val ERR_DOWNLOAD_SUCCESS = 0
        private val ERR_NOT_ENOUGH_MEMORY = -1
        private val ERR_NOT_ENOUGH_FREE_SPACE = -2
        private val ERR_STORAGE_DISCONNECTED = -3
        private val ERR_DOWNLOAD_ERROR = -4
        private val ERR_NO_MORE_FILES = -5
        private val ERR_FILE_IN_PROGRESS = -6

        private val DOWNLOAD = 0
        private val PAUSE = 1
        private val RESUME = 2
        private val TRY_AGAIN = 3
        private val PROCEED_TO_MAP = 4
        private val BTN_COUNT = 5

        @StringRes
        private fun getErrorMessage(res: Int): Int {
            when (res) {
                ERR_NOT_ENOUGH_FREE_SPACE -> return R.string.not_enough_free_space_on_sdcard

                ERR_STORAGE_DISCONNECTED -> return R.string.disconnect_usb_cable

                ERR_DOWNLOAD_ERROR -> return if (ConnectionState.isConnected)
                    R.string.download_has_failed
                else
                    R.string.common_check_internet_connection_dialog
                else -> return R.string.not_enough_memory
            }
        }

        @JvmStatic private external fun nativeGetBytesToDownload(): Int
        @JvmStatic private external fun nativeStartNextFileDownload(listener: Listener): Int
        @JvmStatic private external fun nativeCancelCurrentFile()
    }
}
