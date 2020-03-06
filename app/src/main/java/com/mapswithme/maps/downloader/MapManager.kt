package com.mapswithme.maps.downloader

import android.app.Activity
import android.text.TextUtils
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.mapswithme.maps.R
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Utils.Proc
import com.mapswithme.util.statistics.Statistics
import java.lang.ref.WeakReference

object MapManager {
    private var sCurrentErrorDialog: WeakReference<AlertDialog>? =
        null

    @kotlin.jvm.JvmStatic
    fun sendErrorStat(event: String?, code: Int) {
        val text: String
        text = when (code) {
            CountryItem.Companion.ERROR_NO_INTERNET -> "no_connection"
            CountryItem.Companion.ERROR_OOM -> "no_space"
            else -> "unknown_error"
        }
        Statistics.INSTANCE.trackEvent(
            event!!,
            Statistics.params().add(
                Statistics.EventParam.TYPE,
                text
            )
        )
    }

    @kotlin.jvm.JvmStatic
    fun showError(
        activity: Activity?, errorData: StorageCallbackData,
        dialogClickListener: Proc<Boolean>?
    ) {
        if (!nativeIsAutoretryFailed()) return
        showErrorDialog(activity, errorData, dialogClickListener)
    }

    fun showErrorDialog(
        activity: Activity?, errorData: StorageCallbackData,
        dialogClickListener: Proc<Boolean>?
    ) {
        if (sCurrentErrorDialog != null) {
            val dlg = sCurrentErrorDialog!!.get()
            if (dlg != null && dlg.isShowing) return
        }
        @StringRes val text: Int
        text = when (errorData.errorCode) {
            CountryItem.Companion.ERROR_NO_INTERNET -> R.string.common_check_internet_connection_dialog
            CountryItem.Companion.ERROR_OOM -> R.string.downloader_no_space_title
            else -> throw IllegalArgumentException("Given error can not be displayed: " + errorData.errorCode)
        }
        val dlg =
            AlertDialog.Builder(activity!!)
                .setTitle(R.string.country_status_download_failed)
                .setMessage(text)
                .setNegativeButton(
                    android.R.string.cancel
                ) { dialog, which ->
                    sCurrentErrorDialog = null
                    dialogClickListener?.invoke(false)
                }
                .setPositiveButton(
                    R.string.downloader_retry
                ) { dialog, which ->
                    val app = activity.application
                    val listener: RetryFailedDownloadConfirmationListener =
                        ExpandRetryConfirmationListener(app, dialogClickListener)
                    warn3gAndRetry(activity, errorData.countryId, listener)
                }.create()
        dlg.setCanceledOnTouchOutside(false)
        dlg.show()
        sCurrentErrorDialog =
            WeakReference(dlg)
    }

