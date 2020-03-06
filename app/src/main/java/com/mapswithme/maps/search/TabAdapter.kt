package com.mapswithme.maps.search

import android.content.Context
import android.content.res.ColorStateList
import android.util.SparseArray
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.TabLayoutOnPageChangeListener
import com.mapswithme.maps.R
import com.mapswithme.util.Graphics
import com.mapswithme.util.ThemeUtils
import java.util.*

internal class TabAdapter(
    fragmentManager: FragmentManager,
    pager: ViewPager,
    tabs: TabLayout
) : FragmentPagerAdapter(fragmentManager) {
    internal enum class Tab {
        HISTORY {
            override val titleRes: Int
                get() = R.string.history
            override val fragmentClass: Class<out Fragment>
                get() = SearchHistoryFragment::class.java
        },
        CATEGORIES {
            override val titleRes: Int
                get() = R.string.categories
            override val fragmentClass: Class<out Fragment>
                get() = SearchCategoriesFragment::class.java
        };

        abstract val titleRes: Int
        abstract val fragmentClass: Class<out Fragment>
    }

    internal interface OnTabSelectedListener {
        fun onTabSelected(tab: Tab)
    }

    private inner class PageChangedListener internal constructor(tabs: TabLayout?) :
        TabLayoutOnPageChangeListener(tabs) {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            if (mTabSelectedListener != null) mTabSelectedListener!!.onTabSelected(
                Tab.values()[position]
            )
        }
    }

    private class OnTabSelectedListenerForViewPager internal constructor(viewPager: ViewPager) :
        TabLayout.ViewPagerOnTabSelectedListener(viewPager) {
        private val mContext: Context
        override fun onTabSelected(tab: TabLayout.Tab) {
            super.onTabSelected(tab)
            Graphics.tint(mContext, tab.icon, R.attr.colorAccent)
        }

        override fun onTabUnselected(tab: TabLayout.Tab) {
            super.onTabUnselected(tab)
            Graphics.tint(mContext, tab.icon)
        }

        init {
            mContext = viewPager.context
        }
    }

    private val mPager: ViewPager
    private val mClasses: MutableList<Class<out Fragment>> =
        ArrayList()
    private val mFragments =
        SparseArray<Fragment>()
    private var mTabSelectedListener: OnTabSelectedListener? =
        null

    private fun attachTo(tabs: TabLayout) {
        val context = tabs.context
        for (tab in Tab.values()) {
            val t = tabs.newTab()
            t.setText(tab.titleRes)
            tabs.addTab(t, false)
        }
        val listener: OnPageChangeListener = PageChangedListener(tabs)
        mPager.addOnPageChangeListener(listener)
        tabs.setOnTabSelectedListener(OnTabSelectedListenerForViewPager(mPager))
        listener.onPageSelected(0)
    }

    fun setTabSelectedListener(listener: OnTabSelectedListener?) {
        mTabSelectedListener = listener
    }

    override fun getItem(position: Int): Fragment {
        var res = mFragments[position]
        if (res == null || res.javaClass != mClasses[position]) {
            try {
                res = mClasses[position].newInstance()
                mFragments.put(position, res)
            } catch (ignored: InstantiationException) {
            } catch (ignored: IllegalAccessException) {
            }
        }
        return res
    }

    override fun getCount(): Int {
        return mClasses.size
    }

    companion object {
        private fun getTabTextColor(context: Context): ColorStateList {
            return context.resources
                .getColorStateList(if (ThemeUtils.isNightTheme) R.color.accent_color_selector_night else R.color.accent_color_selector)
        }
    }

    init {
        for (tab in Tab.values()) mClasses.add(
            tab.fragmentClass
        )
        val fragments =
            fragmentManager.fragments
        if (fragments != null) { // Recollect already attached fragments
            for (f in fragments) {
                if (f == null) continue
                val idx = mClasses.indexOf(f.javaClass)
                if (idx > -1) mFragments.put(idx, f)
            }
        }
        mPager = pager
        mPager.adapter = this
        attachTo(tabs)
    }
}