package com.mapswithme.maps.widget.recycler

import android.view.View

interface RecyclerLongClickListener {
    fun onLongItemClick(v: View?, position: Int)
}