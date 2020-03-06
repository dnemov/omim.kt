package com.mapswithme.maps.bookmarks

import android.content.Intent
import android.view.View
import com.cocosw.bottomsheet.BottomSheet
import com.mapswithme.maps.R
import com.mapswithme.maps.auth.Authorizer
import com.mapswithme.maps.auth.TargetFragmentCallback
import com.mapswithme.maps.bookmarks.data.BookmarkCategory
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.widget.BookmarkBackupView
import com.mapswithme.util.UiUtils

class BookmarkCategoriesFragment : BaseBookmarkCategoriesFragment(),
    TargetFragmentCallback, AuthCompleteListener {
    private var mBackupController: BookmarkBackupController? = null
    override fun onPrepareControllers(view: View) {
        super.onPrepareControllers(view)
        val authorizer = Authorizer(this)
        val backupView: BookmarkBackupView = view.findViewById(R.id.backup)
        mBackupController = BookmarkBackupController(
            requireActivity(), backupView, authorizer,
            this
        )
    }

    override fun onStart() {
        super.onStart()
        if (mBackupController != null) mBackupController!!.onStart()
    }

    override fun updateLoadingPlaceholder() {
        super.updateLoadingPlaceholder()
        val isLoading = BookmarkManager.INSTANCE.isAsyncBookmarksLoadingInProgress
        UiUtils.showIf(!isLoading, view!!, R.id.backup, R.id.recycler)
    }

    override fun onStop() {
        super.onStop()
        if (mBackupController != null) mBackupController!!.onStop()
    }

    override fun onTargetFragmentResult(resultCode: Int, data: Intent?) {
        if (mBackupController != null) mBackupController!!.onTargetFragmentResult(resultCode, data)
    }

    override val isTargetAdded: Boolean
        get() = isAdded

    override fun prepareBottomMenuItems(bottomSheet: BottomSheet) {
        val isMultipleItems = adapter?.bookmarkCategories?.let {
            it.size > 1
        } ?: false
        BaseBookmarkCategoriesFragment.setEnableForMenuItem(
            R.id.delete,
            bottomSheet,
            isMultipleItems
        )
        BaseBookmarkCategoriesFragment.setEnableForMenuItem(
            R.id.sharing_options, bottomSheet,
            getSelectedCategory().isSharingOptionsAllowed
        )
    }

    override val type: BookmarkCategory.Type
        get() = BookmarkCategory.Type.PRIVATE

    override fun onAuthCompleted() {
        val fm =
            requireActivity().supportFragmentManager
        val pagerFragment = fm.findFragmentByTag(
            BookmarkCategoriesPagerFragment::class.java.name
        ) as BookmarkCategoriesPagerFragment?
        pagerFragment!!.onAuthCompleted()
    }
}