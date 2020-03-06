package com.mapswithme.maps.gdpr

import androidx.fragment.app.Fragment
import com.mapswithme.maps.base.BaseToolbarActivity

class MwmOptOutActivity : BaseToolbarActivity() {
    override val fragmentClass: Class<out Fragment>
        protected get() = OptOutFragment::class.java
}