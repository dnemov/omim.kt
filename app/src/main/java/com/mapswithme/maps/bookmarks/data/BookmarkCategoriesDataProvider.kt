package com.mapswithme.maps.bookmarks.data

interface BookmarkCategoriesDataProvider {
    val categories: List<BookmarkCategory>
    fun getCategoryById(categoryId: Long): BookmarkCategory
}