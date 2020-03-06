package com.mapswithme.maps.ugc.routes

import androidx.fragment.app.Fragment
import com.mapswithme.maps.base.BaseMwmFragmentActivity

class SendLinkPlaceholderActivity : BaseMwmFragmentActivity() {
    override fun useTransparentStatusBar(): Boolean {
        return false
    }

    override val fragmentClass: Class<out Fragment>
        protected get() = SendLinkPlaceholderFragment::class.java
}