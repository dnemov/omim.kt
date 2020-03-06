package com.mapswithme.util.sharing

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object TargetUtils {
    const val TYPE_TEXT_PLAIN = "text/plain"
    const val TYPE_MESSAGE_RFC822 = "message/rfc822"
    const val EXTRA_SMS_BODY = "sms_body"
    const val EXTRA_SMS_TEXT = Intent.EXTRA_TEXT
    const val URI_STRING_SMS = "sms:"
    fun fillSmsIntent(smsIntent: Intent, body: String?) {
        smsIntent.data = Uri.parse(URI_STRING_SMS)
        smsIntent.putExtra(EXTRA_SMS_BODY, body)
    }

    fun makeAppSettingsLocationIntent(context: Context): Intent? {
        var intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        if (intent.resolveActivity(context.packageManager) != null) return intent
        intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
        return if (intent.resolveActivity(context.packageManager) == null) null else intent
    }
}