package com.mapswithme.maps.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mapswithme.maps.R
import com.mapswithme.maps.WebContainerDelegate
import com.mapswithme.maps.base.OnBackPressListener
import com.mapswithme.util.Constants

class CopyrightFragment : BaseSettingsFragment(), OnBackPressListener {
    private var mDelegate: WebContainerDelegate? = null
    protected override val layoutRes: Int
        protected get() = R.layout.fragment_web_view_with_progress

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)
        mDelegate =
            object : WebContainerDelegate(root!!, Constants.Url.COPYRIGHT) {
                override fun doStartActivity(intent: Intent?) {
                    startActivity(intent)
                }
            }
        return root
    }

    override fun onBackPressed(): Boolean {
        if (!mDelegate!!.onBackPressed()) settingsActivity?.replaceFragment(
            AboutFragment::class.java,
            getString(R.string.about_menu_title), null
        )
        return true
    }
}