package com.mapswithme.maps.maplayer


import com.mapswithme.maps.content.CoreDetachable

interface MapLayerController : CoreDetachable {
    fun turnOn()
    fun turnOff()
    fun show()
    fun showImmediately()
    fun hide()
    fun hideImmediately()
    fun adjust(offsetX: Int, offsetY: Int)
}