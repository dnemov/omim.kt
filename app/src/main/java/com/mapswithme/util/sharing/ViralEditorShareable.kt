package com.mapswithme.util.sharing

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import com.facebook.FacebookSdk
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.widget.ShareDialog
import com.mapswithme.util.UiUtils
import com.mapswithme.util.sharing.TargetUtils.fillSmsIntent
import java.util.*

class ViralEditorShareable(context: Activity?, @DrawableRes resId: Int) :
    BaseShareable(context!!) {
    private val mUri: Uri
    override fun modifyIntent(intent: Intent?, target: SharingTarget?) {
        super.modifyIntent(intent, target)
        intent!!.putExtra(Intent.EXTRA_STREAM, mUri)
    }

    override val mimeType: String
        get() = TargetUtils.TYPE_TEXT_PLAIN

    override fun share(target: SharingTarget) {
        val intent = getTargetIntent(target)
        val lowerCaseName = target.activityName!!.toLowerCase()
        if (lowerCaseName.contains("facebook")) {
            shareFacebook()
            return
        }
        setText(mText + VIRAL_TAIL)
        if (lowerCaseName.contains("sms") || lowerCaseName.contains("mms")) fillSmsIntent(
            intent,
            mText
        ) else if (lowerCaseName.contains("twitter")) setSubject("") else if (!lowerCaseName.contains(
                "mail"
            )
        ) {
            setText(mSubject + "\n" + mText)
            setSubject("")
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        super.share(target)
    }

    private fun shareFacebook() {
        FacebookSdk.sdkInitialize(activity)
        val shareDialog = ShareDialog(activity)
        if (ShareDialog.canShow(ShareLinkContent::class.java)) {
            val linkContent: ShareLinkContent = ShareLinkContent.Builder()
                .setContentUrl(Uri.parse(FACEBOOK_SHARE_URL))
                .build()
            shareDialog.show(linkContent)
        }
    }

    companion object {
        private val FACEBOOK_SHARE_URL =
            "http://maps.me/fb-editor-v1?lang=" + Locale.getDefault().language
        private const val VIRAL_TAIL = " http://maps.me/im_get"
    }

    init {
        mUri = UiUtils.getUriToResId(context!!, resId)
    }
}