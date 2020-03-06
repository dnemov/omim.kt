package com.mapswithme.maps.bookmarks

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

class BookmarksPagerAdapter(
    context: Context,
    fm: FragmentManager,
    factories: List<BookmarksPageFactory>
) : FragmentStatePagerAdapter(fm) {
    private val mFactories: List<BookmarksPageFactory>
    private val mContext: Context
    override fun getItem(position: Int): Fragment {
        return mFactories[position].instantiateFragment()
    }

    override fun getPageTitle(position: Int): CharSequence? {
        val titleResId = mFactories[position].title
        return mContext.resources.getString(titleResId)
    }

    override fun getCount(): Int {
        return mFactories.size
    }

    fun getItemFactory(position: Int): BookmarksPageFactory {
        return mFactories[position]
    }

    init {
        mContext = context.applicationContext
        mFactories = factories
    }
}