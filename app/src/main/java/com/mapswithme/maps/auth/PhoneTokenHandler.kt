package com.mapswithme.maps.auth

import android.content.Intent
import android.text.TextUtils
import com.mapswithme.maps.Framework

class PhoneTokenHandler : TokenHandler {
    override var token: String? = null

    override fun checkToken(requestCode: Int, data: Intent): Boolean {
        if (requestCode != Constants.REQ_CODE_PHONE_AUTH_RESULT) return false
        token = data.getStringExtra(Constants.EXTRA_PHONE_AUTH_TOKEN)
        return !TextUtils.isEmpty(token)
    }

    override val type: Int
        get() = Framework.SOCIAL_TOKEN_PHONE
}