package com.mapswithme.maps.bookmarks

import android.app.Activity
import android.view.View
import com.mapswithme.maps.widget.SearchToolbarController

class BookmarksToolbarController internal constructor(
    root: View, activity: Activity,
    private val mFragment: BookmarksListFragment
) : SearchToolbarController(root, activity) {
    override fun useExtendedToolbar(): Boolean {
        return false
    }

    override fun alwaysShowClearButton(): Boolean {
        return true
    }

    override fun onClearClick() {
        super.onClearClick()
        mFragment.deactivateSearch()
    }

    override fun onTextChanged(query: String) {
        if (hasQuery()) mFragment.runSearch(query) else mFragment.cancelSearch()
    }

}