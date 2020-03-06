package com.mapswithme.maps.widget

import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import java.util.*

class FragmentPageViewProvider internal constructor(
    fragManager: FragmentManager,
    pager: ViewPager
) : PageViewProvider {
    private val supportFragmentManager: FragmentManager
    private val id: Int
    override fun findViewByIndex(index: Int): View? {
        val tag = makePagerFragmentTag(index)
        val page =
            supportFragmentManager.findFragmentByTag(tag)
                ?: throw NoSuchElementException("No such element for tag  : $tag")
        return page.view
    }

    private fun makePagerFragmentTag(index: Int): String {
        return ANDROID_SWITCHER_TAG_SEGMENT + id + SEPARATOR_TAG_SEGMENT + index
    }

    companion object {
        private const val ANDROID_SWITCHER_TAG_SEGMENT = "android:switcher:"
        private const val SEPARATOR_TAG_SEGMENT = ":"
        private fun checkAdapterClass(pager: ViewPager) {
            try {
                val adapter = pager.adapter as FragmentPagerAdapter?
                    ?: throw IllegalStateException("Adapter not found")
            } catch (e: ClassCastException) {
                throw IllegalStateException("Adapter has to be FragmentPagerAdapter or its descendant")
            }
        }
    }

    init {
        checkAdapterClass(pager)
        supportFragmentManager = fragManager
        id = pager.id
    }
}