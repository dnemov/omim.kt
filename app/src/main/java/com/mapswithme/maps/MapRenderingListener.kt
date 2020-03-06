package com.mapswithme.maps

internal interface MapRenderingListener {
    fun onRenderingCreated()
    fun onRenderingRestored()
    fun onRenderingInitializationFinished()
}