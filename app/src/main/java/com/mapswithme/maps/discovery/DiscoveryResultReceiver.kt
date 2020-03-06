package com.mapswithme.maps.discovery

import androidx.annotation.MainThread
import com.mapswithme.maps.promo.PromoCityGallery
import com.mapswithme.maps.search.SearchResult

interface DiscoveryResultReceiver {
    @MainThread
    fun onHotelsReceived(results: Array<SearchResult>)

    @MainThread
    fun onAttractionsReceived(results: Array<SearchResult>)

    @MainThread
    fun onCafesReceived(results: Array<SearchResult>)

    @MainThread
    fun onLocalExpertsReceived(experts: Array<LocalExpert>)

    @MainThread
    fun onError(type: ItemType)

    @MainThread
    fun onCatalogPromoResultReceived(promoCityGallery: PromoCityGallery)

    @MainThread
    fun onNotFound()
}