package com.mapswithme.maps.bookmarks.data

import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.mapswithme.maps.content.DataSource

class CategoryDataSource(override var data: BookmarkCategory) : AdapterDataObserver(),
    DataSource<BookmarkCategory> {

    override fun onChanged() {
        super.onChanged()
        val snapshot =
            BookmarkManager.INSTANCE.getCategoriesSnapshot(data.type.filterStrategy)
        val index = snapshot.indexOfOrThrow(data)
        data = snapshot.items[index]
    }

}