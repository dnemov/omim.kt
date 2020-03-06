package com.mapswithme.maps.bookmarks

import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.mapswithme.maps.Framework
import com.mapswithme.maps.Framework.AuthTokenType
import com.mapswithme.maps.R
import com.mapswithme.maps.auth.Authorizer
import com.mapswithme.maps.background.Notifier
import com.mapswithme.maps.background.Notifier.Companion.from
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.bookmarks.data.BookmarkManager.*
import com.mapswithme.maps.dialog.DialogUtils
import com.mapswithme.maps.widget.BookmarkBackupView
import com.mapswithme.util.*
import com.mapswithme.util.NetworkPolicy.NetworkPolicyListener
import com.mapswithme.util.log.LoggerFactory
import com.mapswithme.util.statistics.Statistics
import com.mapswithme.util.statistics.Statistics.EventName
import java.util.*

class BookmarkBackupController internal constructor(
    private val mContext: FragmentActivity, private val mBackupView: BookmarkBackupView,
    private val mAuthorizer: Authorizer,
    private val mAuthCompleteListener: AuthCompleteListener
) : Authorizer.Callback, BookmarksCloudListener {
    private val mSignInClickListener =
        View.OnClickListener {
            mAuthorizer.authorize()
            Statistics.INSTANCE.trackBmSyncProposalApproved(false)
        }
    private val mEnableClickListener =
        View.OnClickListener {
            INSTANCE.isCloudEnabled = true
            updateWidget()
            Statistics.INSTANCE.trackBmSyncProposalApproved(
                mAuthorizer.isAuthorized
            )
        }
    private val mRestoreClickListener =
        View.OnClickListener { v: View? ->
            requestRestoring()
            Statistics.INSTANCE.trackBmRestoreProposalClick()
        }
    private var mRestoringProgressDialog: ProgressDialog? = null
    private val mRestoreCancelListener =
        DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
            Statistics.INSTANCE.trackEvent(EventName.BM_RESTORE_PROPOSAL_CANCEL)
            INSTANCE.cancelRestoring()
        }

    private fun requestRestoring() {
        if (!ConnectionState.isConnected) {
            val clickListener =
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    Utils.showSystemSettings(mContext)
                }
            DialogUtils.showAlertDialog(
                mContext, R.string.common_check_internet_connection_dialog_title,
                R.string.common_check_internet_connection_dialog,
                R.string.settings, clickListener, R.string.ok
            )
            return
        }
        val policyListener =
            object : NetworkPolicyListener {
                override fun onResult(policy: NetworkPolicy) {
                    LOGGER.d(
                        TAG,
                        "Request bookmark restoring"
                    )
                    INSTANCE.requestRestoring()
                }
            }
        NetworkPolicy.checkNetworkPolicy(mContext.supportFragmentManager, policyListener)
    }

    private fun showRestoringProgressDialog() {
        if (mRestoringProgressDialog != null && mRestoringProgressDialog!!.isShowing) throw AssertionError(
            "Previous progress must be dismissed before " +
                    "showing another one!"
        )
        mRestoringProgressDialog = DialogUtils.createModalProgressDialog(
            mContext,
            R.string.bookmarks_restore_process,
            DialogInterface.BUTTON_NEGATIVE,
            R.string.cancel,
            mRestoreCancelListener
        )
        mRestoringProgressDialog!!.show()
    }

    private fun hideRestoringProgressDialog() {
        if (mRestoringProgressDialog == null || !mRestoringProgressDialog!!.isShowing) return
        mRestoringProgressDialog!!.dismiss()
        mRestoringProgressDialog = null
    }

    private fun updateWidget() {
        if (!mAuthorizer.isAuthorized) {
            mBackupView.setMessage(mContext.getString(R.string.bookmarks_message_unauthorized_user))
            mBackupView.setBackupButtonLabel(mContext.getString(R.string.authorization_button_sign_in))
            if (mAuthorizer.isAuthorizationInProgress) {
                mBackupView.showProgressBar()
                mBackupView.hideBackupButton()
                mBackupView.hideRestoreButton()
            } else {
                mBackupView.hideProgressBar()
                mBackupView.setBackupClickListener(mSignInClickListener)
                mBackupView.showBackupButton()
                mBackupView.hideRestoreButton()
                Statistics.INSTANCE.trackBmSyncProposalShown(
                    mAuthorizer.isAuthorized
                )
            }
            return
        }
        mBackupView.hideProgressBar()
        val isEnabled = INSTANCE.isCloudEnabled
        if (isEnabled) {
            val backupTime = INSTANCE.lastSynchronizationTimestampInMs
            val msg: String
            msg = if (backupTime > 0) {
                mContext.getString(
                    R.string.bookmarks_message_backuped_user,
                    DateUtils.getShortDateFormatter().format(
                        Date(
                            backupTime
                        )
                    )
                )
            } else {
                mContext.getString(R.string.bookmarks_message_unbackuped_user)
            }
            mBackupView.setMessage(msg)
            mBackupView.hideBackupButton()
            mBackupView.setRestoreClickListener(mRestoreClickListener)
            mBackupView.showRestoreButton()
            return
        }
        // If backup is disabled.
        mBackupView.setMessage(mContext.getString(R.string.bookmarks_message_authorized_user))
        mBackupView.setBackupButtonLabel(mContext.getString(R.string.bookmarks_backup))
        mBackupView.setBackupClickListener(mEnableClickListener)
        mBackupView.showBackupButton()
        mBackupView.hideRestoreButton()
        Statistics.INSTANCE.trackBmSyncProposalShown(mAuthorizer.isAuthorized)
    }

    fun onStart() {
        mAuthorizer.attach(this)
        INSTANCE.addCloudListener(this)
        mBackupView.expanded = SharedPropertiesUtils.backupWidgetExpanded
        updateWidget()
    }

    fun onStop() {
        mAuthorizer.detach()
        INSTANCE.removeCloudListener(this)
        SharedPropertiesUtils.backupWidgetExpanded = mBackupView.expanded
    }

    fun onTargetFragmentResult(resultCode: Int, data: Intent?) {
        mAuthorizer.onTargetFragmentResult(resultCode, data)
    }

    override fun onAuthorizationStart() {
        LOGGER.d(
            TAG,
            "onAuthorizationStart"
        )
        updateWidget()
    }

    override fun onAuthorizationFinish(success: Boolean) {
        LOGGER.d(
            TAG,
            "onAuthorizationFinish, success: $success"
        )
        if (success) {
            val notifier =
                from(mContext.application)
            notifier.cancelNotification(Notifier.ID_IS_NOT_AUTHENTICATED)
            INSTANCE.isCloudEnabled = true
            Statistics.INSTANCE.trackEvent(EventName.BM_SYNC_PROPOSAL_ENABLED)
            mAuthCompleteListener.onAuthCompleted()
        } else {
            Toast.makeText(mContext, R.string.profile_authorization_error, Toast.LENGTH_LONG).show()
            Statistics.INSTANCE.trackBmSyncProposalError(
                Framework.TOKEN_MAPSME,
                "Unknown error"
            )
        }
        updateWidget()
    }

    override fun onSocialAuthenticationError(@AuthTokenType type: Int, error: String?) {
        LOGGER.w(
            TAG,
            "onSocialAuthenticationError, type: " + Statistics.getAuthProvider(
                type
            ) +
                    " error: " + error
        )
        Statistics.INSTANCE.trackBmSyncProposalError(type, error)
    }

    override fun onSocialAuthenticationCancel(@AuthTokenType type: Int) {
        LOGGER.i(
            TAG,
            "onSocialAuthenticationCancel, type: " + Statistics.getAuthProvider(
                type
            )
        )
        Statistics.INSTANCE.trackBmSyncProposalError(type, "Cancel")
    }

    override fun onSynchronizationStarted(@SynchronizationType type: Int) {
        LOGGER.d(
            TAG,
            "onSynchronizationStarted, type: " + Statistics.getSynchronizationType(
                type
            )
        )
        when (type) {
            BookmarkManager.CLOUD_BACKUP -> Statistics.INSTANCE.trackEvent(
                EventName.BM_SYNC_STARTED
            )
            BookmarkManager.CLOUD_RESTORE -> showRestoringProgressDialog()
            else -> throw AssertionError("Unsupported synchronization type: $type")
        }
        updateWidget()
    }

    override fun onSynchronizationFinished(
        @SynchronizationType type: Int,
        @SynchronizationResult result: Int,
        errorString: String
    ) {
        LOGGER.d(
            TAG,
            "onSynchronizationFinished, type: " + Statistics.getSynchronizationType(
                type
            )
                    + ", result: " + result + ", errorString = " + errorString
        )
        Statistics.INSTANCE.trackBmSynchronizationFinish(
            type,
            result,
            errorString
        )
        hideRestoringProgressDialog()
        updateWidget()
        if (type == BookmarkManager.CLOUD_BACKUP) return
        when (result) {
            BookmarkManager.CLOUD_AUTH_ERROR, BookmarkManager.CLOUD_NETWORK_ERROR -> {
                val clickListener =
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> requestRestoring() }
                DialogUtils.showAlertDialog(
                    mContext, R.string.error_server_title,
                    R.string.error_server_message, R.string.try_again,
                    clickListener, R.string.cancel
                )
            }
            BookmarkManager.CLOUD_DISK_ERROR -> DialogUtils.showAlertDialog(
                mContext, R.string.dialog_routing_system_error,
                R.string.error_system_message
            )
            BookmarkManager.CLOUD_INVALID_CALL -> throw AssertionError("Check correctness of cloud api usage!")
            BookmarkManager.CLOUD_USER_INTERRUPTED, BookmarkManager.CLOUD_SUCCESS -> {
            }
            else -> throw AssertionError(
                "Unsupported synchronization result: " + result + "," +
                        " error message: " + errorString
            )
        }
    }

    override fun onRestoreRequested(
        @RestoringRequestResult result: Int,
        deviceName: String, backupTimestampInMs: Long
    ) {
        LOGGER.d(
            TAG,
            "onRestoreRequested, result: " + result + ", deviceName = " + deviceName +
                    ", backupTimestampInMs = " + backupTimestampInMs
        )
        Statistics.INSTANCE.trackBmRestoringRequestResult(result)
        hideRestoringProgressDialog()
        when (result) {
            BookmarkManager.CLOUD_BACKUP_EXISTS -> {
                val backupDate =
                    DateUtils.getShortDateFormatter()
                        .format(Date(backupTimestampInMs))
                val clickListener =
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                        showRestoringProgressDialog()
                        INSTANCE.applyRestoring()
                    }
                val msg =
                    mContext.getString(R.string.bookmarks_restore_message, backupDate, deviceName)
                DialogUtils.showAlertDialog(
                    mContext, R.string.bookmarks_restore_title, msg,
                    R.string.restore, clickListener, R.string.cancel, mRestoreCancelListener
                )
            }
            BookmarkManager.CLOUD_NO_BACKUP -> DialogUtils.showAlertDialog(
                mContext, R.string.bookmarks_restore_empty_title,
                R.string.bookmarks_restore_empty_message,
                R.string.ok, mRestoreCancelListener
            )
            BookmarkManager.CLOUD_NOT_ENOUGH_DISK_SPACE -> {
                val tryAgainListener =
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> INSTANCE.requestRestoring() }
                DialogUtils.showAlertDialog(
                    mContext, R.string.routing_not_enough_space,
                    R.string.not_enough_free_space_on_sdcard,
                    R.string.try_again, tryAgainListener, R.string.cancel,
                    mRestoreCancelListener
                )
            }
            else -> throw AssertionError("Unsupported restoring request result: $result")
        }
    }

    override fun onRestoredFilesPrepared() {
        LOGGER.d(
            TAG,
            "onRestoredFilesPrepared()"
        )
        if (mRestoringProgressDialog == null || !mRestoringProgressDialog!!.isShowing) return
        val cancelButton =
            mRestoringProgressDialog!!.getButton(DialogInterface.BUTTON_NEGATIVE)
                ?: throw AssertionError("Restoring progress dialog must contain cancel button!")
        cancelButton.isEnabled = false
    }

    companion object {
        private val TAG = BookmarkBackupController::class.java.simpleName
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
    }

}