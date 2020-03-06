package com.mapswithme.maps.downloader

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentActivity
import com.mapswithme.maps.Framework
import com.mapswithme.maps.Framework.DoAfterUpdate
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmDialogFragment
import com.mapswithme.maps.downloader.CountryItem
import com.mapswithme.maps.downloader.MapManager.StorageCallback
import com.mapswithme.maps.downloader.MapManager.StorageCallbackData
import com.mapswithme.maps.downloader.MapManager.nativeCancel
import com.mapswithme.maps.downloader.MapManager.nativeGetAttributes
import com.mapswithme.maps.downloader.MapManager.nativeGetCurrentDownloadingCountryId
import com.mapswithme.maps.downloader.MapManager.nativeGetName
import com.mapswithme.maps.downloader.MapManager.nativeGetUpdateInfo
import com.mapswithme.maps.downloader.MapManager.nativeIsDownloading
import com.mapswithme.maps.downloader.MapManager.nativeSubscribe
import com.mapswithme.maps.downloader.MapManager.nativeUnsubscribe
import com.mapswithme.maps.downloader.MapManager.nativeUpdate
import com.mapswithme.maps.downloader.MapManager.showErrorDialog
import com.mapswithme.maps.downloader.MapManager.warnOn3gUpdate
import com.mapswithme.maps.onboarding.BaseNewsFragment.NewsDialogListener
import com.mapswithme.maps.widget.WheelProgressView
import com.mapswithme.util.Constants
import com.mapswithme.util.StringUtils
import com.mapswithme.util.UiUtils
import com.mapswithme.util.Utils
import com.mapswithme.util.Utils.Proc
import com.mapswithme.util.log.LoggerFactory
import com.mapswithme.util.statistics.Statistics
import com.mapswithme.util.statistics.Statistics.EventName
import java.util.*

class UpdaterDialogFragment : BaseMwmDialogFragment() {
    private var mTitle: TextView? = null
    private var mUpdateBtn: TextView? = null
    private var mProgressBar: WheelProgressView? = null
    private var mFinishBtn: TextView? = null
    private var mInfo: View? = null
    private var mRelativeStatus: TextView? = null
    private var mTotalSize: String? = null
    var totalSizeBytes: Long = 0
        private set
    private var mAutoUpdate = false
    private var mOutdatedMaps: Array<String>? = null
    /**
     * Stores maps which are left to finish autoupdating process.
     */
    private var mLeftoverMaps: HashSet<String>? = null
    private var mDoneListener: NewsDialogListener? = null
    private var mStorageCallback: DetachableStorageCallback? = null
    private var mProcessedMapId: String? = null
    @StringRes
    private var mCommonStatusResId = Utils.INVALID_ID

    private fun finish() {
        dismiss()
        if (mDoneListener != null) mDoneListener!!.onDialogDone()
    }

    private val mFinishClickListener =
        View.OnClickListener { v: View? ->
            Statistics.INSTANCE.trackDownloaderDialogEvent(
                if (mAutoUpdate) EventName.DOWNLOADER_DIALOG_HIDE else EventName.DOWNLOADER_DIALOG_LATER,
                0
            )
            finish()
        }
    private val mCancelClickListener =
        View.OnClickListener { v: View? ->
            Statistics.INSTANCE.trackDownloaderDialogEvent(
                EventName.DOWNLOADER_DIALOG_CANCEL,
                totalSizeBytes / Constants.MB
            )
            nativeCancel(CountryItem.rootId)
            val info = nativeGetUpdateInfo(CountryItem.rootId)
            if (info == null) {
                finish()
            } else {
                updateTotalSizes(info.totalSize)
                mAutoUpdate = false
                mOutdatedMaps = Framework.nativeGetOutdatedCountries()
                if (mStorageCallback != null) mStorageCallback!!.detach()
                mStorageCallback = DetachableStorageCallback(this, mLeftoverMaps, mOutdatedMaps)
                mStorageCallback!!.attach(this)
                initViews()
            }

        }
    private val mUpdateClickListener =
        View.OnClickListener { v: View? ->
            warnOn3gUpdate(
                activity,
                CountryItem.rootId,
                object : Runnable {
                    override fun run() {
                        mAutoUpdate = true
                        mFinishBtn!!.text = getString(R.string.downloader_hide_screen)
                        mTitle!!.text = getString(R.string.whats_new_auto_update_updating_maps)
                        setProgress(0, 0, totalSizeBytes)
                        setCommonStatus(mProcessedMapId, mCommonStatusResId)
                        nativeUpdate(CountryItem.rootId)
                        UiUtils.show(mProgressBar, mInfo)
                        UiUtils.hide(mUpdateBtn!!)
                        Statistics.INSTANCE.trackDownloaderDialogEvent(
                            EventName.DOWNLOADER_DIALOG_MANUAL_DOWNLOAD,
                            totalSizeBytes / Constants.MB
                        )
                    }
                })
        }

