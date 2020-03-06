package com.mapswithme.maps.bookmarks

import androidx.annotation.CallSuper
import androidx.annotation.StyleRes
import androidx.fragment.app.Fragment
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseToolbarActivity
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.util.ThemeUtils

class BookmarkListActivity : BaseToolbarActivity() {
    @CallSuper
    public override fun onResume() {
        super.onResume()
        // Disable all notifications in BM on appearance of this activity.
// It allows to significantly improve performance in case of bookmarks
// modification. All notifications will be sent on activity's disappearance.
        BookmarkManager.INSTANCE.setNotificationsEnabled(false)
    }

    @CallSuper
    public override fun onPause() { // Allow to send all notifications in BM.
        BookmarkManager.INSTANCE.setNotificationsEnabled(true)
        super.onPause()
    }

    @StyleRes
    override fun getThemeResourceId(theme: String): Int {
        return ThemeUtils.getCardBgThemeResourceId(theme)
    }

    override val fragmentClass: Class<out Fragment>
        protected get() = BookmarksListFragment::class.java

    override val contentLayoutResId: Int
        protected get() = R.layout.bookmarks_activity
}