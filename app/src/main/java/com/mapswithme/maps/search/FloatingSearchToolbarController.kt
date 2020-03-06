package com.mapswithme.maps.search

import android.app.Activity
import android.text.TextUtils
import com.mapswithme.maps.api.ParsedMwmRequest.Companion.currentRequest
import com.mapswithme.maps.api.ParsedMwmRequest.Companion.hasRequest
import com.mapswithme.maps.widget.SearchToolbarController
import com.mapswithme.util.UiUtils

class FloatingSearchToolbarController(
    activity: Activity,
    private val mListener: SearchToolbarListener?
) : SearchToolbarController(activity.window.decorView, activity) {
    private var mVisibilityListener: VisibilityListener? =
        null

    interface VisibilityListener {
        fun onSearchVisibilityChanged(visible: Boolean)
    }

    override fun onUpClick() {
        mListener?.onSearchUpClick(query)
        cancelSearchApiAndHide(true)
    }

    override fun onQueryClick(query: String) {
        super.onQueryClick(query.orEmpty())
        mListener?.onSearchQueryClick(query)
        hide()
    }

    override fun onClearClick() {
        super.onClearClick()
        mListener?.onSearchClearClick()
        cancelSearchApiAndHide(false)
    }

    fun refreshToolbar() {
        showProgress(false)
        if (hasRequest()) {
            UiUtils.show(toolbar)
            if (mVisibilityListener != null) mVisibilityListener!!.onSearchVisibilityChanged(true)
            query = currentRequest!!.title.orEmpty()
        } else if (!TextUtils.isEmpty(SearchEngine.INSTANCE.query)) {
            UiUtils.show(toolbar)
            if (mVisibilityListener != null) mVisibilityListener!!.onSearchVisibilityChanged(true)
            query = SearchEngine.INSTANCE.query.orEmpty()
        } else {
            hide()
            clear()
        }
    }

    private fun cancelSearchApiAndHide(clearText: Boolean) {
        SearchEngine.INSTANCE.cancel()
        if (clearText) clear()
        hide()
    }

    fun hide(): Boolean {
        if (!UiUtils.isVisible(toolbar)) return false
        UiUtils.hide(toolbar)
        if (mVisibilityListener != null) mVisibilityListener!!.onSearchVisibilityChanged(false)
        return true
    }

    fun setVisibilityListener(visibilityListener: VisibilityListener?) {
        mVisibilityListener = visibilityListener
    }

    interface SearchToolbarListener {
        fun onSearchUpClick(query: String?)
        fun onSearchQueryClick(query: String?)
        fun onSearchClearClick()
    }

}