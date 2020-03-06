package com.mapswithme.maps.auth

import android.content.Intent
import android.os.Bundle
import androidx.annotation.CallSuper
import com.mapswithme.maps.Framework.AuthTokenType
import com.mapswithme.maps.background.Notifier
import com.mapswithme.maps.base.BaseMwmDialogFragment
import com.mapswithme.util.statistics.Statistics

class PassportAuthDialogFragment : BaseMwmDialogFragment(), TargetFragmentCallback {
    private val mAuthorizer = Authorizer(this)
    private val mAuthCallback = AuthCallback()
    private var mSavedInstanceState: Bundle? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSavedInstanceState = savedInstanceState
    }

    @CallSuper
    override fun onStart() {
        super.onStart()
        mAuthorizer.attach(mAuthCallback)
        if (mSavedInstanceState == null) mAuthorizer.authorize()
    }

    @CallSuper
    override fun onStop() {
        super.onStop()
        mAuthorizer.detach()
    }

    override fun onTargetFragmentResult(resultCode: Int, data: Intent?) {
        mAuthorizer.onTargetFragmentResult(resultCode, data)
        dismiss()
    }

    override val isTargetAdded: Boolean
        get() = isAdded

    private inner class AuthCallback : Authorizer.Callback {
        override fun onAuthorizationFinish(success: Boolean) {
            dismiss()
            if (success) {
                val notifier =
                    Notifier.from(activity!!.application)
                notifier.cancelNotification(Notifier.ID_IS_NOT_AUTHENTICATED)
            }
        }

        override fun onAuthorizationStart() {}
        override fun onSocialAuthenticationCancel(@AuthTokenType type: Int) {
            Statistics.INSTANCE.trackEvent(Statistics.EventName.UGC_AUTH_DECLINED)
        }

        override fun onSocialAuthenticationError(
            @AuthTokenType type: Int,
            error: String?
        ) {
            Statistics.INSTANCE.trackUGCAuthFailed(type, error)
        }
    }
}