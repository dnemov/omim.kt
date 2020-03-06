package com.mapswithme.maps.bookmarks.data

internal class CacheBookmarkCategoriesDataProvider :
    AbstractBookmarkCategoriesDataProvider() {
    override val categories: List<BookmarkCategory>
        get() = BookmarkManager.INSTANCE.bookmarkCategoriesCache.categories
}