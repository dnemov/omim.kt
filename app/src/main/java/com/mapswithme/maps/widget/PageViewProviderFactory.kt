package com.mapswithme.maps.widget

import androidx.fragment.app.FragmentActivity
import androidx.viewpager.widget.ViewPager

internal object PageViewProviderFactory {
    fun defaultProvider(
        activity: FragmentActivity,
        pager: ViewPager
    ): PageViewProvider {
        return FragmentPageViewProvider(activity.supportFragmentManager, pager)
    }
}