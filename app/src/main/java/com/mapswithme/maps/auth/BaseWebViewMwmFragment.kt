package com.mapswithme.maps.auth

import android.view.View
import android.webkit.WebView
import androidx.annotation.IdRes
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmFragment
import com.mapswithme.util.Utils

open class BaseWebViewMwmFragment : BaseMwmFragment() {
    override fun onBackPressed(): Boolean {
        var root: View
        var webView: WebView? = null
        val goBackAllowed = view.also { root = it!! } != null && Utils.castTo<WebView>(
            root.findViewById(
                webViewResId
            )
        ).also { webView = it } != null && webView!!.canGoBack()
        if (goBackAllowed) webView!!.goBack()
        return goBackAllowed
    }

    @get:IdRes
    protected val webViewResId: Int
        protected get() = R.id.webview
}