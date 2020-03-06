package com.mapswithme.maps.auth

import android.app.Activity
import android.content.Intent
import android.text.TextUtils
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.mapswithme.maps.Framework
import com.mapswithme.maps.Framework.AuthTokenType
import com.mapswithme.maps.auth.Authorizer.Callback

/**
 * An authorizer is responsible for an authorization for the Mapsme server,
 * which is known as "Passport". This process is long and consists two big parts:<br></br>
 * <br></br>
 * 1. A user authentication via social networks which results in obtaining
 * the **social auth token**.<br></br>
 * 2. A user authorization for the Mapsme Server using the obtained social token on the first step.<br></br>
 * <br></br>
 *
 * All callbacks of this authorizer may be listened through [Callback] interface. Also there is
 * a method indicating whether the authorization (see step 2) in a progress
 * or not - [.isAuthorizationInProgress].<br></br>
 * <br></br>
 *
 * **IMPORTANT NOTE**: responsibility of memory leaking (context leaking) is completely
 * on the client of this class. In common case, the careful attaching and detaching to/from instance
 * of this class in activity's/fragment's lifecycle methods, such as onResume()/onPause
 * or onStart()/onStop(), should be enough to avoid memory leaks.
 */
class Authorizer(private val mFragment: Fragment) : AuthorizationListener {
    private var mCallback: Callback? = null
    var isAuthorizationInProgress = false
        private set

    fun attach(callback: Callback) {
        mCallback = callback
    }

    fun detach() {
        mCallback = null
    }

    fun authorize() {
        if (isAuthorized) {
            if (mCallback != null) mCallback!!.onAuthorizationFinish(true)
            return
        }
        val name = SocialAuthDialogFragment::class.java.name
        var fragment = mFragment.childFragmentManager
            .findFragmentByTag(name) as DialogFragment?
        if (fragment != null) return
        fragment = Fragment.instantiate(
            mFragment.context!!,
            name
        ) as DialogFragment
        // A communication with the SocialAuthDialogFragment is implemented via getParentFragment method
// because of 'setTargetFragment' paradigm doesn't survive the activity configuration change
// due to this issue https://issuetracker.google.com/issues/36969568
        fragment!!.show(mFragment.childFragmentManager, name)
    }

    fun onTargetFragmentResult(resultCode: Int, data: Intent?) {
        if (data == null) return
        if (resultCode == Activity.RESULT_CANCELED) {
            if (mCallback == null) return
            @AuthTokenType val type = data.getIntExtra(
                Constants.EXTRA_TOKEN_TYPE,
                Framework.SOCIAL_TOKEN_INVALID
            )
            val isCancel =
                data.getBooleanExtra(Constants.EXTRA_IS_CANCEL, false)
            if (isCancel) {
                mCallback!!.onSocialAuthenticationCancel(type)
                return
            }
            mCallback!!.onSocialAuthenticationError(
                type,
                data.getStringExtra(Constants.EXTRA_AUTH_ERROR)
            )
            return
        }
        if (resultCode != Activity.RESULT_OK) return
        val socialToken =
            data.getStringExtra(Constants.EXTRA_SOCIAL_TOKEN)
        if (!TextUtils.isEmpty(socialToken)) {
            @AuthTokenType val type = data.getIntExtra(
                Constants.EXTRA_TOKEN_TYPE,
                Framework.SOCIAL_TOKEN_INVALID
            )
            val privacyAccepted = data.getBooleanExtra(
                Constants.EXTRA_PRIVACY_POLICY_ACCEPTED,
                false
            )
            val termsOfUseAccepted = data.getBooleanExtra(
                Constants.EXTRA_TERMS_OF_USE_ACCEPTED,
                false
            )
            val promoAccepted =
                data.getBooleanExtra(Constants.EXTRA_PROMO_ACCEPTED, false)
            isAuthorizationInProgress = true
            if (mCallback != null) mCallback!!.onAuthorizationStart()
            Framework.nativeAuthenticateUser(
                socialToken, type, privacyAccepted, termsOfUseAccepted,
                promoAccepted, this
            )
        }
    }

    override fun onAuthorized(success: Boolean) {
        isAuthorizationInProgress = false
        if (mCallback != null) mCallback!!.onAuthorizationFinish(success)
    }

    val isAuthorized: Boolean
        get() = Framework.nativeIsUserAuthenticated()

    interface Callback {
        fun onAuthorizationFinish(success: Boolean)
        fun onAuthorizationStart()
        fun onSocialAuthenticationCancel(@AuthTokenType type: Int)
        fun onSocialAuthenticationError(@AuthTokenType type: Int, error: String?)
    }

}