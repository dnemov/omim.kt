package com.mapswithme.util.sharing

interface ShareableInfoProvider {
    val name: String
    val lat: Double
    val lon: Double
    val scale: Double
    val address: String
}