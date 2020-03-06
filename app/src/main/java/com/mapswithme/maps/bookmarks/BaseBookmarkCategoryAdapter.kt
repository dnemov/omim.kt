package com.mapswithme.maps.bookmarks

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.bookmarks.data.BookmarkCategory

abstract class BaseBookmarkCategoryAdapter<V : RecyclerView.ViewHolder?> internal constructor(
    protected val context: Context,
    var bookmarkCategories: List<BookmarkCategory>
) : RecyclerView.Adapter<V>() {
    fun setItems(items: List<BookmarkCategory>) {
        bookmarkCategories = items
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return bookmarkCategories.size
    }

    fun getCategoryByPosition(position: Int): BookmarkCategory {
        val categories = bookmarkCategories
        if (position < 0 || position > categories.size - 1) throw ArrayIndexOutOfBoundsException(
            position
        )
        return categories[position]
    }

}