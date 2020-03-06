package com.mapswithme.maps.bookmarks

import com.mapswithme.maps.bookmarks.data.BookmarkCategory

interface CategoryListCallback {
    fun onFooterClick()
    fun onMoreOperationClick(item: BookmarkCategory)
}