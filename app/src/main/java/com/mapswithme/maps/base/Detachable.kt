package com.mapswithme.maps.base

interface Detachable<T> {
    fun attach(`object`: T)
    fun detach()
}