package com.mapswithme.maps.ugc.routes

import androidx.fragment.app.Fragment
import com.mapswithme.maps.base.BaseMwmFragmentActivity

class EditCategoryNameActivity : BaseMwmFragmentActivity() {
    override val fragmentClass: Class<out Fragment>
        protected get() = EditCategoryNameFragment::class.java
}