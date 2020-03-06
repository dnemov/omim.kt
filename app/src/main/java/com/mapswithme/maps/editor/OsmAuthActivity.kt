package com.mapswithme.maps.editor

import androidx.fragment.app.Fragment
import com.mapswithme.maps.base.BaseMwmFragmentActivity

class OsmAuthActivity : BaseMwmFragmentActivity() {
    override val fragmentClass: Class<out Fragment>
        protected get() = OsmAuthFragment::class.java
}