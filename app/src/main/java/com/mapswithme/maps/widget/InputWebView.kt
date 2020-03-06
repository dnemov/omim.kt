package com.mapswithme.maps.widget

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView

/**
 * Workaround for not appearing soft keyboard in webview.
 * Check bugreport at https://code.google.com/p/android/issues/detail?id=7189 for more details.
 */
class InputWebView : WebView {
    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context,
        attrs
    ) {
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
    }

    override fun onCheckIsTextEditor(): Boolean {
        return true
    }
}