    private fun notifyNoSpaceInternal(activity: Activity?) {
        AlertDialog.Builder(activity!!)
            .setTitle(R.string.downloader_no_space_title)
            .setMessage(R.string.downloader_no_space_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    /**
     * @return true if there is no space to update the given `root`, so the alert dialog will be shown.
     */
    private fun notifyNoSpaceToUpdate(activity: Activity?, root: String?): Boolean {
        if (nativeHasSpaceToUpdate(root)) return false
        notifyNoSpaceInternal(activity)
        return true
    }

    /**
     * @return true if there is no space to download the given `root`, so the alert dialog will be shown.
     */
    private fun notifyNoSpace(activity: Activity?, root: String?): Boolean {
        if (nativeHasSpaceToDownloadCountry(root)) return false
        notifyNoSpaceInternal(activity)
        return true
    }

    /**
     * @return true if there is no space to download `size` bytes, so the alert dialog will be shown.
     */
    private fun notifyNoSpace(activity: Activity, size: Long): Boolean {
        if (nativeHasSpaceToDownloadAmount(size)) return false
        notifyNoSpaceInternal(activity)
        return true
    }

    private fun warnOn3gInternal(
        activity: Activity?,
        onAcceptListener: Runnable
    ): Boolean {
        if (nativeIsDownloadOn3gEnabled() || !ConnectionState.isMobileConnected) {
            onAcceptListener.run()
            return false
        }
        AlertDialog.Builder(activity!!)
            .setMessage(
                String.format(
                    "%1\$s\n\n%2\$s", activity.getString(R.string.download_over_mobile_header),
                    activity.getString(R.string.download_over_mobile_message)
                )
            )
            .setNegativeButton(android.R.string.no, null)
            .setPositiveButton(android.R.string.yes) { dlg, which ->
                nativeEnableDownloadOn3g()
                onAcceptListener.run()
            }.show()
        return true
    }

    fun warnOn3gUpdate(
        activity: Activity?,
        countryId: String?,
        onAcceptListener: Runnable
    ): Boolean {
        return if (TextUtils.isEmpty(countryId) || !notifyNoSpaceToUpdate(
                activity,
                countryId
            )
        ) warnOn3gInternal(activity, onAcceptListener) else true
    }

    fun warnOn3g(
        activity: Activity?,
        countryId: String?,
        onAcceptListener: Runnable
    ): Boolean {
        return if (TextUtils.isEmpty(countryId) || !notifyNoSpace(
                activity,
                countryId
            )
        ) warnOn3gInternal(activity, onAcceptListener) else true
    }

    @kotlin.jvm.JvmStatic
    fun warnOn3g(
        activity: Activity,
        size: Long,
        onAcceptListener: Runnable
    ): Boolean {
        return !notifyNoSpace(activity, size) && warnOn3gInternal(
            activity,
            onAcceptListener
        )
    }

    @kotlin.jvm.JvmStatic
    fun warn3gAndDownload(
        activity: Activity?,
        countryId: String?,
        onAcceptListener: Runnable?
    ): Boolean {
        return warnOn3g(activity, countryId, Runnable {
            onAcceptListener?.run()
            nativeDownload(countryId)
        })
    }

    fun warn3gAndRetry(
        activity: Activity?,
        countryId: String?,
        onAcceptListener: Runnable?
    ): Boolean {
        return warnOn3g(activity, countryId, Runnable {
            onAcceptListener?.run()
            nativeRetry(countryId)
        })
    }

    /**
     * Retrieves ID of root node.
     */
    @JvmStatic external fun nativeGetRoot(): String?

    /**
     * Moves a file from one place to another.
     */
    @JvmStatic external fun nativeMoveFile(oldFile: String?, newFile: String?): Boolean

    /**
     * Returns `true` if there is enough storage space to download specified amount of data. Or `false` otherwise.
     */
    @JvmStatic external fun nativeHasSpaceToDownloadAmount(bytes: Long): Boolean

    /**
     * Returns `true` if there is enough storage space to download maps with specified `root`. Or `false` otherwise.
     */
    @JvmStatic external fun nativeHasSpaceToDownloadCountry(root: String?): Boolean

    /**
     * Returns `true` if there is enough storage space to update maps with specified `root`. Or `false` otherwise.
     */
    @JvmStatic external fun nativeHasSpaceToUpdate(root: String?): Boolean

    /**
     * Return count of fully downloaded maps (excluding fake MWMs).
     */
    @JvmStatic external fun nativeGetDownloadedCount(): Int

    /**
     * Returns info about updatable data under given `root` or null on error.
     */
    @JvmStatic external fun nativeGetUpdateInfo(root: String?): UpdateInfo?

    /**
     * Retrieves list of country items with its status info.
     * if `root` is `null`, list of downloaded countries is returned.
     */
    @JvmStatic external fun nativeListItems(
        root: String?,
        lat: Double,
        lon: Double,
        hasLocation: Boolean,
        myMapsMode: Boolean,
        result: List<CountryItem>?
    )

    /**
     * Sets following attributes of the given `item`:
     * <pre>
     *
     *  * name;
     *  * directParentId;
     *  * topmostParentId;
     *  * directParentName;
     *  * topmostParentName;
     *  * description;
     *  * size;
     *  * enqueuedSize;
     *  * totalSize;
     *  * childCount;
     *  * totalChildCount;
     *  * status;
     *  * errorCode;
     *  * present;
     *  * progress
     *
    </pre> *
     */
    @JvmStatic external fun nativeGetAttributes(item: CountryItem?)

    /**
     * Returns status for given `root` node.
     */
    @JvmStatic external fun nativeGetStatus(root: String?): Int

    /**
     * Returns downloading error for given `root` node.
     */
    @JvmStatic external fun nativeGetError(root: String?): Int

    /**
     * Returns localized name for given `root` node.
     */
    @JvmStatic external fun nativeGetName(root: String?): String?

    /**
     * Returns country ID corresponding to given coordinates or `null` on error.
     */
    @JvmStatic external fun nativeFindCountry(lat: Double, lon: Double): String?

    /**
     * Determines whether something is downloading now.
     */
    @JvmStatic external fun nativeIsDownloading(): Boolean

    @JvmStatic external fun nativeGetCurrentDownloadingCountryId(): String?
    /**
     * Enqueues given `root` node and its children in downloader.
     */
    @JvmStatic external fun nativeDownload(root: String?)

    /**
     * Enqueues failed items under given `root` node in downloader.
     */
    @JvmStatic external fun nativeRetry(root: String?)

    /**
     * Enqueues given `root` node with its children in downloader.
     */
    @JvmStatic external fun nativeUpdate(root: String?)

    /**
     * Removes given currently downloading `root` node and its children from downloader.
     */
    @JvmStatic external fun nativeCancel(root: String?)

    /**
     * Deletes given installed `root` node with its children.
     */
    @JvmStatic external fun nativeDelete(root: String?)

    /**
     * Registers `callback` of storage status changed. Returns slot ID which should be used to unsubscribe in [.nativeUnsubscribe].
     */
    @JvmStatic external fun nativeSubscribe(callback: StorageCallback?): Int

    /**
     * Unregisters storage status changed callback.
     * @param slot Slot ID returned from [.nativeSubscribe] while registering.
     */
    @JvmStatic external fun nativeUnsubscribe(slot: Int)

    /**
     * Sets callback about current country change. Single subscriber only.
     */
    @JvmStatic external fun nativeSubscribeOnCountryChanged(listener: CurrentCountryChangedListener?)

    /**
     * Removes callback about current country change.
     */
    @JvmStatic external fun nativeUnsubscribeOnCountryChanged()

    /**
     * Determines if there are unsaved editor changes present for given `root`.
     */
    @JvmStatic external fun nativeHasUnsavedEditorChanges(root: String?): Boolean

    /**
     * Fills given `result` list with intermediate nodes from the root node (including) to the given `root` (excluding).
     * For instance, for `root == "Florida"` the resulting list is filled with values: `{ "United States of America", "Countries" }`.
     */
    @JvmStatic external fun nativeGetPathTo(
        root: String?,
        result: List<String>?
    )

    /**
     * Calculates joint progress of downloading countries specified by `countries` array.
     * @return 0 to 100 percent.
     */
    @JvmStatic external fun nativeGetOverallProgress(countries: Array<String>?): Int

    /**
     * Returns `true` if the core will NOT do attempts to download failed maps anymore.
     */
    @JvmStatic external fun nativeIsAutoretryFailed(): Boolean

    /**
     * Returns `true` if the core is allowed to download maps while on 3g network. `false` otherwise.
     */
    @JvmStatic external fun nativeIsDownloadOn3gEnabled(): Boolean

    /**
     * Sets flag which allows to download maps on 3G.
     */
    @JvmStatic external fun nativeEnableDownloadOn3g()

    /**
     * Returns country ID which the current PP object points to, or `null`.
     */
    @JvmStatic external fun nativeGetSelectedCountry(): String?

    class StorageCallbackData(
        val countryId: String,
        val newStatus: Int,
        val errorCode: Int,
        val isLeafNode: Boolean
    )

    interface StorageCallback {
        fun onStatusChanged(data: List<StorageCallbackData>)
        fun onProgress(
            countryId: String,
            localSize: Long,
            remoteSize: Long
        )
    }

    interface CurrentCountryChangedListener {
        fun onCurrentCountryChanged(countryId: String?)
    }
}