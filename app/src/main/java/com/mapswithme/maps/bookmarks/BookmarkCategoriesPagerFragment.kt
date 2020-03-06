package com.mapswithme.maps.bookmarks

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.google.android.material.tabs.TabLayout
import com.mapswithme.maps.R
import com.mapswithme.maps.auth.TargetFragmentCallback
import com.mapswithme.maps.base.BaseMwmFragment
import com.mapswithme.maps.bookmarks.BookmarksPageFactory
import com.mapswithme.maps.dialog.AlertDialogCallback
import com.mapswithme.maps.purchase.PurchaseUtils
import com.mapswithme.util.SharedPropertiesUtils
import com.mapswithme.util.statistics.Statistics
import java.util.*

class BookmarkCategoriesPagerFragment : BaseMwmFragment(), TargetFragmentCallback,
    AlertDialogCallback, AuthCompleteListener {
    private lateinit var mAdapter: BookmarksPagerAdapter
    private var mCatalogDeeplink: String? = null
    private lateinit var mDelegate: BookmarksDownloadFragmentDelegate
    private lateinit var mInvalidSubsDialogCallback: AlertDialogCallback
    private lateinit var mViewPager: ViewPager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mDelegate = BookmarksDownloadFragmentDelegate(this)
        mDelegate.onCreate(savedInstanceState)
        val args = arguments ?: return
        mCatalogDeeplink =
            args.getString(ARG_CATALOG_DEEPLINK)
        mInvalidSubsDialogCallback = InvalidSubscriptionAlertDialogCallback(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mDelegate.onSaveInstanceState(outState)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        mDelegate.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root =
            inflater.inflate(R.layout.fragment_bookmark_categories_pager, container, false)
        mViewPager = root.findViewById(R.id.viewpager)
        val tabLayout: TabLayout = root.findViewById(R.id.sliding_tabs_layout)
        val fm = activity!!.supportFragmentManager
        val dataSet =
            adapterDataSet
        mAdapter = BookmarksPagerAdapter(context!!, fm, dataSet)
        mViewPager.adapter = mAdapter
        mViewPager.currentItem = saveAndGetInitialPage()
        tabLayout.setupWithViewPager(mViewPager)
        mViewPager.addOnPageChangeListener(PageChangeListener())
        mDelegate.onCreateView(savedInstanceState)
        return root
    }

    override fun onStart() {
        super.onStart()
        mDelegate.onStart()
        if (TextUtils.isEmpty(mCatalogDeeplink)) return
        mDelegate.downloadBookmark(mCatalogDeeplink!!)
        mCatalogDeeplink = null
    }

    override fun onResume() {
        super.onResume()
        mDelegate.onResume()
    }

    override fun onPause() {
        super.onPause()
        mDelegate.onPause()
    }

    override fun onStop() {
        super.onStop()
        mDelegate.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mDelegate.onDestroyView()
    }

    private fun saveAndGetInitialPage(): Int {
        val args = arguments
        if (args != null && args.containsKey(ARG_CATEGORIES_PAGE)) {
            val page =
                args.getInt(ARG_CATEGORIES_PAGE)
            SharedPropertiesUtils.setLastVisibleBookmarkCategoriesPage(activity!!, page)
            return page
        }
        return SharedPropertiesUtils.getLastVisibleBookmarkCategoriesPage(activity!!)
    }

    override fun onTargetFragmentResult(resultCode: Int, data: Intent?) {
        mDelegate.onTargetFragmentResult(resultCode, data)
    }

    override val isTargetAdded: Boolean
        get() = mDelegate.isTargetAdded

    override fun onAlertDialogPositiveClick(requestCode: Int, which: Int) {
        mInvalidSubsDialogCallback.onAlertDialogPositiveClick(requestCode, which)
    }

    override fun onAlertDialogNegativeClick(requestCode: Int, which: Int) {
        if (PurchaseUtils.REQ_CODE_CHECK_INVALID_SUBS_DIALOG == requestCode) mViewPager.adapter =
            mAdapter
        mInvalidSubsDialogCallback.onAlertDialogNegativeClick(requestCode, which)
    }

    override fun onAlertDialogCancel(requestCode: Int) {
        mInvalidSubsDialogCallback.onAlertDialogCancel(requestCode)
    }

    override fun onAuthCompleted() {
        mViewPager.adapter = mAdapter
    }

    private inner class PageChangeListener : SimpleOnPageChangeListener() {
        override fun onPageSelected(position: Int) {
            SharedPropertiesUtils.setLastVisibleBookmarkCategoriesPage(activity!!, position)
            val factory = mAdapter.getItemFactory(position)
            Statistics.INSTANCE.trackBookmarksTabEvent(factory.analytics.name)
        }
    }

    companion object {
        const val ARG_CATEGORIES_PAGE = "arg_categories_page"
        const val ARG_CATALOG_DEEPLINK = "arg_catalog_deeplink"
        private val adapterDataSet: List<BookmarksPageFactory>
            private get() = Arrays.asList(*BookmarksPageFactory.values())
    }
}