package com.mapswithme.maps.widget.recycler

import android.view.View

interface RecyclerClickListener {
    fun onItemClick(v: View?, position: Int)
}