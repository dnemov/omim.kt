package com.mapswithme.maps.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import com.mapswithme.maps.Framework
import com.mapswithme.maps.R
import com.mapswithme.util.UiUtils

class PhoneAuthFragment : BaseWebViewMwmFragment() {
    private var mWebView: WebView? = null
    private var mProgress: View? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_web_view_with_progress, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        mWebView = view.findViewById(webViewResId)
        mProgress = view.findViewById(R.id.progress)
        mWebView!!.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                UiUtils.show(mWebView!!)
                UiUtils.hide(mProgress!!)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                url: String
            ): Boolean {
                if (!TextUtils.isEmpty(url) && url.contains("$REDIRECT_URL/?code=")) {
                    val returnIntent = Intent()
                    returnIntent.putExtra(
                        Constants.EXTRA_PHONE_AUTH_TOKEN,
                        url.substring("$REDIRECT_URL/?code=".length)
                    )
                    activity!!.setResult(Activity.RESULT_OK, returnIntent)
                    activity!!.finish()
                    return true
                }
                return super.shouldOverrideUrlLoading(view, url)
            }
        }
        mWebView!!.settings.javaScriptEnabled = true
        mWebView!!.settings.userAgentString = Framework.nativeGetUserAgent()
        mWebView!!.loadUrl(Framework.nativeGetPhoneAuthUrl(REDIRECT_URL))
    }

    companion object {
        private const val REDIRECT_URL = "http://localhost"
    }
}