package com.mapswithme.maps.auth

import android.content.Intent
import android.text.TextUtils
import com.facebook.AccessToken
import com.mapswithme.maps.Framework

internal class FacebookTokenHandler : TokenHandler {
    override fun checkToken(requestCode: Int, data: Intent): Boolean {
        val facebookToken = AccessToken.getCurrentAccessToken()
        return facebookToken != null && !TextUtils.isEmpty(facebookToken.token)
    }

    override val token: String?
        get() {
            val facebookToken = AccessToken.getCurrentAccessToken()
            return facebookToken?.token
        }

    override val type: Int
        get() = Framework.SOCIAL_TOKEN_FACEBOOK
}