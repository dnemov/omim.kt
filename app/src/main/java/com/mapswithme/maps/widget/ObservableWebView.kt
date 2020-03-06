package com.mapswithme.maps.widget

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView

class ObservableWebView : WebView {
    interface Listener {
        fun onScroll(left: Int, top: Int)
        fun onContentReady()
    }

    private var mListener: Listener? = null
    private var mContentReady = false

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

    fun setListener(listener: Listener?) {
        mListener = listener
    }

    override fun onScrollChanged(
        left: Int,
        top: Int,
        oldLeft: Int,
        oldTop: Int
    ) {
        super.onScrollChanged(left, top, oldLeft, oldTop)
        if (mListener != null) mListener!!.onScroll(left, top)
    }

    override fun invalidate() {
        super.invalidate()
        if (!mContentReady && contentHeight > 0) {
            mContentReady = true
            if (mListener != null) mListener!!.onContentReady()
        }
    }
}