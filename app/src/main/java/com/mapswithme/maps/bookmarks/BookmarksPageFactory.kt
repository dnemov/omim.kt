package com.mapswithme.maps.bookmarks

import android.text.TextUtils
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.mapswithme.maps.R
import com.mapswithme.util.statistics.Analytics
import com.mapswithme.util.statistics.Statistics

enum class BookmarksPageFactory(
    val analytics: Analytics,
    val resProvider: BookmarkCategoriesPageResProvider,
    private val mAdapterFooterAvailable: Boolean
) {
    PRIVATE(
        Analytics(Statistics.ParamValue.MY),
        true
    ) {
        override fun instantiateFragment(): Fragment {
            return BookmarkCategoriesFragment()
        }

        override val title: Int
            @StringRes get() = R.string.bookmarks

    },
    DOWNLOADED(
        Analytics(Statistics.ParamValue.DOWNLOADED),
        BookmarkCategoriesPageResProvider.Catalog(),
        false
    ) {
        override fun instantiateFragment(): Fragment {
            return CachedBookmarkCategoriesFragment()
        }

        override val title: Int
            @StringRes get() = R.string.guides
    };

    constructor(analytics: Analytics, hasAdapterFooter: Boolean) : this(
        analytics,
        BookmarkCategoriesPageResProvider.Default(),
        hasAdapterFooter
    )

    abstract fun instantiateFragment(): Fragment
    abstract val title: Int
    fun hasAdapterFooter(): Boolean {
        return mAdapterFooterAvailable
    }

    companion object {
        operator fun get(value: String?): BookmarksPageFactory {
            for (each in values()) {
                if (TextUtils.equals(each.name, value)) {
                    return each
                }
            }
            throw IllegalArgumentException(
                StringBuilder()
                    .append("not found enum instance for value = ")
                    .append(value)
                    .toString()
            )
        }
    }

}