package com.mapswithme.maps.downloader

import androidx.fragment.app.Fragment
import com.mapswithme.maps.base.BaseMwmFragmentActivity
import com.mapswithme.maps.base.OnBackPressListener

class DownloaderActivity : BaseMwmFragmentActivity() {
    override val fragmentClass: Class<out Fragment>
        protected get() = DownloaderFragment::class.java

    override fun onBackPressed() {
        val fragment =
            supportFragmentManager.findFragmentById(fragmentContentResId) as OnBackPressListener?
        if (!fragment!!.onBackPressed()) super.onBackPressed()
    }

    companion object {
        const val EXTRA_OPEN_DOWNLOADED = "open downloaded"
    }
}