    override val customTheme: Int
        protected get() = super.fullscreenTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) { // As long as we use HashSet to store leftover maps this cast is safe.
            mLeftoverMaps =
                null//savedInstanceState.getSerializable(EXTRA_LEFTOVER_MAPS)
            mProcessedMapId =
                savedInstanceState.getString(EXTRA_PROCESSED_MAP_ID)
            mCommonStatusResId =
                savedInstanceState.getInt(EXTRA_COMMON_STATUS_RES_ID)
            mAutoUpdate =
                savedInstanceState.getBoolean(ARG_UPDATE_IMMEDIATELY)
            mTotalSize =
                savedInstanceState.getString(ARG_TOTAL_SIZE)
            totalSizeBytes =
                savedInstanceState.getLong(ARG_TOTAL_SIZE_BYTES, 0L)
            mOutdatedMaps =
                savedInstanceState.getStringArray(ARG_OUTDATED_MAPS)
        } else {
            readArguments()
        }
        mStorageCallback = DetachableStorageCallback(this, mLeftoverMaps, mOutdatedMaps)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(EXTRA_LEFTOVER_MAPS, mLeftoverMaps)
        outState.putString(EXTRA_PROCESSED_MAP_ID, mProcessedMapId)
        outState.putInt(
            EXTRA_COMMON_STATUS_RES_ID,
            mCommonStatusResId
        )
        outState.putBoolean(ARG_UPDATE_IMMEDIATELY, mAutoUpdate)
        outState.putString(ARG_TOTAL_SIZE, mTotalSize)
        outState.putLong(ARG_TOTAL_SIZE_BYTES, totalSizeBytes)
        outState.putStringArray(ARG_OUTDATED_MAPS, mOutdatedMaps)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val res = super.onCreateDialog(savedInstanceState)
        res.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val content =
            View.inflate(activity, R.layout.fragment_updater, null)
        res.setContentView(content)
        mTitle = content.findViewById(R.id.title)
        mUpdateBtn = content.findViewById(R.id.update_btn)
        mProgressBar = content.findViewById(R.id.progress)
        mFinishBtn = content.findViewById(R.id.later_btn)
        mInfo = content.findViewById(R.id.info)
        mRelativeStatus = content.findViewById(R.id.relative_status)
        initViews()
        return res
    }

    override fun onResume() {
        super.onResume()
        // The storage callback must be non-null at this point.
        mStorageCallback!!.attach(this)
        mProgressBar!!.setOnClickListener(mCancelClickListener)
        if (isAllUpdated || Framework.nativeGetOutdatedCountries().size == 0) {
            finish()
            return
        }
        if (mAutoUpdate) {
            if (!nativeIsDownloading()) {
                warnOn3gUpdate(
                    activity,
                    CountryItem.rootId,
                    object : Runnable {
                        override fun run() {
                            nativeUpdate(CountryItem.rootId)
                            Statistics.INSTANCE.trackDownloaderDialogEvent(
                                EventName.DOWNLOADER_DIALOG_DOWNLOAD,
                                totalSizeBytes / Constants.MB
                            )
                        }
                    })
            } else {
                val root = CountryItem(CountryItem.rootId)
                nativeGetAttributes(root)
                updateTotalSizes(root.bytesToDownload)
                setProgress(root.progress, root.downloadedBytes, root.bytesToDownload)
                updateProcessedMapInfo()
                setCommonStatus(mProcessedMapId, mCommonStatusResId)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mProgressBar!!.setOnClickListener(null)
        if (mStorageCallback != null) mStorageCallback!!.detach()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onCancel(dialog: DialogInterface) {
        if (nativeIsDownloading()) nativeCancel(CountryItem.rootId)
        if (mDoneListener != null) mDoneListener!!.onDialogDone()
        super.onCancel(dialog)
    }

    private fun readArguments() {
        val args = arguments ?: return
        mAutoUpdate = args.getBoolean(ARG_UPDATE_IMMEDIATELY)
        if (!mAutoUpdate && nativeIsDownloading()) mAutoUpdate = true
        mTotalSize = args.getString(ARG_TOTAL_SIZE)
        totalSizeBytes = args.getLong(ARG_TOTAL_SIZE_BYTES, 0L)
        mOutdatedMaps = args.getStringArray(ARG_OUTDATED_MAPS)
        if (mLeftoverMaps == null && mOutdatedMaps != null && mOutdatedMaps!!.size > 0) {
            mLeftoverMaps =
                HashSet(Arrays.asList(*mOutdatedMaps!!))
        }
    }

    private fun initViews() {
        UiUtils.showIf(mAutoUpdate, mProgressBar, mInfo)
        UiUtils.showIf(!mAutoUpdate, mUpdateBtn)
        mUpdateBtn!!.text = getString(R.string.whats_new_auto_update_button_size, mTotalSize)
        mUpdateBtn!!.setOnClickListener(mUpdateClickListener)
        mFinishBtn!!.text =
            if (mAutoUpdate) getString(R.string.downloader_hide_screen) else getString(
                R.string.whats_new_auto_update_button_later
            )
        mFinishBtn!!.setOnClickListener(mFinishClickListener)
        mProgressBar!!.post { mProgressBar!!.isPending = true }
        if (mAutoUpdate) setCommonStatus(mProcessedMapId, mCommonStatusResId) else mTitle!!.text =
            getString(R.string.whats_new_auto_update_title)
    }

    private val isAllUpdated: Boolean
        private get() = mOutdatedMaps == null || mLeftoverMaps == null || mLeftoverMaps!!.isEmpty()

    fun getRelativeStatusFormatted(
        progress: Int,
        localSize: Long,
        remoteSize: Long
    ): String {
        return getString(
            R.string.downloader_percent,
            "$progress%",
            (localSize / Constants.MB).toString() + getString(R.string.mb),
            StringUtils.getFileSizeString(remoteSize)
        )
    }

    fun setProgress(progress: Int, localSize: Long, remoteSize: Long) {
        if (mProgressBar!!.isPending) mProgressBar!!.isPending = false
        mProgressBar!!.progress = progress
        mRelativeStatus!!.text = getRelativeStatusFormatted(progress, localSize, remoteSize)
    }

    fun setCommonStatus(mwmId: String?, @StringRes mwmStatusResId: Int) {
        if (mwmId == null || mwmStatusResId == Utils.INVALID_ID) return
        mProcessedMapId = mwmId
        mCommonStatusResId = mwmStatusResId
        val status = getString(mwmStatusResId, nativeGetName(mwmId))
        mTitle!!.text = status
    }

    fun updateTotalSizes(totalSize: Long) {
        mTotalSize = StringUtils.getFileSizeString(totalSize)
        totalSizeBytes = totalSize
    }

    fun updateProcessedMapInfo() {
        mProcessedMapId = nativeGetCurrentDownloadingCountryId()
        if (mProcessedMapId == null) return
        val processedCountryItem = CountryItem(mProcessedMapId)
        nativeGetAttributes(processedCountryItem)
        mCommonStatusResId = when (processedCountryItem.status) {
            CountryItem.STATUS_PROGRESS -> R.string.downloader_process
            CountryItem.STATUS_APPLYING -> R.string.downloader_applying
            else -> Utils.INVALID_ID
        }
    }

    private class DetachableStorageCallback internal constructor(
        private var mFragment: UpdaterDialogFragment?,
        private val mLeftoverMaps: MutableSet<String>?,
        private val mOutdatedMaps: Array<String>?
    ) : StorageCallback {
        private var mListenerSlot = 0
        override fun onStatusChanged(data: List<StorageCallbackData>) {
            var mwmId: String? = null
            @StringRes var mwmStatusResId = 0
            for (item in data) {
                if (!item.isLeafNode) continue
                when (item.newStatus) {
                    CountryItem.STATUS_FAILED -> {
                        showErrorDialog(item)
                        return
                    }
                    CountryItem.STATUS_DONE -> {
                        LOGGER.i(
                            TAG,
                            "Update finished for: " + item.countryId
                        )
                        mLeftoverMaps?.remove(item.countryId)
                    }
                    CountryItem.STATUS_PROGRESS -> {
                        mwmId = item.countryId
                        mwmStatusResId = R.string.downloader_process
                    }
                    CountryItem.STATUS_APPLYING -> {
                        mwmId = item.countryId
                        mwmStatusResId = R.string.downloader_applying
                    }
                    else -> LOGGER.d(
                        TAG, "Ignored status: " + item.newStatus +
                                ".For country: " + item.countryId
                    )
                }
            }
            if (isFragmentAttached) {
                mFragment!!.setCommonStatus(mwmId, mwmStatusResId)
                if (mFragment!!.isAllUpdated) mFragment!!.finish()
            }
        }

        private fun showErrorDialog(item: StorageCallbackData) {
            if (!isFragmentAttached) return
            val text: String
            text = when (item.errorCode) {
                CountryItem.ERROR_NO_INTERNET -> mFragment!!.getString(R.string.common_check_internet_connection_dialog)
                CountryItem.ERROR_OOM -> mFragment!!.getString(R.string.downloader_no_space_title)
                else -> item.errorCode.toString()
            }
            Statistics.INSTANCE.trackDownloaderDialogError(
                mFragment!!.totalSizeBytes / Constants.MB,
                text
            )
            showErrorDialog(
                mFragment!!.activity,
                item,
                object : Proc<Boolean> {
                    override fun invoke(result: Boolean) {
                        if (!isFragmentAttached) return
                        if (result) {
                            warnOn3gUpdate(
                                mFragment!!.activity,
                                CountryItem.rootId,
                                Runnable { nativeUpdate(CountryItem.rootId) })
                        } else {
                            nativeCancel(CountryItem.rootId)
                            mFragment!!.finish()
                        }
                    }
                })
        }

        override fun onProgress(
            countryId: String,
            localSizeBytes: Long,
            remoteSizeBytes: Long
        ) {
            if (!isFragmentAttached) return
            val root = CountryItem(CountryItem.rootId)
            nativeGetAttributes(root)
            mFragment!!.setProgress(root.progress, root.downloadedBytes, root.bytesToDownload)
        }

        fun attach(fragment: UpdaterDialogFragment) {
            mFragment = fragment
            mListenerSlot = nativeSubscribe(this)
        }

        fun detach() {
            if (mFragment == null) throw AssertionError("detach() should be called after attach() and only once")
            mFragment = null
            nativeUnsubscribe(mListenerSlot)
        }

        private val isFragmentAttached: Boolean
            private get() = mFragment != null && mFragment!!.isAdded

    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.DOWNLOADER)
        private val TAG = UpdaterDialogFragment::class.java.simpleName
        private const val EXTRA_LEFTOVER_MAPS = "extra_leftover_maps"
        private const val EXTRA_PROCESSED_MAP_ID = "extra_processed_map_id"
        private const val EXTRA_COMMON_STATUS_RES_ID = "extra_common_status_res_id"
        private const val ARG_UPDATE_IMMEDIATELY = "arg_update_immediately"
        private const val ARG_TOTAL_SIZE = "arg_total_size"
        private const val ARG_TOTAL_SIZE_BYTES = "arg_total_size_bytes"
        private const val ARG_OUTDATED_MAPS = "arg_outdated_maps"
        @JvmStatic
        fun showOn(
            activity: FragmentActivity,
            doneListener: NewsDialogListener?
        ): Boolean {
            val fm = activity.supportFragmentManager
            if (fm.isDestroyed) return false
            val info = nativeGetUpdateInfo(CountryItem.rootId) ?: return false
            @DoAfterUpdate val result: Int
            val f =
                fm.findFragmentByTag(UpdaterDialogFragment::class.java.name)
            if (f != null) {
                (f as UpdaterDialogFragment).mDoneListener = doneListener
                return true
            } else {
                result = Framework.nativeToDoAfterUpdate()
                if (result == Framework.DO_AFTER_UPDATE_NOTHING) return false
                Statistics.INSTANCE.trackDownloaderDialogEvent(
                    EventName.DOWNLOADER_DIALOG_SHOW,
                    info.totalSize / Constants.MB
                )
            }
            val args = Bundle()
            args.putBoolean(
                ARG_UPDATE_IMMEDIATELY,
                result == Framework.DO_AFTER_UPDATE_AUTO_UPDATE
            )
            args.putString(
                ARG_TOTAL_SIZE,
                StringUtils.getFileSizeString(info.totalSize)
            )
            args.putLong(ARG_TOTAL_SIZE_BYTES, info.totalSize)
            args.putStringArray(
                ARG_OUTDATED_MAPS,
                Framework.nativeGetOutdatedCountries()
            )
            val fragment = UpdaterDialogFragment()
            fragment.arguments = args
            fragment.mDoneListener = doneListener
            val transaction = fm.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            fragment.show(transaction, UpdaterDialogFragment::class.java.name)
            return true
        }
    }
}