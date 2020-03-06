package com.mapswithme.maps.bookmarks

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.annotation.StyleRes
import androidx.fragment.app.Fragment
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseToolbarActivity
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.util.SharedPropertiesUtils
import com.mapswithme.util.ThemeUtils

class BookmarkCategoriesActivity : BaseToolbarActivity() {
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
        return ThemeUtils.getWindowBgThemeResourceId(theme)
    }

    override val fragmentClass: Class<out Fragment?>
        protected get() = BookmarkCategoriesPagerFragment::class.java

    override val contentLayoutResId: Int
        protected get() = R.layout.bookmarks_activity

    companion object {
        const val REQ_CODE_DOWNLOAD_BOOKMARK_CATEGORY = 102
        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, BookmarkCategoriesActivity::class.java))
        }

        @JvmStatic
        fun startForResult(
            context: Activity, initialPage: Int,
            catalogDeeplink: String?
        ) {
            val args = Bundle()
            args.putInt(BookmarkCategoriesPagerFragment.Companion.ARG_CATEGORIES_PAGE, initialPage)
            args.putString(
                BookmarkCategoriesPagerFragment.Companion.ARG_CATALOG_DEEPLINK,
                catalogDeeplink
            )
            val intent = Intent(context, BookmarkCategoriesActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtras(args)
            context.startActivityForResult(
                intent,
                REQ_CODE_DOWNLOAD_BOOKMARK_CATEGORY
            )
        }

        @JvmStatic
        fun startForResult(context: Activity) {
            val initialPage =
                SharedPropertiesUtils.getLastVisibleBookmarkCategoriesPage(context)
            startForResult(context, initialPage, null)
        }
    }
}