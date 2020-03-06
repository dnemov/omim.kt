package com.mapswithme.maps.ads

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.facebook.FacebookSdk
import com.facebook.share.model.AppInviteContent
import com.facebook.share.widget.AppInviteDialog
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmDialogFragment
import com.mapswithme.maps.dialog.DialogUtils
import com.mapswithme.util.statistics.Statistics

class FacebookInvitesDialogFragment : BaseMwmDialogFragment() {
    private var mHasInvited = false
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder =
            AlertDialog.Builder(activity!!)
        val inflater = activity!!.layoutInflater
        val root = inflater.inflate(R.layout.fragment_app_invites_dialog, null)
        return builder.setView(root)
            .setNegativeButton(R.string.remind_me_later) { dialog, which ->
                Statistics.INSTANCE.trackEvent(
                    Statistics.EventName.FACEBOOK_INVITE_LATER
                )
            }
            .setPositiveButton(R.string.share) { dialog, which ->
                mHasInvited = true
                showAppInviteDialog()
                Statistics.INSTANCE.trackEvent(Statistics.EventName.FACEBOOK_INVITE_INVITED)
            }.create()
    }

    override fun onResume() {
        super.onResume()
        if (mHasInvited) dismiss()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        Statistics.INSTANCE.trackEvent(Statistics.EventName.FACEBOOK_INVITE_LATER)
    }

    private fun showAppInviteDialog() {
        FacebookSdk.sdkInitialize(activity)
        val content = AppInviteContent.Builder()
            .setApplinkUrl(INVITE_APP_URL)
            .setPreviewImageUrl(INVITE_IMAGE)
            .build()
        if (AppInviteDialog.canShow()) AppInviteDialog.show(this, content) else {
            DialogUtils.showAlertDialog(activity!!, R.string.email_error_title)
            dismiss()
        }
    }

    companion object {
        private const val INVITE_APP_URL = "https://fb.me/958251974218933"
        private const val INVITE_IMAGE = "http://maps.me/images/fb_app_invite_banner.png"
    }
}