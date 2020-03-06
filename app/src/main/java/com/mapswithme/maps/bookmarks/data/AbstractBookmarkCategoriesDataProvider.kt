package com.mapswithme.maps.bookmarks.data

internal abstract class AbstractBookmarkCategoriesDataProvider :
    BookmarkCategoriesDataProvider {
    override fun getCategoryById(categoryId: Long): BookmarkCategory {
        val categories: List<BookmarkCategory> = categories
        for (each in categories) {
            if (each.id == categoryId) return each
        }
        throw IllegalArgumentException("There is no category for id : $categoryId")
    }
}