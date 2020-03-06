package com.mapswithme.maps.bookmarks

import android.view.View

interface OnItemLongClickListener<T> {
    fun onItemLongClick(v: View, item: T)
}