package com.mapswithme.maps.editor

import androidx.fragment.app.Fragment
import com.mapswithme.maps.base.BaseMwmFragmentActivity

class ReportActivity : BaseMwmFragmentActivity() {
    override val fragmentClass: Class<out Fragment>
        protected get() = ReportFragment::class.java
}