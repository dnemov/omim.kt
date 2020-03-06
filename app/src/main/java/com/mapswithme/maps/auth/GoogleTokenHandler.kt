package com.mapswithme.maps.auth

import android.content.Intent
import android.text.TextUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.mapswithme.maps.Framework
import com.mapswithme.util.log.LoggerFactory

internal class GoogleTokenHandler : TokenHandler {
    override var token: String? = null
        private set

    override fun checkToken(requestCode: Int, data: Intent): Boolean {
        if (requestCode != Constants.REQ_CODE_GOOGLE_SIGN_IN) return false
        val task =
            GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account =
                task.getResult(ApiException::class.java)
            if (account != null) token = account.idToken
            return !TextUtils.isEmpty(token)
        } catch (e: ApiException) { // The ApiException status code indicates the detailed failure reason.
// Please refer to the GoogleSignInStatusCodes class reference for more information.
            val logger =
                LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
            logger.w(
                GoogleTokenHandler::class.java.simpleName,
                "signInResult:failed code=" + e.statusCode
            )
        }
        return false
    }

    override val type: Int
        get() = Framework.SOCIAL_TOKEN_GOOGLE
}