package com.mapswithme.maps.base

interface Savable<T> {
    fun onSave(outState: T)
    fun onRestore(inState: T)
}