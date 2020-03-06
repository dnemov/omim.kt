package com.mapswithme.maps.bookmarks

import androidx.fragment.app.Fragment
import com.mapswithme.maps.R
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.bookmarks.data.BookmarkManager.BookmarksCatalogListener
import com.mapswithme.maps.bookmarks.data.BookmarkManager.DefaultBookmarksCatalogListener
import com.mapswithme.maps.dialog.DialogUtils

internal class CatalogListenerDecorator(
    private val mWrapped: BookmarksCatalogListener?,
    private val mFragment: Fragment
) : DefaultBookmarksCatalogListener() {

    constructor(fragment: Fragment) : this(null, fragment) {}

    override fun onImportStarted(serverId: String) {
        mWrapped?.onImportStarted(serverId)
    }

    override fun onImportFinished(
        serverId: String,
        catId: Long,
        successful: Boolean
    ) {
        mWrapped?.onImportFinished(serverId, catId, successful)
        if (successful) onSuccess(
            mFragment,
            catId
        ) else onError(mFragment)
    }

    companion object {
        private fun onSuccess(
            fragment: Fragment,
            catId: Long
        ) {
            val category = BookmarkManager.INSTANCE.getCategoryById(catId)
            val fm =
                fragment.activity!!.supportFragmentManager
            val frag =
                fm.findFragmentByTag(ShowOnMapCatalogCategoryFragment.TAG) as ShowOnMapCatalogCategoryFragment?
            if (frag == null) {
                ShowOnMapCatalogCategoryFragment.newInstance(category)
                    .show(fm, ShowOnMapCatalogCategoryFragment.TAG)
                fm.executePendingTransactions()
                return
            }
            frag.setCategory(category)
        }

        private fun onError(fragment: Fragment) {
            DialogUtils.showAlertDialog(
                fragment.activity!!,
                R.string.title_error_downloading_bookmarks,
                R.string.subtitle_error_downloading_guide
            )
        }
    }

}