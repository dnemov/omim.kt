package com.mapswithme.maps.ugc.routes

import android.app.Activity
import androidx.fragment.app.Fragment
import com.mapswithme.maps.R
import com.mapswithme.maps.bookmarks.data.BookmarkCategory

class UgcRouteSharingOptionsActivity : BaseUgcRouteActivity() {
    override val fragmentClass: Class<out Fragment>
        protected get() = UgcSharingOptionsFragment::class.java

    override val contentLayoutResId: Int
        protected get() = R.layout.fragment_container_layout

    companion object {
        const val REQ_CODE_SHARING_OPTIONS = 307
        @JvmStatic
        fun startForResult(activity: Activity, category: BookmarkCategory) {
            BaseUgcRouteActivity.Companion.startForResult(
                activity,
                category,
                UgcRouteSharingOptionsActivity::class.java,
                REQ_CODE_SHARING_OPTIONS
            )
        }
    }
}