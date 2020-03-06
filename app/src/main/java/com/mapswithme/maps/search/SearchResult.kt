package com.mapswithme.maps.search

import com.mapswithme.maps.bookmarks.data.FeatureId
import com.mapswithme.maps.search.Popularity.Companion.defaultInstance

/**
 * Class instances are created from native code.
 */
class SearchResult : PopularityProvider {
    class Description(
        val featureId: FeatureId,
        val featureType: String,
        val region: String,
        val distance: String,
        val cuisine: String,
        val brand: String,
        val airportIata: String,
        val pricing: String,
        val rating: Float,
        val stars: Int,
        val openNow: Int,
        val hasPopularityHigherPriority: Boolean
    )

    val name: String
    val suggestion: String?
    val lat: Double
    val lon: Double
    val type: Int
    val description: Description?
    // Consecutive pairs of indexes (each pair contains : start index, length), specifying highlighted matches of original query in result
    val highlightRanges: IntArray?
    val isHotel: Boolean
    private val mPopularity: Popularity

    constructor(
        name: String,
        suggestion: String?,
        lat: Double,
        lon: Double,
        highlightRanges: IntArray?
    ) {
        this.name = name
        this.suggestion = suggestion
        this.lat = lat
        this.lon = lon
        isHotel = false
        description = null
        type = TYPE_SUGGEST
        this.highlightRanges = highlightRanges
        mPopularity = defaultInstance()
    }

    constructor(
        name: String,
        description: Description?,
        lat: Double,
        lon: Double,
        highlightRanges: IntArray?,
        isHotel: Boolean,
        isLocalAdsCustomer: Boolean,
        popularity: Popularity
    ) {
        type =
            if (isLocalAdsCustomer) TYPE_LOCAL_ADS_CUSTOMER else TYPE_RESULT
        this.name = name
        this.isHotel = isHotel
        mPopularity = popularity
        suggestion = null
        this.lat = lat
        this.lon = lon
        this.description = description
        this.highlightRanges = highlightRanges
    }

    override fun getPopularity(): Popularity {
        return mPopularity
    }

    companion object {
        const val TYPE_SUGGEST = 0
        const val TYPE_RESULT = 1
        const val TYPE_LOCAL_ADS_CUSTOMER = 2
        // Values should match osm::YesNoUnknown enum.
        const val OPEN_NOW_UNKNOWN = 0
        const val OPEN_NOW_YES = 1
        const val OPEN_NOW_NO = 2
        val EMPTY =
            SearchResult("", "", 0.0, 0.0, intArrayOf())
    }
}