package com.mapswithme.maps.bookmarks.data

import java.util.*

internal class CoreBookmarkCategoriesDataProvider :
    AbstractBookmarkCategoriesDataProvider() {
    override val categories: List<BookmarkCategory>
        get() {
            val categories =
                BookmarkManager.nativeGetBookmarkCategories()
            return categories.toList()
        }
}