package com.mapswithme.maps.base

import android.content.Intent
import androidx.annotation.CallSuper
import com.mapswithme.maps.auth.Authorizer
import com.mapswithme.maps.auth.TargetFragmentCallback

/**
 * A base toolbar fragment which is responsible for the **authorization flow**,
 * starting from the getting an auth token from a social network and passing it to the core
 * to get user authorized for the MapsMe server (Passport).
 */
abstract class BaseToolbarAuthFragment : BaseMwmToolbarFragment(),
    Authorizer.Callback, TargetFragmentCallback {
    private val mAuthorizer = Authorizer(this)
    protected fun authorize() {
        mAuthorizer.authorize()
    }

    @CallSuper
    override fun onStart() {
        super.onStart()
        mAuthorizer.attach(this)
    }

    @CallSuper
    override fun onStop() {
        super.onStop()
        mAuthorizer.detach()
    }

    override fun onTargetFragmentResult(resultCode: Int, data: Intent?) {
        mAuthorizer.onTargetFragmentResult(resultCode, data)
    }

    override val isTargetAdded: Boolean
        get() = isAdded

    protected val isAuthorized: Boolean
        get() = mAuthorizer.isAuthorized
}