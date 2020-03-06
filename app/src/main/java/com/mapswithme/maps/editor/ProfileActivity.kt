package com.mapswithme.maps.editor

import androidx.fragment.app.Fragment
import com.mapswithme.maps.base.BaseMwmFragmentActivity

class ProfileActivity : BaseMwmFragmentActivity() {
    override val fragmentClass: Class<out Fragment>
        protected get() = ProfileFragment::class.java
}