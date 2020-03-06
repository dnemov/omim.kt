package com.mapswithme.maps.base

interface DataChangedListener<T> : Detachable<T> {
    fun onChanged()
}