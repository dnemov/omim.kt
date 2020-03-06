package com.mapswithme.maps.widget.placepage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmFragment
import com.mapswithme.util.Utils
import com.mapswithme.util.statistics.Statistics
import com.mapswithme.util.statistics.Statistics.EventName
import com.mapswithme.util.statistics.Statistics.ParameterBuilder
import java.util.*

class PlaceDescriptionFragment : BaseMwmFragment() {
    private lateinit var mDescription: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mDescription = arguments?.getString(EXTRA_DESCRIPTION).orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root =
            inflater.inflate(R.layout.fragment_place_description, container, false)
        val webView = root.findViewById<WebView>(R.id.webview)
        webView.loadData(
            mDescription + SOURCE_SUFFIX,
            Utils.TEXT_HTML,
            Utils.UTF_8
        )
        webView.isVerticalScrollBarEnabled = true
        webView.webViewClient = PlaceDescriptionClient()
        return root
    }

    private class PlaceDescriptionClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView,
            url: String
        ): Boolean {
            val params = ParameterBuilder().add(
                Statistics.EventParam.URL,
                url
            )
            Statistics.INSTANCE.trackEvent(
                EventName.PLACEPAGE_DESCRIPTION_OUTBOUND_CLICK,
                params
            )
            return super.shouldOverrideUrlLoading(view, url)
        }
    }

    companion object {
        const val EXTRA_DESCRIPTION = "description"
        private const val SOURCE_SUFFIX = "<p><b>wikipedia.org</b></p>"
    }
}