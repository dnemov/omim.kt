package com.mapswithme.maps.purchase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.mapswithme.maps.R
import com.mapswithme.maps.widget.DotPager
import com.mapswithme.maps.widget.ParallaxBackgroundPageListener
import com.mapswithme.maps.widget.ParallaxBackgroundViewPager
import com.mapswithme.util.UiUtils
import java.util.*

class BookmarksAllSubscriptionFragment : AbstractBookmarkSubscriptionFragment() {
    override fun createPurchaseController(): PurchaseController<PurchaseCallback> {
        return PurchaseFactory.createBookmarksAllSubscriptionController(requireContext())
    }

    override fun createFragmentDelegate(fragment: AbstractBookmarkSubscriptionFragment): SubscriptionFragmentDelegate {
        return TwoButtonsSubscriptionFragmentDelegate(fragment)
    }

    override fun onSubscriptionCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(
            R.layout.pager_fragment_bookmarks_all_subscription, container,
            false
        )
        setTopStatusBarOffset(root)
        initViewPager(root)
        return root
    }

    override val subscriptionType: SubscriptionType
        get() = SubscriptionType.BOOKMARKS_ALL

    private fun setTopStatusBarOffset(view: View) {
        val statusBarPlaceholder =
            view.findViewById<View>(R.id.status_bar_placeholder)
        val params = statusBarPlaceholder.layoutParams
        val statusBarHeight = UiUtils.getStatusBarHeight(requireContext())
        params.height = statusBarHeight
        statusBarPlaceholder.layoutParams = params
        val header = view.findViewById<View>(R.id.header)
        val headerParams = header.layoutParams as MarginLayoutParams
        headerParams.topMargin = Math.max(0, headerParams.topMargin - statusBarHeight)
        header.layoutParams = headerParams
    }

    private fun initViewPager(root: View) {
        val items =
            makeItems()
        val viewPager: ParallaxBackgroundViewPager = root.findViewById(R.id.pager)
        val adapter: PagerAdapter = ParallaxFragmentPagerAdapter(
            requireFragmentManager(),
            items
        )
        val pager =
            makeDotPager(root.findViewById(R.id.indicator), viewPager, adapter)
        pager.show()
        val listener: OnPageChangeListener = ParallaxBackgroundPageListener(
            requireActivity(),
            viewPager, items
        )
        viewPager.addOnPageChangeListener(listener)
        viewPager.startAutoScroll()
    }

    private fun makeDotPager(
        indicatorContainer: ViewGroup, viewPager: ViewPager,
        adapter: PagerAdapter
    ): DotPager {
        return DotPager.Builder(requireContext(), viewPager, adapter)
            .setIndicatorContainer(indicatorContainer)
            .setActiveDotDrawable(R.drawable.bookmarks_all_marker_active)
            .setInactiveDotDrawable(R.drawable.bookmarks_all_marker_inactive)
            .build()
    }

    private inner class ParallaxFragmentPagerAdapter internal constructor(
        fragmentManager: FragmentManager,
        private val mItems: List<Int>
    ) : FragmentPagerAdapter(fragmentManager) {
        override fun getItem(i: Int): Fragment {
            return BookmarksAllSubscriptionPageFragment.Companion.newInstance(i)
        }

        override fun getCount(): Int {
            return mItems.size
        }

    }

    companion object {
        private fun makeItems(): List<Int> {
            val items: MutableList<Int> = ArrayList()
            items.add(R.id.img3)
            items.add(R.id.img2)
            items.add(R.id.img1)
            return items
        }
    }
}