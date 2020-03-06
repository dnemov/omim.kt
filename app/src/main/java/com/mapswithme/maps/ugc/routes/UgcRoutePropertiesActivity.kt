package com.mapswithme.maps.ugc.routes

import androidx.fragment.app.Fragment
import com.mapswithme.maps.base.BaseToolbarActivity

class UgcRoutePropertiesActivity : BaseToolbarActivity() {
    override val fragmentClass: Class<out Fragment>
        protected get() = UgcRoutePropertiesFragment::class.java
}