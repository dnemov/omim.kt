package com.mapswithme.maps

import android.annotation.SuppressLint
import android.content.Intent
import android.net.MailTo
import android.net.Uri
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import com.mapswithme.maps.base.OnBackPressListener
import com.mapswithme.util.UiUtils

abstract class WebContainerDelegate(frame: View, url: String) :
    OnBackPressListener {
    val webView: WebView
    private val mProgress: View
    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView(url: String) {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                UiUtils.show(webView)
                UiUtils.hide(mProgress)
            }

            override fun shouldOverrideUrlLoading(
                v: WebView,
                url: String
            ): Boolean {
                if (MailTo.isMailTo(url)) {
                    val parser = MailTo.parse(url)
                    doStartActivity(
                        Intent(Intent.ACTION_SEND)
                            .putExtra(Intent.EXTRA_EMAIL, arrayOf(parser.to))
                            .putExtra(Intent.EXTRA_TEXT, parser.body)
                            .putExtra(Intent.EXTRA_SUBJECT, parser.subject)
                            .putExtra(Intent.EXTRA_CC, parser.cc)
                            .setType("message/rfc822")
                    )
                    v.reload()
                    return true
                }
                doStartActivity(
                    Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse(url))
                )
                return true
            }
        }
        webView.settings.javaScriptEnabled = true
        webView.settings.defaultTextEncodingName = "utf-8"
        webView.loadUrl(url)
    }

    override fun onBackPressed(): Boolean {
        if (!webView.canGoBack()) return false
        webView.goBack()
        return true
    }

    protected abstract fun doStartActivity(intent: Intent?)

    init {
        webView = frame.findViewById(R.id.webview)
        mProgress = frame.findViewById(R.id.progress)
        initWebView(url)
    }
}