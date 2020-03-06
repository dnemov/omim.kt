package com.mapswithme.maps.ugc.routes

import androidx.fragment.app.Fragment
import com.mapswithme.maps.base.BaseToolbarActivity

class UgcRouteTagsActivity : BaseToolbarActivity() {
    override val fragmentClass: Class<out Fragment>
        protected get() = UgcRouteTagsFragment::class.java

    companion object {
        const val EXTRA_TAGS = "selected_tags"
    }
}