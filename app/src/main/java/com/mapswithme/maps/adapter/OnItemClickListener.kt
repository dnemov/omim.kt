package com.mapswithme.maps.adapter

import android.view.View

interface OnItemClickListener<T> {
    fun onItemClick(v: View, item: T)
}