package com.mapswithme.maps.discovery

import androidx.annotation.StringRes
import com.mapswithme.maps.R
import com.mapswithme.maps.search.SearchResult
import com.mapswithme.util.UiUtils

enum class ItemType constructor(
    @StringRes protected val searchCategoryInternal: Int = UiUtils.NO_ID,
    val moreClickEvent: DiscoveryUserEvent = DiscoveryUserEvent.STUB,
    val itemClickEvent: DiscoveryUserEvent = DiscoveryUserEvent.STUB
) {
    ATTRACTIONS(
        R.string.tourism,
        DiscoveryUserEvent.MORE_ATTRACTIONS_CLICKED,
        DiscoveryUserEvent.ATTRACTIONS_CLICKED
    ) {
        override fun onResultReceived(
            callback: DiscoveryResultReceiver,
            results: Array<SearchResult>
        ) {
            callback.onAttractionsReceived(results)
        }

        override val searchCategory: Int
            get() = super.searchCategory
    },
    CAFES(R.string.eat, DiscoveryUserEvent.MORE_CAFES_CLICKED, DiscoveryUserEvent.CAFES_CLICKED) {
        override fun onResultReceived(
            callback: DiscoveryResultReceiver,
            results: Array<SearchResult>
        ) {
            callback.onCafesReceived(results)
        }

        override val searchCategory: Int
            get() = super.searchCategoryInternal
    },
    HOTELS(
        UiUtils.NO_ID, DiscoveryUserEvent.MORE_HOTELS_CLICKED,
        DiscoveryUserEvent.HOTELS_CLICKED
    ) {
        override fun onResultReceived(
            callback: DiscoveryResultReceiver,
            results: Array<SearchResult>
        ) {
            callback.onHotelsReceived(results)
        }

        override val searchCategory: Int
            get() = throw UnsupportedOperationException("Unsupported.")
    },
    LOCAL_EXPERTS(
        UiUtils.NO_ID, DiscoveryUserEvent.MORE_LOCALS_CLICKED,
        DiscoveryUserEvent.LOCALS_CLICKED
    ) {
        override val searchCategory: Int
            get() = throw UnsupportedOperationException("Unsupported.")
    },
    PROMO(UiUtils.NO_ID, DiscoveryUserEvent.MORE_PROMO_CLICKED, DiscoveryUserEvent.PROMO_CLICKED);

    open val searchCategory: Int
        @StringRes get() = throw UnsupportedOperationException("Unsupported by default for $name")

    open fun onResultReceived(
        callback: DiscoveryResultReceiver,
        results: Array<SearchResult>
    ) { /* Do nothing by default */
    }

}