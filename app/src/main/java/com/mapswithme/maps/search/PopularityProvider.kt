package com.mapswithme.maps.search

interface PopularityProvider {
    fun getPopularity(): Popularity?
}