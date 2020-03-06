package com.mapswithme.maps.base

import android.content.Intent
import androidx.annotation.CallSuper
import com.mapswithme.maps.auth.Authorizer
import com.mapswithme.maps.auth.TargetFragmentCallback

abstract class BaseAuthFragment : BaseAsyncOperationFragment(),
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
        protected get() = mAuthorizer.isAuthorized
}