package com.mapswithme.maps.ugc.routes

import android.app.Activity
import android.content.Intent
import com.mapswithme.maps.base.BaseMwmFragmentActivity
import com.mapswithme.maps.bookmarks.data.BookmarkCategory

abstract class BaseUgcRouteActivity : BaseMwmFragmentActivity() {
    companion object {
        const val EXTRA_BOOKMARK_CATEGORY = "bookmark_category"
        @JvmStatic
        protected fun <T> startForResult(
            activity: Activity,
            category: BookmarkCategory,
            targetClass: Class<T>, requestCode: Int
        ) {
            val intent = Intent(activity, targetClass)
                .putExtra(EXTRA_BOOKMARK_CATEGORY, category)
            activity.startActivityForResult(intent, requestCode)
        }
    }
}