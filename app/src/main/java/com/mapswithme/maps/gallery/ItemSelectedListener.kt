package com.mapswithme.maps.gallery

interface ItemSelectedListener<I> {
    fun onItemSelected(item: I, position: Int)
    fun onMoreItemSelected(item: I)
    fun onActionButtonSelected(item: I, position: Int)
}