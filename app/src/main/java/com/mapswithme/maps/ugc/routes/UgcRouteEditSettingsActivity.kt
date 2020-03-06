package com.mapswithme.maps.ugc.routes

import android.app.Activity
import androidx.fragment.app.Fragment
import com.mapswithme.maps.R
import com.mapswithme.maps.bookmarks.data.BookmarkCategory

class UgcRouteEditSettingsActivity : BaseUgcRouteActivity() {
    override val contentLayoutResId: Int
        protected get() = R.layout.fragment_container_layout

    override val fragmentContentResId: Int
        protected get() = R.id.fragment_container

    override val fragmentClass: Class<out Fragment>
        protected get() = UgcRouteEditSettingsFragment::class.java

    companion object {
        const val REQUEST_CODE = 107
        @JvmStatic
        fun startForResult(activity: Activity, category: BookmarkCategory) {
            BaseUgcRouteActivity.startForResult(
                activity,
                category,
                UgcRouteEditSettingsActivity::class.java,
                REQUEST_CODE
            )
        }
    }
}